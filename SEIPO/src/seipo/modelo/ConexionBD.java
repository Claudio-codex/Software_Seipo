package seipo.modelo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionBD {
    private static final String URL_SERVIDOR = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=America/Lima";
    private static final String URL_DB = "jdbc:mysql://localhost:3306/seipo_db?useSSL=false&serverTimezone=America/Lima";

    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection obtenerConexion() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el driver JDBC de MySQL.");
        }

        inicializarBaseDatos();

        return DriverManager.getConnection(URL_DB, USER, PASSWORD);
    }

    private static void inicializarBaseDatos() {
        String[] sqls = {
                "CREATE DATABASE IF NOT EXISTS seipo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;",
                "USE seipo_db;",

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
                        ");"
        };

        try (Connection conn = DriverManager.getConnection(URL_SERVIDOR, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            for (String sql : sqls) {
                stmt.execute(sql);
            }
            actualizarUsuarios(stmt);
            actualizarInventario(stmt);
            actualizarVentas(stmt);
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
            System.err.println("¡Falló la conexión! Revisa que MySQL esté encendido.");
            e.printStackTrace();
        }
    }
}