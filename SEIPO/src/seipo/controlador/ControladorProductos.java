package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Sesion;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ControladorProductos {

    public static class ProductoVO {
        public String codigo, nombre, categoria;
        public double precio;
        public int stock, stockMinimo;
        public Integer stockMaximo; // null: sin límite de sobrestock
        public LocalDate proximoVencimiento; // del lote con existencias que vence antes; null si ninguno vence

        public ProductoVO(String codigo, String nombre, String categoria, double precio, int stock,
                          int stockMinimo, Integer stockMaximo, LocalDate proximoVencimiento) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.categoria = categoria;
            this.precio = precio;
            this.stock = stock;
            this.stockMinimo = stockMinimo;
            this.stockMaximo = stockMaximo;
            this.proximoVencimiento = proximoVencimiento;
        }
    }

    public List<ProductoVO> buscarProductos(String textoBusqueda) throws SQLException {
        List<ProductoVO> lista = new ArrayList<>();
        String sql = "SELECT p.codigo, p.nombre, p.categoria, p.precio, p.stock, p.stock_minimo, p.stock_maximo, " +
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
                    int max = rs.getInt("stock_maximo");
                    Integer maximo = rs.wasNull() ? null : max;
                    lista.add(new ProductoVO(
                            rs.getString("codigo"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getDouble("precio"),
                            rs.getInt("stock"),
                            rs.getInt("stock_minimo"),
                            maximo,
                            vence == null ? null : vence.toLocalDate()
                    ));
                }
            }
        }
        return lista;
    }

    /** Registra el producto en el catálogo con stock 0; el stock entra por la recepción de mercadería. */
    public void registrarProducto(String codigo, String nombre, String categoria, double precio,
                                  int stockMinimo, Integer stockMaximo) throws SQLException {
        validarLimites(stockMinimo, stockMaximo);
        String sqlCheck = "SELECT COUNT(*) FROM productos WHERE codigo = ?";
        String sqlInsert = "INSERT INTO productos (codigo, nombre, categoria, precio, stock, stock_minimo, stock_maximo) " +
                "VALUES (?, ?, ?, ?, 0, ?, ?)";

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
                pstInsert.setInt(5, stockMinimo);
                if (stockMaximo == null) pstInsert.setNull(6, Types.INTEGER);
                else pstInsert.setInt(6, stockMaximo);
                pstInsert.executeUpdate();
            }
        }
    }

    /** Modifica los datos del producto y sus parámetros de stock (solo administradores). El código no cambia. */
    public void actualizarProducto(String codigo, String nombre, String categoria, double precio,
                                   int stockMinimo, Integer stockMaximo) throws SQLException {
        if (!Sesion.esAdministrador()) throw new SQLException("Solo un administrador puede modificar productos.");
        validarLimites(stockMinimo, stockMaximo);
        String sql = "UPDATE productos SET nombre = ?, categoria = ?, precio = ?, stock_minimo = ?, stock_maximo = ? WHERE codigo = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, nombre);
            pst.setString(2, categoria);
            pst.setDouble(3, precio);
            pst.setInt(4, stockMinimo);
            if (stockMaximo == null) pst.setNull(5, Types.INTEGER);
            else pst.setInt(5, stockMaximo);
            pst.setString(6, codigo);
            pst.executeUpdate();
        }
    }

    private static void validarLimites(int stockMinimo, Integer stockMaximo) throws SQLException {
        if (stockMinimo < 0) throw new SQLException("El stock mínimo no puede ser negativo.");
        if (stockMaximo != null && stockMaximo <= stockMinimo) {
            throw new SQLException("El stock máximo debe ser mayor que el mínimo.");
        }
    }
}
