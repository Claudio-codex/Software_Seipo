package seipo.controlador;

import seipo.modelo.ConexionBD;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ControladorInventario {

    public static class ResumenInventario {
        public int totalStock;
        public int bajoStock;
        public int agotados;

        public ResumenInventario(int totalStock, int bajoStock, int agotados) {
            this.totalStock = totalStock;
            this.bajoStock = bajoStock;
            this.agotados = agotados;
        }
    }

    public static class MovimientoVO {
        public String fecha, producto, tipo;
        public int cantidad, stock;

        public MovimientoVO(String fecha, String producto, String tipo, int cantidad, int stock) {
            this.fecha = fecha;
            this.producto = producto;
            this.tipo = tipo;
            this.cantidad = cantidad;
            this.stock = stock;
        }
    }

    public ResumenInventario obtenerResumen() throws SQLException {
        int sumaStockTotal = 0, bajo = 0, agotados = 0;
        String sqlProductos = "SELECT stock FROM productos";
        try (Connection con = ConexionBD.obtenerConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sqlProductos)) {
            while (rs.next()) {
                int stock = rs.getInt("stock");
                sumaStockTotal += stock;
                if (stock == 0) agotados++;
                else if (stock <= seipo.modelo.Datos.STOCK_BAJO) bajo++;
            }
        }
        return new ResumenInventario(sumaStockTotal, bajo, agotados);
    }

    public List<MovimientoVO> obtenerMovimientosRecientes() throws SQLException {
        List<MovimientoVO> lista = new ArrayList<>();
        String sqlMovimientos = "SELECT fecha, producto_nombre, tipo, cantidad, stock_resultante FROM movimientos ORDER BY id DESC LIMIT 15";
        try (Connection con = ConexionBD.obtenerConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sqlMovimientos)) {
            while (rs.next()) {
                lista.add(new MovimientoVO(
                        rs.getString("fecha"),
                        rs.getString("producto_nombre"),
                        rs.getString("tipo"),
                        rs.getInt("cantidad"),
                        rs.getInt("stock_resultante")
                ));
            }
        }
        return lista;
    }
}