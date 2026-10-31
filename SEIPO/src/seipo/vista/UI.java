package seipo.vista;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/** Tema visual (claro, FlatLaf), paleta de colores y utilidades visuales compartidas. Ningún otro archivo de la vista define colores. */
public class UI {

    // Paleta: fondos claros neutros, acento naranja y textos oscuros
    public static final Color FONDO = new Color(0xF7F7F8);          // fondo de las pantallas
    public static final Color MENU = new Color(0xF5F5F6);           // barra lateral
    public static final Color SUPERFICIE = new Color(0xFFFFFF);     // tarjetas, tablas y campos
    public static final Color SUPERFICIE_ALTA = new Color(0xF1F2F4); // encabezado de tablas y botón secundario
    public static final Color BORDE = new Color(0xECECEE);          // borde de tarjetas y separadores
    public static final Color BORDE_CAMPO = new Color(0xE0E0E3);    // borde de campos de texto
    public static final Color LINEA_FILA = new Color(0xEDEEF0);     // línea entre filas de las tablas
    public static final Color TEXTO = new Color(0x1F2328);
    public static final Color TEXTO_SECUNDARIO = new Color(0x6B7280);
    public static final Color TEXTO_ENCABEZADO = new Color(0x374151);
    public static final Color ACENTO = new Color(0xFF5A36);
    public static final Color ACENTO_HOVER = new Color(0xE84A27);
    public static final Color ACENTO_PULSADO = new Color(0xD13F1F);
    public static final Color ACENTO_SUAVE = new Color(0xFFE9E3);   // fila seleccionada y hover del botón de quitar
    public static final Color NARANJA_CLARO = new Color(0xFFDDD2);  // insignias de alerta leve
    public static final Color GRIS_INSIGNIA = new Color(0x9CA3AF);  // insignias de estado normal
    public static final Color ICONO_FONDO = new Color(0xE5E7EB);    // cuadro de icono de los indicadores secundarios
    public static final Color HOVER_NEUTRO = new Color(0xE5E7EB);
    public static final Color PULSADO_NEUTRO = new Color(0xD1D5DB);
    public static final Color SOBRE_ACENTO = Color.WHITE;           // texto e iconos sobre el acento
    public static final Color SOMBRA = new Color(16, 24, 40, 28);   // sombra muy suave (tarjetas y tooltips)
    /** Colores de las series de los gráficos, en orden; los tres últimos son tonos de la misma familia para más de 5 series. */
    public static final Color[] GRAFICOS = {
            ACENTO, GRIS_INSIGNIA, new Color(0xFFA08A), new Color(0xF4511E), new Color(0x5B6068),
            new Color(0xFFC7B8), PULSADO_NEUTRO, TEXTO_ENCABEZADO
    };

    private static final String FAMILIA = familiaDisponible("Segoe UI", "Inter", "SansSerif");
    public static final Font NORMAL = fuente(Font.PLAIN, 14);
    public static final Font NEGRITA = fuente(Font.BOLD, 14);

    /** Radio de las esquinas de tarjetas y botones (diámetro del arco, como en FlatLaf). */
    static final int ARCO = 12;

