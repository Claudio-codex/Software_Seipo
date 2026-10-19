package seipo.vista;

import seipo.controlador.ControladorPasarela;
import seipo.controlador.ControladorPasarela.ConfiguracionVO;
import seipo.controlador.ControladorPasarela.TransaccionVO;
import seipo.modelo.Datos;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

/** Transacciones de la pasarela de pago, su configuración y los datos del negocio para el comprobante (solo administrador). */
public class PanelPagos extends JPanel {

    private static final String[] PERIODOS = {"Hoy", "Últimos 7 días", "Últimos 30 días", "Este año"};

    private final ControladorPasarela pasarela = new ControladorPasarela();

    // Transacciones
    private final JComboBox<String> cboPeriodo = new JComboBox<>(PERIODOS);
    private final JLabel lblAprobadas = new JLabel(), lblMonto = new JLabel(), lblRechazadas = new JLabel(), lblTasa = new JLabel();
    private final DefaultTableModel modelo = UI.modelo("Fecha", "Usuario", "Pago", "Medio", "Monto", "Estado", "Autorización",
            "Venta", "Token", "Respuesta");
    private final JTable tabla = UI.tabla(modelo);
    private final List<TransaccionVO> transacciones = new ArrayList<>();

    // Configuración
    private final JTextField txtNombre = new JTextField(), txtRuc = new JTextField(), txtDireccion = new JTextField(),
            txtProveedor = new JTextField(), txtComercio = new JTextField();
    private final JPasswordField txtClave = new JPasswordField();
    private final JCheckBox chkActiva = new JCheckBox("Aceptar cobros con tarjeta y con Yape o Plin");

    public PanelPagos() {
        setLayout(new BorderLayout());
        JPanel p = UI.base();
        add(p);
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Transacciones", pestanaTransacciones());
        pestanas.addTab("Configuración", pestanaConfiguracion());
        pestanas.addChangeListener(e -> refrescar());
        p.add(pestanas, BorderLayout.CENTER);
    }

