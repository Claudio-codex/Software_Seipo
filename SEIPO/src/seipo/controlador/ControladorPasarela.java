package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Datos;
import seipo.modelo.Sesion;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pasarela de pago simulada para los cobros con Yape o Plin. Sigue el flujo de una API real:
 * solicitud de cobro (se genera un token de pago), respuesta de autorización (aprobada o rechazada con su código)
 * y estado de la transacción. No se conecta a ningún servicio externo: la respuesta depende de los datos de prueba.
 */
public class ControladorPasarela {

    public static final String PENDIENTE = "PENDIENTE", APROBADA = "APROBADA", RECHAZADA = "RECHAZADA",
            REVERTIDA = "REVERTIDA", REEMBOLSADA = "REEMBOLSADA";
    public static final String YAPE = "Yape", PLIN = "Plin";

    /** Código de aprobación de billetera que la pasarela de prueba rechaza. */
    public static final String CODIGO_RECHAZADO = "000000";
    /** Tiempo que tarda la respuesta simulada de la pasarela. */
    static final int LATENCIA_MS = 900;
    private static final String PREFIJO_CLAVE = "sk_test_";
    private static final SecureRandom AZAR = new SecureRandom();

    public static class ConfiguracionVO {
        public String empresaNombre, empresaRuc, empresaDireccion, proveedor, comercio, clave;
        public boolean activa;
    }

    public static class TransaccionVO {
        public int id;
        public Integer ventaId;
        public String token, formaPago, medio, estado, codigoAutorizacion, mensaje, usuario;
        public double monto;
        public LocalDateTime creado, respondido;

        public boolean aprobada() { return APROBADA.equals(estado); }
    }

    // ---------- Configuración ----------