    /** Instala FlatLaf claro con la paleta de SEIPO. Debe llamarse antes de crear cualquier ventana. */
    public static void instalarTema() {
        Map<String, String> v = new HashMap<>();
        v.put("@background", hex(FONDO));
        v.put("@foreground", hex(TEXTO));
        v.put("@componentBackground", hex(SUPERFICIE));
        v.put("@buttonBackground", hex(SUPERFICIE_ALTA));
        v.put("@accentColor", hex(ACENTO));
        v.put("@selectionBackground", hex(ACENTO_SUAVE));
        v.put("@selectionForeground", hex(TEXTO));
        v.put("@selectionInactiveBackground", hex(ACENTO_SUAVE));
        v.put("@selectionInactiveForeground", hex(TEXTO));
        v.put("@disabledForeground", hex(GRIS_INSIGNIA));
        FlatLaf.setGlobalExtraDefaults(v);
        FlatLightLaf.setup();

        UIManager.put("defaultFont", NORMAL);
        UIManager.put("Component.arc", ARCO);
        UIManager.put("Button.arc", ARCO);
        UIManager.put("TextComponent.arc", ARCO);
        UIManager.put("CheckBox.arc", 6);
        // Campos de al menos 32 px de alto y botones de diálogo (Aceptar, Cancelar…) de al menos 40 px
        Insets margenCampo = new Insets(5, 8, 5, 8);
        for (String k : new String[]{"TextField.margin", "PasswordField.margin", "FormattedTextField.margin", "Spinner.padding"}) UIManager.put(k, margenCampo);
        UIManager.put("Button.margin", new Insets(9, 16, 9, 16));
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.focusColor", ACENTO_SUAVE);
        UIManager.put("Component.borderColor", BORDE_CAMPO);
        UIManager.put("Component.disabledBorderColor", BORDE);
        UIManager.put("Component.focusedBorderColor", ACENTO);
        UIManager.put("Component.arrowType", "chevron");
        UIManager.put("ComboBox.buttonArrowColor", TEXTO_SECUNDARIO);
        UIManager.put("ComboBox.buttonBackground", SUPERFICIE);
        UIManager.put("Spinner.buttonBackground", SUPERFICIE);
        UIManager.put("CheckBox.icon.selectedBackground", ACENTO);
        UIManager.put("CheckBox.icon.selectedBorderColor", ACENTO);
        UIManager.put("CheckBox.icon.checkmarkColor", SOBRE_ACENTO);
        UIManager.put("CheckBox.icon.focusedBorderColor", ACENTO);

        // Botones normales (los de los cuadros de diálogo); el botón por defecto lleva el acento
        UIManager.put("Button.background", SUPERFICIE_ALTA);
        UIManager.put("Button.foreground", TEXTO);
        UIManager.put("Button.borderColor", SUPERFICIE_ALTA);
        UIManager.put("Button.focusedBackground", SUPERFICIE_ALTA);
        UIManager.put("Button.hoverBackground", HOVER_NEUTRO);
        UIManager.put("Button.pressedBackground", PULSADO_NEUTRO);
        UIManager.put("Button.focusedBorderColor", ACENTO);
        UIManager.put("Button.default.background", ACENTO);
        UIManager.put("Button.default.foreground", SOBRE_ACENTO);
        UIManager.put("Button.default.borderColor", ACENTO);
        UIManager.put("Button.default.focusedBackground", ACENTO);
        UIManager.put("Button.default.hoverBackground", ACENTO_HOVER);
        UIManager.put("Button.default.pressedBackground", ACENTO_PULSADO);
        UIManager.put("Button.default.focusedBorderColor", ACENTO_PULSADO);
        UIManager.put("Button.default.boldText", true);

        // Barras de desplazamiento delgadas, sin flechas
        UIManager.put("ScrollBar.width", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(1, 1, 1, 1));
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.track", SUPERFICIE);
        UIManager.put("ScrollBar.thumb", PULSADO_NEUTRO);
        UIManager.put("ScrollBar.hoverThumbColor", GRIS_INSIGNIA);
        UIManager.put("ScrollBar.pressedThumbColor", TEXTO_SECUNDARIO);
        // Cada tabla o lista desplazable se ve como una tarjeta blanca
        UIManager.put("ScrollPane.border", new BordeTarjeta());
        UIManager.put("ScrollPane.background", SUPERFICIE);
        UIManager.put("Viewport.background", SUPERFICIE);

        // Tablas: solo líneas horizontales finas, encabezado gris claro y selección con un tinte del acento
        UIManager.put("Table.background", SUPERFICIE);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor", LINEA_FILA);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("Table.cellMargins", new Insets(2, 10, 2, 10));
        UIManager.put("Table.selectionBackground", ACENTO_SUAVE);
        UIManager.put("Table.selectionForeground", TEXTO);
        UIManager.put("Table.selectionInactiveBackground", ACENTO_SUAVE);
        UIManager.put("Table.selectionInactiveForeground", TEXTO);
        UIManager.put("Table.cellFocusColor", ACENTO_SUAVE);
        UIManager.put("TableHeader.background", SUPERFICIE_ALTA);
        UIManager.put("TableHeader.foreground", TEXTO_ENCABEZADO);
        UIManager.put("TableHeader.separatorColor", SUPERFICIE_ALTA);
        UIManager.put("TableHeader.bottomSeparatorColor", LINEA_FILA);
        UIManager.put("TableHeader.cellMargins", new Insets(2, 10, 2, 10));
        UIManager.put("List.selectionBackground", ACENTO_SUAVE);
        UIManager.put("List.selectionForeground", TEXTO);

        UIManager.put("TabbedPane.background", FONDO);
        UIManager.put("TabbedPane.selectedBackground", FONDO);
        UIManager.put("TabbedPane.selectedForeground", ACENTO_PULSADO);
        UIManager.put("TabbedPane.underlineColor", ACENTO);
        UIManager.put("TabbedPane.inactiveUnderlineColor", ACENTO);
        UIManager.put("TabbedPane.contentAreaColor", BORDE);
        UIManager.put("TabbedPane.tabHeight", 38);
        UIManager.put("TabbedPane.hoverColor", SUPERFICIE_ALTA);
        UIManager.put("TabbedPane.focusColor", FONDO);
        UIManager.put("TitlePane.background", SUPERFICIE);
        UIManager.put("ToolTip.background", SUPERFICIE);
        UIManager.put("ToolTip.foreground", TEXTO);
        UIManager.put("ToolTip.border", new CompoundBorder(new LineBorder(BORDE_CAMPO), new EmptyBorder(4, 8, 4, 8)));
        UIManager.put("OptionPane.background", SUPERFICIE);
        UIManager.put("Panel.background", FONDO);
        UIManager.put("Separator.foreground", BORDE);
        UIManager.put("ProgressBar.foreground", ACENTO);
        UIManager.put("PopupMenu.borderColor", BORDE_CAMPO);
        UIManager.put("MenuItem.selectionBackground", ACENTO_SUAVE);
        UIManager.put("MenuItem.selectionForeground", TEXTO);

        // Botones de los cuadros de diálogo en español (Login fija Locale.US para el formato de números)
        UIManager.put("OptionPane.okButtonText", "Aceptar");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");
        UIManager.put("OptionPane.yesButtonText", "Sí");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("FileChooser.saveButtonText", "Guardar");
        UIManager.put("FileChooser.openButtonText", "Abrir");
        UIManager.put("FileChooser.cancelButtonText", "Cancelar");
        UIManager.put("FileChooser.saveInLabelText", "Guardar en:");
        UIManager.put("FileChooser.lookInLabelText", "Buscar en:");
        UIManager.put("FileChooser.fileNameLabelText", "Nombre de archivo:");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Tipo de archivo:");
        // Títulos de los diálogos estándar y el resto del selector de archivos (Login.main deja Locale.US para los números,
        // así que Java mostraría estos textos en inglés)
        UIManager.put("OptionPane.titleText", "Seleccione una opción");
        UIManager.put("OptionPane.messageDialogTitle", "Mensaje");
        UIManager.put("OptionPane.inputDialogTitle", "Entrada");
        String[][] selector = {
                {"openDialogTitleText", "Abrir"}, {"saveDialogTitleText", "Guardar"},
                {"directoryOpenButtonText", "Abrir"}, {"updateButtonText", "Actualizar"}, {"helpButtonText", "Ayuda"},
                {"openButtonToolTipText", "Abrir el archivo seleccionado"}, {"saveButtonToolTipText", "Guardar el archivo seleccionado"},
                {"cancelButtonToolTipText", "Cerrar sin elegir un archivo"}, {"directoryOpenButtonToolTipText", "Abrir la carpeta seleccionada"},
                {"updateButtonToolTipText", "Actualizar la lista"}, {"helpButtonToolTipText", "Ayuda del selector de archivos"},
                {"acceptAllFileFilterText", "Todos los archivos"}, {"fileDescriptionText", "Archivo"}, {"directoryDescriptionText", "Carpeta"},
                {"folderNameLabelText", "Nombre de la carpeta:"},
                {"upFolderToolTipText", "Subir un nivel"}, {"upFolderAccessibleName", "Subir"},
                {"homeFolderToolTipText", "Inicio"}, {"homeFolderAccessibleName", "Inicio"},
                {"newFolderToolTipText", "Crear una carpeta"}, {"newFolderAccessibleName", "Nueva carpeta"},
                {"listViewButtonToolTipText", "Lista"}, {"listViewButtonAccessibleName", "Lista"},
                {"detailsViewButtonToolTipText", "Detalles"}, {"detailsViewButtonAccessibleName", "Detalles"},
                {"viewMenuLabelText", "Ver"}, {"refreshActionLabelText", "Actualizar"}, {"newFolderActionLabelText", "Nueva carpeta"},
                {"listViewActionLabelText", "Lista"}, {"detailsViewActionLabelText", "Detalles"},
                {"fileNameHeaderText", "Nombre"}, {"fileSizeHeaderText", "Tamaño"}, {"fileTypeHeaderText", "Tipo"},
                {"fileDateHeaderText", "Modificado"}, {"fileAttrHeaderText", "Atributos"},
                {"win32.newFolder", "Nueva carpeta"}, {"win32.newFolder.subsequent", "Nueva carpeta ({0})"},
                {"other.newFolder", "NuevaCarpeta"}, {"other.newFolder.subsequent", "NuevaCarpeta.{0}"},
                {"newFolderErrorText", "No se pudo crear la carpeta"},
                {"newFolderParentDoesntExistTitleText", "No se pudo crear la carpeta"},
                {"newFolderParentDoesntExistText", "No se pudo crear la carpeta.\n\nNo se encuentra la ruta indicada."},
                {"renameErrorTitleText", "Error al cambiar el nombre"}, {"renameErrorText", "No se puede cambiar el nombre de {0}"},
                {"renameErrorFileExistsText", "No se puede cambiar el nombre de {0}: ya existe un archivo con ese nombre. Escriba otro nombre."}
        };
        for (String[] t : selector) UIManager.put("FileChooser." + t[0], t[1]);
    }

