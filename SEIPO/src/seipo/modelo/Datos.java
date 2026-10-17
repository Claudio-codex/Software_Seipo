package seipo.modelo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Datos {
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yy");
    public static final int STOCK_BAJO = 10;
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