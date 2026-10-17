package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Datos;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ControladorProductos {

    public static class ProductoVO {
        public String codigo, nombre, categoria;
        public double precio;
        public int stock;

        public ProductoVO(String codigo, String nombre, String categoria, double precio, int stock) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.categoria = categoria;
            this.precio = precio;
            this.stock = stock;
        }
    }

    public List<ProductoVO> buscarProductos(String textoBusqueda) throws SQLException {
        List<ProductoVO> lista = new ArrayList<>();
        boolean filtrar = !textoBusqueda.isEmpty();
        String sql = filtrar ?
                "SELECT codigo, nombre, categoria, precio, stock FROM productos WHERE codigo LIKE ? OR nombre LIKE ? OR categoria LIKE ?" :
                "SELECT codigo, nombre, categoria, precio, stock FROM productos";

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            if (filtrar) {
                String filtro = "%" + textoBusqueda + "%";
                pst.setString(1, filtro);
                pst.setString(2, filtro);
                pst.setString(3, filtro);
            }
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ProductoVO(
                            rs.getString("codigo"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getDouble("precio"),
                            rs.getInt("stock")
                    ));
                }
            }
        }
        return lista;
    }

    public void registrarProducto(String codigo, String nombre, String categoria, double precio, int stock) throws SQLException {
        String sqlCheck = "SELECT COUNT(*) FROM productos WHERE codigo = ?";
        String sqlInsert = "INSERT INTO productos (codigo, nombre, categoria, precio, stock) VALUES (?, ?, ?, ?, ?)";
        String sqlMovimiento = "INSERT INTO movimientos (fecha, producto_nombre, tipo, cantidad, stock_resultante) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.obtenerConexion()) {
            try (PreparedStatement pstCheck = con.prepareStatement(sqlCheck)) {
                pstCheck.setString(1, codigo);
                try (ResultSet rs = pstCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new SQLException("Ya existe un producto con ese código.");
                    }
                }
            }

            try (PreparedStatement pstInsert = con.prepareStatement(sqlInsert)) {
                pstInsert.setString(1, codigo);
                pstInsert.setString(2, nombre);
                pstInsert.setString(3, categoria);
                pstInsert.setDouble(4, precio);
                pstInsert.setInt(5, stock);
                pstInsert.executeUpdate();
            }

            if (stock > 0) {
                try (PreparedStatement pstMov = con.prepareStatement(sqlMovimiento)) {
                    pstMov.setString(1, java.time.LocalDate.now().format(Datos.FECHA));
                    pstMov.setString(2, nombre);
                    pstMov.setString(3, "Ingreso");
                    pstMov.setInt(4, stock);
                    pstMov.setInt(5, stock);
                    pstMov.executeUpdate();
                }
            }
        }
    }
}