    public static Font fuente(int estilo, int tamano) {
        return new Font(FAMILIA, estilo, tamano);
    }

    /** Botón principal: fondo de acento y texto blanco. */
    public static JButton boton(String texto) {
        return crearBoton(texto, ACENTO, ACENTO_HOVER, ACENTO_PULSADO, SOBRE_ACENTO);
    }

    /** Botón para acciones secundarias: fondo gris claro. */
    public static JButton botonSecundario(String texto) {
        return crearBoton(texto, SUPERFICIE_ALTA, HOVER_NEUTRO, PULSADO_NEUTRO, TEXTO);
    }

    /** Botón para acciones que rechazan o quitan algo: fondo gris claro, texto e icono "x" en el acento. */
    public static JButton botonPeligro(String texto) {
        JButton b = crearBoton(texto, SUPERFICIE_ALTA, ACENTO_SUAVE, NARANJA_CLARO, ACENTO_PULSADO);
        b.setIcon(Iconos.de(Iconos.Tipo.CERRAR, 14, ACENTO));
        b.setIconTextGap(6);
        return b;
    }

    private static JButton crearBoton(String texto, Color fondo, Color hover, Color pulsado, Color letra) {
        // Ningún botón de la aplicación mide menos de ALTO_MINIMO_BOTON, aunque se le reduzca el margen o el borde
        JButton b = new JButton(texto) {
            @Override public Dimension getPreferredSize() { return conAltoMinimo(super.getPreferredSize()); }
            @Override public Dimension getMinimumSize() { return conAltoMinimo(super.getMinimumSize()); }
        };
        b.setFont(NEGRITA);
        b.setBackground(fondo);
        b.setForeground(letra);
        b.setFocusPainted(false);
        b.setMargin(new Insets(7, 16, 7, 16));
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.putClientProperty("FlatLaf.style", "focusedBackground: " + hex(fondo) + "; hoverBackground: " + hex(hover) + "; pressedBackground: " + hex(pulsado)
                + "; borderColor: " + hex(fondo) + "; hoverBorderColor: " + hex(hover) + "; focusedBorderColor: " + hex(ACENTO_PULSADO)
                + "; focusWidth: 0; innerFocusWidth: 0; arc: " + ARCO);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Alto mínimo de los botones (para tocarlos y leerlos con comodidad). */
    static final int ALTO_MINIMO_BOTON = 40;

    private static Dimension conAltoMinimo(Dimension d) {
        d.height = Math.max(d.height, ALTO_MINIMO_BOTON);
        return d;
    }

    public static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    /** Ancho máximo que una columna toma por su contenido; un texto más largo se lee completo en el tooltip. */
    private static final int ANCHO_MAXIMO_COLUMNA = 600;
    /** Filas que se miden para ajustar el ancho de las columnas. */
    private static final int FILAS_MEDIDAS = 300;

    /**
     * Tabla de solo lectura con el estilo de SEIPO. Cada columna se ajusta al ancho de su encabezado y de su contenido,
     * así ningún texto queda cortado: si todas caben, la tabla ocupa todo el ancho; si no, aparece el desplazamiento
     * horizontal. Una celda que aun así no entre muestra su texto completo en el tooltip.
     */
    public static JTable tabla(DefaultTableModel m) {
        JTable t = new JTable(m) {
            @Override public boolean getScrollableTracksViewportWidth() {
                return getParent() == null || getPreferredSize().width <= getParent().getWidth();
            }

            @Override public String getToolTipText(java.awt.event.MouseEvent e) {
                int f = rowAtPoint(e.getPoint()), c = columnAtPoint(e.getPoint());
                if (f < 0 || c < 0 || getValueAt(f, c) == null) return null;
                Component r = prepareRenderer(getCellRenderer(f, c), f, c);
                return r.getPreferredSize().width > getColumnModel().getColumn(c).getWidth() ? getValueAt(f, c).toString() : null;
            }
        };
        t.setRowHeight(34);
        t.setFont(NORMAL);
        t.setFillsViewportHeight(true);
        JTableHeader h = t.getTableHeader();
        h.setFont(fuente(Font.BOLD, 14));
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, 36));
        h.setReorderingAllowed(false);
        ToolTipManager.sharedInstance().registerComponent(t);
        // Se reajusta al cambiar los datos; los cambios seguidos (una fila tras otra) se agrupan en un solo ajuste
        Timer ajuste = new Timer(40, e -> ajustarColumnas(t));
        ajuste.setRepeats(false);
        m.addTableModelListener(e -> ajuste.restart());
        ajuste.start();
        return t;
    }

    /** Ancho preferido de cada columna = lo que ocupan su encabezado y sus celdas (sin pasar del máximo). */
    private static void ajustarColumnas(JTable t) {
        javax.swing.table.TableColumnModel cm = t.getColumnModel();
        int filas = Math.min(t.getRowCount(), FILAS_MEDIDAS);
        for (int c = 0; c < cm.getColumnCount(); c++) {
            javax.swing.table.TableColumn col = cm.getColumn(c);
            javax.swing.table.TableCellRenderer rh = col.getHeaderRenderer() != null ? col.getHeaderRenderer() : t.getTableHeader().getDefaultRenderer();
            int ancho = rh.getTableCellRendererComponent(t, col.getHeaderValue(), false, false, -1, c).getPreferredSize().width;
            for (int f = 0; f < filas; f++) ancho = Math.max(ancho, t.prepareRenderer(t.getCellRenderer(f, c), f, c).getPreferredSize().width);
            col.setPreferredWidth(Math.max(col.getMinWidth(), Math.min(ancho + 4, ANCHO_MAXIMO_COLUMNA)));
        }
        t.revalidate();
    }

    /** Ancho inicial de una columna, antes de cargar datos; luego cada columna se ajusta a su contenido. */
    public static void columnaAncha(JTable t, int columna, int ancho) {
        t.getColumnModel().getColumn(columna).setPreferredWidth(ancho);
    }

    // ---------- Insignias de estado ----------

    /** Nivel de alerta de un estado: fuerte (acento sólido), leve (naranja claro) o normal (gris). */
    enum Nivel { FUERTE, LEVE, NORMAL }

    static Nivel nivelEstado(String estado) {
        switch (estado == null ? "" : estado.trim().toLowerCase(Locale.forLanguageTag("es"))) {
            case "agotado": case "vencido": case "rechazada": case "anulada":
                return Nivel.FUERTE;
            case "stock bajo": case "pendiente": case "sobrestock": case "por vencer":
                return Nivel.LEVE;
            default:
                return Nivel.NORMAL;
        }
    }

    /** Muestra la columna de estado como insignias tipo píldora con el color de su nivel de alerta. */
    public static void insignias(JTable t, int columna) {
        javax.swing.table.TableColumn c = t.getColumnModel().getColumn(columna);
        c.setCellRenderer(new Insignia());
        // Ancho para la píldora más larga ("Sobrestock", "Por vencer"), así no se recorta
        c.setMinWidth(112);
        c.setPreferredWidth(Math.max(c.getPreferredWidth(), 112));
    }

    /** Pinta en el acento y en negrita el texto de una columna cuando cumple la condición (por ejemplo, un faltante). */
    public static void resaltar(JTable t, int columna, Predicate<String> condicion) {
        t.getColumnModel().getColumn(columna).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tabla, Object v, boolean sel, boolean foco, int fila, int col) {
                Component c = super.getTableCellRendererComponent(tabla, v, sel, foco, fila, col);
                boolean alerta = v != null && condicion.test(v.toString());
                c.setForeground(alerta ? ACENTO_PULSADO : TEXTO);
                c.setFont(alerta ? NEGRITA : NORMAL);
                return c;
            }
        });
    }

    /** Celda con el texto dentro de una píldora redondeada. */
    private static class Insignia extends DefaultTableCellRenderer {
        private Nivel nivel = Nivel.NORMAL;

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object v, boolean sel, boolean foco, int fila, int col) {
            super.getTableCellRendererComponent(tabla, v, sel, false, fila, col);
            nivel = nivelEstado(v == null ? null : v.toString());
            setFont(fuente(Font.BOLD, 12));
            return this;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            String texto = getText();
            if (texto != null && !texto.isEmpty()) {
                g.setFont(getFont());
                FontMetrics fm = g.getFontMetrics();
                int alto = 22, ancho = fm.stringWidth(texto) + 18;
                int x = getInsets().left, y = (getHeight() - alto) / 2;
                g.setColor(nivel == Nivel.FUERTE ? ACENTO : nivel == Nivel.LEVE ? NARANJA_CLARO : GRIS_INSIGNIA);
                g.fill(new RoundRectangle2D.Float(x, y, ancho, alto, alto, alto));
                g.setColor(nivel == Nivel.LEVE ? ACENTO_PULSADO : SOBRE_ACENTO);
                g.drawString(texto, x + 9, y + (alto + fm.getAscent() - fm.getDescent()) / 2);
            }
            g.dispose();
        }
    }

    // ---------- Tarjetas ----------

    /** Borde de tarjeta: esquinas redondeadas, línea fina y una sombra muy suave abajo. */
    public static Border bordeTarjeta(int arriba, int izquierda, int abajo, int derecha) {
        return new CompoundBorder(new BordeTarjeta(), new EmptyBorder(arriba, izquierda, abajo, derecha));
    }

    /** Deja un panel como tarjeta blanca con el relleno indicado. */
    public static <T extends JComponent> T comoTarjeta(T panel, int relleno) {
        panel.setOpaque(true);
        panel.setBackground(SUPERFICIE);
        panel.setBorder(bordeTarjeta(relleno, relleno + 4, relleno, relleno + 4));
        return panel;
    }

    /**
     * Pinta el contorno redondeado sobre un componente opaco: rellena las esquinas y el margen de la sombra con el
     * fondo de la pantalla, así cualquier panel blanco o tabla desplazable se ve como tarjeta sin cambiar su clase.
     */
    static class BordeTarjeta extends AbstractBorder {
        private static final int MARGEN_SOMBRA = 2;

        @Override public Insets getBorderInsets(Component c) { return new Insets(3, 3, 3 + MARGEN_SOMBRA, 3); }

        @Override public Insets getBorderInsets(Component c, Insets i) {
            i.top = 3; i.left = 3; i.right = 3; i.bottom = 3 + MARGEN_SOMBRA;
            return i;
        }

        @Override public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float alto = h - MARGEN_SOMBRA - 1f;
            Shape tarjeta = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1f, alto, ARCO, ARCO);
            Area fuera = new Area(new Rectangle(x, y, w, h));
            fuera.subtract(new Area(tarjeta));
            g.setColor(fondoDetras(c));
            g.fill(fuera);
            // Sombra: dos contornos desplazados hacia abajo, casi transparentes, recortados para que solo asomen por fuera
            Shape recorte = g.getClip();
            g.clip(fuera);
            for (int i = MARGEN_SOMBRA; i >= 1; i--) {
                g.setColor(new Color(UI.SOMBRA.getRed(), UI.SOMBRA.getGreen(), UI.SOMBRA.getBlue(), 10 + (MARGEN_SOMBRA - i) * 6));
                g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f + i, w - 1f, alto, ARCO, ARCO));
            }
            g.setClip(recorte);
            g.setColor(BORDE);
            g.draw(tarjeta);
            g.dispose();
        }

        /** Color del contenedor sobre el que está la tarjeta (normalmente el fondo de la pantalla). */
        private static Color fondoDetras(Component c) {
            for (Container p = c.getParent(); p != null; p = p.getParent()) {
                if (p.isOpaque() && p.getBackground() != null) return p.getBackground();
            }
            return FONDO;
        }
    }

    /** Tarjeta de indicador sin icono (se mantiene por compatibilidad). */
    public static JPanel tarjeta(String titulo, JLabel valor) {
        return tarjeta(titulo, valor, null, false, null);
    }

    public static JPanel tarjeta(String titulo, JLabel valor, Iconos.Tipo icono, boolean principal) {
        return tarjeta(titulo, valor, icono, principal, null);
    }

    /**
     * Tarjeta de indicador: cuadro de icono redondeado a la izquierda (acento en el principal, gris en los demás),
     * título pequeño gris y valor grande en negrita. {@code extra} va debajo del valor (por ejemplo, la variación).
     */
    public static JPanel tarjeta(String titulo, JLabel valor, Iconos.Tipo icono, boolean principal, JComponent extra) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(SUPERFICIE);
        p.setBorder(bordeTarjeta(12, 12, 12, 12));
        if (icono != null) {
            JPanel cuadro = new CuadroIcono(Iconos.de(icono, 20, principal ? SOBRE_ACENTO : TEXTO_SECUNDARIO),
                    principal ? ACENTO : ICONO_FONDO);
            JPanel arriba = new JPanel(new BorderLayout());
            arriba.setOpaque(false);
            arriba.add(cuadro, BorderLayout.NORTH);
            p.add(arriba, BorderLayout.WEST);
        }
        JPanel textos = new JPanel(new GridLayout(0, 1, 0, 2));
        textos.setOpaque(false);
        JLabel t = new JLabel(titulo);
        t.setToolTipText(titulo);
        t.setFont(fuente(Font.PLAIN, 13));
        t.setForeground(TEXTO_SECUNDARIO);
        valor.setFont(fuente(Font.BOLD, 24));
        valor.setForeground(TEXTO);
        textos.add(t);
        textos.add(valor);
        if (extra != null) textos.add(extra);
        p.add(textos, BorderLayout.CENTER);
        return p;
    }

    /** Cuadro redondeado de 40 px con un icono centrado. */
    private static class CuadroIcono extends JPanel {
        private final Icon icono;
        private final Color fondo;

        CuadroIcono(Icon icono, Color fondo) {
            this.icono = icono;
            this.fondo = fondo;
            setOpaque(false);
            setPreferredSize(new Dimension(40, 40));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fondo);
            g.fillRoundRect(0, 0, 40, 40, ARCO, ARCO);
            icono.paintIcon(this, g, (40 - icono.getIconWidth()) / 2, (40 - icono.getIconHeight()) / 2);
            g.dispose();
        }
    }

    /** Texto de ayuda en gris, para explicar una pantalla o una regla. */
    public static JLabel ayuda(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(fuente(Font.PLAIN, 12));
        l.setForeground(TEXTO_SECUNDARIO);
        return l;
    }

    /** Panel raíz de cada pantalla: fondo gris claro con margen de 20 px y 16 px entre bloques. */
    public static JPanel base() {
        JPanel p = new JPanel(new BorderLayout(16, 16));
        p.setBorder(new EmptyBorder(20, 24, 20, 24));
        p.setBackground(FONDO);
        return p;
    }

    /** Color en formato #RRGGBB, para los estilos de FlatLaf y el HTML de las etiquetas. */
    public static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String familiaDisponible(String... familias) {
        String[] instaladas = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String f : familias) if (Arrays.asList(instaladas).contains(f)) return f;
        return Font.SANS_SERIF;
    }
}
