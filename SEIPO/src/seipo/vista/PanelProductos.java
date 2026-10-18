package seipo.vista;

import seipo.controlador.ControladorProductos;
import seipo.controlador.ControladorProductos.ProductoVO;
import seipo.modelo.Datos;
import seipo.modelo.Sesion;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PanelProductos extends JPanel {

    private final JTextField txtBuscar = new JTextField();
    private final DefaultTableModel modelo = UI.modelo("Código", "Nombre", "Categoría", "Precio", "Stock",
            "Mín.", "Máx.", "Próx. vencimiento");
    private final JTable tabla = UI.tabla(modelo);
    private final ControladorProductos controlador = new ControladorProductos();
    private List<ProductoVO> productos = new ArrayList<>();

    public PanelProductos() {
        setLayout(new BorderLayout());
        UI.columnaAncha(tabla, 1, 260);
        JPanel p = UI.base();
        add(p);

        JPanel norte = new JPanel(new BorderLayout(8, 0));
        norte.setOpaque(false);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        JButton nuevo = UI.boton("+ Nuevo Producto");
        botones.add(nuevo);
        if (Sesion.esAdministrador()) {
            JButton editar = UI.botonSecundario("Editar");
            editar.addActionListener(e -> editarProducto());
            botones.add(editar);
        }
        norte.add(new JLabel("Buscar: "), BorderLayout.WEST);
        norte.add(txtBuscar, BorderLayout.CENTER);
        norte.add(botones, BorderLayout.EAST);
        p.add(norte, BorderLayout.NORTH);
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);

        txtBuscar.addActionListener(e -> refrescar());

        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refrescar(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refrescar(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refrescar(); }
        });

        nuevo.addActionListener(e -> nuevoProducto());
        refrescar();
    }

    public void refrescar() {
        String textoBusqueda = txtBuscar.getText().trim();
        modelo.setRowCount(0);

        try {
            productos = controlador.buscarProductos(textoBusqueda);
            for (ProductoVO pr : productos) {
                String vence = pr.proximoVencimiento == null ? "—" : pr.proximoVencimiento.format(Datos.FECHA_COMPLETA);
                modelo.addRow(new Object[]{pr.codigo, pr.nombre, pr.categoria, Datos.soles(pr.precio), pr.stock,
                        pr.stockMinimo, pr.stockMaximo == null ? "—" : pr.stockMaximo, vence});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar los productos de la base de datos:\n" + ex.getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void nuevoProducto() {
        JTextField c = new JTextField(), n = new JTextField(), cat = new JTextField(), pr = new JTextField(),
                min = new JTextField(String.valueOf(Datos.STOCK_MINIMO_DEFECTO)), max = new JTextField();
        Object[] campos = {"Código:", c, "Nombre:", n, "Categoría:", cat, "Precio:", pr,
                "Stock mínimo (alerta de stock bajo):", min, "Stock máximo (alerta de sobrestock, opcional):", max,
                "El stock se ingresa en Inventario → Recepción de mercadería."};

        if (JOptionPane.showConfirmDialog(this, campos, "Nuevo producto",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try {
            String codigo = c.getText().trim();
            String nombre = n.getText().trim();
            String categoria = cat.getText().trim();

            if (codigo.isEmpty() || nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El código y el nombre son obligatorios.");
                return;
            }

            double precio = Double.parseDouble(pr.getText().trim());

            controlador.registrarProducto(codigo, nombre, categoria, precio,
                    Integer.parseInt(min.getText().trim()), maximo(max));

            txtBuscar.setText("");
            refrescar();
            JOptionPane.showMessageDialog(this, "Producto registrado exitosamente.");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar el producto:\n" + ex.getMessage(),
                    "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Datos inválidos. Revise que el precio y el stock mínimo y máximo sean numéricos.");
        }
    }

    private void editarProducto() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto de la tabla.");
            return;
        }
        ProductoVO sel = productos.get(tabla.convertRowIndexToModel(fila));
        JTextField n = new JTextField(sel.nombre), cat = new JTextField(sel.categoria),
                pr = new JTextField(String.format(java.util.Locale.US, "%.2f", sel.precio)),
                min = new JTextField(String.valueOf(sel.stockMinimo)),
                max = new JTextField(sel.stockMaximo == null ? "" : String.valueOf(sel.stockMaximo));
        Object[] campos = {"Código: " + sel.codigo, "Nombre:", n, "Categoría:", cat, "Precio:", pr,
                "Stock mínimo (alerta de stock bajo):", min, "Stock máximo (alerta de sobrestock, vacío = sin límite):", max};

        if (JOptionPane.showConfirmDialog(this, campos, "Editar producto",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try {
            String nombre = n.getText().trim();
            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre es obligatorio.");
                return;
            }
            controlador.actualizarProducto(sel.codigo, nombre, cat.getText().trim(),
                    Double.parseDouble(pr.getText().trim()), Integer.parseInt(min.getText().trim()), maximo(max));
            refrescar();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el producto:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Datos inválidos. Revise que el precio y el stock mínimo y máximo sean numéricos.");
        }
    }

    private static Integer maximo(JTextField campo) {
        String texto = campo.getText().trim();
        return texto.isEmpty() ? null : Integer.parseInt(texto);
    }
}
