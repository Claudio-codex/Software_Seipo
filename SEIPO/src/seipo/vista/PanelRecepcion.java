package seipo.vista;

import seipo.controlador.ControladorInventario;
import seipo.controlador.ControladorInventario.LineaRecepcion;
import seipo.modelo.Datos;
import seipo.modelo.Datos.ProductoVenta;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

/**
 * Recepción de mercadería: se escanea o escribe el código de cada producto, se indican lote,
 * vencimiento, cantidad y ubicación, y al final se registra todo junto con la guía de remisión.
 */
public class PanelRecepcion extends JPanel {

    private final JTextField txtGuia = new JTextField(14), txtProveedor = new JTextField(20);
    private final JTextField txtCodigo = new JTextField(10), txtLote = new JTextField(8),
            txtVencimiento = new JTextField(8), txtCantidad = new JTextField(5), txtUbicacion = new JTextField(8);
    private final JLabel lblProducto = new JLabel("Escanee o escriba el código y presione Enter");
    private final DefaultTableModel modelo = UI.modelo("Código", "Producto", "Lote", "Vencimiento", "Cantidad", "Ubicación");
    private final JTable tabla = UI.tabla(modelo);
    private final ControladorInventario controlador = new ControladorInventario();
    private final List<LineaRecepcion> lineas = new ArrayList<>();
    private final Runnable alRegistrar;
    private ProductoVenta producto;

