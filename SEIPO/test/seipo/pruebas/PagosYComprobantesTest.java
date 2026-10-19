package seipo.pruebas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seipo.controlador.ControladorCaja;
import seipo.controlador.ControladorClientes;
import seipo.controlador.ControladorComprobantes;
import seipo.controlador.ControladorComprobantes.ComprobanteVO;
import seipo.controlador.ControladorPasarela;
import seipo.controlador.ControladorPasarela.TransaccionVO;
import seipo.controlador.ControladorVentas;
import seipo.modelo.Datos;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Procesos 15 (cobro vía pasarela), comprobante y anulación. */
class PagosYComprobantesTest extends PruebaBase {

    private final ControladorPasarela pasarela = new ControladorPasarela();

    /**
     * El documento (Alcance de esta versión) indica que el cobro con tarjeta se retiró y que el cobro digital es
     * Yape o Plin por la pasarela simulada. Por eso la tarjeta 4111 1111 1111 1111 debe rechazarse como forma de pago,
     * y la autorización aprobada/rechazada se prueba con los datos de prueba de la billetera.
     */
    @Test
    @DisplayName("CP-09 Pago digital: la tarjeta (retirada) se rechaza; Yape aprobado genera token y autorización; Yape rechazado no confirma la venta")
    void cp09_pagoDigital() throws SQLException {
        iniciarSesion("Personal1", "890");
        new ControladorCaja().abrirTurno(100.00);
        int producto = crearProducto("CP09", "10.00", 2, null);
        recibirLote(producto, "CP09-L1", LocalDate.now().plusMonths(6), 10);
        double total = totalEsperado(new BigDecimal("10.00"), BigDecimal.ZERO).doubleValue();
        String antes = huellaVentas();

        // Tarjeta de prueba 4111 1111 1111 1111: forma de pago retirada del sistema
        assertThrows(SQLException.class, () -> pasarela.solicitarCobro("Tarjeta", total), "la pasarela no debe aceptar tarjeta");
        assertThrows(SQLException.class, () -> vender(producto, 1, 0, "Tarjeta", null, null, "4111111111111111"),
                "la venta no debe aceptar tarjeta");
        assertEquals(antes, huellaVentas(), "el intento con tarjeta no deja registros");

        // Clase válida: celular de 9 dígitos que empieza con 9 y código de 6 dígitos
        TransaccionVO solicitud = pasarela.solicitarCobro(Datos.YAPE_PLIN, total);
        assertEquals(ControladorPasarela.PENDIENTE, solicitud.estado);
        TransaccionVO aprobada = pasarela.autorizarBilletera(solicitud.token, ControladorPasarela.YAPE, "987654321", "123456");
        int ventaId = vender(producto, 1, 0, Datos.YAPE_PLIN, null, null, aprobada.token);
        TransaccionVO deVenta = pasarela.transaccionDeVenta(ventaId);

        // Clase inválida: código de aprobación 000000 (rechazo de la pasarela de prueba)
        TransaccionVO solicitud2 = pasarela.solicitarCobro(Datos.YAPE_PLIN, total);
        TransaccionVO rechazada = pasarela.autorizarBilletera(solicitud2.token, ControladorPasarela.YAPE, "987654321", "000000");
        String antesRechazo = huellaVentas();
        assertThrows(SQLException.class, () -> vender(producto, 1, 0, Datos.YAPE_PLIN, null, null, rechazada.token),
                "un cobro rechazado no confirma la venta");

        assertAll(
                () -> assertTrue(solicitud.token != null && solicitud.token.startsWith("tok_"), "se genera token de pago"),
                () -> assertEquals(ControladorPasarela.APROBADA, aprobada.estado),
                () -> assertTrue(aprobada.codigoAutorizacion != null && aprobada.codigoAutorizacion.matches("\\d{6}"), "código de autorización"),
                () -> assertNotNull(deVenta, "el cobro queda vinculado a la venta"),
                () -> assertEquals(aprobada.token, deVenta.token),
                () -> assertTrue(deVenta.medio.endsWith("321") && !deVenta.medio.contains("987654"), "solo se guardan los 3 últimos dígitos"),
                () -> assertEquals(ControladorPasarela.RECHAZADA, rechazada.estado),
                () -> assertNotNull(rechazada.mensaje, "el rechazo indica el motivo"),
                () -> assertEquals(antesRechazo, huellaVentas(), "el rechazo no deja venta registrada"));
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
        ComprobanteVO factura = comprobantes.obtener(vender(producto, 1, 0, Datos.EFECTIVO, conRuc, 20.0, null));
        ComprobanteVO boletaDni = comprobantes.obtener(vender(producto, 1, 0, Datos.EFECTIVO, conDni, 20.0, null));
        ComprobanteVO boletaVarios = comprobantes.obtener(vender(producto, 1, 0, Datos.EFECTIVO, null, 20.0, null));

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
    @DisplayName("CP-11 Anulación: el stock vuelve a los mismos lotes, la venta queda anulada y el cobro digital queda reembolsado")
    void cp11_anulacion() throws SQLException {
        iniciarSesion("admin", "123");
        new ControladorCaja().abrirTurno(100.00);
        int producto = crearProducto("CP11", "10.00", 2, null);
        recibirLote(producto, "CP11-PRONTO", LocalDate.now().plusDays(15), 3);
        recibirLote(producto, "CP11-TARDE", LocalDate.now().plusDays(90), 10);
        double total = totalEsperado(new BigDecimal("50.00"), BigDecimal.ZERO).doubleValue();

        // Venta en efectivo de 5 unidades: toma 3 del primer lote y 2 del segundo
        int ventaEfectivo = vender(producto, 5, 0, Datos.EFECTIVO, null, 100.0, null);
        // Venta digital (Yape o Plin; la tarjeta se retiró de esta versión) de 5 unidades del segundo lote
        TransaccionVO t = pasarela.autorizarBilletera(pasarela.solicitarCobro(Datos.YAPE_PLIN, total).token,
                ControladorPasarela.PLIN, "912345678", "654321");
        int ventaDigital = vender(producto, 5, 0, Datos.YAPE_PLIN, null, null, t.token);
        assertEquals(0, cantidadLote("CP11-PRONTO"));
        assertEquals(3, cantidadLote("CP11-TARDE"));

        ControladorVentas ventas = new ControladorVentas();
        ventas.anularVenta(ventaEfectivo, "Prueba CP-11 efectivo");
        ventas.anularVenta(ventaDigital, "Prueba CP-11 digital");

        assertAll(
                () -> assertEquals(3, cantidadLote("CP11-PRONTO"), "las unidades vuelven al lote de origen"),
                () -> assertEquals(10, cantidadLote("CP11-TARDE"), "las unidades vuelven al lote de origen"),
                () -> assertEquals(13, stockProducto(producto), "stock del producto restaurado"),
                () -> assertEquals(ControladorVentas.ANULADA, ventas.obtenerVenta(ventaEfectivo).estado),
                () -> assertEquals(ControladorVentas.ANULADA, ventas.obtenerVenta(ventaDigital).estado),
                () -> assertEquals(ControladorPasarela.REEMBOLSADA, pasarela.transaccionDeVenta(ventaDigital).estado,
                        "el cobro digital queda reembolsado"),
                () -> assertEquals(3, entero("SELECT COUNT(*) FROM movimientos WHERE producto_nombre = ? AND tipo = 'Anulación'",
                        "Producto de prueba CP11"), "un movimiento de anulación por cada lote devuelto (2 + 1)"));
    }
}
