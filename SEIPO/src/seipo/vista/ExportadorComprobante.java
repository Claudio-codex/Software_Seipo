package seipo.vista;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import seipo.controlador.ControladorComprobantes;
import seipo.controlador.ControladorComprobantes.ComprobanteVO;
import seipo.controlador.ControladorVentas.DetalleVO;
import seipo.controlador.ControladorVentas.VentaVO;
import seipo.modelo.Datos;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Comprobante en PDF con formato de ticket de 80 mm (OpenPDF), en colores claros para imprimir. */
public class ExportadorComprobante {

    private static final float ANCHO = 226; // 80 mm en puntos
    private static final float MARGEN = 12;
    private static final Color GRIS = new Color(85, 85, 85);

    public static void pdf(ComprobanteVO c, File archivo) throws IOException {
        try {
            // Todo el ticket va en una tabla de una columna para medir su alto: el PDF es una sola página continua
            PdfPTable doc = new PdfPTable(1);
            doc.setTotalWidth(ANCHO - 2 * MARGEN);
            doc.setLockedWidth(true);
            VentaVO v = c.venta;

            centrado(doc, c.emisorNombre, 11, true, Color.BLACK);
            centrado(doc, "RUC " + c.emisorRuc, 8, false, Color.BLACK);
            centrado(doc, c.emisorDireccion, 7, false, GRIS);
            espacio(doc, 5);
            centrado(doc, c.titulo(), 8, true, Color.BLACK);
            centrado(doc, c.numeroCompleto(), 11, true, Color.BLACK);
            linea(doc);

            PdfPTable datos = tabla(new float[]{1, 1.6f});
            par(datos, "Fecha de emisión", c.emitido.format(ExportadorReporte.GENERADO));
            par(datos, "Cajero", v.vendedor);
            boolean factura = ControladorComprobantes.FACTURA.equals(c.tipo);
            if (c.clienteNombre == null) {
                par(datos, "Cliente", "Clientes varios");
            } else {
                par(datos, factura ? "Razón social" : "Cliente", c.clienteNombre);
                par(datos, factura ? "RUC" : "DNI", c.clienteDocumento);
            }
            bloque(doc, datos);
            linea(doc);

            PdfPTable items = tabla(new float[]{0.5f, 2.4f, 0.9f, 1f});
            for (String col : new String[]{"Cant.", "Descripción", "P. unit.", "Importe"}) celda(items, col, 7, true, GRIS, col.startsWith("P") || col.startsWith("I"));
            for (DetalleVO d : c.detalle) {
                celda(items, String.valueOf(d.cantidad), 7, false, Color.BLACK, false);
                celda(items, d.producto, 7, false, Color.BLACK, false);
                celda(items, String.format("%.2f", d.precio), 7, false, Color.BLACK, true);
                celda(items, String.format("%.2f", d.importe), 7, false, Color.BLACK, true);
            }
            bloque(doc, items);
            linea(doc);

            PdfPTable totales = tabla(new float[]{1.4f, 1});
            importe(totales, "Subtotal", Datos.soles(v.subtotal), false);
            if (v.descuento > 0) importe(totales, "Descuento", "- " + Datos.soles(v.descuento), false);
            importe(totales, "Op. gravada", Datos.soles(v.subtotal - v.descuento), false);
            importe(totales, "IGV 18%", Datos.soles(v.igv()), false);
            if (v.redondeo() < 0) importe(totales, "Redondeo", "- " + Datos.soles(-v.redondeo()), false);
            importe(totales, "TOTAL", Datos.soles(v.total), true);
            bloque(doc, totales);
            espacio(doc, 4);
            bloque(doc, new Paragraph(ControladorComprobantes.enLetras(v.total), fuente(7, false, GRIS)));
            espacio(doc, 4);

            PdfPTable pago = tabla(new float[]{1, 1.6f});
            par(pago, "Forma de pago", v.formaPago);
            if (v.recibido != null) {
                par(pago, "Recibido", Datos.soles(v.recibido));
                par(pago, "Vuelto", Datos.soles(v.vuelto()));
            }
            bloque(doc, pago);

            if (v.anulada()) {
                espacio(doc, 5);
                centrado(doc, "ANULADO", 14, true, new Color(220, 38, 38));
                centrado(doc, "Por " + v.anuladoPor + ": " + v.motivoAnulacion, 7, false, GRIS);
            }
            linea(doc);
            centrado(doc, "Código de verificación: " + c.codigoVerificacion, 7, true, Color.BLACK);
            centrado(doc, "Representación impresa de la " + c.titulo().toLowerCase() + ".", 7, false, GRIS);
            centrado(doc, "Documento de prueba, sin valor tributario.", 7, false, GRIS);
            centrado(doc, "¡Gracias por su compra!", 8, false, Color.BLACK);

            Document pdf = new Document(new Rectangle(ANCHO, doc.getTotalHeight() + 2 * MARGEN + 2), MARGEN, MARGEN, MARGEN, MARGEN);
            try (OutputStream out = new FileOutputStream(archivo)) {
                PdfWriter.getInstance(pdf, out);
                pdf.open();
                pdf.add(doc);
                pdf.close();
            }
        } catch (DocumentException ex) {
            throw new IOException("No se pudo generar el PDF: " + ex.getMessage(), ex);
        }
    }

    private static void centrado(PdfPTable doc, String texto, float tamano, boolean negrita, Color color) {
        Paragraph p = new Paragraph(texto == null ? "" : texto, fuente(tamano, negrita, color));
        p.setAlignment(Element.ALIGN_CENTER);
        bloque(doc, p);
    }

    /** Agrega un párrafo o una tabla como una fila del ticket. */
    private static void bloque(PdfPTable doc, Element e) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(0);
        c.addElement(e);
        doc.addCell(c);
    }

    private static void espacio(PdfPTable doc, float alto) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setFixedHeight(alto);
        doc.addCell(c);
    }

    /** Línea divisoria: una celda vacía con solo el borde inferior. */
    private static void linea(PdfPTable doc) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(Color.LIGHT_GRAY);
        c.setFixedHeight(4);
        doc.addCell(c);
        espacio(doc, 5);
    }

    private static PdfPTable tabla(float[] anchos) throws DocumentException {
        PdfPTable t = new PdfPTable(anchos.length);
        t.setWidthPercentage(100);
        t.setWidths(anchos);
        return t;
    }

    private static void par(PdfPTable t, String concepto, String valor) {
        celda(t, concepto, 7, false, GRIS, false);
        celda(t, valor == null ? "-" : valor, 7, false, Color.BLACK, true);
    }

    private static void importe(PdfPTable t, String concepto, String valor, boolean destacado) {
        celda(t, concepto, destacado ? 10 : 8, destacado, Color.BLACK, false);
        celda(t, valor, destacado ? 10 : 8, destacado, Color.BLACK, true);
    }

    private static void celda(PdfPTable t, String texto, float tamano, boolean negrita, Color color, boolean derecha) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente(tamano, negrita, color)));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(1.5f);
        c.setHorizontalAlignment(derecha ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        t.addCell(c);
    }

    private static com.lowagie.text.Font fuente(float tamano, boolean negrita, Color color) {
        return new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, tamano,
                negrita ? com.lowagie.text.Font.BOLD : com.lowagie.text.Font.NORMAL, color);
    }
}
