package seipo.controlador;

import seipo.modelo.ConexionBD;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ControladorProductos {

    public static class ProductoVO {
        public String codigo, nombre, categoria;
        public double precio;
        public int stock;
        public LocalDate proximoVencimiento; // del lote con existencias que vence antes; null si ninguno vence

        public ProductoVO(String codigo, String nombre, String categoria, double precio, int stock, LocalDate proximoVencimiento) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.categoria = categoria;
            this.precio = precio;
            this.stock = stock;
            this.proximoVencimiento = proximoVencimiento;
        }
    }

    public List<ProductoVO> buscarProductos(String textoBusqueda) throws SQLException {
        List<ProductoVO> lista = new ArrayList<>();
        String sql = "SELECT p.codigo, p.nombre, p.categoria, p.precio, p.stock, " +
                "(SELECT MIN(l.fecha_vencimiento) FROM lotes l WHERE l.producto_id = p.id AND l.cantidad > 0) AS vence " +
                "FROM productos p WHERE p.codigo LIKE ? OR p.nombre LIKE ? OR p.categoria LIKE ?";

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            String filtro = "%" + textoBusqueda + "%";
            pst.setString(1, filtro);
            pst.setString(2, filtro);
            pst.setString(3, filtro);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Date vence = rs.getDate("vence");
                    lista.add(new ProductoVO(
                            rs.getString("codigo"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getDouble("precio"),
                            rs.getInt("stock"),
                            vence == null ? null : vence.toLocalDate()
                    ));
                }
            }
        }
        return lista;
    }

    /** Registra el producto en el catálogo con stock 0; el stock entra por la recepción de mercadería. */
    public void registrarProducto(String codigo, String nombre, String categoria, double precio) throws SQLException {
        String sqlCheck = "SELECT COUNT(*) FROM productos WHERE codigo = ?";
        String sqlInsert = "INSERT INTO productos (codigo, nombre, categoria, precio, stock) VALUES (?, ?, ?, ?, 0)";

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
                pstInsert.executeUpdate();
            }
        }
    }
}
