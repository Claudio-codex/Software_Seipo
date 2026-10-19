package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Sesion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ControladorUsuarios {

    public static class UsuarioVO {
        public int id;
        public String usuario, nombre, rol;
        public boolean activo;

        public UsuarioVO(int id, String usuario, String nombre, String rol, boolean activo) {
            this.id = id;
            this.usuario = usuario;
            this.nombre = nombre;
            this.rol = rol;
            this.activo = activo;
        }
    }

    public List<UsuarioVO> listarUsuarios() throws SQLException {
        List<UsuarioVO> lista = new ArrayList<>();
        String sql = "SELECT id, usuario, nombre, rol, activo FROM usuarios ORDER BY usuario";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                lista.add(new UsuarioVO(rs.getInt("id"), rs.getString("usuario"), rs.getString("nombre"),
                        rs.getString("rol"), rs.getBoolean("activo")));
            }
        }
        return lista;
    }

    public void registrarUsuario(String usuario, String nombre, String password, String rol) throws SQLException {
        exigirAdministrador("crear cuentas de usuario");
        String sql = "INSERT INTO usuarios (usuario, nombre, password, rol) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, usuario);
            pst.setString(2, nombre);
            pst.setString(3, password);
            pst.setString(4, rol);
            pst.executeUpdate();
        }
    }

    /**
     * Actualiza nombre, rol y estado. Impide que el usuario en sesión se desactive o se quite
     * el rol de administrador, y que el sistema se quede sin administradores activos.
     */
    public void actualizarUsuario(int id, String nombre, String rol, boolean activo) throws SQLException {
        exigirAdministrador("modificar cuentas de usuario");
        boolean quedaAdmin = Sesion.ADMINISTRADOR.equals(rol) && activo;
        if (id == Sesion.getId() && !quedaAdmin) {
            throw new SQLException("No puede desactivarse ni quitarse el rol de administrador a sí mismo.");
        }
        try (Connection con = ConexionBD.obtenerConexion()) {
            if (!quedaAdmin && contarOtrosAdministradores(con, id) == 0) {
                throw new SQLException("Debe quedar al menos un administrador activo.");
            }
            String sql = "UPDATE usuarios SET nombre = ?, rol = ?, activo = ? WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, nombre);
                pst.setString(2, rol);
                pst.setBoolean(3, activo);
                pst.setInt(4, id);
                pst.executeUpdate();
            }
        }
    }

    public void cambiarPassword(int id, String password) throws SQLException {
        exigirAdministrador("restablecer contraseñas");
        String sql = "UPDATE usuarios SET password = ? WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, password);
            pst.setInt(2, id);
            pst.executeUpdate();
        }
    }

    private static void exigirAdministrador(String accion) throws SQLException {
        if (!Sesion.esAdministrador()) throw new SQLException("Solo un administrador puede " + accion + ".");
    }

    private int contarOtrosAdministradores(Connection con, int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE rol = ? AND activo = TRUE AND id <> ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, Sesion.ADMINISTRADOR);
            pst.setInt(2, id);
            try (ResultSet rs = pst.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
