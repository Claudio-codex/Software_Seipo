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
        public String nombre, documento, telefono, correo;
        public int numCompras;
        public String descuentoVIP;

        public ClienteVO(String nombre, String documento, String telefono, String correo, int numCompras, String descuentoVIP) {
            this.nombre = nombre;
            this.documento = documento;
            this.telefono = telefono;
            this.correo = correo;
            this.numCompras = numCompras;
            this.descuentoVIP = descuentoVIP;
        }
    }

    public List<ClienteVO> buscarClientes(String textoBusqueda) throws SQLException {
        List<ClienteVO> lista = new ArrayList<>();
        String sql = "SELECT c.nombre, c.documento, c.telefono, c.correo, " +
                "(SELECT COUNT(*) FROM ventas v WHERE v.forma_pago IS NOT NULL) AS total_ventas " +
                "FROM clientes c WHERE c.nombre LIKE ? OR c.documento LIKE ?";

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            String filtro = "%" + textoBusqueda + "%";
            pst.setString(1, filtro);
            pst.setString(2, filtro);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String nombre = rs.getString("nombre");
                    String documento = rs.getString("documento");
                    String telefono = rs.getString("telefono");
                    String correo = rs.getString("correo");
                    int numCompras = rs.getInt("total_ventas");

                    String descuentoVIP = "Sin Descuento (0%)";
                    if (numCompras >= 5) descuentoVIP = "Platino (15%)";
                    else if (numCompras >= 3) descuentoVIP = "Oro (10%)";
                    else if (numCompras >= 1) descuentoVIP = "Plata (5%)";

                    lista.add(new ClienteVO(nombre, documento, telefono, correo, numCompras, descuentoVIP));
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