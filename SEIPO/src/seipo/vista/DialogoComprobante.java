package seipo.vista;

import seipo.controlador.ControladorComprobantes;
import seipo.controlador.ControladorComprobantes.ComprobanteVO;
import seipo.controlador.ControladorVentas.DetalleVO;
import seipo.controlador.ControladorVentas.VentaVO;
import seipo.modelo.Datos;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Comprobante digital de una venta: vista previa, impresión y descarga en PDF. */
class DialogoComprobante extends JDialog {

    private final ComprobanteVO c;

    /**
     * Abre el comprobante de una venta.
     *
     * @param recienEmitido true justo después de cobrar: muestra arriba el aviso de la venta y el vuelto
     */
    static void mostrar(Component padre, int ventaId, boolean recienEmitido) {
        ComprobanteVO c;
        try {
            c = new ControladorComprobantes().obtener(ventaId);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(padre, "No se pudo cargar el comprobante:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (c == null) {
            JOptionPane.showMessageDialog(padre, "La venta N° " + ventaId + " se registró antes de los comprobantes y no tiene uno.");
            return;
        }
        new DialogoComprobante(SwingUtilities.getWindowAncestor(padre), c, recienEmitido).setVisible(true);
    }

    private DialogoComprobante(Window duenio, ComprobanteVO c, boolean recienEmitido) {
        super(duenio, c.titulo().charAt(0) + c.titulo().substring(1).toLowerCase() + " " + c.numeroCompleto(),
                ModalityType.APPLICATION_MODAL);
        this.c = c;
        JPanel p = UI.base();

        if (recienEmitido) {
            JPanel aviso = new JPanel(new BorderLayout(12, 0));
            aviso.setBackground(UI.FILA_VERDE);
            aviso.setBorder(new CompoundBorder(new LineBorder(UI.BORDE, 1, true), new EmptyBorder(10, 14, 10, 14)));
            JLabel registrada = new JLabel("<html><b>Venta N° " + c.venta.id + " registrada</b><br>"
                    + "<span style='color:#94A3B8'>" + c.numeroCompleto() + " · " + c.venta.formaPago + "</span></html>");
            aviso.add(registrada, BorderLayout.CENTER);
            Double vuelto = c.venta.vuelto();
            if (vuelto != null) {
                JLabel lblVuelto = new JLabel("Vuelto " + Datos.soles(vuelto));
                lblVuelto.setFont(UI.fuente(Font.BOLD, 22));
                lblVuelto.setForeground(UI.EXITO);
                aviso.add(lblVuelto, BorderLayout.EAST);
            }
            p.add(aviso, BorderLayout.NORTH);
        }

        JEditorPane vista = new JEditorPane("text/html", html(c, true));
        vista.setEditable(false);
        vista.setBackground(UI.SUPERFICIE);
        vista.setBorder(new EmptyBorder(14, 18, 14, 18));
        vista.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(vista);
        scroll.setPreferredSize(new Dimension(470, 520));
        p.add(scroll, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.setOpaque(false);
        JButton imprimir = UI.botonSecundario("Imprimir"), pdf = UI.botonSecundario("Guardar PDF"), cerrar = UI.boton("Cerrar");
        imprimir.addActionListener(e -> imprimir());
        pdf.addActionListener(e -> guardarPdf());
        cerrar.addActionListener(e -> dispose());
        botones.add(imprimir);
        botones.add(pdf);
        botones.add(cerrar);
        p.add(botones, BorderLayout.SOUTH);

        setContentPane(p);
        getRootPane().setDefaultButton(cerrar);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(duenio);
        SwingUtilities.invokeLater(cerrar::requestFocusInWindow);
    }

    /** Imprime la versión clara del comprobante (papel blanco) con el diálogo de impresión del sistema. */
    private void imprimir() {
        JEditorPane papel = new JEditorPane("text/html", html(c, false));
        papel.setSize(new Dimension(440, Short.MAX_VALUE));
        papel.setSize(new Dimension(440, papel.getPreferredSize().height));
        try {
            papel.print();
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo imprimir:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarPdf() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar comprobante en PDF");
        fc.setFileFilter(new FileNameExtensionFilter("Documento PDF", "pdf"));
        fc.setSelectedFile(new File(c.numeroCompleto() + ".pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File archivo = fc.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".pdf")) archivo = new File(archivo.getParentFile(), archivo.getName() + ".pdf");
        if (archivo.exists() && JOptionPane.showConfirmDialog(this, archivo.getName() + " ya existe. ¿Reemplazarlo?",
                "Guardar PDF", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            ExportadorComprobante.pdf(c, archivo);
            JOptionPane.showMessageDialog(this, "Comprobante guardado en:\n" + archivo.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo guardar", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** HTML del comprobante: oscuro para la pantalla o claro para imprimir. */
    static String html(ComprobanteVO c, boolean oscuro) {
        String texto = oscuro ? "#E2E8F0" : "#000000", tenue = oscuro ? "#94A3B8" : "#555555", fondo = oscuro ? "#1E293B" : "#FFFFFF";
        VentaVO v = c.venta;
        StringBuilder sb = new StringBuilder("<html><body style='font-family:SansSerif; font-size:" + (oscuro ? "12pt" : "10pt") + "; color:" + texto
                + "; background-color:" + fondo + "'>");
        sb.append("<div align='center'><font size='5'><b>").append(esc(c.emisorNombre)).append("</b></font><br>")
                .append("RUC ").append(c.emisorRuc).append("<br><font color='").append(tenue).append("'>")
                .append(esc(c.emisorDireccion)).append("</font><br><br>")
                .append("<b>").append(c.titulo()).append("</b><br><font size='5'><b>").append(c.numeroCompleto())
                .append("</b></font></div><hr>");

        sb.append("<table width='100%' cellpadding='1'>");
        fila(sb, "Fecha de emisión", c.emitido.format(ExportadorReporte.GENERADO), tenue);
        fila(sb, "Cajero", esc(v.vendedor), tenue);
        if (c.clienteNombre == null) {
            fila(sb, "Cliente", "Clientes varios", tenue);
        } else {
            fila(sb, ControladorComprobantes.FACTURA.equals(c.tipo) ? "Razón social" : "Cliente", esc(c.clienteNombre), tenue);
            fila(sb, ControladorComprobantes.FACTURA.equals(c.tipo) ? "RUC" : "DNI", c.clienteDocumento, tenue);
        }
        sb.append("</table><hr><table width='100%' cellpadding='2'><tr><td><font color='").append(tenue)
                .append("'>Cant.</font></td><td><font color='").append(tenue).append("'>Descripción</font></td><td align='right'><font color='")
                .append(tenue).append("'>P. unit.</font></td><td align='right'><font color='").append(tenue).append("'>Importe</font></td></tr>");
        for (DetalleVO d : c.detalle) {
            sb.append("<tr><td>").append(d.cantidad).append("</td><td>").append(esc(d.producto)).append("</td><td align='right'>")
                    .append(String.format("%.2f", d.precio)).append("</td><td align='right'>").append(String.format("%.2f", d.importe))
                    .append("</td></tr>");
        }
        sb.append("</table><hr><table width='100%' cellpadding='1'>");
        importe(sb, "Subtotal", Datos.soles(v.subtotal));
        if (v.descuento > 0) importe(sb, "Descuento", "− " + Datos.soles(v.descuento));
        importe(sb, "Op. gravada", Datos.soles(v.subtotal - v.descuento));
        importe(sb, "IGV 18%", Datos.soles(v.igv()));
        if (v.redondeo() < 0) importe(sb, "Redondeo", "− " + Datos.soles(-v.redondeo()));
        sb.append("<tr><td><font size='4'><b>TOTAL</b></font></td><td align='right'><font size='4'><b>")
                .append(Datos.soles(v.total)).append("</b></font></td></tr></table>");
        sb.append("<p><font color='").append(tenue).append("'>").append(ControladorComprobantes.enLetras(v.total)).append("</font></p>");

        sb.append("<table width='100%' cellpadding='1'>");
        fila(sb, "Forma de pago", v.formaPago, tenue);
        if (v.recibido != null) {
            fila(sb, "Recibido", Datos.soles(v.recibido), tenue);
            fila(sb, "Vuelto", Datos.soles(v.vuelto()), tenue);
        }
        if (c.pago != null) {
            fila(sb, "Medio", esc(c.pago.medio), tenue);
            fila(sb, "Autorización", c.pago.codigoAutorizacion, tenue);
            fila(sb, "Token de pago", c.pago.token, tenue);
        }
        sb.append("</table>");

        if (v.anulada()) {
            sb.append("<p align='center'><font size='5' color='#DC2626'><b>ANULADO</b></font><br><font color='").append(tenue)
                    .append("'>Por ").append(esc(v.anuladoPor)).append(": ").append(esc(v.motivoAnulacion)).append("</font></p>");
        }
        sb.append("<hr><div align='center'><font color='").append(tenue).append("'>Código de verificación: <b>")
                .append(c.codigoVerificacion).append("</b><br>Representación impresa de la ").append(c.titulo().toLowerCase())
                .append(".<br>Documento de prueba, sin valor tributario.<br>¡Gracias por su compra!</font></div>");
        return sb.append("</body></html>").toString();
    }

    private static void fila(StringBuilder sb, String concepto, String valor, String tenue) {
        sb.append("<tr><td><font color='").append(tenue).append("'>").append(concepto).append("</font></td><td align='right'>")
                .append(valor == null ? "—" : valor).append("</td></tr>");
    }

    private static void importe(StringBuilder sb, String concepto, String valor) {
        sb.append("<tr><td>").append(concepto).append("</td><td align='right'>").append(valor).append("</td></tr>");
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
