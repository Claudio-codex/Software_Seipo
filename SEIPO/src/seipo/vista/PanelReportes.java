package seipo.vista;

import seipo.controlador.ControladorReportes;
import seipo.modelo.Datos;

import java.awt.*;
import java.sql.SQLException;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Pantalla Reportes: indicadores, ventas por día de la semana y productos más vendidos desde MySQL. */
public class PanelReportes extends JPanel {

    private final JLabel lblHoy = new JLabel(), lblPromedio = new JLabel(),
            lblNVentas = new JLabel(), lblProductos = new JLabel();
    private final DefaultTableModel modelo = UI.modelo("Producto", "Unidades", "Importe");
    private final double[] porDia = new double[7];
    private final ControladorReportes controlador = new ControladorReportes();

    private final JPanel grafico = new JPanel() {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            dibujar((Graphics2D) g, getWidth(), getHeight());
        }
    };

    public PanelReportes() {
        setLayout(new BorderLayout());
        JPanel p = UI.base();
        add(p);

        JPanel tarjetas = new JPanel(new GridLayout(1, 4, 10, 0));
        tarjetas.setOpaque(false);
        tarjetas.add(UI.tarjeta("Ventas hoy", lblHoy));
        tarjetas.add(UI.tarjeta("Precio de producto promedio", lblPromedio));
        tarjetas.add(UI.tarjeta("Nº ventas", lblNVentas));
        tarjetas.add(UI.tarjeta("Productos vendidos", lblProductos));
        p.add(tarjetas, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridLayout(1, 2, 10, 0));
        centro.setOpaque(false);

        JPanel izq = new JPanel(new BorderLayout(0, 6));
        izq.setOpaque(false);
        JLabel t1 = new JLabel("Ventas por día");
        t1.setFont(UI.NEGRITA);
        grafico.setBackground(Color.WHITE);
        izq.add(t1, BorderLayout.NORTH);
        izq.add(grafico, BorderLayout.CENTER);

        JPanel der = new JPanel(new BorderLayout(0, 6));
        der.setOpaque(false);
        JLabel t2 = new JLabel("Productos más vendidos");
        t2.setFont(UI.NEGRITA);
        der.add(t2, BorderLayout.NORTH);
        der.add(new JScrollPane(UI.tabla(modelo)), BorderLayout.CENTER);

        centro.add(izq);
        centro.add(der);
        p.add(centro, BorderLayout.CENTER);
        refrescar();
    }

    public void refrescar() {
        try {
            ControladorReportes.DatosReporteVO vo = controlador.obtenerReporte();

            // Actualizar arreglo local de días para el gráfico
            System.arraycopy(vo.porDia, 0, porDia, 0, 7);

            // Actualizar etiquetas de tarjetas superiores
            lblHoy.setText(Datos.soles(vo.totalHoy));
            lblPromedio.setText(Datos.soles(vo.precioPromedio));
            lblNVentas.setText(String.valueOf(vo.numeroVentas));
            lblProductos.setText(String.valueOf(vo.unidadesTotal));

            // Rellenar tabla de productos más vendidos
            modelo.setRowCount(0);
            vo.ranking.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                    .limit(5)
                    .forEach(e -> modelo.addRow(new Object[]{e.getKey(), (int) e.getValue()[0], Datos.soles(e.getValue()[1])}));

            grafico.repaint();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar reportes desde la base de datos:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void dibujar(Graphics2D g, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        String[] dias = {"Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do"};
        double max = 1;
        for (double v : porDia) max = Math.max(max, v);
        int margen = 30, base = h - 28, alto = base - 25;
        int ancho = (w - 2 * margen) / 7;

        g.setFont(UI.NORMAL);
        for (int i = 0; i < 7; i++) {
            int barra = (int) (porDia[i] / max * alto);
            int x = margen + i * ancho + 8;
            g.setColor(UI.AZUL);
            g.fillRect(x, base - barra, ancho - 16, barra);
            g.setColor(Color.DARK_GRAY);
            g.drawString(dias[i], x + (ancho - 16) / 2 - 8, base + 18);

            if (porDia[i] > 0) {
                g.drawString(String.format("%.0f", porDia[i]), x, base - barra - 4);
            }
        }
        g.drawLine(margen, base, w - margen, base);
    }
}