    private JPanel pestanaTransacciones() {
        JPanel t = new JPanel(new BorderLayout(0, 10));
        t.setBorder(new EmptyBorder(14, 0, 0, 0));
        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.setOpaque(false);
        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filtro.setOpaque(false);
        filtro.add(new JLabel("Período "));
        cboPeriodo.setSelectedIndex(1);
        cboPeriodo.addActionListener(e -> cargarTransacciones());
        filtro.add(cboPeriodo);
        filtro.add(Box.createHorizontalStrut(10));
        filtro.add(UI.ayuda("Doble clic en una transacción con venta para ver su comprobante."));
        norte.add(filtro, BorderLayout.NORTH);
        JPanel kpis = new JPanel(new GridLayout(1, 0, 10, 0));
        kpis.setOpaque(false);
        kpis.add(UI.tarjeta("Cobros aprobados", lblAprobadas));
        kpis.add(UI.tarjeta("Monto aprobado", lblMonto));
        kpis.add(UI.tarjeta("Rechazados", lblRechazadas));
        kpis.add(UI.tarjeta("Tasa de aprobación", lblTasa));
        norte.add(kpis, BorderLayout.CENTER);
        t.add(norte, BorderLayout.NORTH);

        int[] anchos = {105, 130, 85, 140, 80, 100, 85, 55, 190, 230};
        for (int i = 0; i < anchos.length; i++) UI.columnaAncha(tabla, i, anchos[i]);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UI.colorearFilas(tabla, f -> {
            String estado = transacciones.get(f).estado;
            if (ControladorPasarela.RECHAZADA.equals(estado)) return UI.FILA_ROJA;
            if (ControladorPasarela.REVERTIDA.equals(estado) || ControladorPasarela.REEMBOLSADA.equals(estado)) return UI.FILA_AMBAR;
            return ControladorPasarela.PENDIENTE.equals(estado) ? UI.FILA_CELESTE : null;
        });
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int f = tabla.getSelectedRow();
                if (e.getClickCount() != 2 || f < 0) return;
                TransaccionVO tr = transacciones.get(tabla.convertRowIndexToModel(f));
                if (tr.ventaId != null) DialogoComprobante.mostrar(PanelPagos.this, tr.ventaId, false);
            }
        });
        t.add(new JScrollPane(tabla), BorderLayout.CENTER);
        t.add(UI.ayuda("Rojo: rechazada por la pasarela · Ámbar: revertida (la venta no se registró) o reembolsada (venta anulada)"
                + " · Celeste: sin respuesta"), BorderLayout.SOUTH);
        return t;
    }

    private JPanel pestanaConfiguracion() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UI.SUPERFICIE);
        form.setBorder(new CompoundBorder(new LineBorder(UI.BORDE, 1, true), new EmptyBorder(18, 22, 18, 22)));
        int y = 0;
        y = titulo(form, y, "Datos del negocio", "Aparecen en la boleta y en la factura.");
        y = campo(form, y, "Nombre o razón social", txtNombre);
        y = campo(form, y, "RUC", txtRuc);
        y = campo(form, y, "Dirección", txtDireccion);
        y = titulo(form, y, "Pasarela de pago", "Credenciales que entrega el proveedor de la pasarela. Modo prueba: los cobros son simulados.");
        GridBagConstraints g = restricciones(1, y++);
        chkActiva.setOpaque(false);
        form.add(chkActiva, g);
        y = campo(form, y, "Proveedor", txtProveedor);
        y = campo(form, y, "Código de comercio", txtComercio);
        JPanel clave = new JPanel(new BorderLayout(8, 0));
        clave.setOpaque(false);
        clave.add(txtClave, BorderLayout.CENTER);
        JCheckBox mostrar = new JCheckBox("Mostrar");
        mostrar.setOpaque(false);
        char oculto = txtClave.getEchoChar();
        mostrar.addActionListener(e -> txtClave.setEchoChar(mostrar.isSelected() ? (char) 0 : oculto));
        clave.add(mostrar, BorderLayout.EAST);
        y = campo(form, y, "Clave secreta", clave);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.setOpaque(false);
        JButton probar = UI.botonSecundario("Probar conexión"), guardar = UI.boton("Guardar cambios");
        probar.addActionListener(e -> probarConexion(probar));
        guardar.addActionListener(e -> guardar());
        botones.add(probar);
        botones.add(guardar);
        g = restricciones(0, y++);
        g.gridwidth = 2;
        g.insets = new Insets(16, 0, 0, 0);
        form.add(botones, g);

        JLabel pruebas = UI.ayuda("<html><b>Datos de prueba de la pasarela</b><br>"
                + "Tarjeta: " + ControladorPasarela.TARJETA_PRUEBA + " (o cualquier número válido) se aprueba; "
                + "si termina en " + ControladorPasarela.FIN_SIN_FONDOS + " se rechaza por fondos insuficientes y si termina en "
                + ControladorPasarela.FIN_BLOQUEADA + ", por tarjeta bloqueada. Vencimiento futuro (MM/AA) y CVV de 3 dígitos.<br>"
                + "Yape o Plin: celular de 9 dígitos que empiece con 9 y cualquier código de aprobación de 6 dígitos, salvo "
                + ControladorPasarela.CODIGO_RECHAZADO + ".<br>"
                + "No se guarda el número de tarjeta ni el CVV: solo la marca y los últimos 4 dígitos.</html>");
        pruebas.setBorder(new EmptyBorder(12, 2, 0, 0));

        JPanel columna = new JPanel(new BorderLayout());
        columna.setOpaque(false);
        columna.add(form, BorderLayout.NORTH);
        columna.add(pruebas, BorderLayout.CENTER);
        columna.setPreferredSize(new Dimension(680, columna.getPreferredSize().height));
        JPanel envoltura = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        envoltura.setBorder(new EmptyBorder(14, 0, 0, 0));
        envoltura.add(columna);
        // Con la ventana baja, el formulario se desplaza para que los botones sigan al alcance
        JScrollPane desplazable = new JScrollPane(envoltura);
        desplazable.setBorder(null);
        desplazable.getVerticalScrollBar().setUnitIncrement(16);
        JPanel pestana = new JPanel(new BorderLayout());
        pestana.add(desplazable);
        return pestana;
    }

    private static int titulo(JPanel form, int y, String texto, String ayuda) {
        GridBagConstraints g = restricciones(0, y);
        g.gridwidth = 2;
        g.insets = new Insets(y == 0 ? 0 : 16, 0, 2, 0);
        JLabel t = new JLabel(texto);
        t.setFont(UI.fuente(Font.BOLD, 16));
        form.add(t, g);
        g = restricciones(0, y + 1);
        g.gridwidth = 2;
        g.insets = new Insets(0, 0, 8, 0);
        form.add(UI.ayuda(ayuda), g);
        return y + 2;
    }

    private static int campo(JPanel form, int y, String etiqueta, JComponent campo) {
        GridBagConstraints g = restricciones(0, y);
        g.weightx = 0;
        JLabel l = new JLabel(etiqueta);
        l.setForeground(UI.TEXTO_SECUNDARIO);
        form.add(l, g);
        form.add(campo, restricciones(1, y));
        return y + 1;
    }

    private static GridBagConstraints restricciones(int x, int y) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(4, 0, 4, 14);
        return g;
    }

    public void refrescar() {
        cargarTransacciones();
        try {
            ConfiguracionVO c = pasarela.leerConfiguracion();
            txtNombre.setText(c.empresaNombre);
            txtRuc.setText(c.empresaRuc);
            txtDireccion.setText(c.empresaDireccion);
            chkActiva.setSelected(c.activa);
            txtProveedor.setText(c.proveedor);
            txtComercio.setText(c.comercio);
            txtClave.setText(c.clave);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al leer la configuración:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarTransacciones() {
        LocalDate hoy = LocalDate.now();
        LocalDate desde;
        switch (cboPeriodo.getSelectedIndex()) {
            case 0: desde = hoy; break;
            case 1: desde = hoy.minusDays(6); break;
            case 2: desde = hoy.minusDays(29); break;
            default: desde = hoy.withDayOfYear(1); break;
        }
        transacciones.clear();
        modelo.setRowCount(0);
        try {
            transacciones.addAll(pasarela.listarTransacciones(desde, hoy));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        int aprobadas = 0, rechazadas = 0;
        double monto = 0;
        for (TransaccionVO t : transacciones) {
            modelo.addRow(new Object[]{t.creado.format(Datos.FECHA_HORA), t.usuario, t.formaPago, t.medio == null ? "—" : t.medio,
                    Datos.soles(t.monto), textoEstado(t.estado), t.codigoAutorizacion == null ? "—" : t.codigoAutorizacion,
                    t.ventaId == null ? "—" : t.ventaId, t.token, t.mensaje == null ? "" : t.mensaje});
            if (ControladorPasarela.RECHAZADA.equals(t.estado)) {
                rechazadas++;
            } else if (t.codigoAutorizacion != null) {
                // Las revertidas y reembolsadas también fueron aprobadas por la pasarela
                aprobadas++;
                if (t.aprobada()) monto += t.monto;
            }
        }
        lblAprobadas.setText(String.valueOf(aprobadas));
        lblMonto.setText(Datos.soles(monto));
        lblMonto.setToolTipText("Cobros aprobados que siguen vigentes (sin revertidos ni reembolsados)");
        lblRechazadas.setText(String.valueOf(rechazadas));
        lblTasa.setText(aprobadas + rechazadas == 0 ? "—" : Graficos.porcentaje((double) aprobadas / (aprobadas + rechazadas)));
    }

    private static String textoEstado(String estado) {
        return estado.charAt(0) + estado.substring(1).toLowerCase();
    }

    private ConfiguracionVO leerFormulario() {
        ConfiguracionVO c = new ConfiguracionVO();
        c.empresaNombre = txtNombre.getText().trim();
        c.empresaRuc = txtRuc.getText().trim();
        c.empresaDireccion = txtDireccion.getText().trim();
        c.activa = chkActiva.isSelected();
        c.proveedor = txtProveedor.getText().trim();
        c.comercio = txtComercio.getText().trim();
        c.clave = new String(txtClave.getPassword()).trim();
        return c;
    }

    private void guardar() {
        try {
            pasarela.guardarConfiguracion(leerFormulario());
            JOptionPane.showMessageDialog(this, "Configuración guardada.\nLos comprobantes nuevos usan estos datos; los ya emitidos no cambian.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo guardar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void probarConexion(JButton boton) {
        ConfiguracionVO c = leerFormulario();
        boton.setEnabled(false);
        boton.setText("Conectando…");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return pasarela.probarConexion(c);
            }

            @Override protected void done() {
                boton.setEnabled(true);
                boton.setText("Probar conexión");
                try {
                    JOptionPane.showMessageDialog(PanelPagos.this, get(), "Probar conexión", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(PanelPagos.this, causa.getMessage(), "Probar conexión", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
