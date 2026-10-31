package seipo.pruebas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seipo.controlador.ControladorCaja;
import seipo.modelo.Datos;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Orden de salida de los lotes (FEFO, proceso 08). CP-07 ya prueba que sale primero el lote que vence antes; estos
 * casos prueban el desempate por orden de ingreso, los lotes vencidos, los productos sin vencimiento y el registro de
 * una venta que consume varios lotes.
 */
class FefoTest extends PruebaBase {

    @BeforeEach
    void abrirTurno() throws SQLException {
        iniciarSesion("Personal1", "890");
        new ControladorCaja().abrirTurno(100.00);
    }

    /** Simula que el lote venció mientras estaba en el almacén (al recibirlo aún estaba vigente). */
    private static void vencerLote(String numeroLote, LocalDate vencimiento) throws SQLException {
        ejecutar("UPDATE lotes SET fecha_vencimiento = ? WHERE numero_lote = ?", java.sql.Date.valueOf(vencimiento), numeroLote);
    }

    private static List<String> lotesDeLaVenta(int ventaId) throws SQLException {
        return listaTextos("SELECT CONCAT(l.numero_lote, ':', vl.cantidad) FROM venta_lotes vl JOIN lotes l ON l.id = vl.lote_id " +
                "WHERE vl.venta_id = ? ORDER BY vl.id", ventaId);
    }

    @Test
    @DisplayName("CP-20 FEFO con el mismo vencimiento: sale primero el lote que entró antes, aunque su número sea mayor")
    void cp20_mismoVencimientoPorIngreso() throws SQLException {
        int producto = crearProducto("CP20", "5.00", 2, null);
        LocalDate vence = LocalDate.now().plusDays(60);
        // Entra primero "Z" y después "A": el número de lote no debe decidir el orden
        recibirLote(producto, "CP20-Z", vence, 4);
        recibirLote(producto, "CP20-A", vence, 4);

        int ventaId = vender(producto, 3, 0, Datos.EFECTIVO, null, 20.0);
        int segunda = vender(producto, 3, 0, Datos.EFECTIVO, null, 20.0);

        assertAll(
                () -> assertEquals(List.of("CP20-Z:3"), lotesDeLaVenta(ventaId), "la primera venta sale del lote que entró antes"),
                () -> assertEquals(List.of("CP20-Z:1", "CP20-A:2"), lotesDeLaVenta(segunda), "termina el primero y sigue con el segundo"),
                () -> assertEquals(0, cantidadLote("CP20-Z")),
                () -> assertEquals(2, cantidadLote("CP20-A")),
                () -> assertEquals(2, stockProducto(producto)));
    }

    @Test
    @DisplayName("CP-21 Lotes vencidos: no se venden; el que vence hoy sí (valor límite) y la venta que necesita el vencido se rechaza sin cambios")
    void cp21_lotesVencidosNoSeVenden() throws SQLException {
        int producto = crearProducto("CP21", "5.00", 2, null);
        recibirLote(producto, "CP21-VENCIDO", LocalDate.now().plusDays(30), 5);
        recibirLote(producto, "CP21-HOY", LocalDate.now().plusDays(30), 2);
        recibirLote(producto, "CP21-VIGENTE", LocalDate.now().plusDays(90), 3);
        vencerLote("CP21-VENCIDO", LocalDate.now().minusDays(1));
        vencerLote("CP21-HOY", LocalDate.now());

        int disponible = producto(producto).stock;
        int ventaId = vender(producto, 4, 0, Datos.EFECTIVO, null, 30.0);

        String antes = huellaVentas();
        SQLException rechazo = assertThrows(SQLException.class, () -> vender(producto, 2, 0, Datos.EFECTIVO, null, 20.0),
                "solo queda 1 unidad vigente; las 5 vencidas no cuentan");

        assertAll(
                () -> assertEquals(5, disponible, "el stock que se ofrece en Ventas no incluye el lote vencido (2 + 3)"),
                () -> assertEquals(List.of("CP21-HOY:2", "CP21-VIGENTE:2"), lotesDeLaVenta(ventaId), "el que vence hoy sale primero; el vencido se salta"),
                () -> assertEquals(5, cantidadLote("CP21-VENCIDO"), "el lote vencido queda intacto"),
                () -> assertEquals(antes, huellaVentas(), "la venta rechazada no deja registros"),
                () -> assertTrue(rechazo.getMessage().contains("vencidos"), "mensaje claro: " + rechazo.getMessage()),
                () -> assertEquals(6, stockProducto(producto), "el stock total sigue incluyendo el vencido (5 + 0 + 1)"));
    }

