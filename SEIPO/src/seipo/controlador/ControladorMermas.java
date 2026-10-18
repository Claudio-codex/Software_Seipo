package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Datos;
import seipo.modelo.Sesion;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ControladorMermas {

    public static final String PENDIENTE = "PENDIENTE", APROBADA = "APROBADA", RECHAZADA = "RECHAZADA";
    public static final String[] CAUSALES = {"Vencido", "Dañado", "Deteriorado", "Otro"};

    /** Unidades del lote reservadas por mermas pendientes; se restan de lo que se puede vender o dar de baja. */
    public static final String RESERVADO_EN_LOTE =
            "(SELECT COALESCE(SUM(m.cantidad), 0) FROM mermas m WHERE m.lote_id = l.id AND m.estado = 'PENDIENTE')";

    public static class MermaVO {
        public int id, cantidad;
        public String codigo, producto, lote, causal, observacion, estado, solicitante, autorizador, motivoRechazo;
        public double precioUnitario;
        public LocalDate vencimiento;
        public LocalDateTime solicitadoEn, autorizadoEn;

        public double valor() { return cantidad * precioUnitario; }
    }

    /** Lote con las unidades que todavía se pueden dar de baja (existencias menos mermas pendientes). */
    public static class LoteDisponibleVO {
        public int id, disponible;
        public String codigo, producto, numeroLote;
        public LocalDate vencimiento;

        @Override
        public String toString() {
            String vence = vencimiento == null ? "sin vencimiento" : "vence " + vencimiento.format(Datos.FECHA_COMPLETA);
            return codigo + " - " + producto + " | lote " + numeroLote + " | " + vence + " | disp. " + disponible;
        }
    }

    /** Lista las mermas, opcionalmente filtradas por estado (null = todas), de la más reciente a la más antigua. */
    public List<MermaVO> listarMermas(String estado) throws SQLException {
        List<MermaVO> lista = new ArrayList<>();
        String sql = "SELECT m.id, p.codigo, p.nombre, l.numero_lote, l.fecha_vencimiento, m.cantidad, m.causal, " +
                "m.observacion, m.precio_unitario, m.estado, m.solicitado_en, m.autorizado_en, m.motivo_rechazo, " +
                "us.usuario AS solicitante, ua.usuario AS autorizador " +
                "FROM mermas m JOIN productos p ON p.id = m.producto_id JOIN lotes l ON l.id = m.lote_id " +
                "JOIN usuarios us ON us.id = m.solicitado_por LEFT JOIN usuarios ua ON ua.id = m.autorizado_por " +
                (estado == null ? "" : "WHERE m.estado = ? ") +
                "ORDER BY m.estado <> 'PENDIENTE', m.solicitado_en DESC, m.id DESC";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            if (estado != null) pst.setString(1, estado);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    MermaVO m = new MermaVO();
                    m.id = rs.getInt("id");
                    m.codigo = rs.getString("codigo");
                    m.producto = rs.getString("nombre");
                    m.lote = rs.getString("numero_lote");
                    Date venc = rs.getDate("fecha_vencimiento");
                    m.vencimiento = venc == null ? null : venc.toLocalDate();
                    m.cantidad = rs.getInt("cantidad");
                    m.causal = rs.getString("causal");
                    m.observacion = rs.getString("observacion");
                    m.precioUnitario = rs.getDouble("precio_unitario");
                    m.estado = rs.getString("estado");
                    Timestamp sol = rs.getTimestamp("solicitado_en"), aut = rs.getTimestamp("autorizado_en");
                    m.solicitadoEn = sol == null ? null : sol.toLocalDateTime();
                    m.autorizadoEn = aut == null ? null : aut.toLocalDateTime();
                    m.motivoRechazo = rs.getString("motivo_rechazo");
                    m.solicitante = rs.getString("solicitante");
                    m.autorizador = rs.getString("autorizador");
                    lista.add(m);
                }
            }
        }
        return lista;
    }

    /** Lotes que aún tienen unidades para dar de baja, en orden FEFO. */
    public List<LoteDisponibleVO> listarLotesDisponibles() throws SQLException {
        List<LoteDisponibleVO> lista = new ArrayList<>();
        String sql = "SELECT * FROM (SELECT l.id, p.codigo, p.nombre, l.numero_lote, l.fecha_vencimiento, " +
                "l.cantidad - " + RESERVADO_EN_LOTE + " AS disponible " +
                "FROM lotes l JOIN productos p ON p.id = l.producto_id WHERE l.cantidad > 0) t " +
                "WHERE disponible > 0 ORDER BY fecha_vencimiento IS NULL, fecha_vencimiento, nombre, id";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                LoteDisponibleVO l = new LoteDisponibleVO();
                l.id = rs.getInt("id");
                l.codigo = rs.getString("codigo");
                l.producto = rs.getString("nombre");
                l.numeroLote = rs.getString("numero_lote");
                Date venc = rs.getDate("fecha_vencimiento");
                l.vencimiento = venc == null ? null : venc.toLocalDate();
                l.disponible = rs.getInt("disponible");
                lista.add(l);
            }
        }
        return lista;
    }

    /** Registra una merma pendiente de autorización; valida que el lote tenga unidades sin reservar. */
    public void registrarMerma(int loteId, int cantidad, String causal, String observacion) throws SQLException {
        String sqlLote = "SELECT l.producto_id, l.cantidad - " + RESERVADO_EN_LOTE + " AS disponible, p.precio " +
                "FROM lotes l JOIN productos p ON p.id = l.producto_id WHERE l.id = ? FOR UPDATE";
        String sqlInsert = "INSERT INTO mermas (lote_id, producto_id, cantidad, causal, observacion, precio_unitario, solicitado_por) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        if (cantidad <= 0) throw new SQLException("La cantidad debe ser mayor que 0.");

        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                int productoId, disponible;
                double precio;
                try (PreparedStatement pst = con.prepareStatement(sqlLote)) {
                    pst.setInt(1, loteId);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) throw new SQLException("El lote no existe.");
                        productoId = rs.getInt("producto_id");
                        disponible = rs.getInt("disponible");
                        precio = rs.getDouble("precio");
                    }
                }
                if (cantidad > disponible) {
                    throw new SQLException("El lote solo tiene " + disponible + " unidad(es) sin mermas pendientes.");
                }
                try (PreparedStatement pst = con.prepareStatement(sqlInsert)) {
                    pst.setInt(1, loteId);
                    pst.setInt(2, productoId);
                    pst.setInt(3, cantidad);
                    pst.setString(4, causal);
                    pst.setString(5, observacion);
                    pst.setDouble(6, precio);
                    pst.setInt(7, Sesion.getId());
                    pst.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Registra como merma "Vencido" todas las unidades sin reservar de los lotes vencidos. Devuelve cuántas se crearon. */
    public int registrarVencidos() throws SQLException {
        String sql = "INSERT INTO mermas (lote_id, producto_id, cantidad, causal, observacion, precio_unitario, solicitado_por) " +
                "SELECT id, producto_id, disponible, 'Vencido', CONCAT('Vencido el ', DATE_FORMAT(fecha_vencimiento, '%d/%m/%Y')), precio, ? " +
                "FROM (SELECT l.id, l.producto_id, l.fecha_vencimiento, p.precio, l.cantidad - " + RESERVADO_EN_LOTE + " AS disponible " +
                "      FROM lotes l JOIN productos p ON p.id = l.producto_id " +
                "      WHERE l.cantidad > 0 AND l.fecha_vencimiento < CURDATE()) t " +
                "WHERE disponible > 0";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, Sesion.getId());
            return pst.executeUpdate();
        }
    }

    /** Aprueba la merma: descuenta el lote y el stock del producto y registra el movimiento, todo en una transacción. */
    public void aprobarMerma(int id) throws SQLException {
        exigirAdministrador();
        String sqlMerma = "SELECT m.lote_id, m.producto_id, m.cantidad, m.causal, m.estado, l.numero_lote, l.cantidad AS en_lote, p.nombre " +
                "FROM mermas m JOIN lotes l ON l.id = m.lote_id JOIN productos p ON p.id = m.producto_id " +
                "WHERE m.id = ? FOR UPDATE";
        String sqlLote = "UPDATE lotes SET cantidad = cantidad - ? WHERE id = ?";
        String sqlStock = "UPDATE productos SET stock = stock - ? WHERE id = ?";
        String sqlNuevoStock = "SELECT stock FROM productos WHERE id = ?";
        String sqlMovimiento = "INSERT INTO movimientos (fecha, producto_nombre, tipo, lote, cantidad, stock_resultante) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlEstado = "UPDATE mermas SET estado = 'APROBADA', autorizado_por = ?, autorizado_en = NOW() WHERE id = ?";

        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                int loteId, productoId, cantidad, enLote;
                String lote, producto;
                try (PreparedStatement pst = con.prepareStatement(sqlMerma)) {
                    pst.setInt(1, id);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) throw new SQLException("La merma no existe.");
                        if (!PENDIENTE.equals(rs.getString("estado"))) throw new SQLException("La merma ya fue procesada.");
                        loteId = rs.getInt("lote_id");
                        productoId = rs.getInt("producto_id");
                        cantidad = rs.getInt("cantidad");
                        enLote = rs.getInt("en_lote");
                        lote = rs.getString("numero_lote");
                        producto = rs.getString("nombre");
                    }
                }
                if (cantidad > enLote) {
                    throw new SQLException("El lote " + lote + " solo tiene " + enLote + " unidad(es).");
                }
                try (PreparedStatement pst = con.prepareStatement(sqlLote)) {
                    pst.setInt(1, cantidad);
                    pst.setInt(2, loteId);
                    pst.executeUpdate();
                }
                try (PreparedStatement pst = con.prepareStatement(sqlStock)) {
                    pst.setInt(1, cantidad);
                    pst.setInt(2, productoId);
                    pst.executeUpdate();
                }
                int nuevoStock;
                try (PreparedStatement pst = con.prepareStatement(sqlNuevoStock)) {
                    pst.setInt(1, productoId);
                    try (ResultSet rs = pst.executeQuery()) {
                        rs.next();
                        nuevoStock = rs.getInt(1);
                    }
                }
                try (PreparedStatement pst = con.prepareStatement(sqlMovimiento)) {
                    pst.setString(1, LocalDate.now().format(Datos.FECHA));
                    pst.setString(2, producto);
                    pst.setString(3, "Merma");
                    pst.setString(4, lote);
                    pst.setInt(5, cantidad);
                    pst.setInt(6, nuevoStock);
                    pst.executeUpdate();
                }
                try (PreparedStatement pst = con.prepareStatement(sqlEstado)) {
                    pst.setInt(1, Sesion.getId());
                    pst.setInt(2, id);
                    pst.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Rechaza la merma; las unidades reservadas vuelven a estar disponibles. */
    public void rechazarMerma(int id, String motivo) throws SQLException {
        exigirAdministrador();
        String sql = "UPDATE mermas SET estado = 'RECHAZADA', autorizado_por = ?, autorizado_en = NOW(), motivo_rechazo = ? " +
                "WHERE id = ? AND estado = 'PENDIENTE'";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, Sesion.getId());
            pst.setString(2, motivo);
            pst.setInt(3, id);
            if (pst.executeUpdate() == 0) throw new SQLException("La merma ya fue procesada.");
        }
    }

    private static void exigirAdministrador() throws SQLException {
        if (!Sesion.esAdministrador()) {
            throw new SQLException("Solo un administrador puede autorizar mermas.");
        }
    }
}
