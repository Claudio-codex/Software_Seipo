package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Sesion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ControladorLogin {

    public enum Resultado { CORRECTO, INCORRECTO, DESACTIVADO }

    /** Valida las credenciales y, si son correctas y el usuario está activo, inicia la sesión. */
    public Resultado iniciarSesion(String usuario, String password) throws SQLException {
        String sql = "SELECT id, usuario, nombre, rol, activo FROM usuarios WHERE usuario = ? AND password = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, usuario);
            pst.setString(2, password);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return Resultado.INCORRECTO;
                if (!rs.getBoolean("activo")) return Resultado.DESACTIVADO;
                Sesion.iniciar(rs.getInt("id"), rs.getString("usuario"), rs.getString("nombre"), rs.getString("rol"));
                return Resultado.CORRECTO;
            }
        }
    }
}
