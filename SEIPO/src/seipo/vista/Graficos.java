package seipo.vista;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.function.DoubleFunction;
import javax.swing.*;

/** Gráficos dibujados con Java2D para el módulo de reportes (columnas, líneas, barras horizontales y dona). */
public class Graficos {

    /** Paleta categórica en orden fijo: se asigna por posición, nunca se repite. */
    // Tonos luminosos para que se distingan sobre el fondo oscuro; la primera serie es el color de acento
    public static final Color[] SERIES = {
            new Color(0x2DD4BF), new Color(0xFB923C), new Color(0x60A5FA), new Color(0xFBBF24),
            new Color(0xF472B6), new Color(0xA78BFA), new Color(0xA3E635), new Color(0xF87171)
    };
    public static final Color OTROS = new Color(0x64748B);
    public static final Color SUPERFICIE = UI.SUPERFICIE;
    public static final Color TEXTO = UI.TEXTO;
    public static final Color TEXTO2 = new Color(0xCBD5E1);
    public static final Color TENUE = UI.TEXTO_SECUNDARIO;
    static final Color GRILLA = new Color(0x2C3A4F);
    static final Color EJE = new Color(0x475569);
    static final Font CHICA = UI.fuente(Font.PLAIN, 11);
    static final Font ETIQUETA = UI.fuente(Font.PLAIN, 12);
    static final Font ETIQUETA_NEGRITA = UI.fuente(Font.BOLD, 12);

    /** Comportamiento común: fondo, estado vacío y tooltip propio que sigue al mouse. */
    abstract static class Base extends JPanel {
        protected int hover = -1;
        protected Point mouse;
        protected DoubleFunction<String> formato = Graficos::compacto;
        protected String nombreSerie = "";

        Base() {
            setBackground(SUPERFICIE);
            setPreferredSize(new Dimension(320, 220));
            MouseAdapter m = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    mouse = e.getPoint();
                    hover = vacio() ? -1 : indiceEn(e.getX(), e.getY());
                    repaint();
                }
                @Override public void mouseExited(MouseEvent e) {
                    mouse = null;
                    hover = -1;
                    repaint();
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        public void setFormato(DoubleFunction<String> formato) { this.formato = formato; repaint(); }
        public void setNombreSerie(String nombre) { this.nombreSerie = nombre; }

        protected abstract boolean vacio();
        protected abstract void dibujar(Graphics2D g, int w, int h);
        protected abstract int indiceEn(int x, int y);
        protected abstract String[] tooltip(int i);

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (vacio()) {
                g.setFont(ETIQUETA);
                g.setColor(TENUE);
                String s = "Sin datos en el período seleccionado";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, getHeight() / 2);
            } else {
                dibujar(g, getWidth(), getHeight());
                if (hover >= 0 && mouse != null) dibujarTooltip(g, tooltip(hover));
            }
            g.dispose();
        }

        private void dibujarTooltip(Graphics2D g, String[] lineas) {
            g.setStroke(new BasicStroke(1));
            g.setFont(ETIQUETA);
            FontMetrics fm = g.getFontMetrics();
            int ancho = 0;
            for (String l : lineas) ancho = Math.max(ancho, fm.stringWidth(l));
            ancho += 18;
            int alto = lineas.length * fm.getHeight() + 10;
            int x = mouse.x + 14, y = mouse.y + 14;
            if (x + ancho > getWidth() - 4) x = mouse.x - ancho - 10;
            if (y + alto > getHeight() - 4) y = mouse.y - alto - 10;
            x = Math.max(4, x);
            y = Math.max(4, y);
            g.setColor(new Color(0, 0, 0, 90));
            g.fillRoundRect(x + 1, y + 2, ancho, alto, 8, 8);
            g.setColor(UI.SUPERFICIE_ALTA);
            g.fillRoundRect(x, y, ancho, alto, 8, 8);
            g.setColor(EJE);
            g.drawRoundRect(x, y, ancho, alto, 8, 8);
            for (int i = 0; i < lineas.length; i++) {
                g.setFont(i == 0 ? ETIQUETA_NEGRITA : ETIQUETA);
                g.setColor(i == 0 ? TEXTO : TEXTO2);
                g.drawString(lineas[i], x + 9, y + 5 + fm.getAscent() + i * fm.getHeight());
            }
        }

