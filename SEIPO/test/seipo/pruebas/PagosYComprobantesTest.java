package seipo.pruebas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seipo.controlador.ControladorCaja;
import seipo.controlador.ControladorClientes;
import seipo.controlador.ControladorComprobantes;
import seipo.controlador.ControladorComprobantes.ComprobanteVO;
import seipo.controlador.ControladorVentas;
import seipo.modelo.Datos;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Proceso 15 (forma de pago), comprobante y anulación. */
class PagosYComprobantesTest extends PruebaBase {

    /**
     * El documento (Alcance de esta versión) indica que el cobro con tarjeta se retiró y que las formas de pago son
     * Efectivo y Yape o Plin. El sistema solo registra la forma de pago: no procesa el cobro digital (no hay pasarela).
     * Por eso la tarjeta 4111 1111 1111 1111 y cualquier otra forma de pago deben rechazarse, y Yape o Plin se registra
     * sin pedir efectivo recibido.
     */
    @Test
    @DisplayName("CP-09 Forma de pago: la tarjeta (retirada) y otras formas se rechazan; Yape o Plin se registra como forma de pago y suma en el turno")
    void cp09_formaDePago() throws SQLException {
        iniciarSesion("Personal1", "890");
        ControladorCaja caja = new ControladorCaja();
        caja.abrirTurno(100.00);
        int producto = crearProducto("CP09", "10.00", 2, null);
        recibirLote(producto, "CP09-L1", LocalDate.now().plusMonths(6), 10);
        double total = totalEsperado(new BigDecimal("10.00"), BigDecimal.ZERO).doubleValue();
        String antes = huellaVentas();

        // Clases inválidas: tarjeta de prueba 4111 1111 1111 1111 (forma de pago retirada) y una forma inexistente
        assertThrows(SQLException.class, () -> vender(producto, 1, 0, "Tarjeta", null, null), "la venta no debe aceptar tarjeta");
        assertThrows(SQLException.class, () -> vender(producto, 1, 0, "Transferencia", null, null),
                "la venta no debe aceptar formas de pago fuera de Efectivo y Yape o Plin");
        String despuesInvalidas = huellaVentas();

        // Clase válida: Yape o Plin, sin efectivo recibido
        int ventaId = vender(producto, 1, 0, Datos.YAPE_PLIN, null, null);
        ControladorVentas.VentaVO venta = new ControladorVentas().obtenerVenta(ventaId);
        ComprobanteVO comprobante = new ControladorComprobantes().obtener(ventaId);
        ControladorCaja.TurnoVO turno = caja.turnoAbierto();

        assertAll(
                () -> assertEquals(antes, despuesInvalidas, "los intentos con formas de pago no válidas no dejan registros"),
                () -> assertEquals(Datos.YAPE_PLIN, venta.formaPago, "la venta guarda la forma de pago"),
                () -> assertNull(venta.recibido, "Yape o Plin no registra efectivo recibido"),
                () -> assertEquals(total, venta.total, 0.001),
                () -> assertNotNull(comprobante, "la venta con Yape o Plin emite comprobante"),
                () -> assertEquals(total, turno.yapePlin, 0.001, "el turno suma la venta en Yape o Plin"),
                () -> assertEquals(100.00, turno.efectivoEnCaja(), 0.001, "Yape o Plin no cambia el efectivo en caja"),
                () -> assertEquals(0, entero("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                        + "AND table_name = 'transacciones_pago'"), "no queda tabla de pasarela de pago"));
    }

    @Test
    @DisplayName("CP-10 Comprobante: RUC emite factura F001, DNI y consumidor final emiten boleta B001, numeración correlativa de 1 en 1")
    void cp10_comprobante() throws SQLException {
        iniciarSesion("Personal1", "890");
        new ControladorCaja().abrirTurno(100.00);
        int producto = crearProducto("CP10", "10.00", 2, null);
        recibirLote(producto, "CP10-L1", LocalDate.now().plusMonths(6), 10);
        ControladorClientes clientes = new ControladorClientes();
        clientes.registrarCliente("Empresa de prueba CP10 S.A.C.", "20123456781", null, null);
        clientes.registrarCliente("Persona de prueba CP10", "71234567", null, null);
        int conRuc = (int) entero("SELECT id FROM clientes WHERE documento = '20123456781'");
        int conDni = (int) entero("SELECT id FROM clientes WHERE documento = '71234567'");
        long ultimaF = entero("SELECT COALESCE(MAX(numero), 0) FROM comprobantes WHERE serie = 'F001'");
        long ultimaB = entero("SELECT COALESCE(MAX(numero), 0) FROM comprobantes WHERE serie = 'B001'");

        ControladorComprobantes comprobantes = new ControladorComprobantes();
        ComprobanteVO factura = comprobantes.obtener(vender(producto, 1, 0, Datos.EFECTIVO, conRuc, 20.0));
        ComprobanteVO boletaDni = comprobantes.obtener(vender(producto, 1, 0, Datos.EFECTIVO, conDni, 20.0));
        ComprobanteVO boletaVarios = comprobantes.obtener(vender(producto, 1, 0, Datos.EFECTIVO, null, 20.0));

        assertAll(
                () -> assertEquals(ControladorComprobantes.FACTURA, factura.tipo),
                () -> assertEquals("F001", factura.serie),
                () -> assertEquals(ultimaF + 1, factura.numero, "la factura sigue a la última F001"),
                () -> assertEquals(ControladorComprobantes.BOLETA, boletaDni.tipo),
                () -> assertEquals("B001", boletaDni.serie),
                () -> assertEquals(ultimaB + 1, boletaDni.numero, "la boleta sigue a la última B001"),
                () -> assertEquals(ControladorComprobantes.BOLETA, boletaVarios.tipo, "consumidor final sin documento: boleta"),
                () -> assertEquals("B001", boletaVarios.serie),
                () -> assertEquals(ultimaB + 2, boletaVarios.numero, "la numeración sube de 1 en 1"),
                () -> assertNotNull(factura.codigoVerificacion, "el comprobante lleva código de verificación"));
    }

    @Test
    @DisplayName("CP-11 Anulación: el stock vuelve a los mismos lotes, la venta queda anulada y sale del cuadre del turno")
    void cp11_anulacion() throws SQLException {
        iniciarSesion("admin", "123");
        new ControladorCaja().abrirTurno(100.00);
        int producto = crearProducto("CP11", "10.00", 2, null);
        recibirLote(producto, "CP11-PRONTO", LocalDate.now().plusDays(15), 3);
        recibirLote(producto, "CP11-TARDE", LocalDate.now().plusDays(90), 10);

        // Venta en efectivo de 5 unidades: toma 3 del primer lote y 2 del segundo
        int ventaEfectivo = vender(producto, 5, 0, Datos.EFECTIVO, null, 100.0);
        // Venta digital (Yape o Plin; la tarjeta se retiró de esta versión) de 5 unidades del segundo lote
        int ventaDigital = vender(producto, 5, 0, Datos.YAPE_PLIN, null, null);
        assertEquals(0, cantidadLote("CP11-PRONTO"));
        assertEquals(3, cantidadLote("CP11-TARDE"));

        ControladorVentas ventas = new ControladorVentas();
        ControladorCaja caja = new ControladorCaja();
        ventas.anularVenta(ventaEfectivo, "Error de registro", "Prueba CP-11 efectivo");
        ventas.anularVenta(ventaDigital, "Cliente desistió", "Prueba CP-11 digital");

        assertAll(
                () -> assertEquals(3, cantidadLote("CP11-PRONTO"), "las unidades vuelven al lote de origen"),
                () -> assertEquals(10, cantidadLote("CP11-TARDE"), "las unidades vuelven al lote de origen"),
                () -> assertEquals(13, stockProducto(producto), "stock del producto restaurado"),
                () -> assertEquals(ControladorVentas.ANULADA, ventas.obtenerVenta(ventaEfectivo).estado),
                () -> assertEquals(ControladorVentas.ANULADA, ventas.obtenerVenta(ventaDigital).estado),
                () -> assertEquals(0, caja.turnoAbierto().totalVendido(), 0.001, "las ventas anuladas salen del total del turno"),
                () -> assertEquals(0, caja.turnoAbierto().yapePlin, 0.001, "la venta con Yape o Plin anulada sale del cuadre"),
                () -> assertEquals(3, entero("SELECT COUNT(*) FROM movimientos WHERE producto_nombre = ? AND tipo = 'Anulación'",
                        "Producto de prueba CP11"), "un movimiento de anulación por cada lote devuelto (2 + 1)"));
    }
}
