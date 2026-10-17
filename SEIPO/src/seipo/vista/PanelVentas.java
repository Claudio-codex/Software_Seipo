package seipo.vista;

import seipo.controlador.ControladorVentas;
import seipo.modelo.Datos;
import seipo.modelo.Datos.ProductoVenta;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class PanelVentas extends JPanel {

    private final JTextField txtBuscar = new JTextField();
    private final DefaultTableModel modelo = UI.modelo("Código", "Descripción", "Cant", "P.Unit", "Importe");
    private final JTable tabla = UI.tabla(modelo);
    private final List<ProductoVenta> items = new ArrayList<>();
    private final List<Integer> cantidades = new ArrayList<>();
    private final JLabel lblSub = new JLabel(), lblDesc = new JLabel(), lblIgv = new JLabel(), lblTotal = new JLabel();
    private double descuentoPct = 0;

    private final JPopupMenu popupSugerencias = new JPopupMenu();
    private final JList<ProductoVenta> listaSugerencias = new JList<>();
    private final DefaultListModel<ProductoVenta> modeloSugerencias = new DefaultListModel<>();
    private final ControladorVentas controlador = new ControladorVentas();

    public PanelVentas() {
        setLayout(new BorderLayout());
        JPanel p = UI.base();
        add(p);

        JPanel norte = new JPanel(new BorderLayout(8, 0));
        norte.setOpaque(false);
        JButton agregar = UI.boton("Agregar");
        norte.add(new JLabel("Código o nombre: "), BorderLayout.WEST);
        norte.add(txtBuscar, BorderLayout.CENTER);
        norte.add(agregar, BorderLayout.EAST);
        p.add(norte, BorderLayout.NORTH);
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel sur = new JPanel(new GridLayout(1, 3, 10, 0));
        sur.setOpaque(false);

        JPanel acciones = new JPanel(new GridLayout(0, 1, 6, 6));
        acciones.setOpaque(false);
        JButton descuento = UI.boton("Aplicar descuento");
        JButton quitar = UI.boton("Quitar ítem");
        acciones.add(descuento);
        acciones.add(quitar);

        JPanel totales = new JPanel(new GridLayout(0, 1));
        totales.setBackground(Color.WHITE);
        totales.setBorder(new CompoundBorder(new LineBorder(Color.LIGHT_GRAY), new EmptyBorder(8, 12, 8, 12)));
        JLabel t = new JLabel("TOTAL");
        t.setFont(UI.NEGRITA);
        lblTotal.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTotal.setForeground(UI.AZUL);
        totales.add(t);
        totales.add(lblSub);
        totales.add(lblDesc);
        totales.add(lblIgv);
        totales.add(lblTotal);

        JPanel pago = new JPanel(new GridLayout(0, 1, 6, 6));
        pago.setOpaque(false);
        pago.add(new JLabel("Forma de pago"));
        for (String f : new String[]{"Efectivo", "Tarjeta", "Yape o Plin"}) {
            JButton b = UI.boton(f);
            b.addActionListener(e -> cobrar(f));
            pago.add(b);
        }

        sur.add(acciones);
        sur.add(totales);
        sur.add(pago);
        p.add(sur, BorderLayout.SOUTH);

        listaSugerencias.setModel(modeloSugerencias);
        listaSugerencias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        popupSugerencias.add(new JScrollPane(listaSugerencias));
        popupSugerencias.setFocusable(false);

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrarSugerencias(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrarSugerencias(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrarSugerencias(); }
        });

        listaSugerencias.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) seleccionarSugerencia();
            }
        });

        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && popupSugerencias.isVisible()) {
                    listaSugerencias.requestFocus();
                    listaSugerencias.setSelectedIndex(0);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (popupSugerencias.isVisible() && !modeloSugerencias.isEmpty()) {
                        listaSugerencias.setSelectedIndex(0);
                        seleccionarSugerencia();
                    } else {
                        agregar();
                    }
                }
            }
        });

        listaSugerencias.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) seleccionarSugerencia();
            }
        });

        agregar.addActionListener(e -> agregar());
        descuento.addActionListener(e -> aplicarDescuento());
        quitar.addActionListener(e -> quitar());
        recalcular();
    }

    private void filtrarSugerencias() {
        String texto = txtBuscar.getText().trim();
        modeloSugerencias.clear();

        if (texto.isEmpty()) {
            popupSugerencias.setVisible(false);
            return;
        }

        try {
            List<ProductoVenta> encontrados = controlador.filtrarSugerencias(texto);
            if (encontrados.isEmpty()) {
                popupSugerencias.setVisible(false);
            } else {
                for (ProductoVenta p : encontrados) modeloSugerencias.addElement(p);
                listaSugerencias.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
                    JLabel lbl = new JLabel(value.codigo + " - " + value.nombre + " (Stock: " + value.stock + ")");
                    lbl.setBorder(new EmptyBorder(4, 8, 4, 8));
                    if (isSelected) {
                        lbl.setBackground(UI.AZUL);
                        lbl.setForeground(Color.WHITE);
                        lbl.setOpaque(true);
                    } else {
                        lbl.setBackground(Color.WHITE);
                        lbl.setForeground(Color.BLACK);
                        lbl.setOpaque(true);
                    }
                    return lbl;
                });
                popupSugerencias.show(txtBuscar, 0, txtBuscar.getHeight());
                txtBuscar.requestFocusInWindow();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void seleccionarSugerencia() {
        ProductoVenta pr = listaSugerencias.getSelectedValue();
        if (pr != null) {
            txtBuscar.setText(pr.codigo);
            popupSugerencias.setVisible(false);
            agregar();
        }
    }

    private void agregar() {
        popupSugerencias.setVisible(false);
        String textoBuscado = txtBuscar.getText().trim();
        if (textoBuscado.isEmpty()) return;

        try {
            ProductoVenta pr = controlador.buscarProductoBD(textoBuscado);
            if (pr == null) {
                JOptionPane.showMessageDialog(this, "Producto no encontrado.");
                return;
            }

            String s = JOptionPane.showInputDialog(this, pr.nombre + " (stock " + pr.stock + ")\nCantidad:", "1");
            if (s == null) return;
            int c = Integer.parseInt(s.trim());

            int indiceExistente = -1;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).codigo.equalsIgnoreCase(pr.codigo)) {
                    indiceExistente = i;
                    break;
                }
            }

            int yaEnCarrito = (indiceExistente != -1) ? cantidades.get(indiceExistente) : 0;
            if (c <= 0 || c + yaEnCarrito > pr.stock) {
                JOptionPane.showMessageDialog(this, "Cantidad inválida o stock insuficiente.");
                return;
            }

            if (indiceExistente != -1) {
                cantidades.set(indiceExistente, cantidades.get(indiceExistente) + c);
            } else {
                items.add(pr);
                cantidades.add(c);
            }
            txtBuscar.setText("");
            refrescarTabla();
        } catch (SQLException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese un número válido o revise la conexión.");
        }
    }

    private void aplicarDescuento() {
        String s = JOptionPane.showInputDialog(this, "Descuento (%):", "0");
        if (s == null) return;
        try {
            double d = Double.parseDouble(s.trim());
            if (d < 0 || d > 100) throw new NumberFormatException();
            descuentoPct = d;
            recalcular();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Porcentaje inválido (0-100).");
        }
    }

    private void quitar() {
        int f = tabla.getSelectedRow();
        if (f < 0) return;
        items.remove(f);
        cantidades.remove(f);
        refrescarTabla();
    }

    private void refrescarTabla() {
        modelo.setRowCount(0);
        for (int i = 0; i < items.size(); i++) {
            ProductoVenta p = items.get(i);
            int c = cantidades.get(i);
            modelo.addRow(new Object[]{p.codigo, p.nombre, c, Datos.soles(p.precio), Datos.soles(p.precio * c)});
        }
        recalcular();
    }

    private double subtotal() {
        double s = 0;
        for (int i = 0; i < items.size(); i++) {
            s += items.get(i).precio * cantidades.get(i);
        }
        return s;
    }

    private void recalcular() {
        double sub = subtotal();
        double desc = sub * descuentoPct / 100;
        double igv = (sub - desc) * Datos.IGV;
        lblSub.setText("Subtotal: " + Datos.soles(sub));
        lblDesc.setText("Descuento: " + Datos.soles(desc));
        lblIgv.setText("IGV 18%: " + Datos.soles(igv));
        lblTotal.setText("TOTAL: " + Datos.soles(sub - desc + igv));
    }

    private void cobrar(String forma) {
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay productos en la venta.");
            return;
        }

        double sub = subtotal();
        double desc = sub * descuentoPct / 100;
        double totalVenta = (sub - desc) * (1 + Datos.IGV);

        try {
            controlador.cobrarVenta(items, cantidades, descuentoPct, forma);
            JOptionPane.showMessageDialog(this, "Venta registrada (" + forma + ")\nTotal: " + Datos.soles(totalVenta));
            items.clear();
            cantidades.clear();
            descuentoPct = 0;
            refrescarTabla();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar la venta en la base de datos:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}