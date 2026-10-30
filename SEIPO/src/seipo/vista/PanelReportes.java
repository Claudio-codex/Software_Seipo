package seipo.vista;

import seipo.controlador.ControladorReportes;
import seipo.controlador.ControladorReportes.*;
import seipo.modelo.Datos;
import seipo.vista.ExportadorReporte.Formato;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.DoubleFunction;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Pantalla Reportes: indicadores del período, gráficos de ventas, productos, categorías, pagos, tendencia e inventario. */
public class PanelReportes extends JPanel {

    private static final String[] PERIODOS = {"Hoy", "Últimos 7 días", "Últimos 30 días", "Este mes", "Mes anterior", "Este año", "Personalizado"};
    private static final int PERSONALIZADO = PERIODOS.length - 1;
    private static final String[] DIAS_SEMANA = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final int MAX_SEGMENTOS = 6;
    private static final int TOP_PRODUCTOS = 10;
    private static final DateTimeFormatter NOMBRE_ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FORMATOS = "formatos"; // propiedad de cada JTable con el formato de sus columnas

    private final ControladorReportes controlador = new ControladorReportes();
    private SwingWorker<DatosReporteVO, Void> trabajo;
    private DatosReporteVO ultimo;
    private boolean ajustando;

    // Filtros
    private final JComboBox<String> cmbPeriodo = new JComboBox<>(PERIODOS);
    private final JSpinner spDesde = spinnerFecha(), spHasta = spinnerFecha();
    private final JLabel lblHoy = new JLabel(), lblEstado = new JLabel();

    // Indicadores
    private final JLabel lblIngresos = new JLabel(), lblNVentas = new JLabel(), lblTicket = new JLabel(),
            lblUnidades = new JLabel(), lblPrecio = new JLabel();
    private final JLabel dIngresos = new JLabel(), dNVentas = new JLabel(), dTicket = new JLabel(),
            dUnidades = new JLabel(), dPrecio = new JLabel();
    private final JPanel[] tarjetas = new JPanel[5];

    // Gráficos
    private final CardLayout cardsSerie = new CardLayout();
    private final JPanel serie = new JPanel(cardsSerie);
    private final Graficos.Columnas colSerie = new Graficos.Columnas(), colHora = new Graficos.Columnas(),
            colSemana = new Graficos.Columnas(), colInventario = new Graficos.Columnas();
    private final Graficos.Lineas linSerie = new Graficos.Lineas(), linMeses = new Graficos.Lineas();
    private final Graficos.BarrasH barProductos = new Graficos.BarrasH();
    private final Graficos.Dona donaCategorias = new Graficos.Dona(), donaPagos = new Graficos.Dona();
    private final JLabel tituloSerie = titulo("Ventas por día"), tituloMeses = titulo("Ventas mensuales");
    private final JComboBox<String> cmbOrdenProductos = new JComboBox<>(new String[]{"Unidades", "Importe"});

    // Tablas
    private final DefaultTableModel mProductos = modelo(new String[]{"Producto", "Categoría", "Unidades", "Importe", "% importe"},
            String.class, String.class, Integer.class, Double.class, Double.class);
    private final DefaultTableModel mCategorias = modelo(new String[]{"Categoría", "Unidades", "Importe", "% importe"},
            String.class, Integer.class, Double.class, Double.class);
    private final DefaultTableModel mPagos = modelo(new String[]{"Forma de pago", "Nº ventas", "Total", "Ticket prom.", "% total"},
            String.class, Integer.class, Double.class, Double.class, Double.class);
    private final DefaultTableModel mInventario = modelo(new String[]{"Categoría", "Productos", "Stock", "Valor", "Vendidas",
                    "Rotación", "Días cobertura", "Stock bajo", "Agotados"},
            String.class, Integer.class, Integer.class, Double.class, Integer.class, Double.class, Double.class, Integer.class, Integer.class);
    private final JTable tProductos = tabla(mProductos, Formato.TEXTO, Formato.TEXTO, Formato.ENTERO, Formato.SOLES, Formato.PORCENTAJE);
    private final JTable tCategorias = tabla(mCategorias, Formato.TEXTO, Formato.ENTERO, Formato.SOLES, Formato.PORCENTAJE);
    private final JTable tPagos = tabla(mPagos, Formato.TEXTO, Formato.ENTERO, Formato.SOLES, Formato.SOLES, Formato.PORCENTAJE);
    private final JTable tInventario = tabla(mInventario, Formato.TEXTO, Formato.ENTERO, Formato.ENTERO, Formato.SOLES,
            Formato.ENTERO, Formato.DECIMAL, Formato.ENTERO, Formato.ENTERO, Formato.ENTERO);

