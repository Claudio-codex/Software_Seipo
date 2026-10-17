package seipo.vista;

import seipo.controlador.ControladorLogin;
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
        setSize(800, 450);
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new GridLayout(1, 2));

        JPanel izq = new JPanel(new GridBagLayout());
        izq.setBackground(UI.AZUL);
        JLabel logo = new JLabel("Seipo");
        logo.setFont(new Font("SansSerif", Font.BOLD, 64));
        logo.setForeground(Color.WHITE);
        izq.add(logo);

        JPanel der = new JPanel(new GridBagLayout());
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel titulo = new JLabel("Iniciar Sesión");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        JButton ingresar = UI.boton("Ingresar");
        form.add(titulo);
        form.add(new JLabel("Usuario"));
        form.add(txtUsuario);
        form.add(new JLabel("Contraseña"));
        form.add(txtClave);
        form.add(ingresar);
        der.add(form);

        raiz.add(izq);
        raiz.add(der);
        setContentPane(raiz);

        ingresar.addActionListener(e -> entrar());
        txtClave.addActionListener(e -> entrar());
        getRootPane().setDefaultButton(ingresar);
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
            if (controlador.validarUsuario(usuario, password)) {
                new Principal().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión con la base de datos:\n" + ex.getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ex) { }
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}