        protected String lineaValor(double v) {
            return nombreSerie.isEmpty() ? formato.apply(v) : nombreSerie + ": " + formato.apply(v);
        }
    }

    /** Columnas verticales de una sola serie. */
    public static class Columnas extends Base {
        private String[] etiquetas = {};
        private double[] valores = {};
        private Rectangle2D[] zonas = {};

        public void setDatos(String[] etiquetas, double[] valores) {
            this.etiquetas = etiquetas;
            this.valores = valores;
            this.zonas = new Rectangle2D[valores.length];
            hover = -1;
            repaint();
        }

        @Override protected boolean vacio() { return maximo(valores) <= 0; }

        @Override protected void dibujar(Graphics2D g, int w, int h) {
            g.setFont(CHICA);
            FontMetrics fm = g.getFontMetrics();
            int arriba = 24, base = h - 26;
            double[] esc = escala(maximo(valores), divisiones(base - arriba));
            int izq = anchoEjeY(fm, esc), der = w - 12;
            ejeY(g, esc, izq, der, arriba, base);

            int n = valores.length;
            double slot = (der - izq) / (double) n;
            double ancho = Math.max(2, Math.min(24, Math.min(slot * 0.7, slot - 2)));
            // Con pocas columnas se muestran todas las etiquetas (recortadas); con muchas, se salta para que no se encimen
            boolean recortarEtiquetas = n <= 12 && slot >= 40;
            int paso = recortarEtiquetas ? 1 : pasoEtiquetas(fm, etiquetas, slot);
            int iMax = indiceMaximo(valores);

            for (int i = 0; i < n; i++) {
                double x = izq + i * slot;
                zonas[i] = new Rectangle2D.Double(x, arriba, slot, base - arriba + 20);
                double alto = valores[i] / esc[0] * (base - arriba);
                g.setColor(hover == -1 || hover == i ? SERIES[0] : atenuar(SERIES[0]));
                barraVertical(g, x + (slot - ancho) / 2, base - alto, ancho, alto);
                if (i % paso == 0) {
                    String e = recortarEtiquetas ? recortar(etiquetas[i], fm, (int) slot - 6) : etiquetas[i];
                    g.setColor(TENUE);
                    g.drawString(e, (int) (x + slot / 2 - fm.stringWidth(e) / 2.0), base + 16);
                }
            }

            // Solo se rotula el máximo; el resto queda en el eje y el tooltip
            String s = formato.apply(valores[iMax]);
            double cx = izq + iMax * slot + slot / 2;
            double y = base - valores[iMax] / esc[0] * (base - arriba) - 6;
            g.setFont(ETIQUETA);
            g.setColor(TEXTO2);
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, (int) Math.max(izq, Math.min(der - sw, cx - sw / 2.0)), (int) y);
        }

        @Override protected int indiceEn(int x, int y) { return buscar(zonas, x, y); }

        @Override protected String[] tooltip(int i) { return new String[]{etiquetas[i], lineaValor(valores[i])}; }
    }

    /** Líneas con una o varias series; NaN = punto sin dato. Con una sola serie se rellena el área. */
    public static class Lineas extends Base {
        private String[] etiquetas = {}, series = {};
        private double[][] valores = {};
        private double[] xs = {};
        private int arriba, base;

        public void setDatos(String[] etiquetas, String[] series, double[][] valores) {
            this.etiquetas = etiquetas;
            this.series = series;
            this.valores = valores;
            this.xs = new double[etiquetas.length];
            hover = -1;
            repaint();
        }

        private double maximoTotal() {
            double max = 0;
            for (double[] s : valores) max = Math.max(max, maximo(s));
            return max;
        }

        @Override protected boolean vacio() { return maximoTotal() <= 0; }

        @Override protected void dibujar(Graphics2D g, int w, int h) {
            g.setFont(CHICA);
            FontMetrics fm = g.getFontMetrics();
            arriba = series.length >= 2 ? 34 : 22;
            base = h - 26;
            double[] esc = escala(maximoTotal(), divisiones(base - arriba));
            int izq = anchoEjeY(fm, esc), der = w - 16;
            if (series.length >= 2) leyenda(g, izq, 16);
            ejeY(g, esc, izq, der, arriba, base);

            int n = etiquetas.length;
            double inicio = izq + 10, fin = der - 10;
            for (int i = 0; i < n; i++) xs[i] = n == 1 ? (inicio + fin) / 2 : inicio + i * (fin - inicio) / (n - 1);

            int paso = pasoEtiquetas(fm, etiquetas, n == 1 ? fin - inicio : (fin - inicio) / (n - 1));
            g.setColor(TENUE);
            for (int i = 0; i < n; i += paso) {
                g.drawString(etiquetas[i], (int) (xs[i] - fm.stringWidth(etiquetas[i]) / 2.0), base + 16);
            }

            if (hover >= 0) {
                g.setColor(EJE);
                g.drawLine((int) xs[hover], arriba, (int) xs[hover], base);
            }

            for (int s = series.length - 1; s >= 0; s--) {
                Color color = SERIES[s];
                Path2D linea = new Path2D.Double();
                Path2D area = new Path2D.Double();
                boolean abierto = false;
                double ultimoX = 0;
                int ultimo = -1;
                for (int i = 0; i < n; i++) {
                    double v = valores[s][i];
                    if (Double.isNaN(v)) {
                        if (abierto) { area.lineTo(ultimoX, base); area.closePath(); }
                        abierto = false;
                        continue;
                    }
                    double y = base - v / esc[0] * (base - arriba);
                    if (!abierto) {
                        linea.moveTo(xs[i], y);
                        area.moveTo(xs[i], base);
                        area.lineTo(xs[i], y);
                        abierto = true;
                    } else {
                        linea.lineTo(xs[i], y);
                        area.lineTo(xs[i], y);
                    }
                    ultimoX = xs[i];
                    ultimo = i;
                }
                if (abierto) { area.lineTo(ultimoX, base); area.closePath(); }

                if (series.length == 1) {
                    g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
                    g.fill(area);
                }
                g.setColor(color);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(linea);
                if (ultimo >= 0) punto(g, xs[ultimo], base - valores[s][ultimo] / esc[0] * (base - arriba), color);
                if (hover >= 0 && !Double.isNaN(valores[s][hover])) {
                    punto(g, xs[hover], base - valores[s][hover] / esc[0] * (base - arriba), color);
                }

                // Valor al final de la primera serie
                if (s == 0 && ultimo >= 0 && hover == -1) {
                    String txt = formato.apply(valores[0][ultimo]);
                    g.setFont(ETIQUETA);
                    g.setColor(TEXTO2);
                    int sw = g.getFontMetrics().stringWidth(txt);
                    double y = base - valores[0][ultimo] / esc[0] * (base - arriba);
                    g.drawString(txt, (int) Math.min(der - sw, xs[ultimo] - sw / 2.0), (int) Math.max(arriba - 4, y - 10));
                }
            }
            g.setStroke(new BasicStroke(1));
        }

        private void leyenda(Graphics2D g, int x, int y) {
            g.setFont(ETIQUETA);
            FontMetrics fm = g.getFontMetrics();
            for (int s = 0; s < series.length; s++) {
                g.setColor(SERIES[s]);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(x, y - 4, x + 14, y - 4);
                g.fillOval(x + 3, y - 8, 8, 8);
                g.setColor(TEXTO2);
                g.drawString(series[s], x + 20, y);
                x += 20 + fm.stringWidth(series[s]) + 18;
            }
            g.setStroke(new BasicStroke(1));
            g.setFont(CHICA);
        }

        @Override protected int indiceEn(int x, int y) {
            if (xs.length == 0 || y < arriba - 10 || y > base + 20) return -1;
            int mejor = 0;
            for (int i = 1; i < xs.length; i++) if (Math.abs(xs[i] - x) < Math.abs(xs[mejor] - x)) mejor = i;
            return mejor;
        }

        @Override protected String[] tooltip(int i) {
            java.util.List<String> lineas = new java.util.ArrayList<>();
            lineas.add(etiquetas[i]);
            for (int s = 0; s < series.length; s++) {
                if (!Double.isNaN(valores[s][i])) lineas.add(series[s] + ": " + formato.apply(valores[s][i]));
            }
            return lineas.toArray(new String[0]);
        }
    }

    /** Barras horizontales (ranking): etiqueta a la izquierda y valor en la punta. */
    public static class BarrasH extends Base {
        private String[] etiquetas = {}, detalles = {};
        private double[] valores = {};
        private Rectangle2D[] zonas = {};

        public void setDatos(String[] etiquetas, double[] valores, String[] detalles) {
            this.etiquetas = etiquetas;
            this.valores = valores;
            this.detalles = detalles;
            this.zonas = new Rectangle2D[valores.length];
            hover = -1;
            repaint();
        }

        @Override protected boolean vacio() { return maximo(valores) <= 0; }

        @Override protected void dibujar(Graphics2D g, int w, int h) {
            g.setFont(ETIQUETA);
            FontMetrics fm = g.getFontMetrics();
            int n = valores.length;
            double fila = Math.min(34, (h - 16) / (double) n);
            double grosor = Math.max(4, Math.min(18, fila - 8));

            int anchoEtiquetas = 0, anchoValores = 0;
            for (int i = 0; i < n; i++) {
                anchoEtiquetas = Math.max(anchoEtiquetas, fm.stringWidth(etiquetas[i]));
                anchoValores = Math.max(anchoValores, fm.stringWidth(formato.apply(valores[i])));
            }
            int izq = Math.min((int) (w * 0.38), anchoEtiquetas) + 20;
            int largoMax = w - izq - anchoValores - 20;
            double max = maximo(valores);

            for (int i = 0; i < n; i++) {
                double y = 8 + i * fila;
                zonas[i] = new Rectangle2D.Double(0, y, w, fila);
                double cy = y + fila / 2;
                g.setColor(TEXTO2);
                g.drawString(recortar(etiquetas[i], fm, izq - 20), 10, (int) (cy + fm.getAscent() / 2.0 - 2));
                double largo = valores[i] / max * largoMax;
                g.setColor(hover == -1 || hover == i ? SERIES[0] : atenuar(SERIES[0]));
                barraHorizontal(g, izq, cy - grosor / 2, largo, grosor);
                g.setColor(TEXTO2);
                g.drawString(formato.apply(valores[i]), (int) (izq + largo + 6), (int) (cy + fm.getAscent() / 2.0 - 2));
            }
            g.setColor(EJE);
            g.drawLine(izq, 8, izq, (int) (8 + n * fila));
        }

        @Override protected int indiceEn(int x, int y) { return buscar(zonas, x, y); }

        @Override protected String[] tooltip(int i) {
            String detalle = (detalles != null && i < detalles.length) ? detalles[i] : null;
            return detalle == null
                    ? new String[]{etiquetas[i], lineaValor(valores[i])}
                    : new String[]{etiquetas[i], lineaValor(valores[i]), detalle};
        }
    }

    /** Dona de parte-todo (máximo 6 segmentos; el resto debe agruparse en "Otros"). */
    public static class Dona extends Base {
        private String[] etiquetas = {};
        private double[] valores = {};
        private Color[] colores = {};
        private double total;
        private double cx, cy, radio, grosor;
        private Rectangle2D[] zonasLeyenda = {};

        public void setDatos(String[] etiquetas, double[] valores, Color[] colores) {
            this.etiquetas = etiquetas;
            this.valores = valores;
            this.colores = colores;
            this.zonasLeyenda = new Rectangle2D[valores.length];
            total = 0;
            for (double v : valores) total += v;
            hover = -1;
            repaint();
        }

        @Override protected boolean vacio() { return total <= 0; }

        @Override protected void dibujar(Graphics2D g, int w, int h) {
            double diametro = Math.min(h - 24, w * 0.48);
            radio = diametro / 2;
            grosor = Math.max(14, radio * 0.32);
            cx = 12 + radio;
            cy = h / 2.0;
            double rMedio = radio - grosor / 2;
            double hueco = valores.length > 1 ? Math.toDegrees(2 / rMedio) : 0;

            double inicio = 90;
            for (int i = 0; i < valores.length; i++) {
                double extension = valores[i] / total * 360;
                if (extension <= 0) continue;
                double dibujado = Math.max(0.5, extension - hueco);
                g.setColor(colores[i]);
                g.setStroke(new BasicStroke((float) (hover == i ? grosor + 6 : grosor), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Double(cx - rMedio, cy - rMedio, 2 * rMedio, 2 * rMedio,
                        inicio - hueco / 2, -dibujado, Arc2D.OPEN));
                inicio -= extension;
            }
            g.setStroke(new BasicStroke(1));

            // Total en el centro
            g.setFont(ETIQUETA_NEGRITA);
            FontMetrics fm = g.getFontMetrics();
            String t = formato.apply(total);
            g.setColor(TEXTO);
            g.drawString(t, (int) (cx - fm.stringWidth(t) / 2.0), (int) cy + 2);
            g.setFont(CHICA);
            fm = g.getFontMetrics();
            g.setColor(TENUE);
            g.drawString("Total", (int) (cx - fm.stringWidth("Total") / 2.0), (int) cy + 16);

            // Leyenda con porcentaje
            g.setFont(ETIQUETA);
            fm = g.getFontMetrics();
            int lx = (int) (cx + radio + 24);
            int filaAlto = 24;
            int ly = (int) (cy - valores.length * filaAlto / 2.0);
            int anchoPct = fm.stringWidth("100.0%");
            for (int i = 0; i < valores.length; i++) {
                int y = ly + i * filaAlto;
                zonasLeyenda[i] = new Rectangle2D.Double(lx - 4, y, w - lx, filaAlto);
                g.setColor(colores[i]);
                g.fillRoundRect(lx, y + 7, 10, 10, 3, 3);
                g.setColor(TEXTO2);
                g.drawString(recortar(etiquetas[i], fm, w - lx - anchoPct - 34), lx + 16, y + 16);
                String pct = porcentaje(valores[i] / total);
                g.setFont(ETIQUETA_NEGRITA);
                g.setColor(TEXTO);
                g.drawString(pct, w - 12 - g.getFontMetrics().stringWidth(pct), y + 16);
                g.setFont(ETIQUETA);
            }
        }

        @Override protected int indiceEn(int x, int y) {
            int leyenda = buscar(zonasLeyenda, x, y);
            if (leyenda >= 0) return leyenda;
            double dx = x - cx, dy = y - cy, d = Math.hypot(dx, dy);
            if (d < radio - grosor - 3 || d > radio + 3) return -1;
            double angulo = Math.toDegrees(Math.atan2(-dy, dx));
            double desdeArriba = ((90 - angulo) % 360 + 360) % 360;
            double acumulado = 0;
            for (int i = 0; i < valores.length; i++) {
                acumulado += valores[i] / total * 360;
                if (desdeArriba <= acumulado) return i;
            }
            return valores.length - 1;
        }

        @Override protected String[] tooltip(int i) {
            return new String[]{etiquetas[i], lineaValor(valores[i]), porcentaje(valores[i] / total) + " del total"};
        }
    }

    // ---------- Utilidades de dibujo ----------

    /** Divisiones del eje Y según la altura disponible, para que las marcas no se encimen. */
    static int divisiones(int altoPlot) {
        return altoPlot < 120 ? 2 : 4;
    }

    /** Máximo "redondo" del eje y su paso, en aproximadamente {@code divisiones} partes. */
    static double[] escala(double max, int divisiones) {
        if (max <= 0) return new double[]{1, 1.0 / divisiones};
        double paso = max / divisiones;
        double magnitud = Math.pow(10, Math.floor(Math.log10(paso)));
        double r = paso / magnitud;
        paso = (r <= 1 ? 1 : r <= 2 ? 2 : r <= 2.5 ? 2.5 : r <= 5 ? 5 : 10) * magnitud;
        return new double[]{paso * Math.ceil(max / paso - 1e-9), paso};
    }

    static int anchoEjeY(FontMetrics fm, double[] esc) {
        return fm.stringWidth(compacto(esc[0])) + 16;
    }

    static void ejeY(Graphics2D g, double[] esc, int izq, int der, int arriba, int base) {
        g.setFont(CHICA);
        FontMetrics fm = g.getFontMetrics();
        int divisiones = (int) Math.round(esc[0] / esc[1]);
        for (int k = 0; k <= divisiones; k++) {
            double v = k * esc[1];
            int y = (int) Math.round(base - v / esc[0] * (base - arriba));
            if (k > 0) {
                g.setColor(GRILLA);
                g.drawLine(izq, y, der, y);
            }
            String s = compacto(v);
            g.setColor(TENUE);
            g.drawString(s, izq - 8 - fm.stringWidth(s), y + fm.getAscent() / 2 - 1);
        }
        g.setColor(EJE);
        g.drawLine(izq, base, der, base);
    }

    /** Cada cuántas etiquetas del eje X se dibuja una para que no se encimen. */
    static int pasoEtiquetas(FontMetrics fm, String[] etiquetas, double espacio) {
        int ancho = 0;
        for (String e : etiquetas) ancho = Math.max(ancho, fm.stringWidth(e));
        return Math.max(1, (int) Math.ceil((ancho + 10) / Math.max(1, espacio)));
    }

    /** Columna con extremo superior redondeado (4px) y base recta. */
    static void barraVertical(Graphics2D g, double x, double y, double ancho, double alto) {
        if (alto <= 0) return;
        double r = Math.min(4, Math.min(ancho / 2, alto));
        Path2D p = new Path2D.Double();
        p.moveTo(x, y + alto);
        p.lineTo(x, y + r);
        p.quadTo(x, y, x + r, y);
        p.lineTo(x + ancho - r, y);
        p.quadTo(x + ancho, y, x + ancho, y + r);
        p.lineTo(x + ancho, y + alto);
        p.closePath();
        g.fill(p);
    }

    /** Barra con extremo derecho redondeado (4px) y base recta. */
    static void barraHorizontal(Graphics2D g, double x, double y, double largo, double grosor) {
        if (largo <= 0) return;
        double r = Math.min(4, Math.min(grosor / 2, largo));
        Path2D p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(x + largo - r, y);
        p.quadTo(x + largo, y, x + largo, y + r);
        p.lineTo(x + largo, y + grosor - r);
        p.quadTo(x + largo, y + grosor, x + largo - r, y + grosor);
        p.lineTo(x, y + grosor);
        p.closePath();
        g.fill(p);
    }

    /** Marcador de 8px con anillo de 2px del color de fondo. */
    static void punto(Graphics2D g, double x, double y, Color color) {
        g.setColor(SUPERFICIE);
        g.fill(new Ellipse2D.Double(x - 6, y - 6, 12, 12));
        g.setColor(color);
        g.fill(new Ellipse2D.Double(x - 4, y - 4, 8, 8));
    }

    static Color atenuar(Color c) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), 110);
    }

    static int buscar(Rectangle2D[] zonas, int x, int y) {
        for (int i = 0; i < zonas.length; i++) if (zonas[i] != null && zonas[i].contains(x, y)) return i;
        return -1;
    }

    static double maximo(double[] valores) {
        double max = 0;
        for (double v : valores) if (!Double.isNaN(v)) max = Math.max(max, v);
        return max;
    }

    static int indiceMaximo(double[] valores) {
        int iMax = 0;
        for (int i = 1; i < valores.length; i++) if (valores[i] > valores[iMax]) iMax = i;
        return iMax;
    }

    /** Recorta el texto con "…" para que quepa en el ancho dado. */
    static String recortar(String s, FontMetrics fm, int ancho) {
        if (fm.stringWidth(s) <= ancho) return s;
        while (s.length() > 1 && fm.stringWidth(s + "…") > ancho) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    /** Número compacto para ejes: 950, 1.2K, 3M. */
    public static String compacto(double v) {
        double a = Math.abs(v);
        if (a >= 1_000_000) return decimal(v / 1_000_000) + "M";
        if (a >= 1_000) return decimal(v / 1_000) + "K";
        return decimal(v);
    }

    private static String decimal(double v) {
        return Math.abs(v - Math.rint(v)) < 1e-9 ? String.format("%.0f", v) : String.format("%.1f", v);
    }

    public static String porcentaje(double fraccion) {
        return String.format("%.1f%%", fraccion * 100);
    }
}