    public PanelReportes() {
        setLayout(new BorderLayout());
        JPanel p = UI.base();
        add(p);

        JPanel norte = new JPanel();
        norte.setOpaque(false);
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        norte.add(barraFiltros());
        norte.add(Box.createVerticalStrut(10));
        norte.add(filaIndicadores());
        p.add(norte, BorderLayout.NORTH);
        p.add(pestanas(), BorderLayout.CENTER);

        configurarGraficos();
        cmbPeriodo.setSelectedIndex(1);
        cmbPeriodo.addActionListener(e -> { if (!ajustando) refrescar(); });
        spDesde.addChangeListener(e -> fechaEditada());
        spHasta.addChangeListener(e -> fechaEditada());
        cmbOrdenProductos.addActionListener(e -> { if (ultimo != null) mostrarProductos(ultimo); });
        refrescar();
    }

    // ---------- Construcción de la interfaz ----------

    private JPanel barraFiltros() {
        JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        f.setOpaque(false);
        f.add(new JLabel("Período:"));
        f.add(cmbPeriodo);
        f.add(Box.createHorizontalStrut(6));
        f.add(new JLabel("Desde"));
        f.add(spDesde);
        f.add(new JLabel("Hasta"));
        f.add(spHasta);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        der.setOpaque(false);
        JButton pdf = UI.botonSecundario("Exportar PDF"), excel = UI.botonSecundario("Exportar Excel");
        pdf.addActionListener(e -> exportar(true));
        excel.addActionListener(e -> exportar(false));
        for (JButton b : new JButton[]{pdf, excel}) {
            b.setBorder(new EmptyBorder(6, 12, 6, 12));
            der.add(b);
        }

        JPanel fila = new JPanel(new BorderLayout());
        fila.setOpaque(false);
        fila.add(f, BorderLayout.CENTER);
        fila.add(der, BorderLayout.EAST);

        // Debajo: ventas de hoy (siempre visibles) y estado de carga
        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        info.setOpaque(false);
        lblHoy.setForeground(Graficos.TEXTO2);
        lblEstado.setForeground(Graficos.TENUE);
        info.add(lblHoy);
        info.add(lblEstado);

        JPanel barra = new JPanel(new BorderLayout(0, 4));
        barra.setOpaque(false);
        barra.add(fila, BorderLayout.NORTH);
        barra.add(info, BorderLayout.SOUTH);
        return barra;
    }

    private JPanel filaIndicadores() {
        JPanel fila = new JPanel(new GridLayout(1, 5, 16, 0));
        fila.setOpaque(false);
        String[] titulos = {"Ingresos", "Nº ventas", "Ticket promedio", "Productos vendidos", "Precio prom. por unidad"};
        JLabel[] valores = {lblIngresos, lblNVentas, lblTicket, lblUnidades, lblPrecio};
        JLabel[] deltas = {dIngresos, dNVentas, dTicket, dUnidades, dPrecio};
        Iconos.Tipo[] iconos = {Iconos.Tipo.MONEDA, Iconos.Tipo.CARRITO, Iconos.Tipo.BILLETE, Iconos.Tipo.CUBO, Iconos.Tipo.ETIQUETA};
        for (int i = 0; i < 5; i++) {
            tarjetas[i] = indicador(titulos[i], valores[i], deltas[i], iconos[i], i == 0);
            fila.add(tarjetas[i]);
        }
        tarjetas[0].setToolTipText("Total cobrado en el período (con descuentos e IGV)");
        tarjetas[4].setToolTipText("Importe de las líneas vendidas (sin IGV) entre unidades vendidas");
        return fila;
    }

    private JPanel indicador(String titulo, JLabel valor, JLabel delta, Iconos.Tipo icono, boolean principal) {
        delta.setFont(UI.fuente(Font.PLAIN, 12));
        JPanel p = UI.tarjeta(titulo, valor, icono, principal, delta);
        valor.setFont(UI.fuente(Font.BOLD, 22));
        return p;
    }

