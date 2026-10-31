package seipo.vista;

import seipo.controlador.ControladorCaja;
import seipo.controlador.ControladorCaja.TurnoVO;
import seipo.controlador.ControladorVentas;
import seipo.controlador.ControladorVentas.AnulacionVO;
import seipo.controlador.ControladorVentas.VentaVO;
import seipo.modelo.Datos;
import seipo.modelo.Sesion;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/** Apertura y cierre de turno, cuadre de caja, ventas del turno, historial de turnos y registro de anulaciones. */
public class PanelCaja extends JPanel {

    private static final String[] PERIODOS = {"Hoy", "Últimos 7 días", "Últimos 30 días", "Este año"};
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ControladorCaja caja = new ControladorCaja();
    private final ControladorVentas ventas = new ControladorVentas();
    private final Runnable alCambiar;

    // Mi turno
    private final CardLayout tarjetasTurno = new CardLayout();
    private final JPanel panelTurno = new JPanel(tarjetasTurno);
    private final JTextField txtMontoInicial = new JTextField("100.00");
    private final JLabel lblInicial = new JLabel(), lblEfectivo = new JLabel(),
            lblYape = new JLabel(), lblEnCaja = new JLabel(), lblInfoTurno = UI.ayuda(" ");
    private final TablaVentas ventasTurno = new TablaVentas(this::refrescar);
    private TurnoVO turno;

    // Historial
    private final JComboBox<String> cboPeriodo = new JComboBox<>(PERIODOS);
    private final DefaultTableModel modeloTurnos = UI.modelo("N°", "Usuario", "Fecha", "Horario", "Ventas", "Total vendido",
            "Inicial", "Efectivo esperado", "Contado", "Diferencia", "Estado");
    private final JTable tablaTurnos = UI.tabla(modeloTurnos);
    private final List<TurnoVO> turnos = new ArrayList<>();

    // Anulaciones
    private final JComboBox<String> cboPeriodoAnulaciones = new JComboBox<>(PERIODOS);
    private final DefaultTableModel modeloAnulaciones = UI.modelo("Fecha y hora", "Venta", "Comprobante", "Usuario", "Rol",
            "Motivo", "Detalle", "Pago", "Monto");
    private final JTable tablaAnulaciones = UI.tabla(modeloAnulaciones);
    private final JLabel lblNumAnulaciones = new JLabel(), lblMontoAnulado = new JLabel();

    /** @param alCambiar se llama al abrir o cerrar un turno o anular una venta (para actualizar otras pantallas) */
    public PanelCaja(Runnable alCambiar) {
        this.alCambiar = alCambiar;
        setLayout(new BorderLayout());
        JPanel p = UI.base();
        add(p);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Mi turno", pestanaTurno());
        pestanas.addTab(Sesion.esAdministrador() ? "Historial de turnos" : "Mis turnos", pestanaHistorial());
        pestanas.addTab("Anulaciones", pestanaAnulaciones());
        pestanas.addChangeListener(e -> refrescar());
        p.add(pestanas, BorderLayout.CENTER);
    }

