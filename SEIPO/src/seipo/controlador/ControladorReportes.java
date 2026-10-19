package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Datos;
import seipo.modelo.Sesion;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ControladorReportes {

    public static final String[] MESES = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Set", "Oct", "Nov", "Dic"};
    public static final String SIN_CATEGORIA = "Sin categoría";
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final int MAX_DIAS_SERIE_DIARIA = 62;

    /** Indicadores de un período. */
    public static class KpisVO {
        public int numeroVentas, unidades;
        public double ingresos;      // total cobrado (con descuento e IGV)
        public double importeBruto;  // suma de precio x cantidad (sin descuento ni IGV)

        public double ticketPromedio() { return numeroVentas == 0 ? 0 : ingresos / numeroVentas; }
        public double precioPromedio() { return unidades == 0 ? 0 : importeBruto / unidades; }
    }

    public static class ProductoVO {
        public String nombre, categoria;
        public int unidades;
        public double importe;
    }

    /** Agrupación genérica: categoría o forma de pago. */
    public static class GrupoVO {
        public String nombre;
        public int cantidad;
        public double importe;

        GrupoVO(String nombre) { this.nombre = nombre; }
    }

    public static class InventarioVO {
        public String categoria;
        public int productos, stock, vendidas, bajo, agotados;
        public double valor;

        InventarioVO(String categoria) { this.categoria = categoria; }

        /** Unidades vendidas en el período por cada unidad en stock; NaN si no hay stock. */
        public double rotacion() { return stock == 0 ? Double.NaN : (double) vendidas / stock; }

        /** Días que dura el stock al ritmo de venta del período; NaN si no hubo ventas. */
        public double diasCobertura(long dias) { return vendidas == 0 ? Double.NaN : stock / (vendidas / (double) dias); }
    }

    public static class DatosReporteVO {
        public LocalDate desde, hasta, desdeAnterior, hastaAnterior;
        public long dias;
        public KpisVO actual, anterior, hoy;
        public boolean serieMensual;
        public String[] serieEtiquetas;
        public double[] serieValores;
        public double[] porHora = new double[24];
        public double[] porDiaSemana = new double[7];
        public int anio;
        public double[] mesesActual = new double[12], mesesAnterior = new double[12]; // NaN = mes futuro
        public List<ProductoVO> productos = new ArrayList<>();
        public List<GrupoVO> categorias = new ArrayList<>(), formasPago = new ArrayList<>();
        public List<InventarioVO> inventario = new ArrayList<>();
    }

    public DatosReporteVO obtenerReporte(LocalDate desde, LocalDate hasta) throws SQLException {
        if (!Sesion.esAdministrador()) throw new SQLException("Solo un administrador puede consultar los reportes.");
        DatosReporteVO vo = new DatosReporteVO();
        vo.desde = desde;
        vo.hasta = hasta;
        vo.dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        vo.desdeAnterior = desde.minusDays(vo.dias);
        vo.hastaAnterior = desde.minusDays(1);
        LocalDate hoy = LocalDate.now();

        try (Connection con = ConexionBD.obtenerConexion()) {
            vo.actual = kpis(con, desde, hasta);
            vo.anterior = kpis(con, vo.desdeAnterior, vo.hastaAnterior);
            vo.hoy = kpis(con, hoy, hoy);
            cargarSerie(con, vo);
            cargarPorHora(con, vo);
            cargarMeses(con, vo, hoy);
            Map<String, String> categoriaDe = new HashMap<>();
            Map<String, InventarioVO> inventario = cargarInventario(con, vo, categoriaDe);
            cargarProductos(con, vo, categoriaDe, inventario);
            cargarFormasPago(con, vo);
        }
        return vo;
    }

    private KpisVO kpis(Connection con, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT v.n, v.ingresos, d.unidades, d.importe FROM " +
                "(SELECT COUNT(*) n, COALESCE(SUM(total), 0) ingresos FROM ventas WHERE estado = 'COMPLETADA' AND fecha BETWEEN ? AND ?) v, " +
                "(SELECT COALESCE(SUM(dv.cantidad), 0) unidades, COALESCE(SUM(dv.importe), 0) importe " +
                " FROM detalle_ventas dv JOIN ventas x ON x.id = dv.venta_id WHERE x.estado = 'COMPLETADA' AND x.fecha BETWEEN ? AND ?) d";
        KpisVO k = new KpisVO();
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            rango(pst, 2, desde, hasta);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    k.numeroVentas = rs.getInt(1);
                    k.ingresos = rs.getDouble(2);
                    k.unidades = rs.getInt(3);
                    k.importeBruto = rs.getDouble(4);
                }
            }
        }
        return k;
    }

    private void cargarSerie(Connection con, DatosReporteVO vo) throws SQLException {
        Map<LocalDate, Double> porFecha = new HashMap<>();
        String sql = "SELECT fecha, SUM(total) FROM ventas WHERE estado = 'COMPLETADA' AND fecha BETWEEN ? AND ? GROUP BY fecha";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            rango(pst, 1, vo.desde, vo.hasta);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    LocalDate fecha = rs.getDate(1).toLocalDate();
                    double total = rs.getDouble(2);
                    porFecha.put(fecha, total);
                    vo.porDiaSemana[fecha.getDayOfWeek().getValue() - 1] += total;
                }
            }
        }

        vo.serieMensual = vo.dias > MAX_DIAS_SERIE_DIARIA;
        if (vo.serieMensual) {
            LocalDate primerMes = vo.desde.withDayOfMonth(1);
            int n = (int) ChronoUnit.MONTHS.between(primerMes, vo.hasta.withDayOfMonth(1)) + 1;
            vo.serieEtiquetas = new String[n];
            vo.serieValores = new double[n];
            for (int i = 0; i < n; i++) {
                LocalDate m = primerMes.plusMonths(i);
                vo.serieEtiquetas[i] = MESES[m.getMonthValue() - 1] + " " + String.format("%02d", m.getYear() % 100);
            }
            for (Map.Entry<LocalDate, Double> e : porFecha.entrySet()) {
                vo.serieValores[(int) ChronoUnit.MONTHS.between(primerMes, e.getKey().withDayOfMonth(1))] += e.getValue();
            }
        } else {
            int n = (int) vo.dias;
            vo.serieEtiquetas = new String[n];
            vo.serieValores = new double[n];
            for (int i = 0; i < n; i++) {
                LocalDate d = vo.desde.plusDays(i);
                vo.serieEtiquetas[i] = d.format(DIA);
                vo.serieValores[i] = porFecha.getOrDefault(d, 0.0);
            }
        }
    }

    private void cargarPorHora(Connection con, DatosReporteVO vo) throws SQLException {
        String sql = "SELECT HOUR(creado_en), SUM(total) FROM ventas WHERE estado = 'COMPLETADA' AND fecha BETWEEN ? AND ? GROUP BY HOUR(creado_en)";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            rango(pst, 1, vo.desde, vo.hasta);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) vo.porHora[rs.getInt(1)] = rs.getDouble(2);
            }
        }
    }

    /** Ventas mensuales del año de la fecha final frente al año anterior (historial estacional). */
    private void cargarMeses(Connection con, DatosReporteVO vo, LocalDate hoy) throws SQLException {
        vo.anio = vo.hasta.getYear();
        if (vo.anio == hoy.getYear()) {
            for (int m = hoy.getMonthValue(); m < 12; m++) vo.mesesActual[m] = Double.NaN;
        }
        String sql = "SELECT YEAR(fecha), MONTH(fecha), SUM(total) FROM ventas WHERE estado = 'COMPLETADA' AND fecha BETWEEN ? AND ? " +
                "GROUP BY YEAR(fecha), MONTH(fecha)";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            rango(pst, 1, LocalDate.of(vo.anio - 1, 1, 1), LocalDate.of(vo.anio, 12, 31));
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    double[] destino = rs.getInt(1) == vo.anio ? vo.mesesActual : vo.mesesAnterior;
                    destino[rs.getInt(2) - 1] = rs.getDouble(3);
                }
            }
        }
    }

    private Map<String, InventarioVO> cargarInventario(Connection con, DatosReporteVO vo, Map<String, String> categoriaDe) throws SQLException {
        Map<String, InventarioVO> porCategoria = new HashMap<>();
        // Agotados y stock bajo con el stock vendible, igual que las alertas de Compras; el valor usa el stock total
        String sql = "SELECT p.nombre, p.categoria, p.precio, p.stock, p.stock_minimo, " + ControladorVentas.STOCK_DISPONIBLE +
                " FROM productos p";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String categoria = categoria(rs.getString(2));
                int stock = rs.getInt(4), disponible = rs.getInt(6);
                categoriaDe.put(rs.getString(1), categoria);

                InventarioVO inv = porCategoria.computeIfAbsent(categoria, InventarioVO::new);
                inv.productos++;
                inv.stock += stock;
                inv.valor += rs.getDouble(3) * stock;
                if (disponible == 0) inv.agotados++;
                else if (disponible <= rs.getInt(5)) inv.bajo++;
            }
        }
        vo.inventario.addAll(porCategoria.values());
        vo.inventario.sort((a, b) -> Double.compare(b.valor, a.valor));
        return porCategoria;
    }

    private void cargarProductos(Connection con, DatosReporteVO vo, Map<String, String> categoriaDe,
                                 Map<String, InventarioVO> inventario) throws SQLException {
        String sql = "SELECT d.producto_nombre, SUM(d.cantidad), SUM(d.importe) FROM detalle_ventas d " +
                "JOIN ventas v ON v.id = d.venta_id WHERE v.estado = 'COMPLETADA' AND v.fecha BETWEEN ? AND ? " +
                "GROUP BY d.producto_nombre ORDER BY SUM(d.cantidad) DESC";
        Map<String, GrupoVO> categorias = new HashMap<>();
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            rango(pst, 1, vo.desde, vo.hasta);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    ProductoVO p = new ProductoVO();
                    p.nombre = rs.getString(1);
                    p.categoria = categoriaDe.getOrDefault(p.nombre, SIN_CATEGORIA);
                    p.unidades = rs.getInt(2);
                    p.importe = rs.getDouble(3);
                    vo.productos.add(p);

                    GrupoVO c = categorias.computeIfAbsent(p.categoria, GrupoVO::new);
                    c.cantidad += p.unidades;
                    c.importe += p.importe;
                    InventarioVO inv = inventario.get(p.categoria);
                    if (inv != null) inv.vendidas += p.unidades;
                }
            }
        }
        vo.categorias.addAll(categorias.values());
        vo.categorias.sort((a, b) -> Double.compare(b.importe, a.importe));
    }

    private void cargarFormasPago(Connection con, DatosReporteVO vo) throws SQLException {
        String sql = "SELECT forma_pago, COUNT(*), SUM(total) FROM ventas WHERE estado = 'COMPLETADA' AND fecha BETWEEN ? AND ? " +
                "GROUP BY forma_pago ORDER BY SUM(total) DESC";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            rango(pst, 1, vo.desde, vo.hasta);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    GrupoVO g = new GrupoVO(rs.getString(1));
                    g.cantidad = rs.getInt(2);
                    g.importe = rs.getDouble(3);
                    vo.formasPago.add(g);
                }
            }
        }
    }

    private static String categoria(String c) {
        return (c == null || c.trim().isEmpty()) ? SIN_CATEGORIA : c.trim();
    }

    /** Asigna {@code pares} parejas (desde, hasta) consecutivas a los parámetros de la consulta. */
    private static void rango(PreparedStatement pst, int pares, LocalDate desde, LocalDate hasta) throws SQLException {
        for (int i = 0; i < pares; i++) {
            pst.setDate(2 * i + 1, java.sql.Date.valueOf(desde));
            pst.setDate(2 * i + 2, java.sql.Date.valueOf(hasta));
        }
    }
}
