package seipo.modelo;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class ConexionBD {
    private static final String PARAMETROS_URL = "?useSSL=false&serverTimezone=America/Lima";

    /** Contenido de config.properties cuando no existe: los valores de un XAMPP recién instalado. */
    private static final String CONFIG_POR_DEFECTO =
            "# Conexión de SEIPO con la base de datos (MariaDB o MySQL).\n" +
            "# Copia config.ejemplo.properties para usar el usuario 'seipo'.\n" +
            "db.host=localhost\n" +
            "db.puerto=3306\n" +
            "db.base=seipo_db\n" +
            "db.usuario=root\n" +
            "db.clave=\n" +
            "\n" +
            "# Ruta de mariadb-dump.exe o mysqldump.exe para respaldo.bat (opcional; solo la usa el respaldo).\n" +
            "#respaldo.dump=C:\\Program Files\\MariaDB 12.3\\bin\\mariadb-dump.exe\n";

    private static Path archivoConfig;
    private static String urlServidor;
    private static String urlBase;
    private static String base;
    private static String usuario;
    private static String clave;

    /** Se pone en true cuando el esquema ya se creó o actualizó en esta ejecución del programa. */
    private static volatile boolean esquemaListo;

    public static Connection obtenerConexion() throws SQLException {
        cargarConfiguracion();
        if (!esquemaListo) inicializarBaseDatos();
        return DriverManager.getConnection(urlBase, usuario, clave);
    }

    /**
     * Lee config.properties del directorio de trabajo (o de la ruta -Dseipo.config) una sola vez por ejecución.
     * Si el archivo no existe lo crea con los valores por defecto.
     */
    private static synchronized void cargarConfiguracion() throws SQLException {
        if (urlBase != null) return;
        String ruta = System.getProperty("seipo.config");
        Path archivo = Paths.get(ruta != null ? ruta : "config.properties").toAbsolutePath();
        Properties config = new Properties();
        try {
            if (Files.exists(archivo)) {
                try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                    config.load(lector);
                }
            } else {
                Files.write(archivo, CONFIG_POR_DEFECTO.getBytes(StandardCharsets.UTF_8));
                System.out.println("Se creó " + archivo + " con los valores por defecto.");
            }
        } catch (IOException e) {
            System.err.println("Aviso: no se pudo leer ni crear " + archivo + "; se usan los valores por defecto. " + e.getMessage());
        }
        String host = config.getProperty("db.host", "localhost").trim();
        String puerto = config.getProperty("db.puerto", "3306").trim();
        String nombreBase = config.getProperty("db.base", "seipo_db").trim();
        if (!nombreBase.matches("[A-Za-z0-9_]+")) {
            throw new SQLException("El nombre de la base en " + archivo + " solo puede tener letras, números y guiones bajos: " + nombreBase);
        }
        archivoConfig = archivo;
        base = nombreBase;
        usuario = config.getProperty("db.usuario", "root").trim();
        clave = config.getProperty("db.clave", "");
        urlServidor = "jdbc:mysql://" + host + ":" + puerto + "/" + PARAMETROS_URL;
        urlBase = "jdbc:mysql://" + host + ":" + puerto + "/" + base + PARAMETROS_URL;
    }

    /** Traduce un error de conexión a un mensaje que dice qué revisar. */
    public static String mensajeError(SQLException e) {
        String archivo = archivoConfig != null ? archivoConfig.toString() : "config.properties";
        String estado = e.getSQLState() != null ? e.getSQLState() : "";
        if (estado.startsWith("08")) {
            return "No se pudo conectar con el servidor de base de datos.\n\n" +
                    "Revise que:\n" +
                    "• El servicio de MariaDB esté iniciado (Servicios de Windows, o MySQL en el panel de XAMPP).\n" +
                    "• El host y el puerto (db.host, db.puerto) sean correctos.\n\n" +
                    "Configuración: " + archivo;
        }
        if (e.getMessage() != null && e.getMessage().contains("authentication plugin")) {
            // Por ejemplo, root en MariaDB para Windows entra con la autenticación de Windows (auth_gssapi_client)
            return "El usuario " + usuario + " usa un método de autenticación que el programa no admite.\n\n" +
                    "Use un usuario con contraseña (por ejemplo 'seipo') en db.usuario y db.clave de:\n" + archivo;
        }
        switch (e.getErrorCode()) {
            case 1045: // ER_ACCESS_DENIED_ERROR
                return "El servidor rechazó el usuario o la contraseña.\n\n" +
                        "Revise db.usuario y db.clave en:\n" + archivo;
            case 1044: // ER_DBACCESS_DENIED_ERROR
            case 1049: // ER_BAD_DB_ERROR
                return "El usuario no tiene acceso a la base " + base + " o la base no existe.\n\n" +
                        "Revise que el usuario tenga permisos sobre la base (GRANT ALL ON " + base + ".*)\n" +
                        "y el valor de db.base en:\n" + archivo;
            default:
                return "Error de conexión con la base de datos:\n" + e.getMessage() + "\n\nConfiguración: " + archivo;
        }
    }

    /**
     * Crea la base, las tablas y las columnas e índices que falten. Corre una sola vez por ejecución;
     * si MySQL no responde, se vuelve a intentar en la siguiente conexión.
     */
    private static synchronized void inicializarBaseDatos() {
        if (esquemaListo) return;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el driver JDBC de MySQL.");
        }
        String[] sqls = {
                "CREATE DATABASE IF NOT EXISTS `" + base + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;",
                "USE `" + base + "`;",

                "CREATE TABLE IF NOT EXISTS usuarios (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    usuario VARCHAR(50) NOT NULL UNIQUE," +
                        "    password VARCHAR(255) NOT NULL," +
                        "    nombre VARCHAR(100) NOT NULL DEFAULT ''," +
                        "    rol VARCHAR(20) NOT NULL DEFAULT 'PERSONAL'," +
                        "    activo BOOLEAN NOT NULL DEFAULT TRUE," +
                        "    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ");",

                "CREATE TABLE IF NOT EXISTS productos (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    codigo VARCHAR(50) NOT NULL UNIQUE," +
                        "    nombre VARCHAR(100) NOT NULL," +
                        "    categoria VARCHAR(50)," +
                        "    precio DECIMAL(10, 2) NOT NULL," +
                        "    stock INT NOT NULL DEFAULT 0," +
                        "    stock_minimo INT NOT NULL DEFAULT 10," +
                        "    stock_maximo INT," + // NULL: sin límite de sobrestock
                        "    actualizado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ");",

                "CREATE TABLE IF NOT EXISTS clientes (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    nombre VARCHAR(100) NOT NULL," +
                        "    documento VARCHAR(20) NOT NULL UNIQUE," +
                        "    telefono VARCHAR(20)," +
                        "    correo VARCHAR(100)," +
                        "    registrado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ");",

                "CREATE TABLE IF NOT EXISTS ventas (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    fecha DATE NOT NULL," +
                        "    total DECIMAL(10, 2) NOT NULL," +
                        "    forma_pago VARCHAR(50) NOT NULL," +
                        "    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ");",

                "CREATE TABLE IF NOT EXISTS detalle_ventas (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    venta_id INT NOT NULL," +
                        "    producto_nombre VARCHAR(100) NOT NULL," +
                        "    cantidad INT NOT NULL," +
                        "    importe DECIMAL(10, 2) NOT NULL," +
                        "    FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE CASCADE" +
                        ");",

                "CREATE TABLE IF NOT EXISTS movimientos (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    fecha VARCHAR(20) NOT NULL," +
                        "    producto_nombre VARCHAR(100) NOT NULL," +
                        "    tipo VARCHAR(20) NOT NULL," +
                        "    cantidad INT NOT NULL," +
                        "    stock_resultante INT NOT NULL," +
                        "    lote VARCHAR(50)," +
                        "    registrado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ");",

                "CREATE TABLE IF NOT EXISTS recepciones (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    fecha DATE NOT NULL," +
                        "    guia_remision VARCHAR(50) NOT NULL," +
                        "    proveedor VARCHAR(100) NOT NULL," +
                        "    usuario_id INT," +
                        "    registrado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)" +
                        ");",

                // Stock por lote para la regla FEFO; productos.stock es la suma de cantidad de sus lotes
                "CREATE TABLE IF NOT EXISTS lotes (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    producto_id INT NOT NULL," +
                        "    recepcion_id INT," +
                        "    numero_lote VARCHAR(50) NOT NULL," +
                        "    fecha_vencimiento DATE," +
                        "    cantidad_inicial INT NOT NULL," +
                        "    cantidad INT NOT NULL," +
                        "    ubicacion VARCHAR(50)," +
                        "    registrado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    FOREIGN KEY (producto_id) REFERENCES productos(id)," +
                        "    FOREIGN KEY (recepcion_id) REFERENCES recepciones(id)," +
                        "    INDEX idx_lotes_fefo (producto_id, fecha_vencimiento)" +
                        ");",

                // Bajas de inventario: se registran PENDIENTE (reservan stock) y el administrador las APRUEBA o RECHAZA
                "CREATE TABLE IF NOT EXISTS mermas (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    lote_id INT NOT NULL," +
                        "    producto_id INT NOT NULL," +
                        "    cantidad INT NOT NULL," +
                        "    causal VARCHAR(30) NOT NULL," +
                        "    observacion VARCHAR(255)," +
                        "    precio_unitario DECIMAL(10, 2) NOT NULL," +
                        "    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'," +
                        "    solicitado_por INT NOT NULL," +
                        "    solicitado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    autorizado_por INT," +
                        "    autorizado_en DATETIME," +
                        "    motivo_rechazo VARCHAR(255)," +
                        "    FOREIGN KEY (lote_id) REFERENCES lotes(id)," +
                        "    FOREIGN KEY (producto_id) REFERENCES productos(id)," +
                        "    FOREIGN KEY (solicitado_por) REFERENCES usuarios(id)," +
                        "    FOREIGN KEY (autorizado_por) REFERENCES usuarios(id)," +
                        "    INDEX idx_mermas_lote (lote_id, estado)" +
                        ");",

                "CREATE TABLE IF NOT EXISTS proveedores (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    ruc VARCHAR(11) NOT NULL UNIQUE," +
                        "    razon_social VARCHAR(150) NOT NULL," +
                        "    contacto VARCHAR(100)," +
                        "    telefono VARCHAR(20)," +
                        "    correo VARCHAR(100)," +
                        "    activo BOOLEAN NOT NULL DEFAULT TRUE," +
                        "    registrado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ");",

                // Estados: PENDIENTE -> APROBADA -> RECIBIDA, o PENDIENTE -> RECHAZADA
                "CREATE TABLE IF NOT EXISTS ordenes_compra (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    proveedor_id INT NOT NULL," +
                        "    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'," +
                        "    observacion VARCHAR(255)," +
                        "    creado_por INT NOT NULL," +
                        "    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    aprobado_por INT," +
                        "    aprobado_en DATETIME," +
                        "    motivo_rechazo VARCHAR(255)," +
                        "    recepcion_id INT," +
                        "    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)," +
                        "    FOREIGN KEY (creado_por) REFERENCES usuarios(id)," +
                        "    FOREIGN KEY (aprobado_por) REFERENCES usuarios(id)," +
                        "    FOREIGN KEY (recepcion_id) REFERENCES recepciones(id)" +
                        ");",

                "CREATE TABLE IF NOT EXISTS detalle_ordenes (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    orden_id INT NOT NULL," +
                        "    producto_id INT NOT NULL," +
                        "    cantidad INT NOT NULL," +
                        "    FOREIGN KEY (orden_id) REFERENCES ordenes_compra(id) ON DELETE CASCADE," +
                        "    FOREIGN KEY (producto_id) REFERENCES productos(id)" +
                        ");",

                // Turno de caja: cada usuario abre uno con el efectivo inicial y al cerrarlo cuadra lo contado contra lo esperado
                "CREATE TABLE IF NOT EXISTS turnos (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    usuario_id INT NOT NULL," +
                        "    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO'," +
                        "    abierto_en DATETIME NOT NULL," +
                        "    monto_inicial DECIMAL(10, 2) NOT NULL," +
                        "    cerrado_en DATETIME," +
                        "    efectivo_esperado DECIMAL(10, 2)," +
                        "    efectivo_contado DECIMAL(10, 2)," +
                        "    observacion VARCHAR(255)," +
                        "    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)," +
                        "    INDEX idx_turnos_usuario (usuario_id, estado)" +
                        ");",

                // Unidades que cada venta tomó de cada lote, para devolverlas al mismo lote si la venta se anula
                "CREATE TABLE IF NOT EXISTS venta_lotes (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    venta_id INT NOT NULL," +
                        "    lote_id INT NOT NULL," +
                        "    producto_id INT NOT NULL," +
                        "    cantidad INT NOT NULL," +
                        "    FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE CASCADE," +
                        "    FOREIGN KEY (lote_id) REFERENCES lotes(id)," +
                        "    FOREIGN KEY (producto_id) REFERENCES productos(id)" +
                        ");",

                // Parámetros que configura el administrador: datos del negocio para el comprobante y la pasarela de pago
                "CREATE TABLE IF NOT EXISTS configuracion (" +
                        "    clave VARCHAR(50) PRIMARY KEY," +
                        "    valor VARCHAR(255) NOT NULL DEFAULT ''" +
                        ");",

                // Cobros digitales enviados a la pasarela. Del celular del cliente solo se guardan los últimos 3 dígitos
                "CREATE TABLE IF NOT EXISTS transacciones_pago (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    token VARCHAR(40) NOT NULL UNIQUE," +
                        "    venta_id INT," +
                        "    forma_pago VARCHAR(50) NOT NULL," +
                        "    monto DECIMAL(10, 2) NOT NULL," +
                        "    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'," +
                        "    medio VARCHAR(60)," +
                        "    codigo_autorizacion VARCHAR(12)," +
                        "    mensaje VARCHAR(150)," +
                        "    usuario_id INT NOT NULL," +
                        "    creado_en DATETIME NOT NULL," +
                        "    respondido_en DATETIME," +
                        "    FOREIGN KEY (venta_id) REFERENCES ventas(id)," +
                        "    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)," +
                        "    INDEX idx_transacciones_fecha (creado_en)" +
                        ");",

                // Boleta o factura de cada venta; guarda los datos del emisor tal como estaban al emitirla
                "CREATE TABLE IF NOT EXISTS comprobantes (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    venta_id INT NOT NULL UNIQUE," +
                        "    tipo VARCHAR(10) NOT NULL," +
                        "    serie VARCHAR(4) NOT NULL," +
                        "    numero INT NOT NULL," +
                        "    emitido_en DATETIME NOT NULL," +
                        "    emisor_ruc VARCHAR(11) NOT NULL," +
                        "    emisor_nombre VARCHAR(150) NOT NULL," +
                        "    emisor_direccion VARCHAR(200) NOT NULL," +
                        "    cliente_documento VARCHAR(20)," +
                        "    cliente_nombre VARCHAR(100)," +
                        "    codigo_verificacion VARCHAR(20) NOT NULL," +
                        "    FOREIGN KEY (venta_id) REFERENCES ventas(id)," +
                        "    UNIQUE KEY uk_comprobante_numero (serie, numero)" +
                        ");"
        };

        try (Connection conn = DriverManager.getConnection(urlServidor, usuario, clave);
             Statement stmt = conn.createStatement()) {

            for (String sql : sqls) {
                stmt.execute(sql);
            }
            actualizarUsuarios(stmt);
            actualizarInventario(stmt);
            actualizarVentas(stmt);
            stmt.execute("INSERT IGNORE INTO configuracion (clave, valor) VALUES " +
                    "('empresa_nombre', 'Bazar Seipo'), ('empresa_ruc', '20999999991'), " +
                    "('empresa_direccion', 'Jr. Lima 000, Tarma, Junín'), ('pasarela_activa', '1'), " +
                    "('pasarela_proveedor', 'Pasarela de prueba'), ('pasarela_comercio', 'COM-000123'), " +
                    "('pasarela_clave', 'sk_test_seipo_4f9a2c7e')");
            crearIndices(stmt);
            esquemaListo = true;
        } catch (SQLException e) {
            System.err.println("Aviso/Error al autoinicializar la BD: " + e.getMessage());
        }
    }

    /**
     * Agrega las columnas de roles a una tabla usuarios creada con la versión anterior.
     * Los usuarios que ya existían quedan como administradores para no perder el acceso.
     */
    private static void actualizarUsuarios(Statement stmt) throws SQLException {
        if (agregarColumna(stmt, "ALTER TABLE usuarios ADD COLUMN rol VARCHAR(20) NOT NULL DEFAULT 'PERSONAL'")) {
            stmt.execute("UPDATE usuarios SET rol = 'ADMINISTRADOR'");
        }
        agregarColumna(stmt, "ALTER TABLE usuarios ADD COLUMN nombre VARCHAR(100) NOT NULL DEFAULT ''");
        agregarColumna(stmt, "ALTER TABLE usuarios ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE");
        stmt.execute("INSERT IGNORE INTO usuarios (usuario, password, nombre, rol) " +
                "VALUES ('admin', '123', 'Administrador', 'ADMINISTRADOR')");
    }

    /**
     * Prepara una base de versiones anteriores: agrega a productos el stock mínimo y máximo
     * y, para el control FEFO, agrega la columna lote a movimientos y pasa el stock de los productos que aún no tienen lotes a un lote "INICIAL"
     * sin fecha de vencimiento. Solo ocurre una vez: cuando la columna lote todavía no existía.
     */
    private static void actualizarInventario(Statement stmt) throws SQLException {
        agregarColumna(stmt, "ALTER TABLE productos ADD COLUMN stock_minimo INT NOT NULL DEFAULT 10");
        agregarColumna(stmt, "ALTER TABLE productos ADD COLUMN stock_maximo INT");
        if (agregarColumna(stmt, "ALTER TABLE movimientos ADD COLUMN lote VARCHAR(50)")) {
            stmt.execute("INSERT INTO lotes (producto_id, numero_lote, cantidad_inicial, cantidad) " +
                    "SELECT p.id, 'INICIAL', p.stock, p.stock FROM productos p " +
                    "WHERE p.stock > 0 AND NOT EXISTS (SELECT 1 FROM lotes l WHERE l.producto_id = p.id)");
        }
    }

    /**
     * Agrega a las ventas el desglose de importes, el cliente, quién vendió, el turno de caja, el efectivo recibido
     * y los datos de anulación. Las ventas anteriores quedan COMPLETADAS y sin turno.
     */
    private static void actualizarVentas(Statement stmt) throws SQLException {
        agregarColumna(stmt, "ALTER TABLE ventas ADD COLUMN subtotal DECIMAL(10, 2)");
        agregarColumna(stmt, "ALTER TABLE ventas ADD COLUMN descuento DECIMAL(10, 2) NOT NULL DEFAULT 0");
        agregarColumna(stmt, "ALTER TABLE ventas ADD COLUMN cliente_id INT");
        agregarColumna(stmt, "ALTER TABLE ventas ADD COLUMN usuario_id INT");
        agregarColumna(stmt, "ALTER TABLE ventas ADD COLUMN turno_id INT");
        agregarColumna(stmt, "ALTER TABLE ventas ADD COLUMN monto_recibido DECIMAL(10, 2)");
        agregarColumna(stmt, "ALTER TABLE ventas ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'COMPLETADA'");
        agregarColumna(stmt, "ALTER TABLE ventas ADD COLUMN anulado_por INT");
        agregarColumna(stmt, "ALTER TABLE ventas ADD COLUMN anulado_en DATETIME");
        agregarColumna(stmt, "ALTER TABLE ventas ADD COLUMN motivo_anulacion VARCHAR(255)");
        agregarColumna(stmt, "ALTER TABLE detalle_ventas ADD COLUMN producto_id INT");
        agregarColumna(stmt, "ALTER TABLE detalle_ventas ADD COLUMN precio_unitario DECIMAL(10, 2)");
    }

    /** Índices para las consultas frecuentes sobre ventas: turnos de caja, compras por cliente y reportes por fecha. */
    private static void crearIndices(Statement stmt) throws SQLException {
        agregarIndice(stmt, "CREATE INDEX idx_ventas_turno ON ventas (turno_id, estado)");
        agregarIndice(stmt, "CREATE INDEX idx_ventas_cliente ON ventas (cliente_id, estado)");
        agregarIndice(stmt, "CREATE INDEX idx_ventas_fecha ON ventas (fecha, estado)");
    }

    /** Ejecuta un CREATE INDEX; no hace nada si el índice ya existía. */
    private static void agregarIndice(Statement stmt, String sql) throws SQLException {
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            if (e.getErrorCode() != 1061) throw e; // ER_DUP_KEYNAME
        }
    }

    /** Ejecuta un ALTER TABLE ... ADD COLUMN; devuelve false si la columna ya existía. */
    private static boolean agregarColumna(Statement stmt, String sql) throws SQLException {
        try {
            stmt.execute(sql);
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1060) return false; // ER_DUP_FIELDNAME
            throw e;
        }
    }

    public static void main(String[] args) {
        System.out.println("Probando autoincremento y conexión...");
        try (Connection conn = obtenerConexion()) {
            if (conn != null) {
                System.out.println("¡Base de datos y tablas creadas/verificadas con éxito! Conexión lista.");
            }
        } catch (SQLException e) {
            System.err.println("¡Falló la conexión!\n" + ConexionBD.mensajeError(e));
        }
    }
}