package seipo.vista;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import seipo.modelo.Datos;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Exporta el reporte de ventas a PDF (OpenPDF) y a Excel .xlsx (Apache POI). */
public class ExportadorReporte {

    /** Azul institucional de los documentos impresos (la pantalla usa el tema oscuro de UI). */
    static final Color AZUL = new Color(33, 84, 160);
    public static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static {
        // POI registra con log4j-api; sin esto avisa en consola que no encuentra un "logging provider"
        if (System.getProperty("log4j2.loggerContextFactory") == null) {
            System.setProperty("log4j2.loggerContextFactory", "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
        }
    }

    public enum Formato { TEXTO, ENTERO, DECIMAL, SOLES, PORCENTAJE }

    public static class Indicador {
        public final String nombre;
        public final double valor;
        public final Formato formato;

        public Indicador(String nombre, double valor, Formato formato) {
            this.nombre = nombre;
            this.valor = valor;
            this.formato = formato;
        }
    }

    /** Una tabla del reporte: columnas con su formato y filas en el orden en que se muestran. */
    public static class Seccion {
        public final String titulo;
        public final String[] columnas;
        public final Formato[] formatos;
        public final List<Object[]> filas = new ArrayList<>();
        public float[] anchos; // relativos; null = iguales

        public Seccion(String titulo, String[] columnas, Formato... formatos) {
            this.titulo = titulo;
            this.columnas = columnas;
            this.formatos = formatos;
        }
    }

    public static class Reporte {
        public String titulo, periodo;
        public LocalDateTime generado = LocalDateTime.now();
        public final List<Indicador> indicadores = new ArrayList<>();
        public final List<Seccion> secciones = new ArrayList<>();
    }

    /** Texto de una celda tal como se ve en pantalla. */
    static String texto(Object v, Formato f) {
        if (v == null) return "";
        if (!(v instanceof Number)) return v.toString();
        double d = ((Number) v).doubleValue();
        if (Double.isNaN(d)) return "—";
        switch (f) {
            case SOLES: return Datos.soles(d);
            case PORCENTAJE: return Graficos.porcentaje(d);
            case DECIMAL: return String.format("%.2f", d);
            case ENTERO: return String.format("%.0f", d);
            default: return v.toString();
        }
    }

    // ---------- PDF ----------

    public static void pdf(Reporte r, File archivo) throws IOException {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 40, 50);
        try (OutputStream out = new FileOutputStream(archivo)) {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PiePagina(r.titulo + " · Generado: " + r.generado.format(GENERADO)));
            doc.open();

            Color azul = AZUL;
            Paragraph titulo = new Paragraph(r.titulo, fuente(18, com.lowagie.text.Font.BOLD, azul));
            doc.add(titulo);
            doc.add(new Paragraph("Período: " + r.periodo, fuente(11, com.lowagie.text.Font.NORMAL, Color.DARK_GRAY)));
            Paragraph generado = new Paragraph("Fecha de generación: " + r.generado.format(GENERADO),
                    fuente(11, com.lowagie.text.Font.NORMAL, Color.DARK_GRAY));
            generado.setSpacingAfter(12);
            doc.add(generado);

            doc.add(tablaIndicadores(r.indicadores));
            for (Seccion s : r.secciones) doc.add(tablaPdf(s));
            doc.close();
        } catch (DocumentException ex) {
            throw new IOException("No se pudo generar el PDF: " + ex.getMessage(), ex);
        }
    }

    private static PdfPTable tablaIndicadores(List<Indicador> indicadores) {
        PdfPTable t = new PdfPTable(indicadores.size());
        t.setWidthPercentage(100);
        for (Indicador i : indicadores) {
            PdfPCell c = new PdfPCell();
            c.setBorderColor(Color.LIGHT_GRAY);
            c.setPadding(8);
            c.addElement(new Phrase(i.nombre, fuente(9, com.lowagie.text.Font.NORMAL, Color.DARK_GRAY)));
            c.addElement(new Phrase(texto(i.valor, i.formato), fuente(14, com.lowagie.text.Font.BOLD, AZUL)));
            t.addCell(c);
        }
        return t;
    }

    private static PdfPTable tablaPdf(Seccion s) throws DocumentException {
        PdfPTable t = new PdfPTable(s.columnas.length);
        // Las tablas de dos columnas no necesitan todo el ancho de la hoja
        t.setWidthPercentage(s.columnas.length <= 2 ? 45 : 100);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        if (s.anchos != null) t.setWidths(s.anchos);
        t.setSpacingBefore(14);
        // Título y encabezado son filas de cabecera: van juntos y se repiten si la tabla continúa en otra página
        t.setHeaderRows(2);
        // Una tabla corta no se parte entre páginas
        t.setKeepTogether(s.filas.size() <= 20);

        PdfPCell titulo = new PdfPCell(new Phrase(s.titulo, fuente(13, com.lowagie.text.Font.BOLD, AZUL)));
        titulo.setColspan(s.columnas.length);
        titulo.setBorder(Rectangle.NO_BORDER);
        titulo.setPaddingLeft(0);
        titulo.setPaddingBottom(6);
        t.addCell(titulo);

        com.lowagie.text.Font encabezado = fuente(10, com.lowagie.text.Font.BOLD, Color.WHITE);
        for (String col : s.columnas) {
            PdfPCell c = new PdfPCell(new Phrase(col, encabezado));
            c.setBackgroundColor(AZUL);
            c.setBorderColor(AZUL);
            c.setPadding(5);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(c);
        }

        com.lowagie.text.Font normal = fuente(9, com.lowagie.text.Font.NORMAL, Color.BLACK);
        Color alterno = new Color(241, 245, 251);
        for (int f = 0; f < s.filas.size(); f++) {
            Object[] fila = s.filas.get(f);
            for (int c = 0; c < s.columnas.length; c++) {
                PdfPCell celda = new PdfPCell(new Phrase(texto(fila[c], s.formatos[c]), normal));
                celda.setPadding(4);
                celda.setBorderColor(Color.LIGHT_GRAY);
                if (f % 2 == 1) celda.setBackgroundColor(alterno);
                celda.setHorizontalAlignment(fila[c] instanceof Number ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                t.addCell(celda);
            }
        }
        if (s.filas.isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase("Sin datos en el período seleccionado", normal));
            vacio.setColspan(s.columnas.length);
            vacio.setPadding(6);
            vacio.setBorderColor(Color.LIGHT_GRAY);
            t.addCell(vacio);
        }
        return t;
    }

    private static com.lowagie.text.Font fuente(float tamano, int estilo, Color color) {
        return new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, tamano, estilo, color);
    }

    /** Pie de página con "Página X de Y"; el total se escribe al cerrar el documento. */
    private static class PiePagina extends PdfPageEventHelper {
        private final String leyenda;
        private PdfTemplate total;
        private BaseFont fuente;

        PiePagina(String leyenda) { this.leyenda = leyenda; }

        @Override public void onOpenDocument(PdfWriter writer, Document doc) {
            total = writer.getDirectContent().createTemplate(30, 12);
            try {
                fuente = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (DocumentException | IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override public void onEndPage(PdfWriter writer, Document doc) {
            PdfContentByte cb = writer.getDirectContent();
            float y = doc.bottom() - 24;
            cb.setColorStroke(Color.LIGHT_GRAY);
            cb.moveTo(doc.left(), y + 14);
            cb.lineTo(doc.right(), y + 14);
            cb.stroke();

            String pagina = "Página " + writer.getPageNumber() + " de ";
            float anchoPagina = fuente.getWidthPoint(pagina, 9);
            float anchoTotal = fuente.getWidthPoint("99", 9);
            float x = doc.right() - anchoPagina - anchoTotal;
            cb.beginText();
            cb.setColorFill(Color.DARK_GRAY);
            cb.setFontAndSize(fuente, 9);
            cb.setTextMatrix(doc.left(), y);
            cb.showText(leyenda);
            cb.setTextMatrix(x, y);
            cb.showText(pagina);
            cb.endText();
            cb.addTemplate(total, x + anchoPagina, y);
        }

        @Override public void onCloseDocument(PdfWriter writer, Document doc) {
            total.beginText();
            total.setColorFill(Color.DARK_GRAY);
            total.setFontAndSize(fuente, 9);
            total.setTextMatrix(0, 0);
            total.showText(String.valueOf(writer.getPageNumber() - 1));
            total.endText();
        }
    }

    // ---------- Excel ----------

    public static void excel(Reporte r, File archivo) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Font negrita = wb.createFont();
            negrita.setBold(true);
            org.apache.poi.ss.usermodel.Font tituloFuente = wb.createFont();
            tituloFuente.setBold(true);
            tituloFuente.setFontHeightInPoints((short) 14);

            CellStyle encabezado = wb.createCellStyle();
            encabezado.setFont(negrita);
            encabezado.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            encabezado.setBorderBottom(BorderStyle.THIN);
            CellStyle titulo = wb.createCellStyle();
            titulo.setFont(tituloFuente);
            CellStyle etiqueta = wb.createCellStyle();
            etiqueta.setFont(negrita);

            Map<Formato, CellStyle> estilos = new EnumMap<>(Formato.class);
            String[][] formatos = {{"SOLES", "\"S/\" #,##0.00"}, {"PORCENTAJE", "0.0%"}, {"DECIMAL", "0.00"}, {"ENTERO", "0"}};
            for (String[] f : formatos) {
                CellStyle st = wb.createCellStyle();
                st.setDataFormat(wb.createDataFormat().getFormat(f[1]));
                estilos.put(Formato.valueOf(f[0]), st);
            }

            // Hoja de resumen: título, período, fecha de generación e indicadores
            Sheet resumen = wb.createSheet("Resumen");
            celda(resumen.createRow(0), 0, r.titulo).setCellStyle(titulo);
            Row fPeriodo = resumen.createRow(1);
            celda(fPeriodo, 0, "Período").setCellStyle(etiqueta);
            celda(fPeriodo, 1, r.periodo);
            Row fGenerado = resumen.createRow(2);
            celda(fGenerado, 0, "Fecha de generación").setCellStyle(etiqueta);
            celda(fGenerado, 1, r.generado.format(GENERADO));
            Row cab = resumen.createRow(4);
            celda(cab, 0, "Indicador").setCellStyle(encabezado);
            celda(cab, 1, "Valor").setCellStyle(encabezado);
            int fila = 5;
            for (Indicador i : r.indicadores) {
                Row row = resumen.createRow(fila++);
                celda(row, 0, i.nombre);
                Cell c = row.createCell(1);
                c.setCellValue(i.valor);
                c.setCellStyle(estilos.get(i.formato));
            }
            ajustar(resumen, 2);

            // Una hoja por tabla
            for (Seccion s : r.secciones) {
                Sheet hoja = wb.createSheet(WorkbookUtil.createSafeSheetName(s.titulo));
                Row encab = hoja.createRow(0);
                for (int c = 0; c < s.columnas.length; c++) celda(encab, c, s.columnas[c]).setCellStyle(encabezado);
                for (int f = 0; f < s.filas.size(); f++) {
                    Row row = hoja.createRow(f + 1);
                    Object[] datos = s.filas.get(f);
                    for (int c = 0; c < s.columnas.length; c++) {
                        Object v = datos[c];
                        if (v instanceof Number) {
                            double d = ((Number) v).doubleValue();
                            if (Double.isNaN(d)) continue; // sin dato: celda vacía
                            Cell cell = row.createCell(c);
                            cell.setCellValue(d);
                            CellStyle st = estilos.get(s.formatos[c]);
                            if (st != null) cell.setCellStyle(st);
                        } else if (v != null) {
                            celda(row, c, v.toString());
                        }
                    }
                }
                hoja.createFreezePane(0, 1);
                if (!s.filas.isEmpty()) {
                    hoja.setAutoFilter(new CellRangeAddress(0, s.filas.size(), 0, s.columnas.length - 1));
                }
                ajustar(hoja, s.columnas.length);
            }

            try (OutputStream out = new FileOutputStream(archivo)) {
                wb.write(out);
            }
        }
    }

    private static Cell celda(Row row, int col, String valor) {
        Cell c = row.createCell(col);
        c.setCellValue(valor);
        return c;
    }

    /** Ajusta el ancho al contenido, con margen para la flecha del filtro. */
    private static void ajustar(Sheet hoja, int columnas) {
        for (int c = 0; c < columnas; c++) {
            hoja.autoSizeColumn(c);
            hoja.setColumnWidth(c, Math.min(255 * 256, hoja.getColumnWidth(c) + 3 * 256));
        }
    }
}
