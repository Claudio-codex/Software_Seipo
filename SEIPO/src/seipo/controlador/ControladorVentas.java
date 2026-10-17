package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Datos;
import seipo.modelo.Datos.ProductoVenta;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ControladorVentas {

    public List<ProductoVenta> filtrarSugerencias(String texto) throws SQLException {
        List<ProductoVenta> encontrados = new ArrayList<>();
        String sql = "SELECT id, codigo, nombre, categoria, precio, stock FROM productos WHERE codigo LIKE ? OR nombre LIKE ?";
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
        String sql = "SELECT id, codigo, nombre, categoria, precio, stock FROM productos WHERE codigo = ? OR nombre = ?";
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
        String sqlUpdateStock = "UPDATE productos SET stock = stock - ? WHERE id = ?";
        String sqlMovimiento = "INSERT INTO movimientos (fecha, producto_nombre, tipo, cantidad, stock_resultante) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
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
                 PreparedStatement pstStock = con.prepareStatement(sqlUpdateStock);
                 PreparedStatement pstMov = con.prepareStatement(sqlMovimiento)) {

                for (int i = 0; i < items.size(); i++) {
                    ProductoVenta p = items.get(i);
                    int c = cantidades.get(i);
                    double importeLinea = p.precio * c;
                    int nuevoStock = p.stock - c;

                    pstDetalle.setLong(1, ventaId);
                    pstDetalle.setString(2, p.nombre);
                    pstDetalle.setInt(3, c);
                    pstDetalle.setDouble(4, importeLinea);
                    pstDetalle.executeUpdate();

                    pstStock.setInt(1, c);
                    pstStock.setInt(2, p.id);
                    pstStock.executeUpdate();

                    pstMov.setString(1, LocalDate.now().format(Datos.FECHA));
                    pstMov.setString(2, p.nombre);
                    pstMov.setString(3, "Salida");
                    pstMov.setInt(4, c);
                    pstMov.setInt(5, nuevoStock);
                    pstMov.executeUpdate();
                }
            }
            con.commit();
        }
    }
}