package seipo.vista;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/** Tema visual (oscuro, FlatLaf), paleta de colores y utilidades visuales compartidas. */
public class UI {

    // Paleta: fondos pizarra azulados, acento turquesa y textos claros con contraste alto (WCAG AA o superior)
    public static final Color FONDO = new Color(0x0F172A);        // fondo de las pantallas
    public static final Color MENU = new Color(0x0B1120);         // menú lateral
    public static final Color SUPERFICIE = new Color(0x1E293B);   // tarjetas, tablas y campos
    public static final Color SUPERFICIE_ALTA = new Color(0x273449); // filas alternas, hover, tooltips
    public static final Color BORDE = new Color(0x334155);
    public static final Color TEXTO = new Color(0xE2E8F0);
    public static final Color TEXTO_SECUNDARIO = new Color(0x94A3B8);
    public static final Color ACENTO = new Color(0x2DD4BF);        // turquesa
    public static final Color ACENTO_OSCURO = new Color(0x0F766E); // selección
    public static final Color TEXTO_SOBRE_ACENTO = new Color(0x042F2E);
    public static final Color EXITO = new Color(0x4ADE80), ALERTA = new Color(0xFBBF24), PELIGRO = new Color(0xF87171);
    /** Se mantiene por compatibilidad: es el color de acento. */
    public static final Color AZUL = ACENTO;

    // Fondos de fila según estado: tonos oscuros que conservan el contraste del texto claro
    public static final Color FILA_ROJA = new Color(0x3F1D25), FILA_AMBAR = new Color(0x3A2F14),
            FILA_VERDE = new Color(0x14352C), FILA_CELESTE = new Color(0x172E4D), FILA_GRIS = new Color(0x1A2333);

    private static final String FAMILIA = familiaDisponible("Segoe UI", "Inter", "SansSerif");
    public static final Font NORMAL = fuente(Font.PLAIN, 14);
    public static final Font NEGRITA = fuente(Font.BOLD, 14);

    /** Instala FlatLaf oscuro con la paleta de SEIPO. Debe llamarse antes de crear cualquier ventana. */
    public static void instalarTema() {
        Map<String, String> v = new HashMap<>();
        v.put("@background", hex(FONDO));
        v.put("@foreground", hex(TEXTO));
        v.put("@componentBackground", hex(SUPERFICIE));
        v.put("@buttonBackground", hex(BORDE));
        v.put("@accentColor", hex(ACENTO));
        v.put("@selectionBackground", hex(ACENTO_OSCURO));
        v.put("@selectionForeground", "#F0FDFA");
        v.put("@disabledForeground", "#64748B");
        FlatLaf.setGlobalExtraDefaults(v);
        FlatDarkLaf.setup();

        UIManager.put("defaultFont", NORMAL);
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.borderColor", BORDE);
        UIManager.put("Component.focusedBorderColor", ACENTO);
        UIManager.put("Button.borderColor", BORDE);
        UIManager.put("Button.hoverBackground", new Color(0x3B4A60));
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollPane.border", new LineBorder(BORDE));
        UIManager.put("Table.background", SUPERFICIE);
        UIManager.put("Table.alternateRowColor", SUPERFICIE_ALTA);
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 0));
        UIManager.put("Table.selectionBackground", ACENTO_OSCURO);
        UIManager.put("TableHeader.background", MENU);
        UIManager.put("TableHeader.foreground", TEXTO_SECUNDARIO);
        UIManager.put("TableHeader.separatorColor", BORDE);
        UIManager.put("TableHeader.bottomSeparatorColor", BORDE);
        UIManager.put("TabbedPane.selectedBackground", FONDO);
        UIManager.put("TabbedPane.underlineColor", ACENTO);
        UIManager.put("TabbedPane.inactiveUnderlineColor", ACENTO_OSCURO);
        UIManager.put("TabbedPane.tabHeight", 38);
        UIManager.put("TabbedPane.hoverColor", SUPERFICIE);
        UIManager.put("TitlePane.background", MENU);
        UIManager.put("ToolTip.background", SUPERFICIE_ALTA);
        UIManager.put("ToolTip.foreground", TEXTO);
        UIManager.put("OptionPane.background", SUPERFICIE);
        UIManager.put("Panel.background", FONDO);
    }

    public static Font fuente(int estilo, int tamano) {
        return new Font(FAMILIA, estilo, tamano);
    }

    /** Botón principal: fondo de acento y texto oscuro. */
    public static JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setFont(NEGRITA);
        b.setBackground(ACENTO);
        b.setForeground(TEXTO_SOBRE_ACENTO);
        b.setFocusPainted(false);
        b.setMargin(new Insets(7, 16, 7, 16));
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Botón para acciones secundarias: fondo neutro. */
    public static JButton botonSecundario(String texto) {
        JButton b = boton(texto);
        b.setBackground(BORDE);
        b.setForeground(TEXTO);
        return b;
    }

    /** Botón para acciones que rechazan o quitan algo. */
    public static JButton botonPeligro(String texto) {
        JButton b = boton(texto);
        b.setBackground(new Color(0x7F1D1D));
        b.setForeground(new Color(0xFEE2E2));
        return b;
    }

    public static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    public static JTable tabla(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(32);
        t.setFont(NORMAL);
        t.setFillsViewportHeight(true);
        JTableHeader h = t.getTableHeader();
        h.setFont(fuente(Font.BOLD, 13));
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, 34));
        h.setReorderingAllowed(false);
        return t;
    }

    /** Da más espacio a una columna (por ejemplo, el nombre del producto); las demás se reparten el resto. */
    public static void columnaAncha(JTable t, int columna, int ancho) {
        t.getColumnModel().getColumn(columna).setPreferredWidth(ancho);
    }

    /**
     * Colorea las filas que requieren atención (vencido, pendiente, etc.). fondoDeFila recibe la fila del
     * modelo y devuelve su color, o null para dejar el fondo normal de la tabla.
     */
    public static void colorearFilas(JTable t, IntFunction<Color> fondoDeFila) {
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tabla, Object v, boolean sel, boolean foco, int fila, int col) {
                Component c = super.getTableCellRendererComponent(tabla, v, sel, foco, fila, col);
                if (!sel) {
                    Color fondo = fondoDeFila.apply(tabla.convertRowIndexToModel(fila));
                    if (fondo != null) c.setBackground(fondo);
                }
                return c;
            }
        });
    }

    /** Tarjeta de indicador: título tenue arriba y valor grande en color de acento. */
    public static JPanel tarjeta(String titulo, JLabel valor) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(SUPERFICIE);
        p.setBorder(new CompoundBorder(new LineBorder(BORDE, 1, true), new EmptyBorder(12, 16, 12, 16)));
        JLabel t = new JLabel(titulo);
        t.setFont(fuente(Font.PLAIN, 13));
        t.setForeground(TEXTO_SECUNDARIO);
        valor.setFont(fuente(Font.BOLD, 26));
        valor.setForeground(ACENTO);
        p.add(t, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    /** Texto de ayuda en gris, para explicar una pantalla o una regla. */
    public static JLabel ayuda(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(fuente(Font.PLAIN, 12));
        l.setForeground(TEXTO_SECUNDARIO);
        return l;
    }

    /** Panel raíz de cada pantalla. */
    public static JPanel base() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBorder(new EmptyBorder(18, 22, 18, 22));
        p.setBackground(FONDO);
        return p;
    }

    private static String familiaDisponible(String... familias) {
        String[] instaladas = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String f : familias) if (Arrays.asList(instaladas).contains(f)) return f;
        return Font.SANS_SERIF;
    }

    private static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}
