package seipo.vista;

import seipo.controlador.ControladorComprobantes;
import seipo.controlador.ControladorComprobantes.EmisorVO;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.border.*;

/** Datos del negocio que aparecen como emisor en la boleta y la factura (solo administrador). */
public class PanelNegocio extends JPanel {

    private final ControladorComprobantes comprobantes = new ControladorComprobantes();
    private final JTextField txtNombre = new JTextField(), txtRuc = new JTextField(), txtDireccion = new JTextField();

    public PanelNegocio() {
        setLayout(new BorderLayout());
        JPanel p = UI.base();
        add(p);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UI.SUPERFICIE);
        form.setBorder(new CompoundBorder(new LineBorder(UI.BORDE, 1, true), new EmptyBorder(18, 22, 18, 22)));
        int y = 0;
        y = titulo(form, y, "Datos del negocio", "Aparecen en la boleta y en la factura.");
        y = campo(form, y, "Nombre o razón social", txtNombre);
        y = campo(form, y, "RUC", txtRuc);
        y = campo(form, y, "Dirección", txtDireccion);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.setOpaque(false);
        JButton guardar = UI.boton("Guardar cambios");
        guardar.addActionListener(e -> guardar());
        botones.add(guardar);
        GridBagConstraints g = restricciones(0, y);
        g.gridwidth = 2;
        g.insets = new Insets(16, 0, 0, 0);
        form.add(botones, g);

        JLabel nota = UI.ayuda("<html>Las formas de pago son Efectivo y Yape o Plin. El sistema solo registra la forma de pago:<br>"
                + "el pago con Yape o Plin lo confirma el cajero en el celular del negocio.</html>");
        nota.setBorder(new EmptyBorder(12, 2, 0, 0));

        JPanel columna = new JPanel(new BorderLayout());
        columna.setOpaque(false);
        columna.add(form, BorderLayout.NORTH);
        columna.add(nota, BorderLayout.CENTER);
        columna.setPreferredSize(new Dimension(680, columna.getPreferredSize().height));
        JPanel envoltura = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        envoltura.setOpaque(false);
        envoltura.add(columna);
        // Con la ventana baja, el formulario se desplaza para que el botón siga al alcance
        JScrollPane desplazable = new JScrollPane(envoltura);
        desplazable.setBorder(null);
        desplazable.getVerticalScrollBar().setUnitIncrement(16);
        p.add(desplazable, BorderLayout.CENTER);
    }

    private static int titulo(JPanel form, int y, String texto, String ayuda) {
        GridBagConstraints g = restricciones(0, y);
        g.gridwidth = 2;
        g.insets = new Insets(0, 0, 2, 0);
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
        try {
            EmisorVO e = comprobantes.leerEmisor();
            txtNombre.setText(e.nombre);
            txtRuc.setText(e.ruc);
            txtDireccion.setText(e.direccion);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al leer los datos del negocio:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardar() {
        EmisorVO e = new EmisorVO();
        e.nombre = txtNombre.getText().trim();
        e.ruc = txtRuc.getText().trim();
        e.direccion = txtDireccion.getText().trim();
        try {
            comprobantes.guardarEmisor(e);
            JOptionPane.showMessageDialog(this, "Datos guardados.\nLos comprobantes nuevos usan estos datos; los ya emitidos no cambian.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo guardar", JOptionPane.ERROR_MESSAGE);
        }
    }
}
