package seipo.pruebas;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seipo.controlador.ControladorCompras;
import seipo.controlador.ControladorCompras.AlertaVO;
import seipo.controlador.ControladorCompras.DetalleOrdenVO;
import seipo.controlador.ControladorMermas;
import seipo.controlador.ControladorMermas.MermaVO;
import seipo.modelo.Sesion;
import seipo.vista.ExportadorReporte;
import seipo.vista.PanelMermas;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Procesos 09-14 (alertas, órdenes de compra, mermas) y 16 (reportes). */
class InventarioYReportesTest extends PruebaBase {

    private final ControladorMermas mermas = new ControladorMermas();

    @Test
    @DisplayName("CP-12 Mermas: nace Pendiente sin descontar stock; al aprobar descuenta y registra autorizador; al rechazar no descuenta")
    void cp12_mermas() throws SQLException {
        iniciarSesion("Personal1", "890");
        int producto = crearProducto("CP12", "4.50", 2, null);
        recibirLote(producto, "CP12-L1", LocalDate.now().plusMonths(3), 10);
        int lote = loteId("CP12-L1");

        // El personal registra dos mermas
        mermas.registrarMerma(lote, 3, "Dañado", "Caja aplastada (CP-12)");
        int mermaAprobar = ultimaMerma();
        mermas.registrarMerma(lote, 2, "Deteriorado", "Empaque abierto (CP-12)");
        int mermaRechazar = ultimaMerma();
        String estadoInicial = estadoMerma(mermaAprobar);
        long stockTrasRegistro = stockProducto(producto), loteTrasRegistro = cantidadLote("CP12-L1");
        // El personal no puede autorizarlas
        assertThrows(SQLException.class, () -> mermas.aprobarMerma(mermaAprobar), "el personal no aprueba mermas");
        Sesion.cerrar();

        // El administrador aprueba una y rechaza la otra
        iniciarSesion("admin", "123");
        mermas.aprobarMerma(mermaAprobar);
        long stockTrasAprobar = stockProducto(producto);
        mermas.rechazarMerma(mermaRechazar, "No corresponde (CP-12)");
        long stockTrasRechazar = stockProducto(producto);

        assertAll(
                () -> assertEquals(ControladorMermas.PENDIENTE, estadoInicial, "la merma nace pendiente"),
                () -> assertEquals(10, stockTrasRegistro, "registrar no descuenta stock"),
                () -> assertEquals(10, loteTrasRegistro, "registrar no descuenta el lote"),
                () -> assertEquals(ControladorMermas.APROBADA, estadoMerma(mermaAprobar)),
                () -> assertEquals(7, stockTrasAprobar, "aprobar descuenta las 3 unidades"),
                () -> assertEquals(7, cantidadLote("CP12-L1")),
                () -> assertEquals(Sesion.getId(), entero("SELECT autorizado_por FROM mermas WHERE id = ?", mermaAprobar),
                        "se registra quién autorizó"),
                () -> assertNotNull(texto("SELECT autorizado_en FROM mermas WHERE id = ?", mermaAprobar), "y cuándo"),
                () -> assertEquals(ControladorMermas.RECHAZADA, estadoMerma(mermaRechazar)),
                () -> assertEquals(7, stockTrasRechazar, "rechazar no descuenta stock"));
    }

    @Test
    @DisplayName("CP-13 Alertas y orden de compra: el producto bajo el mínimo aparece en alertas; la orden se crea y la aprueba el administrador")
    void cp13_alertasYOrdenDeCompra() throws SQLException {
        iniciarSesion("Personal1", "890");
        int producto = crearProducto("CP13", "3.00", 10, 50);
        recibirLote(producto, "CP13-L1", LocalDate.now().plusMonths(6), 4);
        ControladorCompras compras = new ControladorCompras();

        AlertaVO alerta = alertaDe(compras, producto);
        int proveedor = (int) entero("SELECT id FROM proveedores WHERE activo = 1 ORDER BY id LIMIT 1");
        int orden = compras.crearOrden(proveedor, Collections.singletonList(
                new DetalleOrdenVO(producto, "CP13", "Producto de prueba CP13", alerta.sugerido())), "Reposición CP-13");
        String estadoCreada = estadoOrden(orden);
        assertThrows(SQLException.class, () -> compras.aprobarOrden(orden), "el personal no aprueba órdenes");
        Sesion.cerrar();

        iniciarSesion("admin", "123");
        compras.aprobarOrden(orden);
        AlertaVO alertaTrasOrden = alertaDe(compras, producto);

        assertAll(
                () -> assertEquals(ControladorCompras.STOCK_BAJO, alerta.estado, "4 unidades con mínimo 10: stock bajo"),
                () -> assertTrue(alerta.esReposicion(), "es una alerta de reposición"),
                () -> assertTrue(alerta.sugerido() > 0, "sugiere una cantidad a pedir"),
                () -> assertEquals(ControladorCompras.PENDIENTE, estadoCreada, "la orden se crea pendiente"),
                () -> assertEquals(ControladorCompras.APROBADA, estadoOrden(orden), "el administrador la aprueba"),
                () -> assertEquals(alerta.sugerido(), alertaTrasOrden.enPedido, "las unidades aprobadas cuentan como en pedido"));
    }

