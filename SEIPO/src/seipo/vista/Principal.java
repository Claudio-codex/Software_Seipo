package seipo.vista;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class Principal extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel centro = new JPanel(cards);
    private final PanelVentas ventas = new PanelVentas();
    private final PanelProductos productos = new PanelProductos();
    private final PanelInventario inventario = new PanelInventario();
    private final PanelClientes clientes = new PanelClientes();
    private final PanelReportes reportes = new PanelReportes();

    public Principal() {
        setTitle("SEIPO");
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

        centro.add(ventas, "Ventas");
        centro.add(productos, "Productos");
        centro.add(inventario, "Inventario");
        centro.add(clientes, "Clientes");
        centro.add(reportes, "Reportes");

        for (String n : new String[]{"Ventas", "Productos", "Inventario", "Clientes", "Reportes"}) {
            JButton b = new JButton(n);
            b.setFont(new Font("SansSerif", Font.BOLD, 15));
            b.setForeground(Color.WHITE);
            b.setBackground(UI.AZUL);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setAlignmentX(LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(160, 40));
            b.addActionListener(e -> mostrar(n));
            menu.add(b);
            menu.add(Box.createVerticalStrut(6));
        }

        add(menu, BorderLayout.WEST);
        add(centro, BorderLayout.CENTER);
        mostrar("Ventas");
    }

    private void mostrar(String nombre) {
        switch (nombre) {
            case "Productos": productos.refrescar(); break;
            case "Inventario": inventario.refrescar(); break;
            case "Clientes": clientes.refrescar(); break;
            case "Reportes": reportes.refrescar(); break;
            default: break;
        }
        cards.show(centro, nombre);
    }
}