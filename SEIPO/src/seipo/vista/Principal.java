package seipo.vista;

import seipo.controlador.ControladorCompras;
import seipo.controlador.ControladorMermas;
import seipo.modelo.Datos;
import seipo.modelo.Sesion;
import java.awt.*;
import java.sql.SQLException;
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
    private final PanelCompras compras = new PanelCompras(this::actualizarAviso);
    private final PanelClientes clientes = new PanelClientes();
    private JButton btnCompras;
    // Revisión periódica de alertas de stock mientras la aplicación está abierta (proceso SYS-03)
    private final Timer revisionAlertas = new Timer(Datos.MINUTOS_REVISION_ALERTAS * 60_000, e -> actualizarAviso());
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

        List<String> secciones = new ArrayList<>(List.of("Ventas", "Productos", "Inventario", "Mermas", "Compras", "Clientes"));
        centro.add(ventas, "Ventas");
        centro.add(productos, "Productos");
        centro.add(inventario, "Inventario");
        centro.add(mermas, "Mermas");
        centro.add(compras, "Compras");
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
            if (n.equals("Compras")) btnCompras = b;
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
        revisionAlertas.start();
        if (Sesion.esAdministrador()) SwingUtilities.invokeLater(this::avisoAlIngresar);
    }

    /** Muestra en el botón Compras cuántos productos necesitan reposición. */
    private void actualizarAviso() {
        try {
            long reponer = new ControladorCompras().listarAlertas().stream().filter(ControladorCompras.AlertaVO::esReposicion).count();
            btnCompras.setText(reponer == 0 ? "Compras" : "Compras (" + reponer + ")");
            btnCompras.setToolTipText(reponer == 0 ? null : reponer + " producto(s) agotados o con stock bajo");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /** Resumen para el administrador al iniciar sesión: reposición pendiente y aprobaciones por hacer. */
    private void avisoAlIngresar() {
        try {
            ControladorCompras cc = new ControladorCompras();
            long agotados = 0, bajos = 0, sobrestock = 0;
            for (ControladorCompras.AlertaVO a : cc.listarAlertas()) {
                if (ControladorCompras.AGOTADO.equals(a.estado)) agotados++;
                else if (ControladorCompras.STOCK_BAJO.equals(a.estado)) bajos++;
                else sobrestock++;
            }
            int ordenes = cc.contarOrdenesPendientes();
            int mermasPend = new ControladorMermas().listarMermas(ControladorMermas.PENDIENTE).size();
            if (agotados + bajos + sobrestock + ordenes + mermasPend == 0) return;
            StringBuilder sb = new StringBuilder("Alertas del inventario:\n\n");
            if (agotados > 0) sb.append("• ").append(agotados).append(" producto(s) agotado(s)\n");
            if (bajos > 0) sb.append("• ").append(bajos).append(" producto(s) con stock bajo\n");
            if (sobrestock > 0) sb.append("• ").append(sobrestock).append(" producto(s) con sobrestock\n");
            if (ordenes > 0) sb.append("• ").append(ordenes).append(" orden(es) de compra por aprobar\n");
            if (mermasPend > 0) sb.append("• ").append(mermasPend).append(" merma(s) por autorizar\n");
            sb.append("\nRevise las secciones Compras y Mermas.");
            JOptionPane.showMessageDialog(this, sb.toString(), "Alertas", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
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
            case "Compras": compras.refrescar(); break;
            case "Clientes": clientes.refrescar(); break;
            case "Reportes": reportes.refrescar(); break;
            case "Usuarios": usuarios.refrescar(); break;
            default: break;
        }
        cards.show(centro, nombre);
        actualizarAviso();
    }

    private void cerrarSesion() {
        if (JOptionPane.showConfirmDialog(this, "¿Desea cerrar la sesión?", "Cerrar sesión",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        revisionAlertas.stop();
        Sesion.cerrar();
        dispose();
        new Login().setVisible(true);
    }
}
