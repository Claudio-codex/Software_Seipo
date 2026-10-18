package seipo.vista;

import seipo.controlador.ControladorProductos;
import seipo.modelo.Datos;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PanelProductos extends JPanel {

    private final JTextField txtBuscar = new JTextField();
    private final DefaultTableModel modelo = UI.modelo("Código", "Nombre", "Categoría", "Precio", "Stock", "Próx. vencimiento");
    private final ControladorProductos controlador = new ControladorProductos();

    public PanelProductos() {
        setLayout(new BorderLayout());
        JPanel p = UI.base();
        add(p);

        JPanel norte = new JPanel(new BorderLayout(8, 0));
        norte.setOpaque(false);
        JButton nuevo = UI.boton("+ Nuevo Producto");
        norte.add(new JLabel("Buscar: "), BorderLayout.WEST);
        norte.add(txtBuscar, BorderLayout.CENTER);
        norte.add(nuevo, BorderLayout.EAST);
        p.add(norte, BorderLayout.NORTH);
        p.add(new JScrollPane(UI.tabla(modelo)), BorderLayout.CENTER);

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
            List<ControladorProductos.ProductoVO> lista = controlador.buscarProductos(textoBusqueda);
            for (ControladorProductos.ProductoVO pr : lista) {
                String vence = pr.proximoVencimiento == null ? "—" : pr.proximoVencimiento.format(Datos.FECHA_COMPLETA);
                modelo.addRow(new Object[]{pr.codigo, pr.nombre, pr.categoria, Datos.soles(pr.precio), pr.stock, vence});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar los productos de la base de datos:\n" + ex.getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void nuevoProducto() {
        JTextField c = new JTextField(), n = new JTextField(), cat = new JTextField(), pr = new JTextField();
        Object[] campos = {"Código:", c, "Nombre:", n, "Categoría:", cat, "Precio:", pr,
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

            controlador.registrarProducto(codigo, nombre, categoria, precio);

            txtBuscar.setText("");
            refrescar();
            JOptionPane.showMessageDialog(this, "Producto registrado exitosamente.");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar el producto:\n" + ex.getMessage(),
                    "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Datos inválidos. Revise que el precio sea numérico.");
        }
    }
}