package seipo.vista;

import seipo.controlador.ControladorLogin;
import seipo.modelo.ConexionBD;
import java.awt.*;
import java.sql.SQLException;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.*;

public class Login extends JFrame {

    private final JTextField txtUsuario = new JTextField(18);
    private final JPasswordField txtClave = new JPasswordField(18);
    private final ControladorLogin controlador = new ControladorLogin();

    public Login() {
        setTitle("SEIPO - Iniciar sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(860, 500);
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new GridLayout(1, 2));

        // Panel de marca: fondo claro liso, con el logo en el acento
        JPanel izq = new JPanel(new GridBagLayout());
        izq.setBackground(UI.MENU);
        izq.setBorder(new MatteBorder(0, 0, 0, 1, UI.BORDE));
        JPanel marca = new JPanel(new GridLayout(0, 1, 0, 4));
        marca.setOpaque(false);
        JLabel logo = new JLabel("Seipo", Iconos.de(Iconos.Tipo.CARRITO, 56, UI.ACENTO), SwingConstants.LEFT);
        logo.setIconTextGap(14);
        logo.setFont(UI.fuente(Font.BOLD, 60));
        logo.setForeground(UI.TEXTO);
        JLabel lema = new JLabel("Sistema de venta e inventario");
        lema.setFont(UI.fuente(Font.PLAIN, 16));
        lema.setForeground(UI.TEXTO);
        JLabel detalle = new JLabel("Ventas · FEFO · Mermas · Compras");
        detalle.setFont(UI.fuente(Font.PLAIN, 13));
        detalle.setForeground(UI.TEXTO_SECUNDARIO);
        marca.add(logo);
        marca.add(lema);
        marca.add(detalle);
        izq.add(marca);

        JPanel der = new JPanel(new GridBagLayout());
        der.setBackground(UI.FONDO);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UI.SUPERFICIE);
        form.setBorder(UI.bordeTarjeta(26, 30, 28, 30));
        JLabel titulo = new JLabel("Iniciar sesión");
        titulo.setFont(UI.fuente(Font.BOLD, 24));
        JLabel subtitulo = UI.ayuda("Ingrese con su usuario asignado");
        JButton ingresar = UI.boton("Ingresar");
        txtUsuario.putClientProperty("JTextField.placeholderText", "Usuario");
        txtClave.putClientProperty("JTextField.placeholderText", "Contraseña");
        txtClave.putClientProperty("JPasswordField.showRevealButton", true);
        // Filas apiladas con espacios distintos: más aire entre grupos que entre etiqueta y campo
        Object[][] filas = {{titulo, 0}, {subtitulo, 2}, {etiqueta("Usuario"), 22}, {txtUsuario, 6},
                {etiqueta("Contraseña"), 14}, {txtClave, 6}, {ingresar, 24}};
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        for (int i = 0; i < filas.length; i++) {
            g.gridy = i;
            g.insets = new Insets((Integer) filas[i][1], 0, 0, 0);
            if (filas[i][0] instanceof JTextField || filas[i][0] instanceof JButton) g.ipady = 10;
            else g.ipady = 0;
            form.add((Component) filas[i][0], g);
        }
        form.setPreferredSize(new Dimension(340, form.getPreferredSize().height));
        der.add(form);

        raiz.add(izq);
        raiz.add(der);
        setContentPane(raiz);

        ingresar.addActionListener(e -> entrar());
        txtClave.addActionListener(e -> entrar());
        getRootPane().setDefaultButton(ingresar);
    }

    private static JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(UI.fuente(Font.BOLD, 13));
        l.setForeground(UI.TEXTO_SECUNDARIO);
        return l;
    }

    private void entrar() {
        String usuario = txtUsuario.getText().trim();
        String password = new String(txtClave.getPassword());

        if (usuario.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, complete todos los campos.",
                    "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            switch (controlador.iniciarSesion(usuario, password)) {
                case CORRECTO:
                    new Principal().setVisible(true);
                    dispose();
                    break;
                case DESACTIVADO:
                    JOptionPane.showMessageDialog(this, "El usuario está desactivado. Consulte con el administrador.",
                            "Acceso denegado", JOptionPane.WARNING_MESSAGE);
                    break;
                default:
                    JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.",
                            "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ConexionBD.mensajeError(ex),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        UI.instalarTema();
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}