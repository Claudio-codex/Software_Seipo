package seipo.modelo;

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
    /** Billeteras digitales: el único cobro que pasa por la pasarela de pago. */
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

    /** IGV de una venta: 18 % de (subtotal - descuento), en céntimos. */
    public static double igvVenta(double subtotal, double descuento) {
        return redondear((subtotal - descuento) * IGV);
    }

    /**
     * Total a cobrar: (subtotal - descuento) más IGV, redondeado hacia abajo a 10 céntimos, porque no circulan
     * monedas de 1 ni de 5 céntimos (el redondeo favorece al cliente). PanelVentas y ControladorVentas lo usan.
     */
    public static double totalVenta(double subtotal, double descuento) {
        double exacto = redondear(subtotal - descuento + igvVenta(subtotal, descuento));
        return Math.floor(exacto * 10 + 1e-6) / 10;
    }

    /** Lo que se rebajó al redondear el total: 0 o un monto negativo de hasta 9 céntimos. */
    public static double redondeoVenta(double subtotal, double descuento) {
        return redondear(totalVenta(subtotal, descuento) - (subtotal - descuento) - igvVenta(subtotal, descuento));
    }

    /** Indica si un monto en efectivo se puede pagar con monedas de 10 céntimos en adelante. */
    public static boolean esMontoEnMonedas(double v) {
        return Math.abs(v * 10 - Math.round(v * 10)) < 1e-6;
    }
}