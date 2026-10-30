package seipo.vista;

import seipo.controlador.ControladorMermas;
import seipo.controlador.ControladorMermas.LoteDisponibleVO;
import seipo.controlador.ControladorMermas.MermaVO;
import seipo.modelo.Datos;
import seipo.modelo.Sesion;
import seipo.vista.ExportadorReporte.Formato;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Registro de mermas (productos vencidos o dañados) y su autorización por el administrador. */
public class PanelMermas extends JPanel {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
    private static final String[] FILTROS = {"Pendientes", "Aprobadas", "Rechazadas", "Todas"};

    private final JLabel lblPendientes = new JLabel(), lblUnidades = new JLabel(), lblValor = new JLabel();
    private final JComboBox<String> cmbFiltro = new JComboBox<>(FILTROS);
    private final DefaultTableModel modelo = UI.modelo("Solicitada", "Código", "Producto", "Lote", "Vence", "Cant.",
            "Causal", "Valor", "Estado", "Solicitó", "Autorizó", "Observación");
    private final JTable tabla = UI.tabla(modelo);
    private final ControladorMermas controlador = new ControladorMermas();
    private List<MermaVO> mermas = new ArrayList<>();

    public PanelMermas() {
        setLayout(new BorderLayout());
        UI.columnaAncha(tabla, 2, 220);
        UI.columnaAncha(tabla, 11, 220);
        JPanel p = UI.base();
        add(p);

        JPanel tarjetas = new JPanel(new GridLayout(1, 3, 16, 0));
        tarjetas.setOpaque(false);
        tarjetas.add(UI.tarjeta("Pendientes de autorizar", lblPendientes, Iconos.Tipo.RELOJ, true));
        tarjetas.add(UI.tarjeta("Unidades dadas de baja", lblUnidades, Iconos.Tipo.CUBO, false));
        tarjetas.add(UI.tarjeta("Valor perdido", lblValor, Iconos.Tipo.MONEDA, false));

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setOpaque(false);
        JButton registrar = UI.boton("+ Registrar merma");
        JButton vencidos = UI.botonSecundario("Registrar lotes vencidos");
        barra.add(registrar);
        barra.add(vencidos);
        if (Sesion.esAdministrador()) {
            JButton aprobar = UI.boton("Aprobar");
            JButton rechazar = UI.botonPeligro("Rechazar");
            aprobar.addActionListener(e -> aprobar());
            rechazar.addActionListener(e -> rechazar());
            barra.add(aprobar);
            barra.add(rechazar);
        }
        // Reporte de mermas de un período: un solo botón con las dos opciones, para que la barra quepa en la ventana mínima
        JButton exportar = UI.botonSecundario("Exportar");
        exportar.setIcon(new FlechaAbajo());
        exportar.setHorizontalTextPosition(SwingConstants.LEFT);
        exportar.setIconTextGap(8);
        exportar.setToolTipText("Reporte de mermas de un período en PDF o Excel");
        JPopupMenu opcionesExportar = new JPopupMenu();
        JMenuItem pdf = new JMenuItem("Exportar PDF"), excel = new JMenuItem("Exportar Excel");
        pdf.addActionListener(e -> exportar(true));
        excel.addActionListener(e -> exportar(false));
        opcionesExportar.add(pdf);
        opcionesExportar.add(excel);
        exportar.addActionListener(e -> opcionesExportar.show(exportar, 0, exportar.getHeight()));
        JPanel filaBotones = new JPanel(new BorderLayout());
        filaBotones.setOpaque(false);
        filaBotones.add(barra, BorderLayout.CENTER);
        filaBotones.add(exportar, BorderLayout.EAST);

        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.setOpaque(false);
        norte.add(tarjetas, BorderLayout.NORTH);
        norte.add(filaBotones, BorderLayout.CENTER);
        p.add(norte, BorderLayout.NORTH);

        JLabel info = new JLabel(Sesion.esAdministrador()
                ? "Seleccione una o varias mermas pendientes para aprobarlas (descuenta el stock) o rechazarlas."
                : "Las mermas quedan pendientes hasta que el administrador las autorice; mientras tanto esas unidades no se venden.");
        info.setFont(UI.fuente(Font.PLAIN, 12));
        info.setForeground(UI.TEXTO_SECUNDARIO);

        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        UI.insignias(tabla, 8);

        // Texto de ayuda a la izquierda y el filtro de la tabla a la derecha
        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filtro.setOpaque(false);
        filtro.add(new JLabel("Mostrar:"));
        filtro.add(cmbFiltro);
        JPanel lineaInfo = new JPanel(new BorderLayout(10, 0));
        lineaInfo.setOpaque(false);
        lineaInfo.add(info, BorderLayout.CENTER);
        lineaInfo.add(filtro, BorderLayout.EAST);

        JPanel centro = new JPanel(new BorderLayout(0, 5));
        centro.setOpaque(false);
        centro.add(lineaInfo, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        p.add(centro, BorderLayout.CENTER);

        registrar.addActionListener(e -> { if (dialogoRegistrar(this, null)) refrescar(); });
        vencidos.addActionListener(e -> registrarVencidos());
        cmbFiltro.addActionListener(e -> refrescar());
    }

    public void refrescar() {
        modelo.setRowCount(0);
        try {
            List<MermaVO> todas = controlador.listarMermas(null);
            int pendientes = 0, unidades = 0;
            double valor = 0;
            for (MermaVO m : todas) {
                if (ControladorMermas.PENDIENTE.equals(m.estado)) pendientes++;
                if (ControladorMermas.APROBADA.equals(m.estado)) {
                    unidades += m.cantidad;
                    valor += m.valor();
                }
            }
            lblPendientes.setText(String.valueOf(pendientes));
            lblUnidades.setText(String.valueOf(unidades));
            lblValor.setText(Datos.soles(valor));

            String filtro = estadoFiltro();
            mermas = new ArrayList<>();
            for (MermaVO m : todas) {
                if (filtro != null && !filtro.equals(m.estado)) continue;
                mermas.add(m);
                String obs = m.observacion == null ? "" : m.observacion;
                if (m.motivoRechazo != null && !m.motivoRechazo.isEmpty()) obs += (obs.isEmpty() ? "" : " · ") + "Rechazo: " + m.motivoRechazo;
                modelo.addRow(new Object[]{
                        m.solicitadoEn == null ? "" : m.solicitadoEn.format(FECHA_HORA),
                        m.codigo, m.producto, m.lote,
                        m.vencimiento == null ? "—" : m.vencimiento.format(Datos.FECHA_COMPLETA),
                        m.cantidad, m.causal, Datos.soles(m.valor()), textoEstado(m.estado),
                        m.solicitante, m.autorizador == null ? "" : m.autorizador, obs});
            }
        } catch (SQLException ex) {
            error(this, "Error al cargar las mermas", ex);
        }
    }

    /**
     * Pide lote, cantidad, causal y observación y registra la merma pendiente.
     * Si se indica loteId, ese lote queda preseleccionado. Devuelve true si se registró.
     */
    public static boolean dialogoRegistrar(Component padre, Integer loteId) {
        ControladorMermas ctrl = new ControladorMermas();
        List<LoteDisponibleVO> lotes;
        try {
            lotes = ctrl.listarLotesDisponibles();
        } catch (SQLException ex) {
            error(padre, "Error al cargar los lotes", ex);
            return false;
        }
        if (lotes.isEmpty()) {
            JOptionPane.showMessageDialog(padre, "No hay lotes con unidades disponibles para dar de baja.");
            return false;
        }

        JComboBox<LoteDisponibleVO> cmbLote = new JComboBox<>(lotes.toArray(new LoteDisponibleVO[0]));
        if (loteId != null) {
            for (LoteDisponibleVO l : lotes) if (l.id == loteId) cmbLote.setSelectedItem(l);
            if (((LoteDisponibleVO) cmbLote.getSelectedItem()).id != loteId) {
                JOptionPane.showMessageDialog(padre, "Ese lote ya tiene todas sus unidades en mermas pendientes.");
                return false;
            }
        }
        JComboBox<String> cmbCausal = new JComboBox<>(ControladorMermas.CAUSALES);
        LoteDisponibleVO inicial = (LoteDisponibleVO) cmbLote.getSelectedItem();
        if (inicial.vencimiento != null && inicial.vencimiento.isBefore(java.time.LocalDate.now())) cmbCausal.setSelectedItem("Vencido");
        else cmbCausal.setSelectedItem("Dañado");
        JTextField txtCantidad = new JTextField(String.valueOf(inicial.disponible));
        JTextField txtObs = new JTextField();
        cmbLote.addActionListener(e -> txtCantidad.setText(String.valueOf(((LoteDisponibleVO) cmbLote.getSelectedItem()).disponible)));

        Object[] campos = {"Lote:", cmbLote, "Cantidad:", txtCantidad, "Causal:", cmbCausal,
                "Observación (qué ocurrió, dónde se encontró):", txtObs};
        if (JOptionPane.showConfirmDialog(padre, campos, "Registrar merma",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return false;

        LoteDisponibleVO lote = (LoteDisponibleVO) cmbLote.getSelectedItem();
        String causal = (String) cmbCausal.getSelectedItem();
        String obs = txtObs.getText().trim();
        if (!"Vencido".equals(causal) && obs.isEmpty()) {
            JOptionPane.showMessageDialog(padre, "Describa en la observación qué le ocurrió al producto.");
            return false;
        }
        try {
            int cantidad = Integer.parseInt(txtCantidad.getText().trim());
            ctrl.registrarMerma(lote.id, cantidad, causal, obs);
            JOptionPane.showMessageDialog(padre, Sesion.esAdministrador()
                    ? "Merma registrada. Apruébela en la sección Mermas para descontar el stock."
                    : "Merma registrada. Queda pendiente de autorización del administrador.");
            return true;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(padre, "La cantidad debe ser un número entero.");
        } catch (SQLException ex) {
            error(padre, "No se pudo registrar la merma", ex);
        }
        return false;
    }

    private void registrarVencidos() {
        if (JOptionPane.showConfirmDialog(this, "Se registrará una merma \"Vencido\" por cada lote vencido con unidades.\n¿Continuar?",
                "Registrar lotes vencidos", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            int n = controlador.registrarVencidos();
            cmbFiltro.setSelectedIndex(0);
            refrescar();
            JOptionPane.showMessageDialog(this, n == 0 ? "No hay lotes vencidos pendientes de registrar."
                    : n + " merma(s) registrada(s) como pendientes de autorización.");
        } catch (SQLException ex) {
            error(this, "No se pudieron registrar los vencidos", ex);
        }
    }

    private void aprobar() {
        List<MermaVO> sel = pendientesSeleccionadas();
        if (sel.isEmpty()) return;
        int unidades = 0;
        double valor = 0;
        for (MermaVO m : sel) { unidades += m.cantidad; valor += m.valor(); }
        if (JOptionPane.showConfirmDialog(this, "¿Aprobar " + sel.size() + " merma(s)? Se darán de baja " + unidades
                + " unidad(es) por " + Datos.soles(valor) + ".", "Aprobar mermas", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        List<String> errores = new ArrayList<>();
        for (MermaVO m : sel) {
            try {
                controlador.aprobarMerma(m.id);
            } catch (SQLException ex) {
                errores.add(m.producto + " (" + m.lote + "): " + ex.getMessage());
            }
        }
        refrescar();
        if (!errores.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Algunas mermas no se aprobaron:\n" + String.join("\n", errores),
                    "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void rechazar() {
        List<MermaVO> sel = pendientesSeleccionadas();
        if (sel.isEmpty()) return;
        String motivo = JOptionPane.showInputDialog(this, "Motivo del rechazo (" + sel.size() + " merma(s)):");
        if (motivo == null) return;
        if (motivo.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Indique el motivo del rechazo.");
            return;
        }
        try {
            for (MermaVO m : sel) controlador.rechazarMerma(m.id, motivo.trim());
        } catch (SQLException ex) {
            error(this, "No se pudo rechazar", ex);
        }
        refrescar();
    }

    private List<MermaVO> pendientesSeleccionadas() {
        List<MermaVO> sel = new ArrayList<>();
        for (int fila : tabla.getSelectedRows()) {
            MermaVO m = mermas.get(tabla.convertRowIndexToModel(fila));
            if (ControladorMermas.PENDIENTE.equals(m.estado)) sel.add(m);
        }
        if (sel.isEmpty()) JOptionPane.showMessageDialog(this, "Seleccione una o más mermas pendientes.");
        return sel;
    }

    private String estadoFiltro() {
        switch (cmbFiltro.getSelectedIndex()) {
            case 0: return ControladorMermas.PENDIENTE;
            case 1: return ControladorMermas.APROBADA;
            case 2: return ControladorMermas.RECHAZADA;
            default: return null;
        }
    }

    private static String textoEstado(String estado) {
        switch (estado) {
            case ControladorMermas.PENDIENTE: return "Pendiente";
            case ControladorMermas.APROBADA: return "Aprobada";
            default: return "Rechazada";
        }
    }

    private static void error(Component padre, String titulo, SQLException ex) {
        JOptionPane.showMessageDialog(padre, titulo + ":\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Triángulo hacia abajo para el botón con menú desplegable (no depende de que la fuente tenga el símbolo). */
    private static class FlechaAbajo implements Icon {
        @Override public int getIconWidth() { return 9; }
        @Override public int getIconHeight() { return 5; }
        @Override public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(c.getForeground());
            g.fillPolygon(new int[]{x, x + 9, x + 4}, new int[]{y, y, y + 5}, 3);
            g.dispose();
        }
    }

    // ---------- Reporte de mermas ----------

    private void exportar(boolean pdf) {
        LocalDate[] periodo = pedirPeriodo();
        if (periodo == null) return;
        List<MermaVO> lista;
        try {
            lista = controlador.listarMermas(periodo[0], periodo[1]);
        } catch (SQLException ex) {
            error(this, "Error al cargar las mermas del período", ex);
            return;
        }
        if (lista.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay mermas solicitadas entre el " + periodo[0].format(Datos.FECHA_COMPLETA)
                    + " y el " + periodo[1].format(Datos.FECHA_COMPLETA) + ".");
            return;
        }

        String ext = pdf ? "pdf" : "xlsx";
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(pdf ? "Exportar reporte de mermas a PDF" : "Exportar reporte de mermas a Excel");
        fc.setFileFilter(new FileNameExtensionFilter(pdf ? "Documento PDF (*.pdf)" : "Libro de Excel (*.xlsx)", ext));
        fc.setSelectedFile(new File("Reporte_mermas_" + periodo[0] + "_a_" + periodo[1] + "." + ext));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File archivo = fc.getSelectedFile();
        if (!archivo.getName().toLowerCase(Locale.ROOT).endsWith("." + ext)) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + "." + ext);
        }
        if (archivo.exists() && JOptionPane.showConfirmDialog(this, "El archivo ya existe. ¿Desea reemplazarlo?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        try {
            ExportadorReporte.Reporte r = armarReporte(periodo[0], periodo[1], lista);
            if (pdf) ExportadorReporte.pdf(r, archivo);
            else ExportadorReporte.excel(r, archivo);
            JOptionPane.showMessageDialog(this, "Reporte exportado:\n" + archivo.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar el reporte (¿está abierto en otro programa?):\n"
                    + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Pide el período del reporte (por fecha de solicitud de la merma); devuelve {desde, hasta} o null si se cancela. */
    private LocalDate[] pedirPeriodo() {
        String[] rapidos = {"Este mes", "Mes anterior", "Últimos 30 días", "Este año"};
        JComboBox<String> cmbRapido = new JComboBox<>(rapidos);
        JTextField txtDesde = new JTextField(), txtHasta = new JTextField();
        Runnable aplicar = () -> {
            LocalDate hoy = LocalDate.now(), desde = hoy.withDayOfMonth(1), hasta = hoy;
            switch (cmbRapido.getSelectedIndex()) {
                case 1: desde = hoy.minusMonths(1).withDayOfMonth(1); hasta = desde.plusMonths(1).minusDays(1); break;
                case 2: desde = hoy.minusDays(29); break;
                case 3: desde = hoy.withDayOfYear(1); break;
                default: break;
            }
            txtDesde.setText(desde.format(Datos.FECHA_COMPLETA));
            txtHasta.setText(hasta.format(Datos.FECHA_COMPLETA));
        };
        cmbRapido.addActionListener(e -> aplicar.run());
        aplicar.run();
        Object[] campos = {"Período:", cmbRapido, "Desde (dd/mm/aaaa):", txtDesde, "Hasta (dd/mm/aaaa):", txtHasta,
                UI.ayuda("Se incluyen las mermas solicitadas en esas fechas, en cualquier estado.")};
        while (true) {
            if (JOptionPane.showConfirmDialog(this, campos, "Reporte de mermas", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return null;
            try {
                LocalDate desde = LocalDate.parse(txtDesde.getText().trim(), Datos.FECHA_COMPLETA);
                LocalDate hasta = LocalDate.parse(txtHasta.getText().trim(), Datos.FECHA_COMPLETA);
                if (hasta.isBefore(desde)) {
                    JOptionPane.showMessageDialog(this, "La fecha final no puede ser anterior a la inicial.");
                    continue;
                }
                return new LocalDate[]{desde, hasta};
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Fecha inválida. Use el formato dd/mm/aaaa.");
            }
        }
    }

    /**
     * Resumen, desglose por causal y detalle de las mermas del período. El valor perdido y las unidades dadas de baja
     * cuentan solo las aprobadas: las pendientes todavía pueden rechazarse y las rechazadas no salieron del stock.
     */
    private static ExportadorReporte.Reporte armarReporte(LocalDate desde, LocalDate hasta, List<MermaVO> lista) {
        ExportadorReporte.Reporte r = new ExportadorReporte.Reporte();
        r.titulo = "Reporte de mermas";
        r.periodo = desde.format(Datos.FECHA_COMPLETA) + " al " + hasta.format(Datos.FECHA_COMPLETA) + " (por fecha de solicitud)";

        int aprobadas = 0, pendientes = 0, rechazadas = 0, unidades = 0;
        double valor = 0;
        // Por causal: registradas, aprobadas, unidades dadas de baja y valor perdido (en el orden de las causales)
        Map<String, double[]> porCausal = new LinkedHashMap<>();
        for (String c : ControladorMermas.CAUSALES) porCausal.put(c, new double[4]);
        for (MermaVO m : lista) {
            double[] c = porCausal.computeIfAbsent(m.causal, k -> new double[4]);
            c[0]++;
            if (ControladorMermas.APROBADA.equals(m.estado)) {
                aprobadas++;
                unidades += m.cantidad;
                valor += m.valor();
                c[1]++;
                c[2] += m.cantidad;
                c[3] += m.valor();
            } else if (ControladorMermas.PENDIENTE.equals(m.estado)) {
                pendientes++;
            } else {
                rechazadas++;
            }
        }
        // Se redondea a céntimos para que Excel no guarde restos de punto flotante (583.3999…)
        valor = Datos.redondear(valor);
        for (double[] c : porCausal.values()) c[3] = Datos.redondear(c[3]);
        r.indicadores.add(new ExportadorReporte.Indicador("Valor perdido", valor, Formato.SOLES));
        r.indicadores.add(new ExportadorReporte.Indicador("Unidades dadas de baja", unidades, Formato.ENTERO));
        r.indicadores.add(new ExportadorReporte.Indicador("Mermas registradas", lista.size(), Formato.ENTERO));
        r.indicadores.add(new ExportadorReporte.Indicador("Pendientes", pendientes, Formato.ENTERO));
        r.indicadores.add(new ExportadorReporte.Indicador("Rechazadas", rechazadas, Formato.ENTERO));
        r.nota = "El valor perdido y las unidades dadas de baja consideran solo las mermas aprobadas ("
                + aprobadas + " de " + lista.size() + ").";

        ExportadorReporte.Seccion causales = new ExportadorReporte.Seccion("Desglose por causal",
                new String[]{"Causal", "Registradas", "Aprobadas", "Unidades dadas de baja", "Valor perdido", "% del valor perdido"},
                Formato.TEXTO, Formato.ENTERO, Formato.ENTERO, Formato.ENTERO, Formato.SOLES, Formato.PORCENTAJE);
        for (Map.Entry<String, double[]> e : porCausal.entrySet()) {
            double[] c = e.getValue();
            if (c[0] == 0) continue;
            causales.filas.add(new Object[]{e.getKey(), (int) c[0], (int) c[1], (int) c[2], c[3], valor == 0 ? Double.NaN : c[3] / valor});
        }
        causales.filas.add(new Object[]{"Total", lista.size(), aprobadas, unidades, valor, valor == 0 ? Double.NaN : 1.0});
        r.secciones.add(causales);

        ExportadorReporte.Seccion detalle = new ExportadorReporte.Seccion("Detalle de mermas",
                new String[]{"Solicitada", "Código", "Producto", "Lote", "Cant.", "Causal", "Estado", "Valor perdido",
                        "Solicitó", "Autorizó", "Autorizada", "Observación"},
                Formato.TEXTO, Formato.TEXTO, Formato.TEXTO, Formato.TEXTO, Formato.ENTERO, Formato.TEXTO, Formato.TEXTO,
                Formato.SOLES, Formato.TEXTO, Formato.TEXTO, Formato.TEXTO, Formato.TEXTO);
        detalle.anchos = new float[]{1.05f, 0.95f, 1.9f, 1.05f, 0.75f, 0.85f, 1.0f, 0.95f, 1.2f, 1.2f, 1.25f, 1.75f};
        for (MermaVO m : lista) {
            String obs = m.observacion == null ? "" : m.observacion;
            if (m.motivoRechazo != null && !m.motivoRechazo.isEmpty()) obs += (obs.isEmpty() ? "" : " · ") + "Rechazo: " + m.motivoRechazo;
            detalle.filas.add(new Object[]{
                    m.solicitadoEn == null ? "" : m.solicitadoEn.format(FECHA_HORA), m.codigo, m.producto, m.lote,
                    m.cantidad, m.causal, textoEstado(m.estado),
                    // Las pendientes y rechazadas no son pérdida: se muestran con "—" (celda vacía en Excel)
                    ControladorMermas.APROBADA.equals(m.estado) ? Datos.redondear(m.valor()) : Double.NaN,
                    m.solicitante, m.autorizador == null ? "" : m.autorizador,
                    m.autorizadoEn == null ? "" : m.autorizadoEn.format(FECHA_HORA), obs});
        }
        r.secciones.add(detalle);

        r.firmas.add(new String[]{"Elaborado por", Sesion.getNombre()});
        r.firmas.add(new String[]{"Autorizado por", ""});
        return r;
    }
}
