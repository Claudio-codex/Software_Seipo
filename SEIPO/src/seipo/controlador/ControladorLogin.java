package seipo.controlador;

import seipo.modelo.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ControladorLogin {
    public boolean validarUsuario(String usuario, String password) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE usuario = ? AND password = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, usuario);
            pst.setString(2, password);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }
}