package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Contrasenas;
import seipo.modelo.Sesion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ControladorLogin {

    public enum Resultado { CORRECTO, INCORRECTO, DESACTIVADO }

    /** Valida las credenciales y, si son correctas y el usuario está activo, inicia la sesión. */
    public Resultado iniciarSesion(String usuario, String password) throws SQLException {
        String sql = "SELECT id, usuario, nombre, rol, activo, password FROM usuarios WHERE usuario = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, usuario);
            try (ResultSet rs = pst.executeQuery()) {
                // La contraseña se compara en Java contra el hash guardado (ver Contrasenas)
                if (!rs.next()) return Resultado.INCORRECTO;
                String guardado = rs.getString("password");
                if (Contrasenas.estaCifrada(guardado)) {
                    if (!Contrasenas.verificar(password, guardado)) return Resultado.INCORRECTO;
                } else {
                    // Clave en texto plano de una base anterior: si coincide, se cifra en ese momento
                    if (!password.equals(guardado)) return Resultado.INCORRECTO;
                    ConexionBD.cifrarPassword(con, rs.getInt("id"), password);
                }
                if (!rs.getBoolean("activo")) return Resultado.DESACTIVADO;
                Sesion.iniciar(rs.getInt("id"), rs.getString("usuario"), rs.getString("nombre"), rs.getString("rol"));
                return Resultado.CORRECTO;
            }
        }
    }
}