    private JPanel pestanaTurno() {
        // Sin turno: formulario de apertura
        JPanel abrir = new JPanel(new GridBagLayout());
        abrir.setOpaque(false);
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 10));
        form.setBackground(UI.SUPERFICIE);
        form.setBorder(UI.bordeTarjeta(22, 26, 22, 26));
        JLabel titulo = new JLabel("Abrir turno de caja");
        titulo.setFont(UI.fuente(Font.BOLD, 20));
        form.add(titulo);
        form.add(UI.ayuda("Cuente el efectivo con el que empieza la caja (sencillo para dar vuelto)."));
        form.add(new JLabel("Efectivo inicial (S/)"));
        txtMontoInicial.setFont(UI.fuente(Font.BOLD, 18));
        form.add(txtMontoInicial);
        JButton btnAbrir = UI.boton("Abrir turno");
        btnAbrir.addActionListener(e -> abrirTurno());
        txtMontoInicial.addActionListener(e -> abrirTurno());
        form.add(btnAbrir);
        form.setPreferredSize(new Dimension(460, form.getPreferredSize().height));
        abrir.add(form);

        // Con turno: indicadores, ventas del turno y cierre
        JPanel abierto = new JPanel(new BorderLayout(0, 12));
        abierto.setOpaque(false);
        JPanel norte = new JPanel(new BorderLayout(0, 8));
        norte.setOpaque(false);
        JPanel kpis = new JPanel(new GridLayout(1, 0, 16, 0));
        kpis.setOpaque(false);
        kpis.add(UI.tarjeta("Efectivo inicial", lblInicial, Iconos.Tipo.BILLETE, false));
        kpis.add(UI.tarjeta("Ventas en efectivo", lblEfectivo, Iconos.Tipo.CARRITO, false));
        kpis.add(UI.tarjeta("Yape o Plin", lblYape, Iconos.Tipo.CELULAR, false));
        kpis.add(UI.tarjeta("Efectivo en caja", lblEnCaja, Iconos.Tipo.MONEDA, true));
        norte.add(kpis, BorderLayout.CENTER);
        JPanel info = new JPanel(new BorderLayout());
        info.setOpaque(false);
        info.add(lblInfoTurno, BorderLayout.CENTER);
        JButton cerrar = UI.boton("Cerrar turno y cuadrar caja");
        cerrar.addActionListener(e -> cerrarTurno(turno));
        info.add(cerrar, BorderLayout.EAST);
        norte.add(info, BorderLayout.SOUTH);
        abierto.add(norte, BorderLayout.NORTH);
        abierto.add(ventasTurno, BorderLayout.CENTER);

        panelTurno.setOpaque(false);
        panelTurno.add(abrir, "abrir");
        panelTurno.add(abierto, "abierto");
        JPanel envoltura = new JPanel(new BorderLayout());
        envoltura.setBorder(new EmptyBorder(14, 0, 0, 0));
        envoltura.add(panelTurno);
        return envoltura;
    }

    private JPanel pestanaHistorial() {
        JPanel h = new JPanel(new BorderLayout(0, 10));
        h.setBorder(new EmptyBorder(14, 0, 0, 0));
        JPanel norte = new JPanel(new BorderLayout(8, 0));
        norte.setOpaque(false);
        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filtro.setOpaque(false);
        filtro.add(new JLabel("Período "));
        cboPeriodo.setSelectedIndex(2);
        cboPeriodo.addActionListener(e -> cargarHistorial());
        filtro.add(cboPeriodo);
        filtro.add(Box.createHorizontalStrut(10));
        filtro.add(UI.ayuda("Doble clic en un turno para ver sus ventas."));
        norte.add(filtro, BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.setOpaque(false);
        JButton ver = UI.botonSecundario("Ver ventas del turno");
        ver.addActionListener(e -> verVentasTurno());
        botones.add(ver);
        if (Sesion.esAdministrador()) {
            JButton cerrar = UI.botonSecundario("Cerrar turno seleccionado");
            cerrar.setToolTipText("Para cerrar el turno que otro usuario dejó abierto");
            cerrar.addActionListener(e -> {
                TurnoVO t = turnoSeleccionado();
                if (t != null) cerrarTurno(t);
            });
            botones.add(cerrar);
        }
        norte.add(botones, BorderLayout.EAST);
        h.add(norte, BorderLayout.NORTH);

        int[] anchos = {55, 150, 80, 110, 60, 110, 90, 120, 100, 160, 112};
        for (int i = 0; i < anchos.length; i++) UI.columnaAncha(tablaTurnos, i, anchos[i]);
        tablaTurnos.setToolTipText("Doble clic para ver las ventas del turno");
        UI.resaltar(tablaTurnos, 9, texto -> texto.startsWith("Faltante"));
        UI.insignias(tablaTurnos, 10);
        tablaTurnos.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) verVentasTurno();
            }
        });
        h.add(new JScrollPane(tablaTurnos), BorderLayout.CENTER);
        h.add(UI.ayuda("Diferencia: Faltante −S/ … si falta efectivo · Sobrante +S/ … si sobra · Cuadrado si coincide"), BorderLayout.SOUTH);
        return h;
    }

    /** Registro de anulaciones: el administrador ve todas; el personal, solo las suyas. */
    private JPanel pestanaAnulaciones() {
        JPanel a = new JPanel(new BorderLayout(0, 10));
        a.setBorder(new EmptyBorder(14, 0, 0, 0));
        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.setOpaque(false);
        JPanel kpis = new JPanel(new GridLayout(1, 0, 16, 0));
        kpis.setOpaque(false);
        kpis.add(UI.tarjeta("Ventas anuladas", lblNumAnulaciones, Iconos.Tipo.PAPELERA, false));
        kpis.add(UI.tarjeta("Monto anulado", lblMontoAnulado, Iconos.Tipo.MONEDA, true));
        norte.add(kpis, BorderLayout.NORTH);
        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filtro.setOpaque(false);
        filtro.add(new JLabel("Período "));
        cboPeriodoAnulaciones.setSelectedIndex(2);
        cboPeriodoAnulaciones.addActionListener(e -> cargarAnulaciones());
        filtro.add(cboPeriodoAnulaciones);
        filtro.add(Box.createHorizontalStrut(10));
        filtro.add(UI.ayuda(Sesion.esAdministrador() ? "Anulaciones de todos los usuarios." : "Solo sus anulaciones."));
        norte.add(filtro, BorderLayout.SOUTH);
        a.add(norte, BorderLayout.NORTH);

        int[] anchos = {120, 60, 130, 140, 150, 150, 240, 95, 90};
        for (int i = 0; i < anchos.length; i++) UI.columnaAncha(tablaAnulaciones, i, anchos[i]);
        UI.insignias(tablaAnulaciones, 5);
        a.add(new JScrollPane(tablaAnulaciones), BorderLayout.CENTER);
        a.add(UI.ayuda("Las ventas anuladas antes de existir este registro no aparecen aquí; su comprobante sigue mostrando ANULADO y el motivo."),
                BorderLayout.SOUTH);
        return a;
    }

    private void cargarAnulaciones() {
        List<AnulacionVO> lista;
        try {
            lista = ventas.listarAnulaciones(desdePeriodo(cboPeriodoAnulaciones), LocalDate.now());
        } catch (SQLException ex) {
            ex.printStackTrace();
            lista = new ArrayList<>();
        }
        modeloAnulaciones.setRowCount(0);
        double total = 0;
        for (AnulacionVO an : lista) {
            total += an.monto;
            modeloAnulaciones.addRow(new Object[]{an.fecha.format(Datos.FECHA_HORA), an.ventaId,
                    an.comprobante == null ? "—" : an.comprobante, an.usuario, Sesion.textoRol(an.rol), an.motivo,
                    an.detalle == null ? "—" : an.detalle, an.formaPago, Datos.soles(an.monto)});
        }
        lblNumAnulaciones.setText(String.valueOf(lista.size()));
        lblMontoAnulado.setText(Datos.soles(total));
    }

    public void refrescar() {
        try {
            turno = caja.turnoAbierto();
        } catch (SQLException ex) {
            turno = null;
            JOptionPane.showMessageDialog(this, "Error al consultar el turno:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        if (turno == null) {
            tarjetasTurno.show(panelTurno, "abrir");
        } else {
            lblInicial.setText(Datos.soles(turno.montoInicial));
            lblEfectivo.setText(Datos.soles(turno.efectivo));
            lblYape.setText(Datos.soles(turno.yapePlin));
            lblEnCaja.setText(Datos.soles(turno.efectivoEnCaja()));
            lblInfoTurno.setText("Turno N° " + turno.id + " abierto el " + turno.abiertoEn.format(Datos.FECHA_HORA)
                    + "  ·  " + turno.numVentas + " venta(s) por " + Datos.soles(turno.totalVendido())
                    + (turno.numAnuladas > 0 ? "  ·  " + turno.numAnuladas + " anulada(s)" : ""));
            ventasTurno.cargar(turno);
            tarjetasTurno.show(panelTurno, "abierto");
        }
        cargarHistorial();
        cargarAnulaciones();
    }

    /** Primer día del período elegido en un selector de PERIODOS (el último es hoy). */
    private static LocalDate desdePeriodo(JComboBox<String> periodo) {
        LocalDate hoy = LocalDate.now();
        switch (periodo.getSelectedIndex()) {
            case 0: return hoy;
            case 1: return hoy.minusDays(6);
            case 2: return hoy.minusDays(29);
            default: return hoy.withDayOfYear(1);
        }
    }

    private void cargarHistorial() {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = desdePeriodo(cboPeriodo);
        turnos.clear();
        modeloTurnos.setRowCount(0);
        try {
            turnos.addAll(caja.listarTurnos(desde, hoy));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        for (TurnoVO t : turnos) {
            boolean abierto = ControladorCaja.ABIERTO.equals(t.estado);
            Double dif = t.diferencia();
            String horario = t.abiertoEn.format(HORA) + " – " + (t.cerradoEn == null ? "…" : t.cerradoEn.format(HORA));
            modeloTurnos.addRow(new Object[]{t.id, t.usuario, t.abiertoEn.format(Datos.FECHA), horario, t.numVentas,
                    Datos.soles(t.totalVendido()), Datos.soles(t.montoInicial),
                    Datos.soles(abierto ? t.efectivoEnCaja() : t.esperado),
                    t.contado == null ? "—" : Datos.soles(t.contado),
                    dif == null ? "—" : textoDiferencia(dif), abierto ? "Abierto" : "Cerrado"});
        }
    }

    private static String textoDiferencia(double dif) {
        if (Math.abs(dif) < 0.005) return "Cuadrado";
        // Signo y palabra siempre visibles, para que no dependa del color
        return (dif > 0 ? "Sobrante +" : "Faltante −") + Datos.soles(Math.abs(dif));
    }

    private TurnoVO turnoSeleccionado() {
        int f = tablaTurnos.getSelectedRow();
        if (f < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un turno.");
            return null;
        }
        return turnos.get(tablaTurnos.convertRowIndexToModel(f));
    }

    private void abrirTurno() {
        Double monto = leerMonto(txtMontoInicial.getText());
        if (monto == null) {
            JOptionPane.showMessageDialog(this, "Ingrese un monto inicial válido (por ejemplo 100 o 85.50).");
            return;
        }
        try {
            caja.abrirTurno(monto);
            refrescar();
            alCambiar.run();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo abrir el turno", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Diálogo de cierre: muestra lo esperado, pide lo contado y calcula el sobrante o faltante. */
    private void cerrarTurno(TurnoVO t) {
        if (t == null) return;
        if (!ControladorCaja.ABIERTO.equals(t.estado)) {
            JOptionPane.showMessageDialog(this, "Ese turno ya está cerrado.");
            return;
        }
        double esperado = t.efectivoEnCaja();
        JTextField txtContado = new JTextField();
        txtContado.setFont(UI.fuente(Font.BOLD, 18));
        JTextField txtObs = new JTextField();
        JLabel lblDif = new JLabel(" ");
        lblDif.setFont(UI.fuente(Font.BOLD, 16));
        Runnable actualizar = () -> {
            Double c = leerMonto(txtContado.getText());
            if (c == null) {
                lblDif.setForeground(UI.TEXTO_SECUNDARIO);
                lblDif.setText("Ingrese el efectivo contado");
                return;
            }
            double dif = c - esperado;
            lblDif.setForeground(dif <= -0.005 ? UI.ACENTO_PULSADO : UI.TEXTO);
            lblDif.setText(textoDiferencia(dif));
        };
        txtContado.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { actualizar.run(); }
            @Override public void removeUpdate(DocumentEvent e) { actualizar.run(); }
            @Override public void changedUpdate(DocumentEvent e) { actualizar.run(); }
        });
        actualizar.run();

        JLabel resumen = new JLabel("<html><table>"
                + filaResumen("Usuario", t.usuario)
                + filaResumen("Efectivo inicial", Datos.soles(t.montoInicial))
                + filaResumen("Ventas en efectivo", Datos.soles(t.efectivo))
                + filaResumen("Yape o Plin (no entra a caja)", Datos.soles(t.yapePlin))
                + filaResumen("<b>Efectivo esperado</b>", "<b>" + Datos.soles(esperado) + "</b>")
                + "</table></html>");
        Object[] campos = {resumen, " ", "Efectivo contado en caja (S/):", txtContado, lblDif,
                "Observación (obligatoria si hay diferencia):", txtObs};
        while (true) {
            if (JOptionPane.showConfirmDialog(this, campos, "Cerrar turno N° " + t.id,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            Double contado = leerMonto(txtContado.getText());
            if (contado == null) {
                JOptionPane.showMessageDialog(this, "Ingrese el efectivo contado (por ejemplo 245.37).");
                continue;
            }
            String obs = txtObs.getText().trim();
            if (Math.abs(contado - esperado) >= 0.005 && obs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Hay una diferencia de caja: explique el motivo en la observación.");
                continue;
            }
            try {
                caja.cerrarTurno(t.id, contado, obs);
                JOptionPane.showMessageDialog(this, "Turno N° " + t.id + " cerrado.\nEsperado: " + Datos.soles(esperado)
                        + "\nContado: " + Datos.soles(contado) + "\nResultado: " + textoDiferencia(contado - esperado));
                refrescar();
                alCambiar.run();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo cerrar el turno", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
    }

    private static String filaResumen(String concepto, String valor) {
        return "<tr><td style='padding-right:24px'>" + concepto + "</td><td align='right'>" + valor + "</td></tr>";
    }

    private void verVentasTurno() {
        TurnoVO t = turnoSeleccionado();
        if (t == null) return;
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "Ventas del turno N° " + t.id + " · " + t.usuario,
                Dialog.ModalityType.APPLICATION_MODAL);
        JPanel c = UI.base();
        TablaVentas tv = new TablaVentas(this::refrescar);
        tv.cargar(t);
        c.add(tv, BorderLayout.CENTER);
        d.setContentPane(c);
        d.setSize(980, 560);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    /** Lee un monto en efectivo en céntimos; null si no es un número válido o es negativo. */
    static Double leerMonto(String texto) {
        try {
            double v = Double.parseDouble(texto.trim().replace(',', '.'));
            return v >= 0 ? Datos.redondear(v) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Tabla de ventas de un turno con su detalle y la anulación (administrador, o personal en su propio turno abierto). */
    private class TablaVentas extends JPanel {
        private final DefaultTableModel modelo = UI.modelo("N°", "Hora", "Comprobante", "Cliente", "Vendedor", "Pago", "Total", "Recibido", "Vuelto", "Estado");
        private final JTable tabla = UI.tabla(modelo);
        private final List<VentaVO> lista = new ArrayList<>();
        private final Runnable alAnular;
        private final JButton btnAnular = UI.botonPeligro("Anular venta");
        private TurnoVO turno;

        TablaVentas(Runnable alAnular) {
            super(new BorderLayout(0, 8));
            this.alAnular = alAnular;
            setOpaque(false);
            int[] anchos = {55, 55, 120, 190, 130, 95, 85, 85, 80, 90};
            for (int i = 0; i < anchos.length; i++) UI.columnaAncha(tabla, i, anchos[i]);
            UI.insignias(tabla, 9);
            tabla.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) verDetalle();
                }
            });
            // El botón solo se habilita sobre una venta que el usuario puede anular (el controlador lo vuelve a validar)
            tabla.getSelectionModel().addListSelectionListener(e -> actualizarBoton());
            add(new JScrollPane(tabla), BorderLayout.CENTER);
            JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            botones.setOpaque(false);
            JButton detalle = UI.botonSecundario("Ver comprobante");
            detalle.addActionListener(e -> verDetalle());
            botones.add(detalle);
            btnAnular.addActionListener(e -> anular());
            btnAnular.setEnabled(false);
            botones.add(btnAnular);
            botones.add(UI.ayuda(Sesion.esAdministrador()
                    ? "  Al anular, el stock vuelve a sus lotes. Solo en turnos abiertos."
                    : "  Puede anular solo las ventas que registró en su turno abierto. El stock vuelve a sus lotes."));
            add(botones, BorderLayout.SOUTH);
        }

        void cargar(TurnoVO turno) {
            this.turno = turno;
            lista.clear();
            modelo.setRowCount(0);
            try {
                lista.addAll(ventas.listarVentasTurno(turno.id));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            for (VentaVO v : lista) {
                Double vuelto = v.vuelto();
                modelo.addRow(new Object[]{v.id, v.fecha.format(HORA), v.comprobante == null ? "—" : v.comprobante, v.cliente == null ? "Clientes varios" : v.cliente,
                        v.vendedor, v.formaPago, Datos.soles(v.total), v.recibido == null ? "—" : Datos.soles(v.recibido),
                        vuelto == null ? "—" : Datos.soles(vuelto), v.anulada() ? "Anulada" : "Completada"});
            }
            actualizarBoton();
        }

        private void actualizarBoton() {
            int f = tabla.getSelectedRow();
            VentaVO v = f < 0 ? null : lista.get(tabla.convertRowIndexToModel(f));
            btnAnular.setEnabled(ControladorVentas.puedeAnular(v, turno));
        }

        private VentaVO seleccionada() {
            int f = tabla.getSelectedRow();
            if (f < 0) {
                JOptionPane.showMessageDialog(this, "Seleccione una venta.");
                return null;
            }
            return lista.get(tabla.convertRowIndexToModel(f));
        }

        /** El comprobante muestra el detalle, el pago y, si corresponde, la anulación. */
        private void verDetalle() {
            VentaVO v = seleccionada();
            if (v != null) DialogoComprobante.mostrar(this, v.id, false);
        }

        /** Diálogo de anulación: motivo de la lista, detalle (obligatorio con "Otro") y confirmación. */
        private void anular() {
            VentaVO v = seleccionada();
            if (v == null) return;
            if (v.anulada()) {
                JOptionPane.showMessageDialog(this, "Esa venta ya está anulada.");
                return;
            }
            JComboBox<String> cboMotivo = new JComboBox<>(Datos.MOTIVOS_ANULACION);
            cboMotivo.setSelectedIndex(-1);
            JTextField txtDetalle = new JTextField();
            JLabel lblDetalle = new JLabel();
            Runnable etiqueta = () -> lblDetalle.setText(Datos.MOTIVO_OTRO.equals(cboMotivo.getSelectedItem())
                    ? "Detalle (obligatorio, mínimo " + Datos.DETALLE_MINIMO_OTRO + " caracteres):" : "Detalle (opcional):");
            cboMotivo.addActionListener(e -> etiqueta.run());
            etiqueta.run();
            JLabel aviso = new JLabel("<html>Anular la venta N° " + v.id + " por <b>" + Datos.soles(v.total) + "</b> (" + v.formaPago + ").<br>"
                    + "El stock vuelve a sus lotes y el monto sale del cuadre del turno. No se puede deshacer.</html>");
            Object[] campos = {aviso, " ", "Motivo de la anulación:", cboMotivo, lblDetalle, txtDetalle};
            while (true) {
                if (JOptionPane.showConfirmDialog(this, campos, "Anular venta", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) return;
                String motivo = (String) cboMotivo.getSelectedItem();
                String error = ControladorVentas.validarMotivo(motivo, txtDetalle.getText());
                if (error != null) {
                    JOptionPane.showMessageDialog(this, error);
                    continue;
                }
                try {
                    ventas.anularVenta(v.id, motivo, txtDetalle.getText());
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo anular", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                break;
            }
            JOptionPane.showMessageDialog(this, "Venta N° " + v.id + " anulada. El stock volvió a sus lotes."
                    + (Datos.EFECTIVO.equals(v.formaPago) ? "\nDevuelva " + Datos.soles(v.total) + " en efectivo al cliente."
                    : "\nEl sistema no devuelve el pago con " + v.formaPago + ": devuelva " + Datos.soles(v.total)
                    + " al cliente desde el celular del negocio."));
            cargar(turno);
            alAnular.run();
            alCambiar.run();
        }
    }
}
