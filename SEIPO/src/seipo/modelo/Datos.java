package seipo.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public class Datos {
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yy");
    /** Formato para ingresar y mostrar fechas de vencimiento. Estricto: rechaza fechas que no existen, como 31/02. */
    public static final DateTimeFormatter FECHA_COMPLETA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    /** Stock mínimo que se propone al registrar un producto (cada producto tiene el suyo). */
    public static final int STOCK_MINIMO_DEFECTO = 10;
    /** Cada cuántos minutos se revisan las alertas de stock con la aplicación abierta. */
    public static final int MINUTOS_REVISION_ALERTAS = 5;
    /** Días antes del vencimiento en que un lote se marca como "Por vencer". */
    public static final int DIAS_POR_VENCER = 30;
    public static final double IGV = 0.18;
    /** Descuento manual máximo (%) que puede aplicar el personal; el administrador no tiene tope y el VIP no cuenta. */
    public static final double DESCUENTO_MAX_PERSONAL = 10;
    public static final String EFECTIVO = "Efectivo";
    /** Billeteras digitales: solo se registra la forma de pago; el cajero confirma el pago en el celular del negocio. */
    public static final String YAPE_PLIN = "Yape o Plin";
    public static final String[] FORMAS_PAGO = {EFECTIVO, YAPE_PLIN};
    /** Fecha y hora para el historial de ventas y turnos. */
    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    public static class ProductoVenta {
        public int id;
        public String codigo, nombre, categoria;
        public double precio;
        public int stock;

        public ProductoVenta(int id, String codigo, String nombre, String categoria, double precio, int stock) {
            this.id = id;
            this.codigo = codigo;
            this.nombre = nombre;
            this.categoria = categoria;
            this.precio = precio;
            this.stock = stock;
        }

        @Override
        public String toString() {
            return codigo + " - " + nombre + " (Stock: " + stock + ")";
        }
    }

    public static String soles(double v) {
        return String.format("S/ %.2f", v);
    }

    /** Redondea a céntimos, como se guarda en la base de datos. */
    public static double redondear(double v) {
        return Math.round(v * 100) / 100.0;
    }

    /** Valor de venta (base gravada): subtotal - descuento, en céntimos. */
    private static BigDecimal base(double subtotal, double descuento) {
        return BigDecimal.valueOf(subtotal).subtract(BigDecimal.valueOf(descuento)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal igv(BigDecimal base) {
        return base.multiply(BigDecimal.valueOf(IGV)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * IGV de una venta: 18 % de la base total (subtotal - descuento), una sola vez y no línea por línea, con 2
     * decimales redondeados mitad hacia arriba. Se calcula con BigDecimal para que 0.495 dé 0.50 y no 0.49.
     */
    public static double igvVenta(double subtotal, double descuento) {
        return igv(base(subtotal, descuento)).doubleValue();
    }

    /**
     * Total a cobrar: base más IGV, con 2 decimales. No se redondea a 10 céntimos porque el negocio debe declarar
     * exactamente el IGV cobrado. PanelVentas y ControladorVentas lo usan.
     */
    public static double totalVenta(double subtotal, double descuento) {
        BigDecimal base = base(subtotal, descuento);
        return base.add(igv(base)).doubleValue();
    }
}