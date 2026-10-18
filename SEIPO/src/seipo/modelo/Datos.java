package seipo.modelo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Datos {
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yy");
    /** Formato para ingresar y mostrar fechas de vencimiento. */
    public static final DateTimeFormatter FECHA_COMPLETA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final int STOCK_BAJO = 10;
    /** Días antes del vencimiento en que un lote se marca como "Por vencer". */
    public static final int DIAS_POR_VENCER = 30;
    public static final double IGV = 0.18;

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
}