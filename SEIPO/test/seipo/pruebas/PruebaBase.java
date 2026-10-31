package seipo.pruebas;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import seipo.controlador.ControladorInventario;
import seipo.controlador.ControladorLogin;
import seipo.controlador.ControladorVentas;
import seipo.modelo.ConexionBD;
import seipo.modelo.Datos.ProductoVenta;
import seipo.modelo.Sesion;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base de las pruebas de caja negra. Las pruebas se conectan a la base seipo_test (nunca a seipo_db):
 * antes de la primera prueba se crea desde SEIPO/bd/seipo_db.sql y después de cada prueba se vuelve a cargar,
 * así cada caso empieza con los mismos datos y la base queda como estaba.
 */
public abstract class PruebaBase {

    static final Path RAIZ = buscarRaiz();
    static final Path CONFIG = RAIZ.resolve("SEIPO/test/config.pruebas.properties");
    static final Path VOLCADO = RAIZ.resolve("SEIPO/bd/seipo_db.sql");
    static final String BASE_PRUEBAS = "seipo_test";

    private static boolean baseCreada;

    static {
        // ConexionBD lee la configuración una sola vez: se fija antes de cualquier conexión
        System.setProperty("seipo.config", CONFIG.toString());
    }

    @BeforeAll
    static void prepararBase() throws Exception {
        if (!baseCreada) {
            recargarBase();
            baseCreada = true;
        }
    }

    @AfterEach
    void restaurarBase() throws Exception {
        Sesion.cerrar();
        recargarBase();
    }

    // ---------------------------------------------------------------- Base de datos de pruebas

    /** Carga el volcado en seipo_test con el cliente de MariaDB/MySQL (reemplaza la base completa). */
    static void recargarBase() throws IOException, InterruptedException {
        Properties config = leerConfig();
        if (!BASE_PRUEBAS.equals(config.getProperty("db.base"))) {
            throw new IllegalStateException("La configuración de pruebas debe usar la base " + BASE_PRUEBAS + ", nunca la de producción.");
        }
        String sql = new String(Files.readAllBytes(VOLCADO), StandardCharsets.UTF_8).replace("`seipo_db`", "`" + BASE_PRUEBAS + "`");
        ProcessBuilder pb = new ProcessBuilder(buscarCliente(config), "--host=" + config.getProperty("db.host", "localhost"),
                "--port=" + config.getProperty("db.puerto", "3306"), "--user=" + config.getProperty("db.usuario"),
                "--default-character-set=utf8mb4");
        pb.environment().put("MYSQL_PWD", config.getProperty("db.clave", ""));
        pb.redirectErrorStream(true);
        Path salida = Files.createTempFile("seipo_test_carga", ".log");
        pb.redirectOutput(salida.toFile());
        Process p = pb.start();
        try (OutputStream in = p.getOutputStream()) {
            in.write(sql.getBytes(StandardCharsets.UTF_8));
        }
        int codigo = p.waitFor();
        String log = new String(Files.readAllBytes(salida), StandardCharsets.UTF_8);
        Files.deleteIfExists(salida);
        if (codigo != 0) throw new IllegalStateException("No se pudo cargar " + BASE_PRUEBAS + " (código " + codigo + "):\n" + log);
    }

    private static Properties leerConfig() throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(CONFIG, StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }

    private static String buscarCliente(Properties config) {
        String indicado = config.getProperty("prueba.cliente");
        if (indicado != null && !indicado.trim().isEmpty()) return indicado.trim();
        String[] candidatos = {"C:\\Program Files\\MariaDB 12.3\\bin\\mariadb.exe", "C:\\Program Files\\MariaDB 12.3\\bin\\mysql.exe",
                "C:\\xampp\\mysql\\bin\\mysql.exe"};
        for (String c : candidatos) if (Files.exists(Paths.get(c))) return c;
        return "mariadb"; // en el PATH
    }

    private static Path buscarRaiz() {
        Path p = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (Path d = p; d != null; d = d.getParent()) {
            if (Files.exists(d.resolve("SEIPO/bd/seipo_db.sql"))) return d;
        }
        throw new IllegalStateException("No se encontró la raíz del repositorio desde " + p);
    }

    // ---------------------------------------------------------------- Consultas de verificación

    static Connection conexion() throws SQLException {
        return ConexionBD.obtenerConexion();
    }

