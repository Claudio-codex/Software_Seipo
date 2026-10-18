package seipo.vista;

import seipo.controlador.ControladorCompras;
import seipo.controlador.ControladorCompras.AlertaVO;
import seipo.controlador.ControladorCompras.DetalleOrdenVO;
import seipo.controlador.ControladorCompras.OrdenVO;
import seipo.controlador.ControladorCompras.ProveedorVO;
import seipo.controlador.ControladorInventario;
import seipo.controlador.ControladorInventario.LineaRecepcion;
import seipo.modelo.Datos;
import seipo.modelo.Datos.ProductoVenta;
import seipo.modelo.Sesion;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Alertas de stock, órdenes de compra (crear, aprobar, recibir) y proveedores. */
public class PanelCompras extends JPanel {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
    private static final Color ROJO = new Color(254, 226, 226), AMBAR = new Color(254, 243, 199),
            CELESTE = new Color(219, 234, 254), VERDE = new Color(220, 252, 231), GRIS = new Color(241, 245, 249);
    private static final String[] FILTROS = {"Pendientes", "Aprobadas", "Recibidas", "Rechazadas", "Todas"};
    private static final String[] ESTADOS = {ControladorCompras.PENDIENTE, ControladorCompras.APROBADA,
            ControladorCompras.RECIBIDA, ControladorCompras.RECHAZADA, null};

    private final ControladorCompras controlador = new ControladorCompras();
    private final ControladorInventario inventario = new ControladorInventario();
    private final Runnable alCambiar;

    private final DefaultTableModel modeloAlertas = UI.modelo("Código", "Producto", "Disponible", "Mín.", "Máx.",
            "En pedido", "Estado", "Sugerido");
    private final JTable tablaAlertas = UI.tabla(modeloAlertas);
    private List<AlertaVO> alertas = new ArrayList<>();

    private final DefaultTableModel modeloOrdenes = UI.modelo("N°", "Creada", "Proveedor", "Ítems", "Unidades",
            "Estado", "Creó", "Aprobó", "Guía", "Observación");
    private final JTable tablaOrdenes = UI.tabla(modeloOrdenes);
    private final JComboBox<String> cmbFiltro = new JComboBox<>(FILTROS);
    private List<OrdenVO> ordenes = new ArrayList<>();

    private final DefaultTableModel modeloProveedores = UI.modelo("RUC", "Razón social", "Contacto", "Teléfono", "Correo", "Estado");
    private final JTable tablaProveedores = UI.tabla(modeloProveedores);
    private List<ProveedorVO> proveedores = new ArrayList<>();

    /** alCambiar se ejecuta cuando cambian alertas u órdenes, para actualizar el aviso del menú. */
    public PanelCompras(Runnable alCambiar) {
        this.alCambiar = alCambiar;
        setLayout(new BorderLayout());
        JPanel p = UI.base();
        add(p);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setFont(UI.NEGRITA);
        pestanas.addTab("Alertas de stock", pestanaAlertas());
        pestanas.addTab("Órdenes de compra", pestanaOrdenes());
        pestanas.addTab("Proveedores", pestanaProveedores());
        p.add(pestanas, BorderLayout.CENTER);
    }

    public void refrescar() {
        cargarAlertas();
        cargarOrdenes();
        cargarProveedores();
    }

    // ------------------------------------------------------------------ Alertas

    private JPanel pestanaAlertas() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setOpaque(false);
        JButton crear = UI.boton("Crear orden de compra");
        crear.addActionListener(e -> ordenDesdeAlertas());
        barra.add(crear);
        JLabel info = new JLabel("💡 Seleccione los productos a reponer (o ninguno para todos). La cantidad sugerida "
                + "completa el stock máximo descontando lo que ya está en pedido. El sobrestock indica productos para promocionar.");
        info.setFont(new Font("SansSerif", Font.ITALIC, 12));
        info.setForeground(new Color(100, 116, 139));
        JPanel norte = new JPanel(new BorderLayout(0, 5));
        norte.setOpaque(false);
        norte.add(barra, BorderLayout.NORTH);
        norte.add(info, BorderLayout.CENTER);
        p.add(norte, BorderLayout.NORTH);

