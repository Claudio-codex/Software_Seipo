package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Datos;
import seipo.modelo.Datos.ProductoVenta;
import seipo.modelo.Sesion;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ControladorInventario {

    public static class ResumenInventario {
        public int totalStock;
        public int bajoStock;
        public int agotados;
        public int lotesVencidos;
        public int lotesPorVencer;

        public ResumenInventario(int totalStock, int bajoStock, int agotados, int lotesVencidos, int lotesPorVencer) {
            this.totalStock = totalStock;
            this.bajoStock = bajoStock;
            this.agotados = agotados;
            this.lotesVencidos = lotesVencidos;
            this.lotesPorVencer = lotesPorVencer;
        }
    }

    public static class MovimientoVO {
        public String fecha, producto, tipo, lote;
        public int cantidad, stock;

        public MovimientoVO(String fecha, String producto, String tipo, String lote, int cantidad, int stock) {
            this.fecha = fecha;
            this.producto = producto;
            this.tipo = tipo;
            this.lote = lote;
            this.cantidad = cantidad;
            this.stock = stock;
        }
    }

    public static class LoteVO {
        public int id;
        public String codigo, producto, numeroLote, ubicacion;
        public LocalDate vencimiento; // null si el producto no vence
        public int cantidad;

        public LoteVO(int id, String codigo, String producto, String numeroLote, LocalDate vencimiento, int cantidad, String ubicacion) {
            this.id = id;
            this.codigo = codigo;
            this.producto = producto;
            this.numeroLote = numeroLote;
            this.vencimiento = vencimiento;
            this.cantidad = cantidad;
            this.ubicacion = ubicacion;
        }

        /** Días que faltan para el vencimiento (negativo si ya venció); null si no vence. */
        public Long diasParaVencer() {
            return vencimiento == null ? null : ChronoUnit.DAYS.between(LocalDate.now(), vencimiento);
        }

        public String estado() {
            Long dias = diasParaVencer();
            if (dias == null) return "Sin vencimiento";
            if (dias < 0) return "Vencido";
            if (dias <= Datos.DIAS_POR_VENCER) return "Por vencer";
            return "Vigente";
        }
    }

    /** Línea de una recepción de mercadería. */
    public static class LineaRecepcion {
        public ProductoVenta producto;
        public String numeroLote, ubicacion;
        public LocalDate vencimiento;
        public int cantidad;

        public LineaRecepcion(ProductoVenta producto, String numeroLote, LocalDate vencimiento, int cantidad, String ubicacion) {
            this.producto = producto;
            this.numeroLote = numeroLote;
            this.vencimiento = vencimiento;
            this.cantidad = cantidad;
            this.ubicacion = ubicacion;
        }
    }

    public ResumenInventario obtenerResumen() throws SQLException {
        int sumaStockTotal = 0, bajo = 0, agotados = 0, vencidos = 0, porVencer = 0;
        String sqlProductos = "SELECT stock FROM productos";
        String sqlLotes = "SELECT " +
                "SUM(fecha_vencimiento < CURDATE()), " +
                "SUM(fecha_vencimiento BETWEEN CURDATE() AND CURDATE() + INTERVAL ? DAY) " +
                "FROM lotes WHERE cantidad > 0";
        try (Connection con = ConexionBD.obtenerConexion()) {
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sqlProductos)) {
                while (rs.next()) {
                    int stock = rs.getInt("stock");
                    sumaStockTotal += stock;
                    if (stock == 0) agotados++;
                    else if (stock <= Datos.STOCK_BAJO) bajo++;
                }
            }
            try (PreparedStatement pst = con.prepareStatement(sqlLotes)) {
                pst.setInt(1, Datos.DIAS_POR_VENCER);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        vencidos = rs.getInt(1);
                        porVencer = rs.getInt(2);
                    }
                }
            }
        }
        return new ResumenInventario(sumaStockTotal, bajo, agotados, vencidos, porVencer);
    }

    public List<MovimientoVO> obtenerMovimientosRecientes() throws SQLException {
        List<MovimientoVO> lista = new ArrayList<>();
        String sqlMovimientos = "SELECT fecha, producto_nombre, tipo, lote, cantidad, stock_resultante FROM movimientos ORDER BY id DESC LIMIT 15";
        try (Connection con = ConexionBD.obtenerConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sqlMovimientos)) {
            while (rs.next()) {
                lista.add(new MovimientoVO(
                        rs.getString("fecha"),
                        rs.getString("producto_nombre"),
                        rs.getString("tipo"),
                        rs.getString("lote"),
                        rs.getInt("cantidad"),
                        rs.getInt("stock_resultante")
                ));
            }
        }
        return lista;
    }

    /** Lotes con existencias, en orden FEFO (primero el que vence antes; los que no vencen al final). */
    public List<LoteVO> listarLotes(String textoBusqueda) throws SQLException {
        List<LoteVO> lista = new ArrayList<>();
        String sql = "SELECT l.id, p.codigo, p.nombre, l.numero_lote, l.fecha_vencimiento, l.cantidad, l.ubicacion " +
                "FROM lotes l JOIN productos p ON p.id = l.producto_id " +
                "WHERE l.cantidad > 0 AND (p.codigo LIKE ? OR p.nombre LIKE ? OR l.numero_lote LIKE ?) " +
                "ORDER BY l.fecha_vencimiento IS NULL, l.fecha_vencimiento, p.nombre, l.id";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            String filtro = "%" + textoBusqueda + "%";
            pst.setString(1, filtro);
            pst.setString(2, filtro);
            pst.setString(3, filtro);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Date venc = rs.getDate("fecha_vencimiento");
                    lista.add(new LoteVO(rs.getInt("id"), rs.getString("codigo"), rs.getString("nombre"),
                            rs.getString("numero_lote"), venc == null ? null : venc.toLocalDate(),
                            rs.getInt("cantidad"), rs.getString("ubicacion")));
                }
            }
        }
        return lista;
    }

    /** Corrige la fecha de vencimiento y la ubicación de un lote (solo administradores). */
    public void actualizarLote(int id, LocalDate vencimiento, String ubicacion) throws SQLException {
        if (!Sesion.esAdministrador()) {
            throw new SQLException("Solo un administrador puede modificar los lotes.");
        }
        String sql = "UPDATE lotes SET fecha_vencimiento = ?, ubicacion = ? WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setDate(1, vencimiento == null ? null : Date.valueOf(vencimiento));
            pst.setString(2, ubicacion);
            pst.setInt(3, id);
            pst.executeUpdate();
        }
    }

    /** Busca un producto por su código exacto (lo que envía la lectora de códigos de barras). */
    public ProductoVenta buscarPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT id, codigo, nombre, categoria, precio, stock FROM productos WHERE codigo = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, codigo);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new ProductoVenta(rs.getInt("id"), rs.getString("codigo"), rs.getString("nombre"),
                            rs.getString("categoria"), rs.getDouble("precio"), rs.getInt("stock"));
                }
            }
        }
        return null;
    }

    /**
     * Registra en una sola transacción la recepción de mercadería: la cabecera con la guía de
     * remisión, un lote por línea, el aumento de stock y el movimiento de ingreso.
     */
    public void registrarRecepcion(String guia, String proveedor, List<LineaRecepcion> lineas) throws SQLException {
        String sqlGuia = "SELECT COUNT(*) FROM recepciones WHERE guia_remision = ? AND proveedor = ?";
        String sqlRecepcion = "INSERT INTO recepciones (fecha, guia_remision, proveedor, usuario_id) VALUES (?, ?, ?, ?)";
        String sqlLote = "INSERT INTO lotes (producto_id, recepcion_id, numero_lote, fecha_vencimiento, cantidad_inicial, cantidad, ubicacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlStock = "UPDATE productos SET stock = stock + ? WHERE id = ?";
        String sqlNuevoStock = "SELECT stock FROM productos WHERE id = ?";
        String sqlMovimiento = "INSERT INTO movimientos (fecha, producto_nombre, tipo, lote, cantidad, stock_resultante) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement pst = con.prepareStatement(sqlGuia)) {
                    pst.setString(1, guia);
                    pst.setString(2, proveedor);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new SQLException("La guía " + guia + " de " + proveedor + " ya fue registrada.");
                        }
                    }
                }

                long recepcionId;
                try (PreparedStatement pst = con.prepareStatement(sqlRecepcion, Statement.RETURN_GENERATED_KEYS)) {
                    pst.setDate(1, Date.valueOf(LocalDate.now()));
                    pst.setString(2, guia);
                    pst.setString(3, proveedor);
                    pst.setInt(4, Sesion.getId());
                    pst.executeUpdate();
                    try (ResultSet rs = pst.getGeneratedKeys()) {
                        rs.next();
                        recepcionId = rs.getLong(1);
                    }
                }

                try (PreparedStatement pstLote = con.prepareStatement(sqlLote);
                     PreparedStatement pstStock = con.prepareStatement(sqlStock);
                     PreparedStatement pstNuevo = con.prepareStatement(sqlNuevoStock);
                     PreparedStatement pstMov = con.prepareStatement(sqlMovimiento)) {
                    for (LineaRecepcion l : lineas) {
                        pstLote.setInt(1, l.producto.id);
                        pstLote.setLong(2, recepcionId);
                        pstLote.setString(3, l.numeroLote);
                        pstLote.setDate(4, l.vencimiento == null ? null : Date.valueOf(l.vencimiento));
                        pstLote.setInt(5, l.cantidad);
                        pstLote.setInt(6, l.cantidad);
                        pstLote.setString(7, l.ubicacion);
                        pstLote.executeUpdate();

                        pstStock.setInt(1, l.cantidad);
                        pstStock.setInt(2, l.producto.id);
                        pstStock.executeUpdate();

                        int nuevoStock;
                        pstNuevo.setInt(1, l.producto.id);
                        try (ResultSet rs = pstNuevo.executeQuery()) {
                            rs.next();
                            nuevoStock = rs.getInt(1);
                        }

                        pstMov.setString(1, LocalDate.now().format(Datos.FECHA));
                        pstMov.setString(2, l.producto.nombre);
                        pstMov.setString(3, "Ingreso");
                        pstMov.setString(4, l.numeroLote);
                        pstMov.setInt(5, l.cantidad);
                        pstMov.setInt(6, nuevoStock);
                        pstMov.executeUpdate();
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
