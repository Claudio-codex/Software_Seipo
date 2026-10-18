package seipo.vista;

import seipo.controlador.ControladorClientes;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public class PanelClientes extends JPanel {

    private final JTextField txtBuscar = new JTextField();
    private final DefaultTableModel modelo = UI.modelo("Nombre", "DNI / RUC", "Teléfono", "Correo", "Nº Compras", "Descuento VIP");
    private final JTable tabla = UI.tabla(modelo);
    private final ControladorClientes controlador = new ControladorClientes();

    public PanelClientes() {
        setLayout(new BorderLayout());
        JPanel p = UI.base();
        add(p);

        JPanel norte = new JPanel(new BorderLayout(8, 0));
        norte.setOpaque(false);
        JButton nuevo = UI.boton("+ Nuevo Cliente");
        norte.add(new JLabel("Buscar cliente: "), BorderLayout.WEST);
        norte.add(txtBuscar, BorderLayout.CENTER);
        norte.add(nuevo, BorderLayout.EAST);
        p.add(norte, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 5));
        centro.setOpaque(false);
        JLabel lblInfo = new JLabel("Los descuentos VIP se asignan automáticamente según el volumen de compras.");
        lblInfo.setFont(UI.fuente(Font.PLAIN, 12));
        lblInfo.setForeground(UI.TEXTO_SECUNDARIO);

        centro.add(lblInfo, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        p.add(centro, BorderLayout.CENTER);

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refrescar(); }
            @Override public void removeUpdate(DocumentEvent e) { refrescar(); }
            @Override public void changedUpdate(DocumentEvent e) { refrescar(); }
        });

        txtBuscar.addActionListener(e -> refrescar());
        nuevo.addActionListener(e -> nuevoCliente());
        refrescar();
    }

    public void refrescar() {
        String textoBusqueda = txtBuscar.getText().trim();
        modelo.setRowCount(0);

        try {
            List<ControladorClientes.ClienteVO> lista = controlador.buscarClientes(textoBusqueda);
            for (ControladorClientes.ClienteVO c : lista) {
                modelo.addRow(new Object[]{c.nombre, c.documento, c.telefono, c.correo, c.numCompras + " compras", c.descuentoVIP});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar los clientes:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void nuevoCliente() {
        JTextField n = new JTextField(), d = new JTextField(), t = new JTextField(), c = new JTextField();
        Object[] campos = {"Nombre:", n, "DNI / RUC:", d, "Teléfono:", t, "Correo:", c};

        if (JOptionPane.showConfirmDialog(this, campos, "Registrar Nuevo Cliente",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        String nombre = n.getText().trim();
        String doc = d.getText().trim();
        String telefono = t.getText().trim();
        String correo = c.getText().trim();

        if (nombre.isEmpty() || !(doc.matches("\\d{8}") || doc.matches("\\d{11}"))) {
            JOptionPane.showMessageDialog(this, "Nombre obligatorio y DNI (8 dígitos) o RUC (11 dígitos).");
            return;
        }

        try {
            controlador.registrarCliente(nombre, doc, telefono, correo);
            txtBuscar.setText("");
            refrescar();
            JOptionPane.showMessageDialog(this, "¡Cliente registrado exitosamente con categoría inicial!");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar (es posible que el DNI/RUC ya esté registrado):\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}