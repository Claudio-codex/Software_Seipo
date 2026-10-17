package seipo.vista;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

/** Utilidades visuales compartidas. */
public class UI {
    public static final Color AZUL = new Color(33, 84, 160);
    public static final Color FONDO = new Color(245, 246, 248);
    public static final Font NORMAL = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font NEGRITA = new Font("SansSerif", Font.BOLD, 14);

    public static JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setFont(NEGRITA);
        b.setBackground(AZUL);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        return b;
    }

    public static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    public static JTable tabla(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(26);
        t.setFont(NORMAL);
        t.getTableHeader().setFont(NEGRITA);
        return t;
    }

    public static JPanel tarjeta(String titulo, JLabel valor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(Color.LIGHT_GRAY), new EmptyBorder(12, 16, 12, 16)));
        JLabel t = new JLabel(titulo);
        t.setFont(NORMAL);
        valor.setFont(new Font("SansSerif", Font.BOLD, 26));
        valor.setForeground(AZUL);
        p.add(t, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    public static JPanel base() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(15, 15, 15, 15));
        p.setBackground(FONDO);
        return p;
    }
}
