package seipo.pruebas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seipo.controlador.ControladorCaja;
import seipo.controlador.ControladorCaja.TurnoVO;
import seipo.controlador.ControladorClientes;
import seipo.controlador.ControladorClientes.ClienteVO;
import seipo.controlador.ControladorVentas;
import seipo.controlador.ControladorVentas.VentaVO;
import seipo.modelo.Datos;
import seipo.modelo.Sesion;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Procesos 01, 03, 06-08 y 12: turno de caja, venta, descuento VIP, FEFO y cierre. */
class CajaYVentasTest extends PruebaBase {

    private final ControladorCaja caja = new ControladorCaja();

    @Test
    @DisplayName("CP-03 Apertura de turno: se abre con S/ 100.00 y se rechaza un segundo turno sin cerrar el primero")
    void cp03_aperturaDeTurno() throws SQLException {
        iniciarSesion("Personal1", "890");
        assertNull(caja.turnoAbierto(), "precondición: el usuario no tiene turno abierto");

        caja.abrirTurno(100.00);
        TurnoVO turno = caja.turnoAbierto();
        assertNotNull(turno, "el primer turno debe quedar registrado");
        assertEquals(ControladorCaja.ABIERTO, turno.estado);
        assertEquals(100.00, turno.montoInicial, 0.001);

        assertThrows(SQLException.class, () -> caja.abrirTurno(50.00), "el segundo turno debe rechazarse");
        assertEquals(1, entero("SELECT COUNT(*) FROM turnos WHERE usuario_id = ? AND estado = 'ABIERTO'", Sesion.getId()),
                "debe existir un solo turno abierto");
    }

    @Test
    @DisplayName("CP-04 Venta sin turno: se bloquea, no se registra nada y no se descuenta stock")
    void cp04_ventaSinTurno() throws SQLException {
        iniciarSesion("Personal1", "890");
        int producto = crearProducto("CP04", "10.00", 2, null);
        recibirLote(producto, "CP04-L1", LocalDate.now().plusMonths(6), 5);
        assertNull(caja.turnoAbierto(), "precondición: el usuario no tiene turno abierto");
        String antes = huellaVentas();

        SQLException e = assertThrows(SQLException.class,
                () -> vender(producto, 1, 0, Datos.EFECTIVO, null, 20.0));
        assertTrue(e.getMessage().toLowerCase().contains("turno"), "el mensaje debe indicar que falta el turno: " + e.getMessage());
        assertEquals(antes, huellaVentas(), "no debe quedar ninguna venta, detalle, comprobante, movimiento ni cambio de stock");
        assertEquals(5, stockProducto(producto));
    }

    @Test
    @DisplayName("CP-05 Venta en efectivo: total con IGV y redondeo a 10 céntimos, vuelto correcto y stock baja 1")
    void cp05_ventaEnEfectivo() throws SQLException {
        iniciarSesion("Personal1", "890");
        caja.abrirTurno(100.00);
        // Precio elegido para que el redondeo actúe: 12.35 + IGV 2.22 = 14.57 -> 14.50
        int producto = crearProducto("CP05", "12.35", 2, null);
        recibirLote(producto, "CP05-L1", LocalDate.now().plusMonths(6), 10);

        int ventaId = vender(producto, 1, 0, Datos.EFECTIVO, null, 20.00);
        VentaVO v = new ControladorVentas().obtenerVenta(ventaId);

        BigDecimal total = totalEsperado(new BigDecimal("12.35"), BigDecimal.ZERO);
        assertAll(
                () -> assertEquals(new BigDecimal("14.50"), total, "cálculo de referencia del documento"),
                () -> assertEquals(total.doubleValue(), v.total, 0.001, "total cobrado"),
                () -> assertEquals(2.22, v.igv(), 0.001, "IGV 18 %"),
                () -> assertEquals(-0.07, v.redondeo(), 0.001, "redondeo hacia abajo a 10 céntimos"),
                () -> assertEquals(20.00 - total.doubleValue(), v.vuelto(), 0.001, "vuelto = pagado - total"),
                () -> assertEquals(9, stockProducto(producto), "el stock baja 1"),
                () -> assertEquals(9, cantidadLote("CP05-L1"), "el lote baja 1"));
    }

