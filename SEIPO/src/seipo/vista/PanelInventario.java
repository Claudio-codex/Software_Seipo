package seipo.vista;

import seipo.controlador.ControladorInventario;
import seipo.controlador.ControladorInventario.LoteVO;
import seipo.modelo.Datos;
import seipo.modelo.Sesion;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class PanelInventario extends JPanel {

    private static final String[] FILTROS = {"Todos", "Vencidos", "Por vencer"};

    private final JLabel lblTotal = new JLabel(), lblBajo = new JLabel(), lblAgotados = new JLabel(),
            lblVencidos = new JLabel(), lblPorVencer = new JLabel();
    private final DefaultTableModel modelo = UI.modelo("Fecha", "Producto", "Tipo", "Lote", "Cantidad", "Stock");
    private final DefaultTableModel modeloLotes = UI.modelo("Código", "Producto", "Lote", "Vencimiento", "Días", "Cantidad", "En merma", "Ubicación", "Estado");
    private final JTable tablaLotes = UI.tabla(modeloLotes);
    private final JTextField txtBuscarLote = new JTextField();
    private final JComboBox<String> cmbFiltro = new JComboBox<>(FILTROS);
    private final ControladorInventario controlador = new ControladorInventario();
    private List<LoteVO> lotes = new ArrayList<>();

    public PanelInventario() {
        setLayout(new BorderLayout());
        UI.columnaAncha(tablaLotes, 1, 240);
        JPanel p = UI.base();
        add(p);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setFont(UI.NEGRITA);
        pestanas.addTab("Resumen", resumen());
        pestanas.addTab("Lotes y vencimientos", lotes());
        pestanas.addTab("Recepción de mercadería", new PanelRecepcion(this::refrescar));
        p.add(pestanas, BorderLayout.CENTER);

        refrescar();
    }

    private JPanel resumen() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setOpaque(false);

        JPanel tarjetas = new JPanel(new GridLayout(1, 5, 16, 0));
        tarjetas.setOpaque(false);
        tarjetas.add(UI.tarjeta("Productos totales", lblTotal, Iconos.Tipo.CUBO, true));
        tarjetas.add(UI.tarjeta("Stock bajo", lblBajo, Iconos.Tipo.ALERTA, false));
        tarjetas.add(UI.tarjeta("Agotados", lblAgotados, Iconos.Tipo.AGOTADO, false));
        tarjetas.add(UI.tarjeta("Lotes vencidos", lblVencidos, Iconos.Tipo.CALENDARIO, false));
        tarjetas.add(UI.tarjeta("Por vencer (" + Datos.DIAS_POR_VENCER + " días)", lblPorVencer, Iconos.Tipo.RELOJ, false));
        p.add(tarjetas, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 6));
        centro.setOpaque(false);
        JLabel t = new JLabel("Movimientos recientes");
        t.setFont(UI.NEGRITA);
        centro.add(t, BorderLayout.NORTH);
        JTable tablaMovimientos = UI.tabla(modelo);
        UI.columnaAncha(tablaMovimientos, 1, 240);
        centro.add(new JScrollPane(tablaMovimientos), BorderLayout.CENTER);
        p.add(centro, BorderLayout.CENTER);
        return p;
    }

    private JPanel lotes() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);

        JPanel norte = new JPanel(new BorderLayout(8, 0));
        norte.setOpaque(false);
        norte.add(new JLabel("Buscar producto o lote: "), BorderLayout.WEST);
        norte.add(txtBuscarLote, BorderLayout.CENTER);
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derecha.setOpaque(false);
        derecha.add(new JLabel("Mostrar:"));
        derecha.add(cmbFiltro);
        JButton merma = UI.botonSecundario("Registrar merma");
        merma.addActionListener(e -> registrarMerma());
        derecha.add(merma);
        if (Sesion.esAdministrador()) {
            JButton editar = UI.botonSecundario("Editar lote");
            editar.addActionListener(e -> editarLote());
            derecha.add(editar);
        }
        norte.add(derecha, BorderLayout.EAST);
        p.add(norte, BorderLayout.NORTH);

        JLabel info = new JLabel("Orden FEFO: arriba el lote que vence primero, que es el que se vende primero. "
                + "Los lotes vencidos y las unidades en merma no se pueden vender.");
        info.setFont(UI.fuente(Font.PLAIN, 12));
        info.setForeground(UI.TEXTO_SECUNDARIO);

        UI.insignias(tablaLotes, 8);

        JPanel centro = new JPanel(new BorderLayout(0, 5));
        centro.setOpaque(false);
        centro.add(info, BorderLayout.NORTH);
        centro.add(new JScrollPane(tablaLotes), BorderLayout.CENTER);
        p.add(centro, BorderLayout.CENTER);

        txtBuscarLote.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { cargarLotes(); }
            @Override public void removeUpdate(DocumentEvent e) { cargarLotes(); }
            @Override public void changedUpdate(DocumentEvent e) { cargarLotes(); }
        });
        cmbFiltro.addActionListener(e -> cargarLotes());
        return p;
    }

    public void refrescar() {
        try {
            ControladorInventario.ResumenInventario res = controlador.obtenerResumen();
            lblTotal.setText(String.valueOf(res.totalStock));
            lblBajo.setText(String.valueOf(res.bajoStock));
            lblAgotados.setText(String.valueOf(res.agotados));
            lblVencidos.setText(String.valueOf(res.lotesVencidos));
            lblPorVencer.setText(String.valueOf(res.lotesPorVencer));

            modelo.setRowCount(0);
            List<ControladorInventario.MovimientoVO> movs = controlador.obtenerMovimientosRecientes();
            for (ControladorInventario.MovimientoVO m : movs) {
                modelo.addRow(new Object[]{m.fecha, m.producto, m.tipo, m.lote == null ? "—" : m.lote, m.cantidad, m.stock});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        cargarLotes();
    }

    private void cargarLotes() {
        modeloLotes.setRowCount(0);
        String filtro = (String) cmbFiltro.getSelectedItem();
        try {
            List<LoteVO> todos = controlador.listarLotes(txtBuscarLote.getText().trim());
            lotes = new ArrayList<>();
            for (LoteVO l : todos) {
                if ("Vencidos".equals(filtro) && !"Vencido".equals(l.estado())) continue;
                if ("Por vencer".equals(filtro) && !"Por vencer".equals(l.estado())) continue;
                lotes.add(l);
                Long dias = l.diasParaVencer();
                modeloLotes.addRow(new Object[]{l.codigo, l.producto, l.numeroLote,
                        l.vencimiento == null ? "—" : l.vencimiento.format(Datos.FECHA_COMPLETA),
                        dias == null ? "—" : dias, l.cantidad, l.enMerma == 0 ? "" : l.enMerma,
                        l.ubicacion == null ? "" : l.ubicacion, l.estado()});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void registrarMerma() {
        int fila = tablaLotes.getSelectedRow();
        Integer loteId = fila < 0 ? null : lotes.get(tablaLotes.convertRowIndexToModel(fila)).id;
        if (PanelMermas.dialogoRegistrar(this, loteId)) refrescar();
    }

    private void editarLote() {
        int fila = tablaLotes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un lote de la tabla.");
            return;
        }
        LoteVO l = lotes.get(tablaLotes.convertRowIndexToModel(fila));
        JTextField venc = new JTextField(l.vencimiento == null ? "" : l.vencimiento.format(Datos.FECHA_COMPLETA));
        JTextField ubic = new JTextField(l.ubicacion == null ? "" : l.ubicacion);
        Object[] campos = {"Producto: " + l.producto, "Lote: " + l.numeroLote,
                "Vencimiento (dd/mm/aaaa, vacío si no vence):", venc, "Ubicación:", ubic};
        if (JOptionPane.showConfirmDialog(this, campos, "Editar lote",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try {
            String texto = venc.getText().trim();
            LocalDate vencimiento = texto.isEmpty() ? null : LocalDate.parse(texto, Datos.FECHA_COMPLETA);
            controlador.actualizarLote(l.id, vencimiento, ubic.getText().trim());
            refrescar();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Fecha inválida. Use el formato dd/mm/aaaa.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el lote:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
