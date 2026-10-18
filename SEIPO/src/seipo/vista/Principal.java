package seipo.vista;

import seipo.modelo.Sesion;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

public class Principal extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel centro = new JPanel(cards);
    private final PanelVentas ventas = new PanelVentas();
    private final PanelProductos productos = new PanelProductos();
    private final PanelInventario inventario = new PanelInventario();
    private final PanelMermas mermas = new PanelMermas();
    private final PanelClientes clientes = new PanelClientes();
    // Solo se crean para el administrador
    private PanelReportes reportes;
    private PanelUsuarios usuarios;

    public Principal() {
        setTitle("SEIPO - " + Sesion.getNombre() + " (" + Sesion.textoRol(Sesion.getRol()) + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 620);
        setLocationRelativeTo(null);

        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(UI.AZUL);
        menu.setBorder(new EmptyBorder(15, 10, 15, 10));
        JLabel logo = new JLabel("SEIPO");
        logo.setFont(new Font("SansSerif", Font.BOLD, 28));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(LEFT_ALIGNMENT);
        menu.add(logo);
        menu.add(Box.createVerticalStrut(20));

        List<String> secciones = new ArrayList<>(List.of("Ventas", "Productos", "Inventario", "Mermas", "Clientes"));
        centro.add(ventas, "Ventas");
        centro.add(productos, "Productos");
        centro.add(inventario, "Inventario");
        centro.add(mermas, "Mermas");
        centro.add(clientes, "Clientes");
        if (Sesion.esAdministrador()) {
            reportes = new PanelReportes();
            usuarios = new PanelUsuarios();
            centro.add(reportes, "Reportes");
            centro.add(usuarios, "Usuarios");
            secciones.add("Reportes");
            secciones.add("Usuarios");
        }

        for (String n : secciones) {
            JButton b = botonMenu(n);
            b.addActionListener(e -> mostrar(n));
            menu.add(b);
            menu.add(Box.createVerticalStrut(6));
        }

        menu.add(Box.createVerticalGlue());
        JLabel lblUsuario = new JLabel("<html><b>" + Sesion.getNombre() + "</b><br>"
                + Sesion.textoRol(Sesion.getRol()) + "</html>");
        lblUsuario.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblUsuario.setForeground(Color.WHITE);
        lblUsuario.setAlignmentX(LEFT_ALIGNMENT);
        menu.add(lblUsuario);
        menu.add(Box.createVerticalStrut(6));
        JButton salir = botonMenu("Cerrar sesión");
        salir.setFont(new Font("SansSerif", Font.PLAIN, 13));
        salir.addActionListener(e -> cerrarSesion());
        menu.add(salir);

        add(menu, BorderLayout.WEST);
        add(centro, BorderLayout.CENTER);
        mostrar("Ventas");
    }

    private static JButton botonMenu(String texto) {
        JButton b = new JButton(texto);
        b.setFont(new Font("SansSerif", Font.BOLD, 15));
        b.setForeground(Color.WHITE);
        b.setBackground(UI.AZUL);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(160, 40));
        return b;
    }

    private void mostrar(String nombre) {
        switch (nombre) {
            case "Productos": productos.refrescar(); break;
            case "Inventario": inventario.refrescar(); break;
            case "Mermas": mermas.refrescar(); break;
            case "Clientes": clientes.refrescar(); break;
            case "Reportes": reportes.refrescar(); break;
            case "Usuarios": usuarios.refrescar(); break;
            default: break;
        }
        cards.show(centro, nombre);
    }

    private void cerrarSesion() {
        if (JOptionPane.showConfirmDialog(this, "¿Desea cerrar la sesión?", "Cerrar sesión",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Sesion.cerrar();
        dispose();
        new Login().setVisible(true);
    }
}
