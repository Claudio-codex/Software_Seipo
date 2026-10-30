package seipo.vista;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/** Iconos lineales dibujados con Java2D sobre una cuadrícula de 24 × 24 (no dependen de archivos ni de librerías). */
public final class Iconos {

    public enum Tipo {
        CARRITO, BILLETE, PERSONAS, ETIQUETA, CUBO, PAPELERA, CAMION, BARRAS, TIENDA, USUARIO,
        SALIR, ALERTA, AGOTADO, MONEDA, RELOJ, CALENDARIO, CELULAR, CERRAR
    }

    private Iconos() {
    }

    public static Icon de(Tipo tipo, int tamano, Color color) {
        return new Icono(tipo, tamano, color);
    }

    private static final class Icono implements Icon {
        private final Tipo tipo;
        private final int tamano;
        private final Color color;

        Icono(Tipo tipo, int tamano, Color color) {
            this.tipo = tipo;
            this.tamano = tamano;
            this.color = color;
        }

        @Override public int getIconWidth() { return tamano; }
        @Override public int getIconHeight() { return tamano; }

        @Override public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.translate(x, y);
            g.scale(tamano / 24.0, tamano / 24.0);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            dibujar(g);
            g.dispose();
        }

        private void dibujar(Graphics2D g) {
            switch (tipo) {
                case CARRITO:
                    g.draw(linea(2, 3.5, 5, 3.5, 7.5, 15, 18.5, 15, 20.5, 7, 6.2, 7));
                    circulo(g, 9, 19.2, 1.4);
                    circulo(g, 17, 19.2, 1.4);
                    break;
                case BILLETE:
                    g.draw(new RoundRectangle2D.Double(2.5, 6.5, 19, 11, 3, 3));
                    g.draw(new Ellipse2D.Double(9.5, 9.5, 5, 5));
                    g.draw(new Line2D.Double(5.5, 9.5, 5.5, 9.5));
                    g.draw(new Line2D.Double(18.5, 14.5, 18.5, 14.5));
                    break;
                case PERSONAS:
                    g.draw(new Ellipse2D.Double(5.5, 4.5, 7, 7));
                    g.draw(new Arc2D.Double(3, 13.5, 12, 12, 0, 180, Arc2D.OPEN));
                    g.draw(new Arc2D.Double(14, 5, 6, 6, 270, 270, Arc2D.OPEN));
                    g.draw(new Arc2D.Double(15, 13.5, 7.5, 9, 0, 90, Arc2D.OPEN));
                    break;
                case ETIQUETA:
                    g.draw(linea(3.5, 3.5, 11.5, 3.5, 20.5, 12.5, 12.5, 20.5, 3.5, 11.5, 3.5, 3.5));
                    circulo(g, 8, 8, 1.3);
                    break;
                case CUBO:
                    g.draw(linea(12, 2.5, 20.5, 7, 20.5, 17, 12, 21.5, 3.5, 17, 3.5, 7, 12, 2.5));
                    g.draw(linea(3.5, 7, 12, 11.5, 20.5, 7));
                    g.draw(new Line2D.Double(12, 11.5, 12, 21.5));
                    break;
                case PAPELERA:
                    g.draw(new Line2D.Double(4, 6.5, 20, 6.5));
                    g.draw(linea(9, 6.5, 9, 4, 15, 4, 15, 6.5));
                    g.draw(linea(6, 6.5, 7, 20.5, 17, 20.5, 18, 6.5));
                    g.draw(new Line2D.Double(10, 10.5, 10, 16.5));
                    g.draw(new Line2D.Double(14, 10.5, 14, 16.5));
                    break;
                case CAMION:
                    g.draw(linea(2.5, 16.5, 2.5, 6, 13.5, 6, 13.5, 16.5));
                    g.draw(linea(13.5, 9, 17.5, 9, 21, 12.5, 21, 16.5, 19.5, 16.5));
                    g.draw(new Line2D.Double(9.5, 16.5, 14.5, 16.5));
                    g.draw(new Ellipse2D.Double(5, 15, 3.5, 3.5));
                    g.draw(new Ellipse2D.Double(15.5, 15, 3.5, 3.5));
                    break;
                case BARRAS:
                    g.draw(new RoundRectangle2D.Double(4, 12, 3.6, 8.5, 1.5, 1.5));
                    g.draw(new RoundRectangle2D.Double(10.2, 7, 3.6, 13.5, 1.5, 1.5));
                    g.draw(new RoundRectangle2D.Double(16.4, 3.5, 3.6, 17, 1.5, 1.5));
                    break;
                case TIENDA:
                    g.draw(linea(3, 9.5, 4.8, 4, 19.2, 4, 21, 9.5, 3, 9.5));
                    g.draw(linea(4.5, 9.5, 4.5, 20.5, 19.5, 20.5, 19.5, 9.5));
                    g.draw(linea(10, 20.5, 10, 14.5, 14, 14.5, 14, 20.5));
                    break;
                case USUARIO:
                    g.draw(new Ellipse2D.Double(8, 3.5, 8, 8));
                    g.draw(new Arc2D.Double(4.5, 14, 15, 13, 0, 180, Arc2D.OPEN));
                    break;
                case SALIR:
                    g.draw(linea(10, 4, 5, 4, 5, 20, 10, 20));
                    g.draw(linea(15, 8, 19, 12, 15, 16));
                    g.draw(new Line2D.Double(19, 12, 9.5, 12));
                    break;
                case ALERTA:
                    g.draw(linea(12, 3.5, 21, 19.5, 3, 19.5, 12, 3.5));
                    g.draw(new Line2D.Double(12, 9.5, 12, 13.5));
                    circulo(g, 12, 16.6, 0.6);
                    break;
                case AGOTADO:
                    g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                    g.draw(new Line2D.Double(6.5, 6.5, 17.5, 17.5));
                    break;
                case MONEDA:
                    g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                    g.draw(linea(14.5, 8.5, 10.5, 8.5, 9.5, 9.5, 9.5, 11, 10.5, 12, 13.5, 12, 14.5, 13, 14.5, 14.5, 13.5, 15.5, 9.5, 15.5));
                    g.draw(new Line2D.Double(12, 6.5, 12, 8.5));
                    g.draw(new Line2D.Double(12, 15.5, 12, 17.5));
                    break;
                case RELOJ:
                    g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                    g.draw(linea(12, 7.5, 12, 12, 15, 14));
                    break;
                case CALENDARIO:
                    g.draw(new RoundRectangle2D.Double(3.5, 5, 17, 15.5, 3, 3));
                    g.draw(new Line2D.Double(3.5, 9.5, 20.5, 9.5));
                    g.draw(new Line2D.Double(8, 3, 8, 6.5));
                    g.draw(new Line2D.Double(16, 3, 16, 6.5));
                    break;
                case CELULAR:
                    g.draw(new RoundRectangle2D.Double(6.5, 2.5, 11, 19, 3.5, 3.5));
                    g.draw(new Line2D.Double(10.5, 18, 13.5, 18));
                    break;
                case CERRAR:
                    g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(new Line2D.Double(6, 6, 18, 18));
                    g.draw(new Line2D.Double(18, 6, 6, 18));
                    break;
                default:
                    break;
            }
        }

        /** Polilínea por pares de coordenadas x, y. */
        private static Path2D linea(double... p) {
            Path2D.Double path = new Path2D.Double();
            path.moveTo(p[0], p[1]);
            for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
            return path;
        }

        private static void circulo(Graphics2D g, double cx, double cy, double r) {
            g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        }
    }
}
