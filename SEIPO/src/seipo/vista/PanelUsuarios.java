package seipo.vista;

import seipo.controlador.ControladorUsuarios;
import seipo.controlador.ControladorUsuarios.UsuarioVO;
import seipo.modelo.Sesion;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Gestión de usuarios y roles (solo administradores). */
public class PanelUsuarios extends JPanel {

    private static final String[] ROLES = {Sesion.textoRol(Sesion.PERSONAL), Sesion.textoRol(Sesion.ADMINISTRADOR)};

    private final DefaultTableModel modelo = UI.modelo("Usuario", "Nombre", "Rol", "Estado");
    private final JTable tabla = UI.tabla(modelo);
    private final ControladorUsuarios controlador = new ControladorUsuarios();
    private List<UsuarioVO> usuarios = new ArrayList<>();

    public PanelUsuarios() {
        setLayout(new BorderLayout());
        JPanel p = UI.base();
        add(p);

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        norte.setOpaque(false);
        JButton nuevo = UI.boton("+ Nuevo Usuario");
        JButton editar = UI.botonSecundario("Editar");
        JButton clave = UI.botonSecundario("Restablecer contraseña");
        norte.add(nuevo);
        norte.add(editar);
        norte.add(clave);
        p.add(norte, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 5));
        centro.setOpaque(false);
        JLabel lblInfo = new JLabel("El personal de tienda accede a Ventas, Caja, Clientes, Productos, Inventario, Mermas y Compras. "
                + "El administrador además autoriza mermas y órdenes de compra y gestiona Reportes, Pagos y Usuarios.");
        lblInfo.setFont(UI.fuente(Font.PLAIN, 12));
        lblInfo.setForeground(UI.TEXTO_SECUNDARIO);
        centro.add(lblInfo, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        p.add(centro, BorderLayout.CENTER);

        nuevo.addActionListener(e -> nuevoUsuario());
        editar.addActionListener(e -> editarUsuario());
        clave.addActionListener(e -> restablecerPassword());
    }

    public void refrescar() {
        modelo.setRowCount(0);
        try {
            usuarios = controlador.listarUsuarios();
            for (UsuarioVO u : usuarios) {
                modelo.addRow(new Object[]{u.usuario, u.nombre, Sesion.textoRol(u.rol), u.activo ? "Activo" : "Desactivado"});
            }
        } catch (SQLException ex) {
            error("Error al cargar los usuarios", ex);
        }
    }

    private void nuevoUsuario() {
        JTextField u = new JTextField(), n = new JTextField();
        JPasswordField c = new JPasswordField(), c2 = new JPasswordField();
        JComboBox<String> rol = new JComboBox<>(ROLES);
        Object[] campos = {"Usuario:", u, "Nombre completo:", n, "Rol:", rol,
                "Contraseña:", c, "Repetir contraseña:", c2};

        if (JOptionPane.showConfirmDialog(this, campos, "Registrar Nuevo Usuario",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        String usuario = u.getText().trim();
        String nombre = n.getText().trim();
        String password = validarPassword(c, c2);
        if (usuario.isEmpty() || nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Usuario y nombre son obligatorios.");
            return;
        }
        if (password == null) return;

        try {
            controlador.registrarUsuario(usuario, nombre, password, rolElegido(rol));
            refrescar();
            JOptionPane.showMessageDialog(this, "¡Usuario registrado exitosamente!");
        } catch (SQLException ex) {
            error("Error al registrar (es posible que el usuario ya exista)", ex);
        }
    }

    private void editarUsuario() {
        UsuarioVO sel = seleccionado();
        if (sel == null) return;

        JTextField n = new JTextField(sel.nombre);
        JComboBox<String> rol = new JComboBox<>(ROLES);
        rol.setSelectedItem(Sesion.textoRol(sel.rol));
        JCheckBox activo = new JCheckBox("Usuario activo", sel.activo);
        Object[] campos = {"Usuario: " + sel.usuario, "Nombre completo:", n, "Rol:", rol, activo};

        if (JOptionPane.showConfirmDialog(this, campos, "Editar Usuario",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        String nombre = n.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.");
            return;
        }

        try {
            controlador.actualizarUsuario(sel.id, nombre, rolElegido(rol), activo.isSelected());
            refrescar();
        } catch (SQLException ex) {
            error("No se pudo actualizar el usuario", ex);
        }
    }

    private void restablecerPassword() {
        UsuarioVO sel = seleccionado();
        if (sel == null) return;

        JPasswordField c = new JPasswordField(), c2 = new JPasswordField();
        Object[] campos = {"Usuario: " + sel.usuario, "Nueva contraseña:", c, "Repetir contraseña:", c2};
        if (JOptionPane.showConfirmDialog(this, campos, "Restablecer Contraseña",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        String password = validarPassword(c, c2);
        if (password == null) return;

        try {
            controlador.cambiarPassword(sel.id, password);
            JOptionPane.showMessageDialog(this, "Contraseña actualizada.");
        } catch (SQLException ex) {
            error("No se pudo cambiar la contraseña", ex);
        }
    }

    private UsuarioVO seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla.");
            return null;
        }
        return usuarios.get(tabla.convertRowIndexToModel(fila));
    }

    /** Devuelve la contraseña si ambas coinciden y no está vacía; si no, avisa y devuelve null. */
    private String validarPassword(JPasswordField c, JPasswordField c2) {
        String password = new String(c.getPassword());
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La contraseña no puede estar vacía.");
            return null;
        }
        if (!password.equals(new String(c2.getPassword()))) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.");
            return null;
        }
        return password;
    }

    private static String rolElegido(JComboBox<String> rol) {
        return rol.getSelectedIndex() == 1 ? Sesion.ADMINISTRADOR : Sesion.PERSONAL;
    }

    private void error(String titulo, SQLException ex) {
        JOptionPane.showMessageDialog(this, titulo + ":\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