    @Test
    @DisplayName("CP-22 Productos sin vencimiento: sus lotes salen por orden de ingreso")
    void cp22_sinVencimientoPorIngreso() throws SQLException {
        int producto = crearProducto("CP22", "15.00", 2, null);
        recibirLote(producto, "CP22-SEGUNDO", null, 3);
        recibirLote(producto, "CP22-PRIMERO", null, 3);
        recibirLote(producto, "CP22-TERCERO", null, 3);

        int ventaId = vender(producto, 5, 0, Datos.EFECTIVO, null, 100.0);

        assertAll(
                () -> assertEquals(List.of("CP22-SEGUNDO:3", "CP22-PRIMERO:2"), lotesDeLaVenta(ventaId),
                        "sale del que entró primero y sigue con el siguiente; el nombre no importa"),
                () -> assertEquals(0, cantidadLote("CP22-SEGUNDO")),
                () -> assertEquals(1, cantidadLote("CP22-PRIMERO")),
                () -> assertEquals(3, cantidadLote("CP22-TERCERO"), "el último en entrar queda intacto"),
                () -> assertEquals(4, stockProducto(producto)));
    }

    @Test
    @DisplayName("CP-23 Venta que consume varios lotes: los recorre por vencimiento y registra de qué lote salió cada unidad (venta_lotes y movimientos)")
    void cp23_ventaConVariosLotes() throws SQLException {
        int producto = crearProducto("CP23", "4.00", 2, null);
        // Ingreso desordenado: el que vence último entra primero
        recibirLote(producto, "CP23-C", LocalDate.now().plusDays(90), 5);
        recibirLote(producto, "CP23-A", LocalDate.now().plusDays(10), 2);
        recibirLote(producto, "CP23-SIN", null, 4);
        recibirLote(producto, "CP23-B", LocalDate.now().plusDays(40), 2);
        // Stock 13: A (2) -> B (2) -> C (5) -> sin vencimiento (4)

        int ventaId = vender(producto, 10, 0, Datos.EFECTIVO, null, 50.0);
        String nombre = "Producto de prueba CP23";

        assertAll(
                () -> assertEquals(List.of("CP23-A:2", "CP23-B:2", "CP23-C:5", "CP23-SIN:1"), lotesDeLaVenta(ventaId),
                        "venta_lotes: un registro por lote, en orden de vencimiento y el lote sin vencimiento al final"),
                () -> assertEquals(List.of("CP23-A:2:11", "CP23-B:2:9", "CP23-C:5:4", "CP23-SIN:1:3"),
                        listaTextos("SELECT CONCAT(lote, ':', cantidad, ':', stock_resultante) FROM movimientos " +
                                "WHERE producto_nombre = ? AND tipo = 'Salida' ORDER BY id", nombre),
                        "movimientos: una salida por lote, con su cantidad y el stock que queda"),
                () -> assertEquals(10, entero("SELECT SUM(cantidad) FROM venta_lotes WHERE venta_id = ?", ventaId), "suma = unidades vendidas"),
                () -> assertEquals(3, cantidadLote("CP23-SIN")),
                () -> assertEquals(3, stockProducto(producto)),
                () -> assertEquals(entero("SELECT SUM(cantidad) FROM lotes WHERE producto_id = ?", producto), stockProducto(producto),
                        "el stock del producto sigue igual a la suma de sus lotes"));
    }
}