    private JTabbedPane pestanas() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UI.NEGRITA);

        serie.setOpaque(false);
        serie.add(colSerie, "columnas");
        serie.add(linSerie, "lineas");
        JPanel productosCab = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        productosCab.setOpaque(false);
        productosCab.add(new JLabel("Ordenar por"));
        productosCab.add(cmbOrdenProductos);

        anchos(tProductos, 170, 90, 70, 85, 75);
        anchos(tPagos, 110, 75, 90, 90, 70);

        tabs.addTab("Resumen", fila(false, 0.5,
                seccion(tituloSerie, null, serie),
                seccion(titulo("Ventas por hora del día"), null, colHora)));
        tabs.addTab("Productos", fila(false, 0.42,
                seccion(titulo("Top " + TOP_PRODUCTOS + " productos"), productosCab, barProductos),
                seccion(titulo("Detalle por producto"), null, sinMarco(tProductos))));
        tabs.addTab("Categorías", fila(false, 0.5,
                seccion(titulo("Ventas por categoría (importe sin IGV)"), null, donaCategorias),
                seccion(titulo("Detalle por categoría"), null, sinMarco(tCategorias))));
        tabs.addTab("Formas de pago", fila(false, 0.45,
                seccion(titulo("Total cobrado por forma de pago"), null, donaPagos),
                seccion(titulo("Detalle por forma de pago"), null, sinMarco(tPagos))));
        tabs.addTab("Tendencia", fila(false, 0.55,
                seccion(tituloMeses, null, linMeses),
                seccion(titulo("Ventas por día de la semana"), null, colSemana)));

        anchos(tInventario, 120, 70, 60, 90, 70, 70, 95, 75, 70);
        tInventario.getTableHeader().setToolTipText(
                "Rotación = unidades vendidas en el período / stock actual. Días cobertura = stock / venta diaria promedio.");
        tabs.addTab("Inventario", fila(true, 0.5,
                seccion(titulo("Valor del inventario por categoría"), null, colInventario),
                seccion(titulo("Inventario y rotación por categoría"), null, sinMarco(tInventario))));
        return tabs;
    }

    private void configurarGraficos() {
        DoubleFunction<String> soles = Datos::soles;
        for (Graficos.Base g : new Graficos.Base[]{colSerie, linSerie, colHora, colSemana, linMeses, donaCategorias, donaPagos, colInventario}) {
            g.setFormato(soles);
        }
        colSerie.setNombreSerie("Ventas");
        linSerie.setNombreSerie("Ventas");
        colHora.setNombreSerie("Ventas");
        colSemana.setNombreSerie("Ventas");
        donaCategorias.setNombreSerie("Importe");
        donaPagos.setNombreSerie("Total");
        colInventario.setNombreSerie("Valor");
    }

    // ---------- Carga de datos ----------

    /** Recalcula las fechas del período elegido y recarga (lo llama Principal al mostrar la pantalla). */
    public void refrescar() {
        int i = cmbPeriodo.getSelectedIndex();
        if (i != PERSONALIZADO) {
            LocalDate hoy = LocalDate.now(), desde, hasta = hoy;
            switch (i) {
                case 0: desde = hoy; break;
                case 1: desde = hoy.minusDays(6); break;
                case 2: desde = hoy.minusDays(29); break;
                case 3: desde = hoy.withDayOfMonth(1); break;
                case 4: desde = hoy.minusMonths(1).withDayOfMonth(1); hasta = hoy.withDayOfMonth(1).minusDays(1); break;
                default: desde = hoy.withDayOfYear(1); break;
            }
            ajustando = true;
            spDesde.setValue(aDate(desde));
            spHasta.setValue(aDate(hasta));
            ajustando = false;
        }
        cargar();
    }

    private void fechaEditada() {
        if (ajustando) return;
        ajustando = true;
        cmbPeriodo.setSelectedIndex(PERSONALIZADO);
        ajustando = false;
        cargar();
    }

    private void cargar() {
        LocalDate desde = aLocal(spDesde), hasta = aLocal(spHasta);
        if (desde.isAfter(hasta)) {
            lblEstado.setText("La fecha inicial es posterior a la final");
            return;
        }
        if (trabajo != null) trabajo.cancel(false);
        lblEstado.setText("Cargando…");
        SwingWorker<DatosReporteVO, Void> w = new SwingWorker<DatosReporteVO, Void>() {
            @Override protected DatosReporteVO doInBackground() throws Exception {
                return controlador.obtenerReporte(desde, hasta);
            }

            @Override protected void done() {
                if (isCancelled()) return;
                lblEstado.setText("");
                try {
                    mostrar(get());
                } catch (InterruptedException ignored) {
                } catch (ExecutionException ex) {
                    JOptionPane.showMessageDialog(PanelReportes.this, "Error al cargar reportes desde la base de datos:\n"
                            + ex.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.getCause().printStackTrace();
                }
            }
        };
        trabajo = w;
        w.execute();
    }

    // ---------- Presentación ----------

    private void mostrar(DatosReporteVO vo) {
        ultimo = vo;
        KpisVO a = vo.actual, b = vo.anterior;
        lblHoy.setText("Hoy: " + Datos.soles(vo.hoy.ingresos) + " en " + vo.hoy.numeroVentas
                + (vo.hoy.numeroVentas == 1 ? " venta" : " ventas"));

        lblIngresos.setText(Datos.soles(a.ingresos));
        lblNVentas.setText(String.valueOf(a.numeroVentas));
        lblTicket.setText(Datos.soles(a.ticketPromedio()));
        lblUnidades.setText(String.valueOf(a.unidades));
        lblPrecio.setText(Datos.soles(a.precioPromedio()));
        variacion(dIngresos, a.ingresos, b.ingresos, false);
        variacion(dNVentas, a.numeroVentas, b.numeroVentas, false);
        variacion(dTicket, a.ticketPromedio(), b.ticketPromedio(), false);
        variacion(dUnidades, a.unidades, b.unidades, false);
        variacion(dPrecio, a.precioPromedio(), b.precioPromedio(), true);
        String anterior = "Período anterior: " + vo.desdeAnterior.format(Datos.FECHA) + " – " + vo.hastaAnterior.format(Datos.FECHA);
        for (JLabel d : new JLabel[]{dIngresos, dNVentas, dTicket, dUnidades, dPrecio}) d.setToolTipText(anterior);

        // Resumen
        boolean comoLinea = vo.serieValores.length > 31;
        tituloSerie.setText(vo.serieMensual ? "Ventas por mes" : "Ventas por día");
        if (comoLinea) linSerie.setDatos(vo.serieEtiquetas, new String[]{"Ventas"}, new double[][]{vo.serieValores});
        else colSerie.setDatos(vo.serieEtiquetas, vo.serieValores);
        cardsSerie.show(serie, comoLinea ? "lineas" : "columnas");

        int primera = 8, ultimaHora = 20;
        for (int h = 0; h < 24; h++) {
            if (vo.porHora[h] > 0) {
                primera = Math.min(primera, h);
                ultimaHora = Math.max(ultimaHora, h);
            }
        }
        String[] horas = new String[ultimaHora - primera + 1];
        double[] porHora = new double[horas.length];
        for (int h = primera; h <= ultimaHora; h++) {
            horas[h - primera] = h + "h";
            porHora[h - primera] = vo.porHora[h];
        }
        colHora.setDatos(horas, porHora);

        mostrarProductos(vo);
        mostrarCategorias(vo);
        mostrarPagos(vo);

        // Tendencia
        tituloMeses.setText("Ventas mensuales: " + vo.anio + " vs " + (vo.anio - 1));
        linMeses.setDatos(ControladorReportes.MESES, new String[]{String.valueOf(vo.anio), String.valueOf(vo.anio - 1)},
                new double[][]{vo.mesesActual, vo.mesesAnterior});
        colSemana.setDatos(DIAS_SEMANA, vo.porDiaSemana);

        mostrarInventario(vo);
    }

    private void mostrarProductos(DatosReporteVO vo) {
        boolean porImporte = cmbOrdenProductos.getSelectedIndex() == 1;
        List<ProductoVO> orden = new ArrayList<>(vo.productos);
        orden.sort(porImporte ? (x, y) -> Double.compare(y.importe, x.importe) : (x, y) -> Integer.compare(y.unidades, x.unidades));
        int n = Math.min(TOP_PRODUCTOS, orden.size());
        String[] nombres = new String[n], detalles = new String[n];
        double[] valores = new double[n];
        for (int i = 0; i < n; i++) {
            ProductoVO pr = orden.get(i);
            nombres[i] = pr.nombre;
            valores[i] = porImporte ? pr.importe : pr.unidades;
            detalles[i] = porImporte ? pr.unidades + " unidades" : "Importe: " + Datos.soles(pr.importe);
        }
        barProductos.setFormato(porImporte ? Datos::soles : v -> String.format("%.0f", v));
        barProductos.setNombreSerie(porImporte ? "Importe" : "Unidades");
        barProductos.setDatos(nombres, valores, detalles);

        double total = 0;
        for (ProductoVO pr : vo.productos) total += pr.importe;
        mProductos.setRowCount(0);
        for (ProductoVO pr : orden) {
            mProductos.addRow(new Object[]{pr.nombre, pr.categoria, pr.unidades, pr.importe, total == 0 ? 0.0 : pr.importe / total});
        }
    }

    private void mostrarCategorias(DatosReporteVO vo) {
        double total = 0;
        for (GrupoVO c : vo.categorias) total += c.importe;
        mCategorias.setRowCount(0);
        for (GrupoVO c : vo.categorias) {
            mCategorias.addRow(new Object[]{c.nombre, c.cantidad, c.importe, total == 0 ? 0.0 : c.importe / total});
        }
        dona(donaCategorias, vo.categorias, null);
    }

    private void mostrarPagos(DatosReporteVO vo) {
        double total = 0;
        for (GrupoVO g : vo.formasPago) total += g.importe;
        mPagos.setRowCount(0);
        for (GrupoVO g : vo.formasPago) {
            mPagos.addRow(new Object[]{g.nombre, g.cantidad, g.importe, g.importe / g.cantidad, total == 0 ? 0.0 : g.importe / total});
        }
        // Color fijo por forma de pago, para que no cambie al filtrar
        dona(donaPagos, vo.formasPago, Arrays.asList(Datos.FORMAS_PAGO));
    }

    /** Dona con los primeros segmentos y el resto agrupado en "Otros". */
    private void dona(Graficos.Dona dona, List<GrupoVO> grupos, List<String> colorFijo) {
        int n = Math.min(grupos.size(), MAX_SEGMENTOS);
        boolean agrupar = grupos.size() > MAX_SEGMENTOS;
        String[] nombres = new String[n];
        double[] valores = new double[n];
        Color[] colores = new Color[n];
        int siguiente = colorFijo == null ? 0 : colorFijo.size();
        for (int i = 0; i < n; i++) {
            if (agrupar && i == n - 1) {
                nombres[i] = "Otros";
                for (int j = i; j < grupos.size(); j++) valores[i] += grupos.get(j).importe;
                colores[i] = Graficos.OTROS;
                break;
            }
            GrupoVO g = grupos.get(i);
            nombres[i] = g.nombre;
            valores[i] = g.importe;
            int fijo = colorFijo == null ? -1 : colorFijo.indexOf(g.nombre);
            colores[i] = Graficos.SERIES[fijo >= 0 ? fijo : siguiente++ % Graficos.SERIES.length];
        }
        dona.setDatos(nombres, valores, colores);
    }

    private void mostrarInventario(DatosReporteVO vo) {
        int n = vo.inventario.size();
        String[] nombres = new String[n];
        double[] valores = new double[n];
        mInventario.setRowCount(0);
        for (int i = 0; i < n; i++) {
            InventarioVO inv = vo.inventario.get(i);
            nombres[i] = inv.categoria;
            valores[i] = inv.valor;
            mInventario.addRow(new Object[]{inv.categoria, inv.productos, inv.stock, inv.valor, inv.vendidas,
                    inv.rotacion(), inv.diasCobertura(vo.dias), inv.bajo, inv.agotados});
        }
        colInventario.setDatos(nombres, valores);
    }

    private void variacion(JLabel l, double actual, double anterior, boolean neutral) {
        if (anterior == 0) {
            l.setText(actual == 0 ? "Sin variación" : "Sin datos previos");
            l.setForeground(Graficos.TENUE);
            return;
        }
        double v = (actual - anterior) / anterior * 100;
        if (Math.abs(v) < 0.05) {
            l.setText("= Sin cambio 0.0%");
            l.setForeground(Graficos.TENUE);
            return;
        }
        // Flecha, palabra y signo siempre visibles, para que no dependa del color
        l.setText(String.format("%s%.1f%%", v > 0 ? "▲ Sube +" : "▼ Baja −", Math.abs(v)));
        l.setToolTipText("Comparado con el período anterior de la misma duración");
        l.setForeground(neutral || v > 0 ? Graficos.TEXTO2 : UI.ACENTO_PULSADO);
    }

    // ---------- Exportación ----------

    private void exportar(boolean pdf) {
        if (ultimo == null) {
            JOptionPane.showMessageDialog(this, "El reporte todavía se está cargando.");
            return;
        }
        String ext = pdf ? "pdf" : "xlsx";
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(pdf ? "Exportar reporte a PDF" : "Exportar reporte a Excel");
        fc.setFileFilter(new FileNameExtensionFilter(pdf ? "Documento PDF (*.pdf)" : "Libro de Excel (*.xlsx)", ext));
        fc.setSelectedFile(new File("Reporte_SEIPO_" + LocalDateTime.now().format(NOMBRE_ARCHIVO) + "." + ext));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File archivo = fc.getSelectedFile();
        if (!archivo.getName().toLowerCase(Locale.ROOT).endsWith("." + ext)) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + "." + ext);
        }
        if (archivo.exists() && JOptionPane.showConfirmDialog(this, "El archivo ya existe. ¿Desea reemplazarlo?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        try {
            ExportadorReporte.Reporte r = armarReporte(ultimo);
            if (pdf) ExportadorReporte.pdf(r, archivo);
            else ExportadorReporte.excel(r, archivo);
            JOptionPane.showMessageDialog(this, "Reporte exportado:\n" + archivo.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar el reporte (¿está abierto en otro programa?):\n"
                    + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Reúne indicadores y tablas del período filtrado, con las filas en el orden en que se ven en pantalla. */
    private ExportadorReporte.Reporte armarReporte(DatosReporteVO vo) {
        ExportadorReporte.Reporte r = new ExportadorReporte.Reporte();
        r.titulo = "Reporte de ventas – SEIPO";
        r.periodo = vo.desde.format(FECHA_LARGA) + " al " + vo.hasta.format(FECHA_LARGA)
                + " (" + cmbPeriodo.getSelectedItem() + ")";
        KpisVO k = vo.actual;
        r.indicadores.add(new ExportadorReporte.Indicador("Ingresos", k.ingresos, Formato.SOLES));
        r.indicadores.add(new ExportadorReporte.Indicador("Nº ventas", k.numeroVentas, Formato.ENTERO));
        r.indicadores.add(new ExportadorReporte.Indicador("Ticket promedio", k.ticketPromedio(), Formato.SOLES));
        r.indicadores.add(new ExportadorReporte.Indicador("Productos vendidos", k.unidades, Formato.ENTERO));
        r.indicadores.add(new ExportadorReporte.Indicador("Precio prom. por unidad", k.precioPromedio(), Formato.SOLES));

        ExportadorReporte.Seccion serie = new ExportadorReporte.Seccion(vo.serieMensual ? "Ventas por mes" : "Ventas por día",
                new String[]{vo.serieMensual ? "Mes" : "Día", "Ventas"}, Formato.TEXTO, Formato.SOLES);
        for (int i = 0; i < vo.serieValores.length; i++) serie.filas.add(new Object[]{vo.serieEtiquetas[i], vo.serieValores[i]});
        serie.anchos = new float[]{1, 1};
        r.secciones.add(serie);

        r.secciones.add(seccion("Productos", tProductos));
        r.secciones.add(seccion("Categorías", tCategorias));
        r.secciones.add(seccion("Formas de pago", tPagos));
        r.secciones.add(seccion("Inventario por categoría", tInventario));
        return r;
    }

    /** Copia una tabla tal como se ve: orden de filas y columnas actual, con el formato de cada columna. */
    private static ExportadorReporte.Seccion seccion(String titulo, JTable t) {
        int n = t.getColumnCount();
        String[] columnas = new String[n];
        Formato[] formatos = new Formato[n];
        float[] anchos = new float[n];
        for (int c = 0; c < n; c++) {
            columnas[c] = t.getColumnName(c);
            formatos[c] = formatoDe(t, c);
            anchos[c] = t.getColumnModel().getColumn(c).getPreferredWidth();
        }
        ExportadorReporte.Seccion s = new ExportadorReporte.Seccion(titulo, columnas, formatos);
        s.anchos = anchos;
        for (int f = 0; f < t.getRowCount(); f++) {
            Object[] fila = new Object[n];
            for (int c = 0; c < n; c++) fila[c] = t.getValueAt(f, c);
            s.filas.add(fila);
        }
        return s;
    }

    private static Formato formatoDe(JTable t, int col) {
        return ((Formato[]) t.getClientProperty(FORMATOS))[t.convertColumnIndexToModel(col)];
    }

    // ---------- Utilidades de interfaz ----------

    private static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(UI.NEGRITA);
        return l;
    }

    /** Tabla desplazable sin marco propio, porque ya va dentro de la tarjeta de su sección. */
    private static JScrollPane sinMarco(JTable t) {
        JScrollPane s = new JScrollPane(t);
        s.setBorder(BorderFactory.createEmptyBorder());
        return s;
    }

    private static JPanel seccion(JLabel titulo, JComponent extra, JComponent contenido) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(Graficos.SUPERFICIE);
        p.setBorder(UI.bordeTarjeta(12, 14, 12, 14));
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.add(titulo, BorderLayout.CENTER);
        if (extra != null) cab.add(extra, BorderLayout.EAST);
        p.add(cab, BorderLayout.NORTH);
        p.add(contenido, BorderLayout.CENTER);
        return p;
    }

    /** Dos secciones lado a lado (o una sobre otra si {@code vertical}); la primera ocupa la fracción {@code peso}. */
    private static JPanel fila(boolean vertical, double peso, JComponent primera, JComponent segunda) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UI.FONDO);
        p.setBorder(new EmptyBorder(16, 0, 0, 0));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = vertical ? 1 : peso;
        c.weighty = vertical ? peso : 1;
        c.insets = vertical ? new Insets(0, 0, 8, 0) : new Insets(0, 0, 0, 8);
        // Tamaño preferido mínimo para que el reparto lo decidan solo los pesos
        primera.setPreferredSize(new Dimension(10, 10));
        segunda.setPreferredSize(new Dimension(10, 10));
        p.add(primera, c);
        c.gridx = vertical ? 0 : 1;
        c.gridy = vertical ? 1 : 0;
        c.weightx = vertical ? 1 : 1 - peso;
        c.weighty = vertical ? 1 - peso : 1;
        c.insets = vertical ? new Insets(8, 0, 0, 0) : new Insets(0, 8, 0, 0);
        p.add(segunda, c);
        return p;
    }

    private static void anchos(JTable t, int... anchos) {
        for (int i = 0; i < anchos.length; i++) t.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
    }

    /** Modelo de solo lectura con tipos por columna, para que la tabla ordene números como números. */
    private static DefaultTableModel modelo(String[] columnas, Class<?>... tipos) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return tipos[c]; }
        };
    }

    /** Tabla ordenable; cada columna Double se muestra con su formato, el mismo que se usa al exportar. */
    private static JTable tabla(DefaultTableModel m, Formato... formatos) {
        JTable t = UI.tabla(m);
        t.setAutoCreateRowSorter(true);
        t.putClientProperty(FORMATOS, formatos);
        for (int c = 0; c < m.getColumnCount(); c++) {
            if (m.getColumnClass(c) == Double.class) {
                Formato f = formatos[c];
                DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
                    @Override protected void setValue(Object v) { setText(ExportadorReporte.texto(v, f)); }
                };
                r.setHorizontalAlignment(SwingConstants.RIGHT);
                t.getColumnModel().getColumn(c).setCellRenderer(r);
            }
        }
        return t;
    }

    private static JSpinner spinnerFecha() {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        s.setPreferredSize(new Dimension(135, s.getPreferredSize().height));
        return s;
    }

    private static Date aDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate aLocal(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