    public ConfiguracionVO leerConfiguracion() throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            return leerConfiguracion(con);
        }
    }

    static ConfiguracionVO leerConfiguracion(Connection con) throws SQLException {
        Map<String, String> v = new HashMap<>();
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT clave, valor FROM configuracion")) {
            while (rs.next()) v.put(rs.getString(1), rs.getString(2));
        }
        ConfiguracionVO c = new ConfiguracionVO();
        c.empresaNombre = v.getOrDefault("empresa_nombre", "");
        c.empresaRuc = v.getOrDefault("empresa_ruc", "");
        c.empresaDireccion = v.getOrDefault("empresa_direccion", "");
        c.activa = "1".equals(v.get("pasarela_activa"));
        c.proveedor = v.getOrDefault("pasarela_proveedor", "");
        c.comercio = v.getOrDefault("pasarela_comercio", "");
        c.clave = v.getOrDefault("pasarela_clave", "");
        return c;
    }

    public void guardarConfiguracion(ConfiguracionVO c) throws SQLException {
        if (!Sesion.esAdministrador()) throw new SQLException("Solo el administrador puede cambiar la configuración.");
        if (c.empresaNombre.isEmpty() || c.empresaDireccion.isEmpty()) {
            throw new SQLException("Ingrese el nombre y la dirección del negocio: aparecen en el comprobante.");
        }
        if (!c.empresaRuc.matches("\\d{11}")) throw new SQLException("El RUC del negocio debe tener 11 dígitos.");
        if (c.activa) validarCredenciales(c);
        String[][] valores = {{"empresa_nombre", c.empresaNombre}, {"empresa_ruc", c.empresaRuc},
                {"empresa_direccion", c.empresaDireccion}, {"pasarela_activa", c.activa ? "1" : "0"},
                {"pasarela_proveedor", c.proveedor}, {"pasarela_comercio", c.comercio}, {"pasarela_clave", c.clave}};
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(
                     "INSERT INTO configuracion (clave, valor) VALUES (?, ?) ON DUPLICATE KEY UPDATE valor = VALUES(valor)")) {
            for (String[] fila : valores) {
                pst.setString(1, fila[0]);
                pst.setString(2, fila[1]);
                pst.addBatch();
            }
            pst.executeBatch();
        }
    }

    /** Simula la verificación de credenciales con la pasarela; devuelve el mensaje de la respuesta. */
    public String probarConexion(ConfiguracionVO c) throws SQLException {
        validarCredenciales(c);
        pausa();
        return "Conexión correcta con " + c.proveedor + " (comercio " + c.comercio + ", modo prueba).";
    }

    private static void validarCredenciales(ConfiguracionVO c) throws SQLException {
        if (c.proveedor.isEmpty() || c.comercio.isEmpty()) throw new SQLException("Ingrese el proveedor y el código de comercio.");
        if (!c.clave.startsWith(PREFIJO_CLAVE) || c.clave.length() < PREFIJO_CLAVE.length() + 6) {
            throw new SQLException("La clave secreta no es válida: la de prueba empieza con " + PREFIJO_CLAVE + ".");
        }
    }

    // ---------- Cobro ----------

    /** Paso 1: envía la solicitud de cobro y devuelve la transacción PENDIENTE con su token de pago. */
    public TransaccionVO solicitarCobro(String forma, double monto) throws SQLException {
        if (!Datos.YAPE_PLIN.equals(forma)) throw new SQLException("La pasarela de pago solo atiende cobros con " + Datos.YAPE_PLIN + ".");
        if (monto <= 0) throw new SQLException("El monto a cobrar debe ser mayor que cero.");
        try (Connection con = ConexionBD.obtenerConexion()) {
            ConfiguracionVO c = leerConfiguracion(con);
            if (!c.activa) throw new SQLException("El cobro con " + Datos.YAPE_PLIN + " está desactivado. Pida al administrador que lo active en Pagos.");
            validarCredenciales(c);
            TransaccionVO t = new TransaccionVO();
            t.token = "tok_" + hex(12);
            t.formaPago = forma;
            t.monto = monto;
            t.estado = PENDIENTE;
            t.creado = LocalDateTime.now().withNano(0);
            try (PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO transacciones_pago (token, forma_pago, monto, estado, usuario_id, creado_en) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                pst.setString(1, t.token);
                pst.setString(2, forma);
                pst.setDouble(3, monto);
                pst.setString(4, PENDIENTE);
                pst.setInt(5, Sesion.getId());
                pst.setTimestamp(6, Timestamp.valueOf(t.creado));
                pst.executeUpdate();
                try (ResultSet rs = pst.getGeneratedKeys()) {
                    if (rs.next()) t.id = rs.getInt(1);
                }
            }
            return t;
        }
    }

    /** Paso 2 con Yape o Plin: el cliente da su celular y el código de aprobación que ve en su aplicación. */
    public TransaccionVO autorizarBilletera(String token, String billetera, String celular, String codigo) throws SQLException {
        String cel = soloDigitos(celular);
        String medio = billetera + " " + (cel.length() == 9 ? "••• ••• " + cel.substring(6) : cel);
        String error = validarCelular(celular);
        if (error == null) error = validarCodigo(codigo);
        String rechazo = error != null ? error : CODIGO_RECHAZADO.equals(codigo.trim()) ? "Código de aprobación incorrecto o vencido" : null;
        return responder(token, medio, rechazo);
    }

    private TransaccionVO responder(String token, String medio, String rechazo) throws SQLException {
        pausa();
        boolean aprobada = rechazo == null;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement("UPDATE transacciones_pago SET estado = ?, medio = ?, codigo_autorizacion = ?, " +
                     "mensaje = ?, respondido_en = ? WHERE token = ? AND estado = 'PENDIENTE' AND usuario_id = ?")) {
            pst.setString(1, aprobada ? APROBADA : RECHAZADA);
            pst.setString(2, medio);
            pst.setString(3, aprobada ? String.format("%06d", AZAR.nextInt(1_000_000)) : null);
            pst.setString(4, aprobada ? "Operación aprobada" : rechazo);
            pst.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now().withNano(0)));
            pst.setString(6, token);
            pst.setInt(7, Sesion.getId());
            if (pst.executeUpdate() == 0) throw new SQLException("La transacción ya fue procesada o no existe.");
        }
        return buscar("WHERE t.token = ?", token);
    }

    /** Devuelve el cobro aprobado cuya venta no se pudo registrar (por ejemplo, por falta de stock). */
    public void revertir(String token) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement("UPDATE transacciones_pago SET estado = 'REVERTIDA', " +
                     "mensaje = 'Cobro revertido: la venta no se registró' WHERE token = ? AND estado = 'APROBADA' AND venta_id IS NULL")) {
            pst.setString(1, token);
            pst.executeUpdate();
        }
    }

    /** Cobro digital de una venta (aprobado o reembolsado); null si la venta se pagó en efectivo. */
    public TransaccionVO transaccionDeVenta(int ventaId) throws SQLException {
        return buscar("WHERE t.venta_id = ?", ventaId);
    }

    /** Transacciones de un rango de fechas, de la más reciente a la más antigua (solo administrador). */
    public List<TransaccionVO> listarTransacciones(LocalDate desde, LocalDate hasta) throws SQLException {
        List<TransaccionVO> lista = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(SELECT + "WHERE t.creado_en >= ? AND t.creado_en < ? ORDER BY t.id DESC")) {
            pst.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            pst.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) lista.add(leer(rs));
            }
        }
        return lista;
    }

    private static final String SELECT = "SELECT t.*, COALESCE(NULLIF(u.nombre, ''), u.usuario) AS usuario " +
            "FROM transacciones_pago t LEFT JOIN usuarios u ON u.id = t.usuario_id ";

    private TransaccionVO buscar(String filtro, Object valor) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(SELECT + filtro)) {
            pst.setObject(1, valor);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    private static TransaccionVO leer(ResultSet rs) throws SQLException {
        TransaccionVO t = new TransaccionVO();
        t.id = rs.getInt("id");
        t.token = rs.getString("token");
        t.ventaId = (Integer) rs.getObject("venta_id");
        t.formaPago = rs.getString("forma_pago");
        t.monto = rs.getDouble("monto");
        t.estado = rs.getString("estado");
        t.medio = rs.getString("medio");
        t.codigoAutorizacion = rs.getString("codigo_autorizacion");
        t.mensaje = rs.getString("mensaje");
        t.usuario = rs.getString("usuario");
        t.creado = rs.getTimestamp("creado_en").toLocalDateTime();
        Timestamp r = rs.getTimestamp("respondido_en");
        t.respondido = r == null ? null : r.toLocalDateTime();
        return t;
    }

    // ---------- Validaciones del formulario (la vista las usa para avisar antes de enviar) ----------

    public static String validarCelular(String celular) {
        return soloDigitos(celular).matches("9\\d{8}") ? null : "El celular debe tener 9 dígitos y empezar con 9.";
    }

    public static String validarCodigo(String codigo) {
        return codigo != null && codigo.trim().matches("\\d{6}") ? null : "El código de aprobación tiene 6 dígitos.";
    }

    static String soloDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    private static String hex(int bytes) {
        byte[] b = new byte[bytes];
        AZAR.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static void pausa() {
        try {
            Thread.sleep(LATENCIA_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
