package seipo.controlador;

import seipo.modelo.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ControladorClientes {

    public static class ClienteVO {
        public int id;
        public String nombre, documento, telefono, correo;
        public int numCompras;
        public String descuentoVIP;
        /** Porcentaje de descuento VIP que se aplica al venderle. */
        public double porcentajeVIP;

        public ClienteVO(int id, String nombre, String documento, String telefono, String correo, int numCompras) {
            this.id = id;
            this.nombre = nombre;
            this.documento = documento;
            this.telefono = telefono;
            this.correo = correo;
            this.numCompras = numCompras;
            if (numCompras >= 5) { porcentajeVIP = 15; descuentoVIP = "Platino (15%)"; }
            else if (numCompras >= 3) { porcentajeVIP = 10; descuentoVIP = "Oro (10%)"; }
            else if (numCompras >= 1) { porcentajeVIP = 5; descuentoVIP = "Plata (5%)"; }
            else { porcentajeVIP = 0; descuentoVIP = "Sin Descuento (0%)"; }
        }

        @Override
        public String toString() {
            return nombre + " · " + documento;
        }
    }

    public List<ClienteVO> buscarClientes(String textoBusqueda) throws SQLException {
        List<ClienteVO> lista = new ArrayList<>();
        // Solo cuentan las compras no anuladas del propio cliente
        String sql = "SELECT c.id, c.nombre, c.documento, c.telefono, c.correo, " +
                "(SELECT COUNT(*) FROM ventas v WHERE v.cliente_id = c.id AND v.estado = 'COMPLETADA') AS total_ventas " +
                "FROM clientes c WHERE c.nombre LIKE ? OR c.documento LIKE ? ORDER BY c.nombre";

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            String filtro = "%" + textoBusqueda + "%";
            pst.setString(1, filtro);
            pst.setString(2, filtro);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ClienteVO(rs.getInt("id"), rs.getString("nombre"), rs.getString("documento"),
                            rs.getString("telefono"), rs.getString("correo"), rs.getInt("total_ventas")));
                }
            }
        }
        return lista;
    }

    public void registrarCliente(String nombre, String doc, String telefono, String correo) throws SQLException {
        String sqlInsert = "INSERT INTO clientes (nombre, documento, telefono, correo) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sqlInsert)) {
            pst.setString(1, nombre);
            pst.setString(2, doc);
            pst.setString(3, telefono);
            pst.setString(4, correo);
            pst.executeUpdate();
        }
    }
}
