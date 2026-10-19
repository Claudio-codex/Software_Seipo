package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Datos;
import seipo.modelo.Sesion;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Apertura y cierre de turnos de caja con el cuadre del efectivo. */
public class ControladorCaja {

    public static final String ABIERTO = "ABIERTO", CERRADO = "CERRADO";

    public static class TurnoVO {
        public int id, usuarioId, numVentas, numAnuladas;
        public String usuario, estado, observacion;
        public LocalDateTime abiertoEn, cerradoEn;
        public double montoInicial, efectivo, yapePlin;
        /** Todo lo vendido en el turno (incluye ventas antiguas con formas de pago que ya no se usan, como tarjeta). */
        public double vendido;
        /** Solo para turnos cerrados: lo que debía haber en caja y lo que se contó. */
        public Double esperado, contado;

        public double totalVendido() { return Datos.redondear(vendido); }

        /** Efectivo que debe haber en caja ahora: el inicial más las ventas en efectivo. */
        public double efectivoEnCaja() { return Datos.redondear(montoInicial + efectivo); }

        /** Sobrante (positivo) o faltante (negativo) del cierre; null si el turno sigue abierto. */
        public Double diferencia() { return contado == null || esperado == null ? null : Datos.redondear(contado - esperado); }
    }

    private static final String SELECT_TURNO =
            "SELECT t.id, t.usuario_id, COALESCE(NULLIF(u.nombre, ''), u.usuario) AS usuario, t.estado, t.abierto_en, " +
            "t.cerrado_en, t.monto_inicial, t.efectivo_esperado, t.efectivo_contado, t.observacion, " +
            "(SELECT COALESCE(SUM(v.total), 0) FROM ventas v WHERE v.turno_id = t.id AND v.estado = 'COMPLETADA' AND v.forma_pago = 'Efectivo') AS efectivo, " +
            "(SELECT COALESCE(SUM(v.total), 0) FROM ventas v WHERE v.turno_id = t.id AND v.estado = 'COMPLETADA' AND v.forma_pago = 'Yape o Plin') AS yape, " +
            "(SELECT COALESCE(SUM(v.total), 0) FROM ventas v WHERE v.turno_id = t.id AND v.estado = 'COMPLETADA') AS total_vendido, " +
            "(SELECT COUNT(*) FROM ventas v WHERE v.turno_id = t.id AND v.estado = 'COMPLETADA') AS num_ventas, " +
            "(SELECT COUNT(*) FROM ventas v WHERE v.turno_id = t.id AND v.estado = 'ANULADA') AS num_anuladas " +
            "FROM turnos t JOIN usuarios u ON u.id = t.usuario_id ";