        tablaAlertas.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tablaAlertas.setDefaultRenderer(Object.class, colores(fila -> {
            String estado = alertas.get(fila).estado;
            return ControladorCompras.AGOTADO.equals(estado) ? ROJO
                    : ControladorCompras.STOCK_BAJO.equals(estado) ? AMBAR : CELESTE;
        }));
        p.add(new JScrollPane(tablaAlertas), BorderLayout.CENTER);
        return p;
    }

    private void cargarAlertas() {
        modeloAlertas.setRowCount(0);
        try {
            alertas = controlador.listarAlertas();
            for (AlertaVO a : alertas) {
                modeloAlertas.addRow(new Object[]{a.codigo, a.nombre, a.disponible, a.minimo,
                        a.maximo == null ? "—" : a.maximo, a.enPedido == 0 ? "" : a.enPedido, a.estado,
                        a.esReposicion() ? a.sugerido() : ""});
            }
        } catch (SQLException ex) {
            error("Error al cargar las alertas", ex);
        }
    }

    private void ordenDesdeAlertas() {
        List<DetalleOrdenVO> lineas = new ArrayList<>();
        int[] filas = tablaAlertas.getSelectedRows();
        List<AlertaVO> elegidas = new ArrayList<>();
        if (filas.length == 0) elegidas.addAll(alertas);
        else for (int f : filas) elegidas.add(alertas.get(tablaAlertas.convertRowIndexToModel(f)));
        for (AlertaVO a : elegidas) {
            if (a.esReposicion() && a.sugerido() > 0) lineas.add(new DetalleOrdenVO(a.productoId, a.codigo, a.nombre, a.sugerido()));
        }
        if (lineas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay productos por reponer en la selección (o ya están en pedido).");
            return;
        }
        dialogoOrden(lineas);
    }

    // ---------------------------------------------------------------- Órdenes

    private JPanel pestanaOrdenes() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setOpaque(false);
        JButton nueva = UI.boton("+ Nueva orden");
        JButton detalle = UI.boton("Ver detalle");
        JButton recibir = UI.boton("Recibir mercadería");
        nueva.addActionListener(e -> dialogoOrden(new ArrayList<>()));
        detalle.addActionListener(e -> verDetalle());
        recibir.addActionListener(e -> recibir());
        barra.add(nueva);
        barra.add(detalle);
        barra.add(recibir);
        if (Sesion.esAdministrador()) {
            JButton aprobar = UI.boton("Aprobar");
            JButton rechazar = UI.boton("Rechazar");
            aprobar.addActionListener(e -> aprobar());
            rechazar.addActionListener(e -> rechazar());
            barra.add(aprobar);
            barra.add(rechazar);
        }
        barra.add(new JLabel("   Mostrar:"));
        barra.add(cmbFiltro);
        cmbFiltro.addActionListener(e -> cargarOrdenes());
        p.add(barra, BorderLayout.NORTH);

        tablaOrdenes.setDefaultRenderer(Object.class, colores(fila -> {
            String estado = ordenes.get(fila).estado;
            switch (estado) {
                case ControladorCompras.PENDIENTE: return AMBAR;
                case ControladorCompras.APROBADA: return CELESTE;
                case ControladorCompras.RECIBIDA: return VERDE;
                default: return GRIS;
            }
        }));
        p.add(new JScrollPane(tablaOrdenes), BorderLayout.CENTER);
        return p;
    }

    private void cargarOrdenes() {
        modeloOrdenes.setRowCount(0);
        try {
            ordenes = controlador.listarOrdenes(ESTADOS[cmbFiltro.getSelectedIndex()]);
            for (OrdenVO o : ordenes) {
                String obs = o.observacion == null ? "" : o.observacion;
                if (o.motivoRechazo != null) obs += (obs.isEmpty() ? "" : " · ") + "Rechazo: " + o.motivoRechazo;
                modeloOrdenes.addRow(new Object[]{o.id, o.creadoEn == null ? "" : o.creadoEn.format(FECHA_HORA),
                        o.proveedor, o.items, o.unidades, textoEstado(o.estado), o.creadoPor,
                        o.aprobadoPor == null ? "" : o.aprobadoPor, o.guia == null ? "" : o.guia, obs});
            }
        } catch (SQLException ex) {
            error("Error al cargar las órdenes", ex);
        }
    }

    /** Diálogo para crear una orden; lineas trae las cantidades sugeridas (puede estar vacía). */
    private void dialogoOrden(List<DetalleOrdenVO> lineas) {
        List<ProveedorVO> activos;
        try {
            activos = controlador.listarProveedores(true);
        } catch (SQLException ex) {
            error("Error al cargar los proveedores", ex);
            return;
        }
        if (activos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Registre primero un proveedor en la pestaña Proveedores.");
            return;
        }
        JComboBox<ProveedorVO> cmbProveedor = new JComboBox<>(activos.toArray(new ProveedorVO[0]));
        DefaultTableModel m = new DefaultTableModel(new String[]{"Código", "Producto", "Cantidad"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 2; }
            @Override public Class<?> getColumnClass(int c) { return c == 2 ? Integer.class : String.class; }
        };
        for (DetalleOrdenVO d : lineas) m.addRow(new Object[]{d.codigo, d.nombre, d.cantidad});
        JTable t = UI.tabla(m);
        JScrollPane scroll = new JScrollPane(t);
        scroll.setPreferredSize(new Dimension(520, 220));

        JTextField txtCodigo = new JTextField(10);
        JButton agregar = UI.boton("Agregar");
        JButton quitar = UI.boton("Quitar");
        Runnable agregarProducto = () -> {
            String codigo = txtCodigo.getText().trim();
            if (codigo.isEmpty()) return;
            try {
                ProductoVenta pr = inventario.buscarPorCodigo(codigo);
                if (pr == null) {
                    JOptionPane.showMessageDialog(this, "No existe un producto con código " + codigo + ".");
                    return;
                }
                for (DetalleOrdenVO d : lineas) {
                    if (d.productoId == pr.id) {
                        JOptionPane.showMessageDialog(this, "El producto ya está en la orden.");
                        return;
                    }
                }
                lineas.add(new DetalleOrdenVO(pr.id, pr.codigo, pr.nombre, 1));
                m.addRow(new Object[]{pr.codigo, pr.nombre, 1});
                txtCodigo.setText("");
            } catch (SQLException ex) {
                error("Error al buscar el producto", ex);
            }
        };
        txtCodigo.addActionListener(e -> agregarProducto.run());
        agregar.addActionListener(e -> agregarProducto.run());
        quitar.addActionListener(e -> {
            int f = t.getSelectedRow();
            if (f >= 0) {
                lineas.remove(f);
                m.removeRow(f);
            }
        });
        JPanel filaAgregar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filaAgregar.add(new JLabel("Código:"));
        filaAgregar.add(txtCodigo);
        filaAgregar.add(agregar);
        filaAgregar.add(quitar);
        JTextField txtObs = new JTextField();

        Object[] campos = {"Proveedor:", cmbProveedor, "Productos (puede editar la cantidad):", scroll, filaAgregar,
                "Observación:", txtObs};
        while (true) {
            if (JOptionPane.showConfirmDialog(this, campos, "Nueva orden de compra",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            if (t.isEditing()) t.getCellEditor().stopCellEditing();
            if (lineas.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Agregue al menos un producto.");
                continue;
            }
            boolean valido = true;
            for (int i = 0; i < lineas.size(); i++) {
                Object v = m.getValueAt(i, 2);
                int cantidad = v instanceof Integer ? (Integer) v : 0;
                if (cantidad <= 0) {
                    JOptionPane.showMessageDialog(this, "La cantidad de " + lineas.get(i).nombre + " debe ser mayor que 0.");
                    valido = false;
                    break;
                }
                lineas.get(i).cantidad = cantidad;
            }
            if (!valido) continue;
            try {
                ProveedorVO prov = (ProveedorVO) cmbProveedor.getSelectedItem();
                int n = controlador.crearOrden(prov.id, lineas, txtObs.getText().trim());
                cmbFiltro.setSelectedIndex(0);
                refrescar();
                alCambiar.run();
                JOptionPane.showMessageDialog(this, "Orden de compra N° " + n + " creada. Queda pendiente de aprobación.");
                return;
            } catch (SQLException ex) {
                error("No se pudo crear la orden", ex);
                return;
            }
        }
    }

    private void verDetalle() {
        OrdenVO o = ordenSeleccionada(null);
        if (o == null) return;
        try {
            StringBuilder sb = new StringBuilder("Orden N° " + o.id + " — " + o.proveedor + " — " + textoEstado(o.estado) + "\n\n");
            for (DetalleOrdenVO d : controlador.detalleOrden(o.id)) {
                sb.append(d.cantidad).append(" × ").append(d.codigo).append("  ").append(d.nombre).append('\n');
            }
            if (o.guia != null) sb.append("\nRecibida con guía ").append(o.guia);
            JOptionPane.showMessageDialog(this, sb.toString(), "Detalle de la orden", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            error("Error al cargar el detalle", ex);
        }
    }

    private void aprobar() {
        OrdenVO o = ordenSeleccionada(ControladorCompras.PENDIENTE);
        if (o == null) return;
        if (JOptionPane.showConfirmDialog(this, "¿Aprobar la orden N° " + o.id + " a " + o.proveedor + " ("
                + o.unidades + " unidades)?", "Aprobar orden", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            controlador.aprobarOrden(o.id);
            refrescar();
            alCambiar.run();
        } catch (SQLException ex) {
            error("No se pudo aprobar", ex);
        }
    }

    private void rechazar() {
        OrdenVO o = ordenSeleccionada(ControladorCompras.PENDIENTE);
        if (o == null) return;
        String motivo = JOptionPane.showInputDialog(this, "Motivo del rechazo de la orden N° " + o.id + ":");
        if (motivo == null) return;
        if (motivo.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Indique el motivo del rechazo.");
            return;
        }
        try {
            controlador.rechazarOrden(o.id, motivo.trim());
            refrescar();
            alCambiar.run();
        } catch (SQLException ex) {
            error("No se pudo rechazar", ex);
        }
    }

    /** Recepción de una orden aprobada: por cada producto se indica lo recibido, lote, vencimiento y ubicación. */
    private void recibir() {
        OrdenVO o = ordenSeleccionada(ControladorCompras.APROBADA);
        if (o == null) return;
        List<DetalleOrdenVO> detalle;
        try {
            detalle = controlador.detalleOrden(o.id);
        } catch (SQLException ex) {
            error("Error al cargar el detalle", ex);
            return;
        }
        DefaultTableModel m = new DefaultTableModel(new String[]{"Código", "Producto", "Pedido", "Recibido",
                "Lote", "Vence (dd/mm/aaaa)", "Ubicación"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c >= 3; }
            @Override public Class<?> getColumnClass(int c) { return c == 2 || c == 3 ? Integer.class : String.class; }
        };
        for (DetalleOrdenVO d : detalle) m.addRow(new Object[]{d.codigo, d.nombre, d.cantidad, d.cantidad, "", "", ""});
        JTable t = UI.tabla(m);
        JScrollPane scroll = new JScrollPane(t);
        scroll.setPreferredSize(new Dimension(760, 220));
        JTextField txtGuia = new JTextField();
        Object[] campos = {"Orden N° " + o.id + " — " + o.proveedor, "Guía de remisión:", txtGuia,
                "Complete lote y vencimiento (vacío si no vence). Recibido = 0 si no llegó.", scroll};

        while (true) {
            if (JOptionPane.showConfirmDialog(this, campos, "Recibir mercadería",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            if (t.isEditing()) t.getCellEditor().stopCellEditing();
            String guia = txtGuia.getText().trim();
            if (guia.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Indique la guía de remisión.");
                continue;
            }
            List<LineaRecepcion> lineas = new ArrayList<>();
            String problema = null;
            for (int i = 0; i < detalle.size() && problema == null; i++) {
                DetalleOrdenVO d = detalle.get(i);
                Object v = m.getValueAt(i, 3);
                int recibido = v instanceof Integer ? (Integer) v : -1;
                String lote = texto(m.getValueAt(i, 4)), venc = texto(m.getValueAt(i, 5)), ubic = texto(m.getValueAt(i, 6));
                if (recibido < 0) { problema = d.nombre + ": la cantidad recibida no es válida."; break; }
                if (recibido == 0) continue;
                if (lote.isEmpty()) { problema = d.nombre + ": indique el número de lote."; break; }
                LocalDate vencimiento = null;
                if (!venc.isEmpty()) {
                    try {
                        vencimiento = LocalDate.parse(venc, Datos.FECHA_COMPLETA);
                    } catch (DateTimeParseException ex) {
                        problema = d.nombre + ": fecha de vencimiento inválida (use dd/mm/aaaa).";
                        break;
                    }
                    if (vencimiento.isBefore(LocalDate.now())) { problema = d.nombre + ": el lote ya está vencido."; break; }
                }
                ProductoVenta pr = new ProductoVenta(d.productoId, d.codigo, d.nombre, null, 0, 0);
                lineas.add(new LineaRecepcion(pr, lote, vencimiento, recibido, ubic));
            }
            if (problema == null && lineas.isEmpty()) problema = "No se recibió ningún producto.";
            if (problema != null) {
                JOptionPane.showMessageDialog(this, problema);
                continue;
            }
            try {
                inventario.registrarRecepcion(guia, o.proveedor, lineas, o.id);
                refrescar();
                alCambiar.run();
                JOptionPane.showMessageDialog(this, "Mercadería recibida. La orden N° " + o.id + " quedó como Recibida y el stock fue actualizado.");
                return;
            } catch (SQLException ex) {
                error("No se pudo registrar la recepción", ex);
                return;
            }
        }
    }

    /** Orden seleccionada en la tabla; si estado no es null, exige que la orden esté en ese estado. */
    private OrdenVO ordenSeleccionada(String estado) {
        int fila = tablaOrdenes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una orden de la tabla.");
            return null;
        }
        OrdenVO o = ordenes.get(tablaOrdenes.convertRowIndexToModel(fila));
        if (estado != null && !estado.equals(o.estado)) {
            JOptionPane.showMessageDialog(this, "Esta acción requiere una orden en estado " + textoEstado(estado).toLowerCase() + ".");
            return null;
        }
        return o;
    }

    // ------------------------------------------------------------- Proveedores

    private JPanel pestanaProveedores() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setOpaque(false);
        JButton nuevo = UI.boton("+ Nuevo proveedor");
        nuevo.addActionListener(e -> nuevoProveedor());
        barra.add(nuevo);
        if (Sesion.esAdministrador()) {
            JButton editar = UI.boton("Editar");
            editar.addActionListener(e -> editarProveedor());
            barra.add(editar);
        }
        p.add(barra, BorderLayout.NORTH);
        p.add(new JScrollPane(tablaProveedores), BorderLayout.CENTER);
        return p;
    }

    private void cargarProveedores() {
        modeloProveedores.setRowCount(0);
        try {
            proveedores = controlador.listarProveedores(false);
            for (ProveedorVO pr : proveedores) {
                modeloProveedores.addRow(new Object[]{pr.ruc, pr.razonSocial, texto(pr.contacto), texto(pr.telefono),
                        texto(pr.correo), pr.activo ? "Activo" : "Inactivo"});
            }
        } catch (SQLException ex) {
            error("Error al cargar los proveedores", ex);
        }
    }

    private void nuevoProveedor() {
        JTextField ruc = new JTextField(), razon = new JTextField(), contacto = new JTextField(),
                tel = new JTextField(), correo = new JTextField();
        Object[] campos = {"RUC (11 dígitos):", ruc, "Razón social:", razon, "Contacto:", contacto,
                "Teléfono:", tel, "Correo:", correo};
        if (JOptionPane.showConfirmDialog(this, campos, "Nuevo proveedor",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (!ruc.getText().trim().matches("\\d{11}") || razon.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El RUC debe tener 11 dígitos y la razón social es obligatoria.");
            return;
        }
        try {
            controlador.registrarProveedor(ruc.getText().trim(), razon.getText().trim(), contacto.getText().trim(),
                    tel.getText().trim(), correo.getText().trim());
            cargarProveedores();
        } catch (SQLException ex) {
            error("No se pudo registrar (es posible que el RUC ya exista)", ex);
        }
    }

    private void editarProveedor() {
        int fila = tablaProveedores.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un proveedor de la tabla.");
            return;
        }
        ProveedorVO pr = proveedores.get(tablaProveedores.convertRowIndexToModel(fila));
        JTextField razon = new JTextField(pr.razonSocial), contacto = new JTextField(texto(pr.contacto)),
                tel = new JTextField(texto(pr.telefono)), correo = new JTextField(texto(pr.correo));
        JCheckBox activo = new JCheckBox("Proveedor activo", pr.activo);
        Object[] campos = {"RUC: " + pr.ruc, "Razón social:", razon, "Contacto:", contacto, "Teléfono:", tel,
                "Correo:", correo, activo};
        if (JOptionPane.showConfirmDialog(this, campos, "Editar proveedor",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (razon.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "La razón social es obligatoria.");
            return;
        }
        try {
            controlador.actualizarProveedor(pr.id, razon.getText().trim(), contacto.getText().trim(),
                    tel.getText().trim(), correo.getText().trim(), activo.isSelected());
            cargarProveedores();
        } catch (SQLException ex) {
            error("No se pudo actualizar el proveedor", ex);
        }
    }

    // ---------------------------------------------------------------- Utilidades

    private interface ColorDeFila { Color de(int filaModelo); }

    private static DefaultTableCellRenderer colores(ColorDeFila color) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int fila, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foco, fila, col);
                if (!sel) c.setBackground(color.de(t.convertRowIndexToModel(fila)));
                return c;
            }
        };
    }

    private static String textoEstado(String estado) {
        switch (estado) {
            case ControladorCompras.PENDIENTE: return "Pendiente";
            case ControladorCompras.APROBADA: return "Aprobada";
            case ControladorCompras.RECIBIDA: return "Recibida";
            default: return "Rechazada";
        }
    }

    private static String texto(Object v) {
        return v == null ? "" : v.toString().trim();
    }

    private void error(String titulo, SQLException ex) {
        JOptionPane.showMessageDialog(this, titulo + ":\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
