package seipo.vista;

import seipo.controlador.ControladorInventario;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PanelInventario extends JPanel {

    private final JLabel lblTotal = new JLabel(), lblBajo = new JLabel(), lblAgotados = new JLabel();
    private final DefaultTableModel modelo = UI.modelo("Fecha", "Producto", "Tipo", "Cantidad", "Stock");
    private final ControladorInventario controlador = new ControladorInventario();

    public PanelInventario() {
        setLayout(new BorderLayout());
        JPanel p = UI.base();
        add(p);

        JPanel tarjetas = new JPanel(new GridLayout(1, 3, 10, 0));
        tarjetas.setOpaque(false);
        tarjetas.add(UI.tarjeta("Productos totales", lblTotal));
        tarjetas.add(UI.tarjeta("Stock bajo", lblBajo));
        tarjetas.add(UI.tarjeta("Agotados", lblAgotados));
        p.add(tarjetas, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 6));
        centro.setOpaque(false);
        JLabel t = new JLabel("Movimientos recientes");
        t.setFont(UI.NEGRITA);
        centro.add(t, BorderLayout.NORTH);
        centro.add(new JScrollPane(UI.tabla(modelo)), BorderLayout.CENTER);
        p.add(centro, BorderLayout.CENTER);

        refrescar();
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (aFlag) {
            refrescar();
        }
    }

    public void refrescar() {
        try {
            ControladorInventario.ResumenInventario res = controlador.obtenerResumen();
            lblTotal.setText(String.valueOf(res.totalStock));
            lblBajo.setText(String.valueOf(res.bajoStock));
            lblAgotados.setText(String.valueOf(res.agotados));

            modelo.setRowCount(0);
            List<ControladorInventario.MovimientoVO> movs = controlador.obtenerMovimientosRecientes();
            for (ControladorInventario.MovimientoVO m : movs) {
                modelo.addRow(new Object[]{m.fecha, m.producto, m.tipo, m.cantidad, m.stock});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}