    static long entero(String sql, Object... parametros) throws SQLException {
        try (Connection con = conexion(); PreparedStatement pst = preparar(con, sql, parametros); ResultSet rs = pst.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    static BigDecimal decimal(String sql, Object... parametros) throws SQLException {
        try (Connection con = conexion(); PreparedStatement pst = preparar(con, sql, parametros); ResultSet rs = pst.executeQuery()) {
            rs.next();
            BigDecimal v = rs.getBigDecimal(1);
            return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
        }
    }

    static String texto(String sql, Object... parametros) throws SQLException {
        try (Connection con = conexion(); PreparedStatement pst = preparar(con, sql, parametros); ResultSet rs = pst.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    /** Primera columna de todas las filas, en el orden de la consulta. */
    static List<String> listaTextos(String sql, Object... parametros) throws SQLException {
        List<String> lista = new ArrayList<>();
        try (Connection con = conexion(); PreparedStatement pst = preparar(con, sql, parametros); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) lista.add(rs.getString(1));
        }
        return lista;
    }

    static int ejecutar(String sql, Object... parametros) throws SQLException {
        try (Connection con = conexion(); PreparedStatement pst = preparar(con, sql, parametros)) {
            return pst.executeUpdate();
        }
    }

    static int insertar(String sql, Object... parametros) throws SQLException {
        try (Connection con = conexion(); PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < parametros.length; i++) pst.setObject(i + 1, parametros[i]);
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static PreparedStatement preparar(Connection con, String sql, Object... parametros) throws SQLException {
        PreparedStatement pst = con.prepareStatement(sql);
        for (int i = 0; i < parametros.length; i++) pst.setObject(i + 1, parametros[i]);
        return pst;
    }

    /** Número de filas de las tablas que una venta modifica; sirve para comprobar que no quedó nada a medias. */
    static String huellaVentas() throws SQLException {
        return entero("SELECT COUNT(*) FROM ventas") + "/" + entero("SELECT COUNT(*) FROM detalle_ventas") + "/"
                + entero("SELECT COUNT(*) FROM venta_lotes") + "/" + entero("SELECT COUNT(*) FROM comprobantes") + "/"
                + entero("SELECT COUNT(*) FROM movimientos") + "/" + entero("SELECT COALESCE(SUM(cantidad), 0) FROM lotes")
                + "/" + entero("SELECT COALESCE(SUM(stock), 0) FROM productos");
    }

    // ---------------------------------------------------------------- Datos de prueba

    static void iniciarSesion(String usuario, String clave) throws SQLException {
        assertEquals(ControladorLogin.Resultado.CORRECTO, new ControladorLogin().iniciarSesion(usuario, clave),
                "No se pudo iniciar sesión con el usuario de demostración " + usuario);
    }

    /** Crea un producto de prueba con stock 0 (el stock solo entra por recepción). */
    static int crearProducto(String codigo, String precio, int minimo, Integer maximo) throws SQLException {
        return insertar("INSERT INTO productos (codigo, nombre, categoria, precio, stock, stock_minimo, stock_maximo) VALUES (?, ?, ?, ?, 0, ?, ?)",
                codigo, "Producto de prueba " + codigo, "Pruebas", new BigDecimal(precio), minimo, maximo);
    }

    /** Ingresa un lote por recepción de mercadería, como lo haría el personal. */
    static void recibirLote(int productoId, String numeroLote, LocalDate vencimiento, int cantidad) throws SQLException {
        List<ControladorInventario.LineaRecepcion> lineas = new ArrayList<>();
        lineas.add(new ControladorInventario.LineaRecepcion(producto(productoId), numeroLote, vencimiento, cantidad, "A-1"));
        new ControladorInventario().registrarRecepcion("GR-" + numeroLote, "Proveedor de prueba", lineas);
    }

    static ProductoVenta producto(int productoId) throws SQLException {
        String codigo = texto("SELECT codigo FROM productos WHERE id = ?", productoId);
        return new ControladorVentas().buscarProductoBD(codigo);
    }

    static int loteId(String numeroLote) throws SQLException {
        return (int) entero("SELECT id FROM lotes WHERE numero_lote = ?", numeroLote);
    }

    static long cantidadLote(String numeroLote) throws SQLException {
        return entero("SELECT cantidad FROM lotes WHERE numero_lote = ?", numeroLote);
    }

    static long stockProducto(int productoId) throws SQLException {
        return entero("SELECT stock FROM productos WHERE id = ?", productoId);
    }

    /** Cobra una venta de un solo producto. */
    static int vender(int productoId, int cantidad, double descuentoPct, String forma, Integer clienteId,
                      Double recibido) throws SQLException {
        return new ControladorVentas().cobrarVenta(Collections.singletonList(producto(productoId)),
                Collections.singletonList(cantidad), descuentoPct, forma, clienteId, recibido);
    }

    /** Total esperado según el documento: (subtotal - descuento) + IGV 18 % con 2 decimales, sin redondeo a 10 céntimos. */
    static BigDecimal totalEsperado(BigDecimal subtotal, BigDecimal descuento) {
        BigDecimal base = subtotal.subtract(descuento);
        BigDecimal igv = base.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        return base.add(igv);
    }
}