    /** Turno abierto del usuario que inició sesión, o null si no tiene uno. */
    public TurnoVO turnoAbierto() throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(SELECT_TURNO + "WHERE t.usuario_id = ? AND t.estado = 'ABIERTO'")) {
            pst.setInt(1, Sesion.getId());
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public void abrirTurno(double montoInicial) throws SQLException {
        if (montoInicial < 0) throw new SQLException("El monto inicial no puede ser negativo.");
        if (!Datos.esMontoEnMonedas(montoInicial)) throw new SQLException("El monto inicial no puede tener monedas de 1 ni 5 céntimos.");
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement pst = con.prepareStatement(
                        "SELECT id FROM turnos WHERE usuario_id = ? AND estado = 'ABIERTO' FOR UPDATE")) {
                    pst.setInt(1, Sesion.getId());
                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) throw new SQLException("Ya tiene un turno abierto. Ciérrelo antes de abrir otro.");
                    }
                }
                try (PreparedStatement pst = con.prepareStatement(
                        "INSERT INTO turnos (usuario_id, abierto_en, monto_inicial) VALUES (?, ?, ?)")) {
                    pst.setInt(1, Sesion.getId());
                    pst.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    pst.setDouble(3, montoInicial);
                    pst.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * Cierra el turno guardando el efectivo esperado (inicial + ventas en efectivo no anuladas) y el contado.
     * Cada usuario cierra el suyo; el administrador puede cerrar el de cualquiera.
     */
    public void cerrarTurno(int turnoId, double contado, String observacion) throws SQLException {
        if (contado < 0) throw new SQLException("El efectivo contado no puede ser negativo.");
        if (!Datos.esMontoEnMonedas(contado)) throw new SQLException("El efectivo contado no puede tener monedas de 1 ni 5 céntimos.");
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                double inicial;
                try (PreparedStatement pst = con.prepareStatement(
                        "SELECT usuario_id, estado, monto_inicial FROM turnos WHERE id = ? FOR UPDATE")) {
                    pst.setInt(1, turnoId);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) throw new SQLException("El turno no existe.");
                        if (!ABIERTO.equals(rs.getString("estado"))) throw new SQLException("El turno ya está cerrado.");
                        if (rs.getInt("usuario_id") != Sesion.getId() && !Sesion.esAdministrador()) {
                            throw new SQLException("Solo el administrador puede cerrar el turno de otro usuario.");
                        }
                        inicial = rs.getDouble("monto_inicial");
                    }
                }
                double efectivo;
                try (PreparedStatement pst = con.prepareStatement("SELECT COALESCE(SUM(total), 0) FROM ventas " +
                        "WHERE turno_id = ? AND estado = 'COMPLETADA' AND forma_pago = ?")) {
                    pst.setInt(1, turnoId);
                    pst.setString(2, Datos.EFECTIVO);
                    try (ResultSet rs = pst.executeQuery()) {
                        rs.next();
                        efectivo = rs.getDouble(1);
                    }
                }
                try (PreparedStatement pst = con.prepareStatement("UPDATE turnos SET estado = 'CERRADO', cerrado_en = ?, " +
                        "efectivo_esperado = ?, efectivo_contado = ?, observacion = ? WHERE id = ?")) {
                    pst.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                    pst.setDouble(2, Datos.redondear(inicial + efectivo));
                    pst.setDouble(3, contado);
                    pst.setString(4, observacion == null || observacion.isEmpty() ? null : observacion);
                    pst.setInt(5, turnoId);
                    pst.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * Turnos abiertos en el rango de fechas, del más reciente al más antiguo. El personal solo ve los suyos;
     * los turnos abiertos aparecen siempre, aunque se hayan abierto antes del rango.
     */
    public List<TurnoVO> listarTurnos(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = SELECT_TURNO + "WHERE (DATE(t.abierto_en) BETWEEN ? AND ? OR t.estado = 'ABIERTO')" +
                (Sesion.esAdministrador() ? "" : " AND t.usuario_id = ?") + " ORDER BY t.abierto_en DESC";
        List<TurnoVO> lista = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setDate(1, java.sql.Date.valueOf(desde));
            pst.setDate(2, java.sql.Date.valueOf(hasta));
            if (!Sesion.esAdministrador()) pst.setInt(3, Sesion.getId());
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) lista.add(leer(rs));
            }
        }
        return lista;
    }

    private static TurnoVO leer(ResultSet rs) throws SQLException {
        TurnoVO t = new TurnoVO();
        t.id = rs.getInt("id");
        t.usuarioId = rs.getInt("usuario_id");
        t.usuario = rs.getString("usuario");
        t.estado = rs.getString("estado");
        t.abiertoEn = rs.getTimestamp("abierto_en").toLocalDateTime();
        Timestamp cierre = rs.getTimestamp("cerrado_en");
        t.cerradoEn = cierre == null ? null : cierre.toLocalDateTime();
        t.montoInicial = rs.getDouble("monto_inicial");
        t.esperado = decimal(rs, "efectivo_esperado");
        t.contado = decimal(rs, "efectivo_contado");
        t.observacion = rs.getString("observacion");
        t.efectivo = rs.getDouble("efectivo");
        t.vendido = rs.getDouble("total_vendido");
        t.yapePlin = rs.getDouble("yape");
        t.numVentas = rs.getInt("num_ventas");
        t.numAnuladas = rs.getInt("num_anuladas");
        return t;
    }

    /** Lee un DECIMAL que puede ser NULL. */
    static Double decimal(ResultSet rs, String columna) throws SQLException {
        java.math.BigDecimal v = rs.getBigDecimal(columna);
        return v == null ? null : v.doubleValue();
    }
}