    @Test
    @DisplayName("CP-15 Reporte de mermas: se generan PDF y Excel no vacíos y sus totales coinciden con la base de datos")
    void cp15_reporteDeMermas(@TempDir Path carpeta) throws Exception {
        iniciarSesion("Personal1", "890");
        int producto = crearProducto("CP15", "7.90", 2, null);
        recibirLote(producto, "CP15-L1", LocalDate.now().plusMonths(3), 20);
        int lote = loteId("CP15-L1");
        mermas.registrarMerma(lote, 4, "Dañado", "CP-15 aprobada");
        int m1 = ultimaMerma();
        mermas.registrarMerma(lote, 2, "Otro", "CP-15 rechazada");
        int m2 = ultimaMerma();
        mermas.registrarMerma(lote, 1, "Deteriorado", "CP-15 pendiente");
        Sesion.cerrar();
        iniciarSesion("admin", "123");
        mermas.aprobarMerma(m1);
        mermas.rechazarMerma(m2, "CP-15");

        LocalDate hoy = LocalDate.now();
        List<MermaVO> lista = mermas.listarMermas(hoy, hoy);
        ExportadorReporte.Reporte r = armarReporteComoLaPantalla(hoy, hoy, lista);
        File pdf = carpeta.resolve("mermas.pdf").toFile(), xlsx = carpeta.resolve("mermas.xlsx").toFile();
        ExportadorReporte.pdf(r, pdf);
        ExportadorReporte.excel(r, xlsx);

        // Totales según la base: solo las mermas aprobadas del período cuentan como pérdida
        String periodo = "solicitado_en >= CURDATE() AND solicitado_en < CURDATE() + INTERVAL 1 DAY";
        BigDecimal valorBD = decimal("SELECT COALESCE(SUM(cantidad * precio_unitario), 0) FROM mermas WHERE estado = 'APROBADA' AND " + periodo);
        long unidadesBD = entero("SELECT COALESCE(SUM(cantidad), 0) FROM mermas WHERE estado = 'APROBADA' AND " + periodo);
        long registradasBD = entero("SELECT COUNT(*) FROM mermas WHERE " + periodo);

        Map<String, Double> excel = indicadoresExcel(xlsx);
        String textoPdf = textoPdf(pdf);
        String valorTexto = String.format("S/ %.2f", valorBD.doubleValue());

        assertAll(
                () -> assertEquals(new BigDecimal("31.60"), valorBD, "referencia: 4 x S/ 7.90"),
                () -> assertTrue(pdf.length() > 0, "el PDF no está vacío"),
                () -> assertTrue(xlsx.length() > 0, "el Excel no está vacío"),
                () -> assertEquals(valorBD.doubleValue(), excel.get("Valor perdido"), 0.001, "valor perdido en Excel"),
                () -> assertEquals(unidadesBD, excel.get("Unidades dadas de baja"), 0.001, "unidades en Excel"),
                () -> assertEquals(registradasBD, excel.get("Mermas registradas"), 0.001, "mermas registradas en Excel"),
                () -> assertTrue(textoPdf.contains(valorTexto), "el PDF muestra el valor perdido " + valorTexto),
                () -> assertTrue(textoPdf.contains("Autorizado por"), "el PDF termina con las líneas de firma"));
    }

    // ---------------------------------------------------------------- Ayudas

    private static int ultimaMerma() throws SQLException {
        return (int) entero("SELECT MAX(id) FROM mermas");
    }

    private static String estadoMerma(int id) throws SQLException {
        return texto("SELECT estado FROM mermas WHERE id = ?", id);
    }

    private static String estadoOrden(int id) throws SQLException {
        return texto("SELECT estado FROM ordenes_compra WHERE id = ?", id);
    }

    private static AlertaVO alertaDe(ControladorCompras compras, int productoId) throws SQLException {
        return compras.listarAlertas().stream().filter(a -> a.productoId == productoId).findFirst()
                .orElseThrow(() -> new AssertionError("El producto " + productoId + " no aparece en las alertas"));
    }

    /** Arma el reporte con el mismo método que usa el botón Exportar de la pantalla Mermas. */
    private static ExportadorReporte.Reporte armarReporteComoLaPantalla(LocalDate desde, LocalDate hasta, List<MermaVO> lista) throws Exception {
        Method m = PanelMermas.class.getDeclaredMethod("armarReporte", LocalDate.class, LocalDate.class, List.class);
        m.setAccessible(true);
        return (ExportadorReporte.Reporte) m.invoke(null, desde, hasta, lista);
    }

    /** Lee la tabla de indicadores (nombre en la columna A y valor numérico en la B) de la hoja de resumen. */
    private static Map<String, Double> indicadoresExcel(File xlsx) throws Exception {
        Map<String, Double> valores = new HashMap<>();
        try (FileInputStream in = new FileInputStream(xlsx); Workbook wb = new XSSFWorkbook(in)) {
            Sheet resumen = wb.getSheetAt(0);
            for (Row row : resumen) {
                if (row.getCell(0) == null || row.getCell(1) == null) continue;
                try {
                    valores.put(row.getCell(0).getStringCellValue(), row.getCell(1).getNumericCellValue());
                } catch (IllegalStateException noNumerica) {
                    // Filas de título o encabezado
                }
            }
        }
        return valores;
    }

    private static String textoPdf(File pdf) throws Exception {
        // Desde los bytes y no desde la ruta: con la ruta, OpenPDF mapea el archivo en memoria y en Windows no se puede
        // borrar hasta que el recolector libere el mapeo, aunque se cierre el lector. Por eso JUnit a veces no podía borrar
        // la carpeta temporal de CP-15 (corridas 6, 13, 14 y 15)
        PdfReader reader = new PdfReader(java.nio.file.Files.readAllBytes(pdf.toPath()));
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) sb.append(extractor.getTextFromPage(i)).append('\n');
            return sb.toString();
        } finally {
            reader.close();
        }
    }
}
