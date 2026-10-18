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
                        "    registrado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ");"
        };

        try (Connection conn = DriverManager.getConnection(URL_SERVIDOR, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            for (String sql : sqls) {
                stmt.execute(sql);
            }
            actualizarUsuarios(stmt);
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