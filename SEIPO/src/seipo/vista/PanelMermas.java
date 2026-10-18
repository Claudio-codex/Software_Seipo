package seipo.vista;

import seipo.controlador.ControladorMermas;
import seipo.controlador.ControladorMermas.LoteDisponibleVO;
import seipo.controlador.ControladorMermas.MermaVO;
import seipo.modelo.Datos;
import seipo.modelo.Sesion;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
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

        JPanel tarjetas = new JPanel(new GridLayout(1, 3, 10, 0));
        tarjetas.setOpaque(false);
        tarjetas.add(UI.tarjeta("Pendientes de autorizar", lblPendientes));
        tarjetas.add(UI.tarjeta("Unidades dadas de baja", lblUnidades));
        tarjetas.add(UI.tarjeta("Valor perdido", lblValor));

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
        barra.add(new JLabel("   Mostrar:"));
        barra.add(cmbFiltro);

        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.setOpaque(false);
        norte.add(tarjetas, BorderLayout.NORTH);
        norte.add(barra, BorderLayout.CENTER);
        p.add(norte, BorderLayout.NORTH);

        JLabel info = new JLabel(Sesion.esAdministrador()
                ? "Seleccione una o varias mermas pendientes para aprobarlas (descuenta el stock) o rechazarlas."
                : "Las mermas quedan pendientes hasta que el administrador las autorice; mientras tanto esas unidades no se venden.");
        info.setFont(UI.fuente(Font.PLAIN, 12));
        info.setForeground(UI.TEXTO_SECUNDARIO);

        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        UI.colorearFilas(tabla, fila -> {
            String estado = mermas.get(fila).estado;
            return ControladorMermas.PENDIENTE.equals(estado) ? UI.FILA_AMBAR
                    : ControladorMermas.RECHAZADA.equals(estado) ? UI.FILA_GRIS : null;
        });

        JPanel centro = new JPanel(new BorderLayout(0, 5));
        centro.setOpaque(false);
        centro.add(info, BorderLayout.NORTH);
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
}