    /*
     * Aclaración del requerimiento (no es un cambio de código): el equipo definió que "10 % desde la tercera compra"
     * del documento significa "10 % después de 3 compras", es decir, a partir de la 4.ª compra. En la corrida 1 esta
     * prueba esperaba 10 % ya en la 3.ª compra y falló (observación OBS-01). El código del sistema no se modificó.
     */
    @Test
    @DisplayName("CP-06 Descuento VIP: valor límite del umbral de 10 % (después de 3 compras, a partir de la 4.ª)")
    void cp06_descuentoVip() throws SQLException {
        iniciarSesion("Personal1", "890");
        caja.abrirTurno(100.00);
        int producto = crearProducto("CP06", "100.00", 2, null);
        recibirLote(producto, "CP06-L1", LocalDate.now().plusMonths(6), 20);

        // Cliente A: ya hizo 3 compras, la que hace ahora es su 4.ª -> 10 %
        // Cliente B: ya hizo 2 compras, la que hace ahora es su 3.ª -> 5 % (un punto por debajo del umbral)
        int clienteA = crearClienteConCompras("Cliente VIP CP06", "70000006", 3);
        int clienteB = crearClienteConCompras("Cliente límite CP06", "70000016", 2);
        ClienteVO a = new ControladorClientes().buscarClientes("70000006").get(0);
        ClienteVO b = new ControladorClientes().buscarClientes("70000016").get(0);

        int ventaA = vender(producto, 1, a.porcentajeVIP, Datos.EFECTIVO, clienteA, 200.0);
        int ventaB = vender(producto, 1, b.porcentajeVIP, Datos.EFECTIVO, clienteB, 200.0);
        VentaVO va = new ControladorVentas().obtenerVenta(ventaA);
        VentaVO vb = new ControladorVentas().obtenerVenta(ventaB);

        assertAll(
                () -> assertEquals(10, a.porcentajeVIP, 0.001, "4.ª compra (3 compras previas): 10 %"),
                () -> assertEquals(10.00, va.descuento, 0.001, "descuento aplicado en la 4.ª compra sobre S/ 100.00"),
                () -> assertEquals(5, b.porcentajeVIP, 0.001, "3.ª compra (2 compras previas): todavía 5 %"),
                () -> assertEquals(5.00, vb.descuento, 0.001, "descuento aplicado en la 3.ª compra sobre S/ 100.00"));
    }

    @Test
    @DisplayName("CP-07 FEFO: la venta descuenta del lote que vence primero y el otro queda intacto")
    void cp07_fefo() throws SQLException {
        iniciarSesion("Personal1", "890");
        caja.abrirTurno(100.00);
        int producto = crearProducto("CP07", "8.00", 2, null);
        // Se recibe primero el lote que vence después, para que el orden de ingreso no coincida con el de vencimiento
        recibirLote(producto, "CP07-TARDE", LocalDate.now().plusDays(120), 10);
        recibirLote(producto, "CP07-PRONTO", LocalDate.now().plusDays(20), 10);

        int ventaId = vender(producto, 3, 0, Datos.EFECTIVO, null, 50.0);

        assertAll(
                () -> assertEquals(7, cantidadLote("CP07-PRONTO"), "sale del lote que vence primero"),
                () -> assertEquals(10, cantidadLote("CP07-TARDE"), "el otro lote queda intacto"),
                () -> assertEquals(1, entero("SELECT COUNT(*) FROM venta_lotes WHERE venta_id = ?", ventaId), "un solo lote usado"),
                () -> assertEquals(loteId("CP07-PRONTO"), entero("SELECT lote_id FROM venta_lotes WHERE venta_id = ?", ventaId)),
                () -> assertEquals(17, stockProducto(producto), "stock del producto = suma de sus lotes"));
    }

