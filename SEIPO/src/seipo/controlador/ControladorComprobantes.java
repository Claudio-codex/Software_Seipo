package seipo.controlador;

import seipo.controlador.ControladorVentas.DetalleVO;
import seipo.controlador.ControladorVentas.VentaVO;
import seipo.modelo.ConexionBD;
import seipo.modelo.Sesion;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Comprobante digital de cada venta: boleta para consumidores (DNI o sin documento) y factura para clientes con RUC.
 * Se emite en la misma transacción que la venta, con numeración correlativa por serie.
 */
public class ControladorComprobantes {

    public static final String BOLETA = "BOLETA", FACTURA = "FACTURA";
    public static final String SERIE_BOLETA = "B001", SERIE_FACTURA = "F001";
    private static final DateTimeFormatter SEGUNDOS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class ComprobanteVO {
        public String tipo, serie, emisorRuc, emisorNombre, emisorDireccion, clienteDocumento, clienteNombre, codigoVerificacion;
        public int numero;
        public LocalDateTime emitido;
        public VentaVO venta;
        public List<DetalleVO> detalle;

        public String numeroCompleto() { return serie + "-" + String.format("%08d", numero); }

        public String titulo() { return FACTURA.equals(tipo) ? "FACTURA ELECTRÓNICA" : "BOLETA DE VENTA ELECTRÓNICA"; }
    }

    /** Datos del negocio que aparecen como emisor en cada comprobante. */
    public static class EmisorVO {
        public String nombre, ruc, direccion;
    }

    // ---------- Datos del negocio ----------

