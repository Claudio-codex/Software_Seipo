package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Datos;
import seipo.modelo.Datos.ProductoVenta;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ControladorVentas {

    /**
     * Stock que se puede vender: lotes no vencidos menos las unidades reservadas por mermas pendientes
     * (los vencidos se dan de baja como merma).
     */
    private static final String STOCK_DISPONIBLE =
            "(SELECT COALESCE(SUM(l.cantidad - " + ControladorMermas.RESERVADO_EN_LOTE + "), 0) " +
            "FROM lotes l WHERE l.producto_id = p.id " +
            "AND (l.fecha_vencimiento IS NULL OR l.fecha_vencimiento >= CURDATE())) AS stock";

    public List<ProductoVenta> filtrarSugerencias(String texto) throws SQLException {
        List<ProductoVenta> encontrados = new ArrayList<>();
        String sql = "SELECT p.id, p.codigo, p.nombre, p.categoria, p.precio, " + STOCK_DISPONIBLE +
                " FROM productos p WHERE p.codigo LIKE ? OR p.nombre LIKE ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, "%" + texto + "%");
            pst.setString(2, "%" + texto + "%");
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    encontrados.add(new ProductoVenta(
                            rs.getInt("id"),
                            rs.getString("codigo"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getDouble("precio"),
                            rs.getInt("stock")
                    ));
                }
            }
        }
        return encontrados;
    }

    public ProductoVenta buscarProductoBD(String texto) throws SQLException {
        String sql = "SELECT p.id, p.codigo, p.nombre, p.categoria, p.precio, " + STOCK_DISPONIBLE +
                " FROM productos p WHERE p.codigo = ? OR p.nombre = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, texto);
            pst.setString(2, texto);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new ProductoVenta(
                            rs.getInt("id"),
                            rs.getString("codigo"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getDouble("precio"),
                            rs.getInt("stock")
                    );
                }
            }
        }
        return null;
    }

    public void cobrarVenta(List<ProductoVenta> items, List<Integer> cantidades, double descuentoPct, String forma) throws SQLException {
        double sub = 0;
        for (int i = 0; i < items.size(); i++) {
            sub += items.get(i).precio * cantidades.get(i);
        }
        double desc = sub * descuentoPct / 100;
        double totalVenta = (sub - desc) * (1 + Datos.IGV);

        String sqlVenta = "INSERT INTO ventas (fecha, total, forma_pago) VALUES (?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_ventas (venta_id, producto_nombre, cantidad, importe) VALUES (?, ?, ?, ?)";
        // FEFO: primero el lote que vence antes y al final los que no vencen. FOR UPDATE bloquea los lotes hasta el commit.
        // Las unidades reservadas por mermas pendientes no se venden
        String sqlLotes = "SELECT l.id, l.numero_lote, l.cantidad - " + ControladorMermas.RESERVADO_EN_LOTE + " AS cantidad " +
                "FROM lotes l WHERE l.producto_id = ? AND l.cantidad > 0 " +
                "AND (l.fecha_vencimiento IS NULL OR l.fecha_vencimiento >= CURDATE()) " +
                "ORDER BY l.fecha_vencimiento IS NULL, l.fecha_vencimiento, l.id FOR UPDATE";
        String sqlUpdateLote = "UPDATE lotes SET cantidad = cantidad - ? WHERE id = ?";
        String sqlStockActual = "SELECT stock FROM productos WHERE id = ? FOR UPDATE";
        String sqlUpdateStock = "UPDATE productos SET stock = stock - ? WHERE id = ?";
        String sqlMovimiento = "INSERT INTO movimientos (fecha, producto_nombre, tipo, lote, cantidad, stock_resultante) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                long ventaId = -1;

                try (PreparedStatement pstVenta = con.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                    pstVenta.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
                    pstVenta.setDouble(2, totalVenta);
                    pstVenta.setString(3, forma);
                    pstVenta.executeUpdate();
                    try (ResultSet rsKeys = pstVenta.getGeneratedKeys()) {
                        if (rsKeys.next()) ventaId = rsKeys.getLong(1);
                    }
                }

                try (PreparedStatement pstDetalle = con.prepareStatement(sqlDetalle);
                     PreparedStatement pstLotes = con.prepareStatement(sqlLotes);
                     PreparedStatement pstUpdLote = con.prepareStatement(sqlUpdateLote);
                     PreparedStatement pstStockActual = con.prepareStatement(sqlStockActual);
                     PreparedStatement pstStock = con.prepareStatement(sqlUpdateStock);
                     PreparedStatement pstMov = con.prepareStatement(sqlMovimiento)) {

                    for (int i = 0; i < items.size(); i++) {
                        ProductoVenta p = items.get(i);
                        int c = cantidades.get(i);
                        double importeLinea = p.precio * c;

                        pstDetalle.setLong(1, ventaId);
                        pstDetalle.setString(2, p.nombre);
                        pstDetalle.setInt(3, c);
                        pstDetalle.setDouble(4, importeLinea);
                        pstDetalle.executeUpdate();

                        int stock;
                        pstStockActual.setInt(1, p.id);
                        try (ResultSet rs = pstStockActual.executeQuery()) {
                            rs.next();
                            stock = rs.getInt(1);
                        }

                        // Descuenta lote por lote y registra una salida por cada lote usado
                        int pendiente = c;
                        pstLotes.setInt(1, p.id);
                        try (ResultSet rs = pstLotes.executeQuery()) {
                            while (pendiente > 0 && rs.next()) {
                                int enLote = rs.getInt("cantidad");
                                if (enLote <= 0) continue; // lote reservado completo por mermas pendientes
                                int tomar = Math.min(pendiente, enLote);
                                pendiente -= tomar;
                                stock -= tomar;

                                pstUpdLote.setInt(1, tomar);
                                pstUpdLote.setInt(2, rs.getInt("id"));
                                pstUpdLote.executeUpdate();

                                pstMov.setString(1, LocalDate.now().format(Datos.FECHA));
                                pstMov.setString(2, p.nombre);
                                pstMov.setString(3, "Salida");
                                pstMov.setString(4, rs.getString("numero_lote"));
                                pstMov.setInt(5, tomar);
                                pstMov.setInt(6, stock);
                                pstMov.executeUpdate();
                            }
                        }
                        if (pendiente > 0) {
                            throw new SQLException("Stock disponible insuficiente para " + p.nombre
                                    + " (faltan " + pendiente + "). Los lotes vencidos no se pueden vender.");
                        }

                        pstStock.setInt(1, c);
                        pstStock.setInt(2, p.id);
                        pstStock.executeUpdate();
                    }
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }
}