    @Test
    @DisplayName("CP-08 Stock insuficiente: con stock + 1 se rechaza sin registros parciales; con el stock exacto procede")
    void cp08_stockInsuficiente() throws SQLException {
        iniciarSesion("Personal1", "890");
        caja.abrirTurno(100.00);
        int producto = crearProducto("CP08", "5.00", 2, null);
        recibirLote(producto, "CP08-L1", LocalDate.now().plusDays(30), 2);
        recibirLote(producto, "CP08-L2", LocalDate.now().plusDays(60), 2);
        String antes = huellaVentas();

        // Valor límite superior inválido: stock + 1 = 5 (la venta alcanza a tomar 4 antes de fallar)
        assertThrows(SQLException.class, () -> vender(producto, 5, 0, Datos.EFECTIVO, null, 50.0));
        assertEquals(antes, huellaVentas(), "el rechazo no debe dejar venta, detalle, lotes ni comprobante a medias (rollback)");
        assertEquals(4, stockProducto(producto));

        // Valor límite válido: exactamente el stock disponible
        int ventaId = vender(producto, 4, 0, Datos.EFECTIVO, null, 50.0);
        assertTrue(ventaId > 0);
        assertEquals(0, stockProducto(producto));
        assertEquals(0, cantidadLote("CP08-L1") + cantidadLote("CP08-L2"));
    }

    @Test
    @DisplayName("CP-14 Cierre de turno: faltante y sobrante por la diferencia exacta; el turno cerrado no admite ventas")
    void cp14_cierreDeTurno() throws SQLException {
        iniciarSesion("Personal1", "890");
        int producto = crearProducto("CP14", "10.00", 2, null);
        recibirLote(producto, "CP14-L1", LocalDate.now().plusMonths(6), 10);

        // Turno 1: esperado = 100.00 + 11.80 = 111.80; se cuentan 110.00 -> faltante de 1.80
        caja.abrirTurno(100.00);
        int turno1 = caja.turnoAbierto().id;
        vender(producto, 1, 0, Datos.EFECTIVO, null, 20.0);
        caja.cerrarTurno(turno1, 110.00, "Prueba de faltante");

        // Turno 2: esperado = 50.00 + 11.80 = 61.80; se cuentan 62.30 -> sobrante de 0.50
        caja.abrirTurno(50.00);
        int turno2 = caja.turnoAbierto().id;
        vender(producto, 1, 0, Datos.EFECTIVO, null, 20.0);
        caja.cerrarTurno(turno2, 62.30, "Prueba de sobrante");

        TurnoVO t1 = turno(turno1), t2 = turno(turno2);
        String antes = huellaVentas();
        assertThrows(SQLException.class, () -> vender(producto, 1, 0, Datos.EFECTIVO, null, 20.0),
                "con el turno cerrado no se admiten ventas");

        assertAll(
                () -> assertEquals(ControladorCaja.CERRADO, t1.estado),
                () -> assertEquals(111.80, t1.esperado, 0.001, "esperado = inicial + ventas en efectivo"),
                () -> assertEquals(-1.80, t1.diferencia(), 0.001, "faltante exacto"),
                () -> assertEquals(ControladorCaja.CERRADO, t2.estado),
                () -> assertEquals(61.80, t2.esperado, 0.001),
                () -> assertEquals(0.50, t2.diferencia(), 0.001, "sobrante exacto"),
                () -> assertEquals(antes, huellaVentas(), "la venta rechazada no deja registros"),
                () -> assertNull(caja.turnoAbierto(), "no queda turno abierto"));
    }

    private TurnoVO turno(int id) throws SQLException {
        return caja.listarTurnos(LocalDate.now(), LocalDate.now()).stream().filter(t -> t.id == id).findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró el turno " + id));
    }

    /** Registra un cliente con DNI y le agrega ventas completadas previas (historial). */
    static int crearClienteConCompras(String nombre, String dni, int compras) throws SQLException {
        new ControladorClientes().registrarCliente(nombre, dni, null, null);
        int clienteId = (int) entero("SELECT id FROM clientes WHERE documento = ?", dni);
        for (int i = 0; i < compras; i++) {
            insertar("INSERT INTO ventas (fecha, subtotal, descuento, total, forma_pago, cliente_id, estado) " +
                    "VALUES (?, 10.00, 0, 11.80, 'Efectivo', ?, 'COMPLETADA')", LocalDate.now().minusDays(10 + i), clienteId);
        }
        return clienteId;
    }
}