    public EmisorVO leerEmisor() throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            return leerEmisor(con);
        }
    }

    static EmisorVO leerEmisor(Connection con) throws SQLException {
        Map<String, String> v = new HashMap<>();
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT clave, valor FROM configuracion")) {
            while (rs.next()) v.put(rs.getString(1), rs.getString(2));
        }
        EmisorVO e = new EmisorVO();
        e.nombre = v.getOrDefault("empresa_nombre", "");
        e.ruc = v.getOrDefault("empresa_ruc", "");
        e.direccion = v.getOrDefault("empresa_direccion", "");
        return e;
    }

    public void guardarEmisor(EmisorVO e) throws SQLException {
        if (!Sesion.esAdministrador()) throw new SQLException("Solo el administrador puede cambiar los datos del negocio.");
        if (e.nombre.isEmpty() || e.direccion.isEmpty()) {
            throw new SQLException("Ingrese el nombre y la dirección del negocio: aparecen en el comprobante.");
        }
        if (!e.ruc.matches("\\d{11}")) throw new SQLException("El RUC del negocio debe tener 11 dígitos.");
        String[][] valores = {{"empresa_nombre", e.nombre}, {"empresa_ruc", e.ruc}, {"empresa_direccion", e.direccion}};
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

    // ---------- Comprobantes ----------

    /** Factura si el cliente tiene RUC (11 dígitos); boleta en los demás casos. */
    public static String tipoPara(String documento) {
        return documento != null && documento.matches("\\d{11}") ? FACTURA : BOLETA;
    }

    /** Emite el comprobante de una venta dentro de la transacción de la venta y devuelve su número completo. */
    static String emitir(Connection con, int ventaId, Integer clienteId, double total) throws SQLException {
        String documento = null, nombre = null;
        if (clienteId != null) {
            try (PreparedStatement pst = con.prepareStatement("SELECT documento, nombre FROM clientes WHERE id = ?")) {
                pst.setInt(1, clienteId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        documento = rs.getString(1);
                        nombre = rs.getString(2);
                    }
                }
            }
        }
        String tipo = tipoPara(documento);
        String serie = FACTURA.equals(tipo) ? SERIE_FACTURA : SERIE_BOLETA;
        int numero;
        // FOR UPDATE bloquea la serie hasta el commit para que dos cajas no tomen el mismo número
        try (PreparedStatement pst = con.prepareStatement("SELECT COALESCE(MAX(numero), 0) + 1 FROM comprobantes WHERE serie = ? FOR UPDATE")) {
            pst.setString(1, serie);
            try (ResultSet rs = pst.executeQuery()) {
                rs.next();
                numero = rs.getInt(1);
            }
        }
        EmisorVO emisor = leerEmisor(con);
        LocalDateTime emitido = LocalDateTime.now().withNano(0);
        try (PreparedStatement pst = con.prepareStatement("INSERT INTO comprobantes (venta_id, tipo, serie, numero, emitido_en, " +
                "emisor_ruc, emisor_nombre, emisor_direccion, cliente_documento, cliente_nombre, codigo_verificacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            pst.setInt(1, ventaId);
            pst.setString(2, tipo);
            pst.setString(3, serie);
            pst.setInt(4, numero);
            pst.setTimestamp(5, Timestamp.valueOf(emitido));
            pst.setString(6, emisor.ruc);
            pst.setString(7, emisor.nombre);
            pst.setString(8, emisor.direccion);
            pst.setString(9, documento);
            pst.setString(10, nombre);
            pst.setString(11, codigoVerificacion(emisor.ruc, serie, numero, total, emitido));
            pst.executeUpdate();
        }
        return serie + "-" + String.format("%08d", numero);
    }

    /** Comprobante completo de una venta para mostrarlo, imprimirlo o guardarlo en PDF; null si la venta no tiene. */
    public ComprobanteVO obtener(int ventaId) throws SQLException {
        ComprobanteVO c = new ComprobanteVO();
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement("SELECT * FROM comprobantes WHERE venta_id = ?")) {
            pst.setInt(1, ventaId);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return null;
                c.tipo = rs.getString("tipo");
                c.serie = rs.getString("serie");
                c.numero = rs.getInt("numero");
                c.emitido = rs.getTimestamp("emitido_en").toLocalDateTime();
                c.emisorRuc = rs.getString("emisor_ruc");
                c.emisorNombre = rs.getString("emisor_nombre");
                c.emisorDireccion = rs.getString("emisor_direccion");
                c.clienteDocumento = rs.getString("cliente_documento");
                c.clienteNombre = rs.getString("cliente_nombre");
                c.codigoVerificacion = rs.getString("codigo_verificacion");
            }
        }
        ControladorVentas ventas = new ControladorVentas();
        c.venta = ventas.obtenerVenta(ventaId);
        c.detalle = ventas.detalleVenta(ventaId);
        return c;
    }

    private static final String[] MENORES = {"cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
            "diez", "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete", "dieciocho", "diecinueve",
            "veinte", "veintiuno", "veintidós", "veintitrés", "veinticuatro", "veinticinco", "veintiséis", "veintisiete",
            "veintiocho", "veintinueve"};
    private static final String[] DECENAS = {"", "", "", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"};
    private static final String[] CENTENAS = {"", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos",
            "seiscientos", "setecientos", "ochocientos", "novecientos"};

    /** Importe en letras, como se escribe en el comprobante: "SON: QUINCE CON 30/100 SOLES". */
    public static String enLetras(double monto) {
        long soles = (long) Math.floor(monto + 1e-6);
        int centimos = (int) Math.round((monto - soles) * 100);
        return ("son: " + letras(soles) + " con " + String.format("%02d", centimos) + "/100 soles").toUpperCase(Locale.forLanguageTag("es-PE"));
    }

    private static String letras(long n) {
        if (n >= 1_000_000) {
            long m = n / 1_000_000, r = n % 1_000_000;
            return (m == 1 ? "un millón" : apocope(letras(m)) + " millones") + (r > 0 ? " " + letras(r) : "");
        }
        if (n >= 1000) {
            long m = n / 1000, r = n % 1000;
            return (m == 1 ? "mil" : apocope(letras(m)) + " mil") + (r > 0 ? " " + letras(r) : "");
        }
        if (n >= 100) {
            long r = n % 100;
            return n == 100 ? "cien" : CENTENAS[(int) (n / 100)] + (r > 0 ? " " + letras(r) : "");
        }
        if (n < 30) return MENORES[(int) n];
        long r = n % 10;
        return DECENAS[(int) (n / 10)] + (r > 0 ? " y " + MENORES[(int) r] : "");
    }

    /** "veintiuno mil" se dice "veintiún mil" y "treinta y uno mil", "treinta y un mil". */
    private static String apocope(String s) {
        if (s.endsWith("veintiuno")) return s.substring(0, s.length() - 3) + "ún";
        return s.endsWith("uno") ? s.substring(0, s.length() - 1) : s;
    }

    /**
     * Código que identifica el comprobante: resumen SHA-256 del emisor, el número, el total y la fecha de emisión.
     * Si se altera alguno de esos datos, el código ya no coincide.
     */
    public static String codigoVerificacion(String ruc, String serie, int numero, double total, LocalDateTime emitido) {
        String datos = ruc + "|" + serie + "-" + numero + "|" + String.format(Locale.US, "%.2f", total) + "|" + emitido.format(SEGUNDOS);
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(datos.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                if (i > 0 && i % 2 == 0) sb.append('-');
                sb.append(String.format("%02X", h[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