    public PanelRecepcion(Runnable alRegistrar) {
        this.alRegistrar = alRegistrar;
        setLayout(new BorderLayout(0, 10));
        UI.columnaAncha(tabla, 1, 260);
        setOpaque(false);

        JPanel cabecera = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        cabecera.setOpaque(false);
        cabecera.add(new JLabel("Guía de remisión:"));
        cabecera.add(txtGuia);
        cabecera.add(new JLabel("Proveedor:"));
        cabecera.add(txtProveedor);

        JPanel linea = new JPanel(new GridBagLayout());
        linea.setBackground(UI.SUPERFICIE);
        linea.setBorder(new CompoundBorder(new LineBorder(UI.BORDE, 1, true), new EmptyBorder(8, 10, 8, 10)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;
        String[] titulos = {"Código", "Lote", "Vencimiento (dd/mm/aaaa)", "Cantidad", "Ubicación"};
        JTextField[] campos = {txtCodigo, txtLote, txtVencimiento, txtCantidad, txtUbicacion};
        for (int i = 0; i < campos.length; i++) {
            g.gridx = i; g.gridy = 0;
            linea.add(new JLabel(titulos[i]), g);
            g.gridy = 1;
            linea.add(campos[i], g);
        }
        JButton agregar = UI.boton("Agregar");
        g.gridx = campos.length; g.gridy = 1;
        linea.add(agregar, g);
        g.gridx = 0; g.gridy = 2; g.gridwidth = campos.length + 1;
        lblProducto.setFont(UI.NEGRITA);
        linea.add(lblProducto, g);

        JPanel norte = new JPanel(new BorderLayout(0, 8));
        norte.setOpaque(false);
        norte.add(cabecera, BorderLayout.NORTH);
        norte.add(linea, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sur.setOpaque(false);
        JButton quitar = UI.botonPeligro("Quitar línea");
        JButton registrar = UI.boton("Registrar recepción");
        sur.add(quitar);
        sur.add(registrar);
        add(sur, BorderLayout.SOUTH);

        // La lectora de códigos escribe el código y envía Enter
        txtCodigo.addActionListener(e -> buscarProducto());
        txtLote.addActionListener(e -> txtVencimiento.requestFocus());
        txtVencimiento.addActionListener(e -> txtCantidad.requestFocus());
        txtCantidad.addActionListener(e -> txtUbicacion.requestFocus());
        txtUbicacion.addActionListener(e -> agregarLinea());
        agregar.addActionListener(e -> agregarLinea());
        quitar.addActionListener(e -> quitarLinea());
        registrar.addActionListener(e -> registrar());
    }

    private void buscarProducto() {
        String codigo = txtCodigo.getText().trim();
        producto = null;
        if (codigo.isEmpty()) return;
        try {
            producto = controlador.buscarPorCodigo(codigo);
            if (producto == null) {
                lblProducto.setText("No existe un producto con código " + codigo + ". Regístrelo primero en Productos.");
                lblProducto.setForeground(UI.PELIGRO);
            } else {
                lblProducto.setText(producto.nombre + " — stock actual: " + producto.stock);
                lblProducto.setForeground(UI.AZUL);
                txtLote.requestFocus();
            }
        } catch (SQLException ex) {
            error("Error al buscar el producto", ex);
        }
    }

    private void agregarLinea() {
        if (producto == null || !producto.codigo.equals(txtCodigo.getText().trim())) {
            buscarProducto();
            if (producto == null) return;
        }
        String lote = txtLote.getText().trim();
        if (lote.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Indique el número de lote.");
            txtLote.requestFocus();
            return;
        }
        LocalDate vencimiento = null;
        String textoVenc = txtVencimiento.getText().trim();
        if (!textoVenc.isEmpty()) {
            try {
                vencimiento = LocalDate.parse(textoVenc, Datos.FECHA_COMPLETA);
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Fecha de vencimiento inválida. Use dd/mm/aaaa, o déjela vacía si el producto no vence.");
                txtVencimiento.requestFocus();
                return;
            }
            if (vencimiento.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "El lote ya está vencido. No se puede recibir.");
                txtVencimiento.requestFocus();
                return;
            }
        }
        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser un número entero mayor que 0.");
            txtCantidad.requestFocus();
            return;
        }
        String ubicacion = txtUbicacion.getText().trim();

        lineas.add(new LineaRecepcion(producto, lote, vencimiento, cantidad, ubicacion));
        modelo.addRow(new Object[]{producto.codigo, producto.nombre, lote,
                vencimiento == null ? "Sin vencimiento" : vencimiento.format(Datos.FECHA_COMPLETA), cantidad, ubicacion});

        // Deja todo listo para escanear el siguiente producto
        producto = null;
        for (JTextField f : new JTextField[]{txtCodigo, txtLote, txtVencimiento, txtCantidad, txtUbicacion}) f.setText("");
        lblProducto.setText("Escanee o escriba el código y presione Enter");
        lblProducto.setForeground(UI.TEXTO_SECUNDARIO);
        txtCodigo.requestFocus();
    }

    private void quitarLinea() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione la línea que desea quitar.");
            return;
        }
        int i = tabla.convertRowIndexToModel(fila);
        lineas.remove(i);
        modelo.removeRow(i);
    }

    private void registrar() {
        String guia = txtGuia.getText().trim();
        String proveedor = txtProveedor.getText().trim();
        if (guia.isEmpty() || proveedor.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La guía de remisión y el proveedor son obligatorios.");
            return;
        }
        if (lineas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agregue al menos un producto.");
            return;
        }
        int unidades = 0;
        for (LineaRecepcion l : lineas) unidades += l.cantidad;
        if (JOptionPane.showConfirmDialog(this, "¿Registrar la guía " + guia + " con " + lineas.size()
                + " lote(s) y " + unidades + " unidad(es)?", "Confirmar recepción",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        try {
            controlador.registrarRecepcion(guia, proveedor, lineas);
            lineas.clear();
            modelo.setRowCount(0);
            txtGuia.setText("");
            txtProveedor.setText("");
            alRegistrar.run();
            JOptionPane.showMessageDialog(this, "Recepción registrada. El stock fue actualizado.");
        } catch (SQLException ex) {
            error("No se pudo registrar la recepción", ex);
        }
    }

    private void error(String titulo, SQLException ex) {
        JOptionPane.showMessageDialog(this, titulo + ":\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
