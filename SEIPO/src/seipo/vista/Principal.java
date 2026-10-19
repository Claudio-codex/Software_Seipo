package seipo.vista;

import seipo.controlador.ControladorCaja;
import seipo.controlador.ControladorCompras;
import seipo.controlador.ControladorMermas;
import seipo.modelo.Datos;
import seipo.modelo.Sesion;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;

public class Principal extends JFrame {

    /** Sección del menú: a qué grupo pertenece y qué se hace en ella (se muestra en la cabecera). */
    private static class Seccion {
        final String grupo, descripcion;
        final JPanel panel;
        Seccion(String grupo, String descripcion, JPanel panel) {
            this.grupo = grupo;
            this.descripcion = descripcion;
            this.panel = panel;
        }
    }

    private static final DateTimeFormatter FECHA_CABECERA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "PE"));

    private final CardLayout cards = new CardLayout();
    private final JPanel centro = new JPanel(cards);
    private final PanelVentas ventas = new PanelVentas(() -> mostrar("Caja"));
    private final PanelCaja caja = new PanelCaja(this::actualizarAviso);
    private final PanelProductos productos = new PanelProductos();
    private final PanelInventario inventario = new PanelInventario();
    private final PanelMermas mermas = new PanelMermas();
    private final PanelCompras compras = new PanelCompras(this::actualizarAviso);
    private final PanelClientes clientes = new PanelClientes();
    // Solo se crean para el administrador
    private PanelReportes reportes;
    private PanelUsuarios usuarios;
    private PanelPagos pagos;

    private final Map<String, Seccion> secciones = new LinkedHashMap<>();
    private final Map<String, ItemMenu> items = new LinkedHashMap<>();
    private final JLabel lblTitulo = new JLabel(), lblDescripcion = new JLabel();
    // Revisión periódica de alertas de stock mientras la aplicación está abierta (proceso SYS-03)
    private final Timer revisionAlertas = new Timer(Datos.MINUTOS_REVISION_ALERTAS * 60_000, e -> actualizarAviso());

    public Principal() {
        setTitle("SEIPO - " + Sesion.getNombre() + " (" + Sesion.textoRol(Sesion.getRol()) + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1240, 760);
        setMinimumSize(new Dimension(1050, 650));
        setLocationRelativeTo(null);

        secciones.put("Ventas", new Seccion("OPERACIÓN", "Busque productos, arme la venta, elija el cliente y cobre.", ventas));
        secciones.put("Caja", new Seccion("OPERACIÓN", "Apertura y cierre de turno, cuadre de caja y ventas del turno.", caja));
        secciones.put("Clientes", new Seccion("OPERACIÓN", "Consulte y registre clientes con su DNI o RUC.", clientes));
        secciones.put("Productos", new Seccion("INVENTARIO", "Catálogo de productos, precios y stock mínimo y máximo.", productos));
        secciones.put("Inventario", new Seccion("INVENTARIO", "Resumen del almacén, lotes en orden FEFO y recepción de mercadería.", inventario));
        secciones.put("Mermas", new Seccion("INVENTARIO", "Bajas por productos vencidos o dañados y su autorización.", mermas));
        secciones.put("Compras", new Seccion("INVENTARIO", "Alertas de stock, órdenes de compra y proveedores.", compras));
        if (Sesion.esAdministrador()) {
            reportes = new PanelReportes();
            usuarios = new PanelUsuarios();
            pagos = new PanelPagos();
            secciones.put("Reportes", new Seccion("GESTIÓN", "Indicadores de ventas e inventario, gráficos y exportación.", reportes));
            secciones.put("Pagos", new Seccion("GESTIÓN", "Cobros de la pasarela de pago, su configuración y los datos del comprobante.", pagos));
            secciones.put("Usuarios", new Seccion("GESTIÓN", "Cuentas de acceso y roles del personal.", usuarios));
        }
        for (Map.Entry<String, Seccion> s : secciones.entrySet()) centro.add(s.getValue().panel, s.getKey());

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setBackground(UI.FONDO);
        contenido.add(cabecera(), BorderLayout.NORTH);
        contenido.add(centro, BorderLayout.CENTER);

        add(menu(), BorderLayout.WEST);
        add(contenido, BorderLayout.CENTER);
        mostrar("Ventas");
        revisionAlertas.start();
        if (Sesion.esAdministrador()) SwingUtilities.invokeLater(this::avisoAlIngresar);
    }

    private JPanel menu() {
        // Logo arriba y usuario abajo quedan fijos; si la ventana es baja, la lista de secciones se desplaza
        JPanel lateral = new JPanel(new BorderLayout(0, 8));
        lateral.setBackground(UI.MENU);
        lateral.setBorder(new CompoundBorder(new MatteBorder(0, 0, 0, 1, UI.BORDE), new EmptyBorder(20, 12, 16, 12)));
        lateral.setPreferredSize(new Dimension(220, 0));
        JPanel arriba = new JPanel();
        arriba.setLayout(new BoxLayout(arriba, BoxLayout.Y_AXIS));
        arriba.setOpaque(false);
        ListaMenu menu = new ListaMenu();
        JPanel abajo = new JPanel();
        abajo.setLayout(new BoxLayout(abajo, BoxLayout.Y_AXIS));
        abajo.setOpaque(false);

        JLabel logo = new JLabel("Seipo");
        logo.setFont(UI.fuente(Font.BOLD, 28));
        logo.setForeground(UI.ACENTO);
        logo.setBorder(new EmptyBorder(0, 10, 0, 0));
        logo.setAlignmentX(LEFT_ALIGNMENT);
        JLabel lema = new JLabel("Venta e inventario");
        lema.setFont(UI.fuente(Font.PLAIN, 12));
        lema.setForeground(UI.TEXTO_SECUNDARIO);
        lema.setBorder(new EmptyBorder(0, 11, 0, 0));
        lema.setAlignmentX(LEFT_ALIGNMENT);
        arriba.add(logo);
        arriba.add(lema);

        String grupoActual = null;
        for (Map.Entry<String, Seccion> s : secciones.entrySet()) {
            if (!s.getValue().grupo.equals(grupoActual)) {
                grupoActual = s.getValue().grupo;
                JLabel g = new JLabel(grupoActual);
                g.setFont(UI.fuente(Font.BOLD, 11));
                g.setForeground(new Color(0x64748B));
                g.setBorder(new EmptyBorder(menu.getComponentCount() == 0 ? 10 : 14, 12, 4, 0));
                g.setAlignmentX(LEFT_ALIGNMENT);
                menu.add(g);
            }
            String nombre = s.getKey();
            ItemMenu item = new ItemMenu(nombre);
            item.addActionListener(e -> mostrar(nombre));
            items.put(nombre, item);
            menu.add(item);
            menu.add(Box.createVerticalStrut(2));
        }

        menu.add(Box.createVerticalGlue());
        JScrollPane desplazable = new JScrollPane(menu, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        desplazable.setBorder(null);
        desplazable.getViewport().setBackground(UI.MENU);

        // Tarjeta del usuario con sus iniciales
        JPanel usuario = new JPanel(new BorderLayout(10, 0));
        usuario.setBackground(UI.SUPERFICIE);
        usuario.setBorder(new CompoundBorder(new LineBorder(UI.BORDE, 1, true), new EmptyBorder(8, 10, 8, 10)));
        usuario.setAlignmentX(LEFT_ALIGNMENT);
        usuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        usuario.add(new Avatar(iniciales(Sesion.getNombre())), BorderLayout.WEST);
        JLabel datos = new JLabel("<html><b>" + Sesion.getNombre() + "</b><br><span style='color:#94A3B8'>"
                + Sesion.textoRol(Sesion.getRol()) + "</span></html>");
        datos.setFont(UI.fuente(Font.PLAIN, 12));
        usuario.add(datos, BorderLayout.CENTER);
        abajo.add(usuario);
        abajo.add(Box.createVerticalStrut(8));

        JButton salir = UI.botonSecundario("Cerrar sesión");
        salir.setAlignmentX(LEFT_ALIGNMENT);
        salir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        salir.addActionListener(e -> cerrarSesion());
        abajo.add(salir);

        lateral.add(arriba, BorderLayout.NORTH);
        lateral.add(desplazable, BorderLayout.CENTER);
        lateral.add(abajo, BorderLayout.SOUTH);
        return lateral;
    }

    private JPanel cabecera() {
        JPanel cab = new JPanel(new BorderLayout());
        cab.setBackground(UI.FONDO);
        cab.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, UI.BORDE), new EmptyBorder(16, 22, 14, 22)));
        JPanel textos = new JPanel(new GridLayout(0, 1, 0, 2));
        textos.setOpaque(false);
        lblTitulo.setFont(UI.fuente(Font.BOLD, 22));
        lblTitulo.setForeground(UI.TEXTO);
        lblDescripcion.setFont(UI.fuente(Font.PLAIN, 13));
        lblDescripcion.setForeground(UI.TEXTO_SECUNDARIO);
        textos.add(lblTitulo);
        textos.add(lblDescripcion);
        cab.add(textos, BorderLayout.CENTER);
        String hoy = LocalDate.now().format(FECHA_CABECERA);
        JLabel fecha = new JLabel(Character.toUpperCase(hoy.charAt(0)) + hoy.substring(1));
        fecha.setFont(UI.fuente(Font.PLAIN, 13));
        fecha.setForeground(UI.TEXTO_SECUNDARIO);
        cab.add(fecha, BorderLayout.EAST);
        return cab;
    }

    /** Muestra en el ítem Compras cuántos productos necesitan reposición. */
    private void actualizarAviso() {
        try {
            long reponer = new ControladorCompras().listarAlertas().stream().filter(ControladorCompras.AlertaVO::esReposicion).count();
            ItemMenu item = items.get("Compras");
            item.setAviso((int) reponer);
            item.setToolTipText(reponer == 0 ? null : reponer + " producto(s) agotados o con stock bajo");
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

    private void mostrar(String nombre) {
        switch (nombre) {
            case "Ventas": ventas.refrescar(); break;
            case "Caja": caja.refrescar(); break;
            case "Productos": productos.refrescar(); break;
            case "Inventario": inventario.refrescar(); break;
            case "Mermas": mermas.refrescar(); break;
            case "Compras": compras.refrescar(); break;
            case "Clientes": clientes.refrescar(); break;
            case "Reportes": reportes.refrescar(); break;
            case "Usuarios": usuarios.refrescar(); break;
            case "Pagos": pagos.refrescar(); break;
            default: break;
        }
        cards.show(centro, nombre);
        for (Map.Entry<String, ItemMenu> e : items.entrySet()) e.getValue().setActivo(e.getKey().equals(nombre));
        lblTitulo.setText(nombre);
        lblDescripcion.setText(secciones.get(nombre).descripcion);
        actualizarAviso();
    }

    private void cerrarSesion() {
        String pregunta = "¿Desea cerrar la sesión?";
        try {
            if (new ControladorCaja().turnoAbierto() != null) {
                pregunta = "Su turno de caja sigue abierto (puede cerrarlo en la sección Caja).\n" + pregunta;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        if (JOptionPane.showConfirmDialog(this, pregunta, "Cerrar sesión",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        revisionAlertas.stop();
        Sesion.cerrar();
        dispose();
        new Login().setVisible(true);
    }

    private static String iniciales(String nombre) {
        StringBuilder sb = new StringBuilder();
        for (String parte : nombre.trim().split("\\s+")) {
            if (!parte.isEmpty() && sb.length() < 2) sb.append(Character.toUpperCase(parte.charAt(0)));
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    /** Lista de secciones: ocupa el ancho del menú y, si sobra alto, también todo el alto (la barra aparece solo si falta). */
    private static class ListaMenu extends JPanel implements Scrollable {
        ListaMenu() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(UI.MENU);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }

    /** Ítem del menú lateral: resalta la sección activa con una barra de acento y muestra un aviso numérico. */
    private static class ItemMenu extends JButton {
        private boolean activo, encima;
        private int aviso;

        ItemMenu(String texto) {
            super(texto);
            setFont(UI.fuente(Font.PLAIN, 14));
            setForeground(UI.TEXTO_SECUNDARIO);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(7, 14, 7, 14));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { encima = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { encima = false; repaint(); }
            });
        }

        void setActivo(boolean activo) {
            this.activo = activo;
            setFont(UI.fuente(activo ? Font.BOLD : Font.PLAIN, 14));
            setForeground(activo ? UI.TEXTO : UI.TEXTO_SECUNDARIO);
            repaint();
        }

        void setAviso(int aviso) {
            this.aviso = aviso;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (activo || encima) {
                g.setColor(activo ? UI.SUPERFICIE : new Color(0x141C2E));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            if (activo) {
                g.setColor(UI.ACENTO);
                g.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4);
            }
            if (aviso > 0) {
                String n = String.valueOf(aviso);
                g.setFont(UI.fuente(Font.BOLD, 11));
                FontMetrics fm = g.getFontMetrics();
                int w = Math.max(22, fm.stringWidth(n) + 12), h = 20;
                int x = getWidth() - w - 10, y = (getHeight() - h) / 2;
                g.setColor(UI.ALERTA);
                g.fillRoundRect(x, y, w, h, h, h);
                g.setColor(new Color(0x422006));
                g.drawString(n, x + (w - fm.stringWidth(n)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Círculo con las iniciales del usuario. */
    private static class Avatar extends JComponent {
        private final String texto;

        Avatar(String texto) {
            this.texto = texto;
            setPreferredSize(new Dimension(36, 36));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight());
            g.setColor(UI.ACENTO_OSCURO);
            g.fillOval(0, (getHeight() - d) / 2, d, d);
            g.setFont(UI.fuente(Font.BOLD, 13));
            g.setColor(new Color(0xCCFBF1));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(texto, (d - fm.stringWidth(texto)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }
}
