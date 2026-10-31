package seipo.pruebas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seipo.controlador.ControladorCaja;
import seipo.controlador.ControladorVentas;
import seipo.controlador.ControladorVentas.AnulacionVO;
import seipo.modelo.Datos;
import seipo.modelo.Sesion;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Anulación de ventas por el personal en su propio turno y registro de anulaciones (CAM-02). Reglas: el personal
 * anula solo las ventas que registró en su turno, y solo mientras ese turno siga abierto; el administrador anula
 * cualquier venta de un turno abierto; el motivo se elige de una lista y "Otro" exige un detalle de 5 caracteres o más.
 */
class AnulacionesTest extends PruebaBase {

    private final ControladorCaja caja = new ControladorCaja();
    private final ControladorVentas ventas = new ControladorVentas();

    /** Filas de las tablas que toca una anulación; si no cambia, el rechazo no dejó nada a medias. */
    private static String huellaAnulacion() throws SQLException {
        return huellaVentas() + "/" + entero("SELECT COUNT(*) FROM anulaciones") + "/"
                + entero("SELECT COUNT(*) FROM ventas WHERE estado = 'ANULADA'");
    }

    @Test
    @DisplayName("CP-16 Anulación por el personal: anula una venta propia de su turno abierto con motivo; el stock vuelve a los lotes y queda el registro")
    void cp16_personalAnulaVentaPropia() throws SQLException {
        iniciarSesion("Personal1", "890");
        caja.abrirTurno(100.00);
        int turnoId = caja.turnoAbierto().id;
        int producto = crearProducto("CP16", "10.00", 2, null);
        recibirLote(producto, "CP16-PRONTO", LocalDate.now().plusDays(15), 3);
        recibirLote(producto, "CP16-TARDE", LocalDate.now().plusDays(90), 10);
        // 5 unidades: 3 del lote que vence primero y 2 del otro; total 5 x 11.80 = 59.00
        int ventaId = vender(producto, 5, 0, Datos.EFECTIVO, null, 60.00);

        LocalDateTime antes = LocalDateTime.now().withNano(0);
        ventas.anularVenta(ventaId, "Cliente desistió", "Prueba CP-16");
        LocalDateTime despues = LocalDateTime.now();

        int personal1 = (int) entero("SELECT id FROM usuarios WHERE usuario = 'Personal1'");
        List<AnulacionVO> registro = ventas.listarAnulaciones(LocalDate.now(), LocalDate.now());
        AnulacionVO a = registro.stream().filter(x -> x.ventaId == ventaId).findFirst()
                .orElseThrow(() -> new AssertionError("La anulación no quedó en el registro"));
        assertAll(
                () -> assertEquals(3, cantidadLote("CP16-PRONTO"), "las unidades vuelven al lote de origen"),
                () -> assertEquals(10, cantidadLote("CP16-TARDE"), "las unidades vuelven al lote de origen"),
                () -> assertEquals(13, stockProducto(producto), "stock del producto restaurado"),
                () -> assertEquals(ControladorVentas.ANULADA, ventas.obtenerVenta(ventaId).estado),
                () -> assertEquals(0, caja.turnoAbierto().totalVendido(), 0.001, "la venta anulada sale del cuadre del turno"),
                () -> assertEquals(1, entero("SELECT COUNT(*) FROM anulaciones WHERE venta_id = ?", ventaId), "un solo registro"),
                () -> assertEquals(personal1, entero("SELECT usuario_id FROM anulaciones WHERE venta_id = ?", ventaId), "usuario que anuló"),
                () -> assertEquals(Sesion.PERSONAL, texto("SELECT rol FROM anulaciones WHERE venta_id = ?", ventaId), "rol"),
                () -> assertEquals(turnoId, entero("SELECT turno_id FROM anulaciones WHERE venta_id = ?", ventaId), "turno"),
                () -> assertEquals("Cliente desistió", a.motivo),
                () -> assertEquals("Prueba CP-16", a.detalle),
                () -> assertEquals(59.00, a.monto, 0.001, "monto de la venta"),
                () -> assertFalse(a.fecha.isBefore(antes) || a.fecha.isAfter(despues), "fecha y hora de la anulación"));
    }

    @Test
    @DisplayName("CP-17 Anulación no permitida: el personal no anula ventas de otro usuario ni de un turno cerrado; no cambia nada")
    void cp17_personalNoAnulaAjenaNiCerrada() throws SQLException {
        // Venta de Personal1 en su turno abierto; personal2 intenta anularla desde su propio turno
        iniciarSesion("Personal1", "890");
        int producto = crearProducto("CP17", "10.00", 2, null);
        recibirLote(producto, "CP17-L1", LocalDate.now().plusMonths(6), 10);
        caja.abrirTurno(100.00);
        int ventaAjena = vender(producto, 1, 0, Datos.EFECTIVO, null, 20.0);
        iniciarSesion("personal2", "456");
        caja.abrirTurno(50.00);
        String antesAjena = huellaAnulacion();
        SQLException ajena = assertThrows(SQLException.class,
                () -> ventas.anularVenta(ventaAjena, "Error de registro", null), "venta de otro usuario");
        String despuesAjena = huellaAnulacion();

        // Venta propia de Personal1 cuyo turno ya se cerró
        iniciarSesion("Personal1", "890");
        int ventaCerrada = vender(producto, 1, 0, Datos.EFECTIVO, null, 20.0);
        int turnoPersonal1 = caja.turnoAbierto().id;
        caja.cerrarTurno(turnoPersonal1, caja.turnoAbierto().efectivoEnCaja(), "");
        String antesCerrada = huellaAnulacion();
        SQLException cerrada = assertThrows(SQLException.class,
                () -> ventas.anularVenta(ventaCerrada, "Error de registro", null), "venta de un turno cerrado");

        assertAll(
                () -> assertEquals(antesAjena, despuesAjena, "el rechazo de la venta ajena no deja cambios"),
                () -> assertTrue(ajena.getMessage().contains("su propio turno"), "mensaje claro: " + ajena.getMessage()),
                () -> assertEquals(antesCerrada, huellaAnulacion(), "el rechazo del turno cerrado no deja cambios"),
                () -> assertTrue(cerrada.getMessage().contains("turno abierto"), "mensaje claro: " + cerrada.getMessage()),
                () -> assertEquals(ControladorVentas.COMPLETADA, ventas.obtenerVenta(ventaAjena).estado),
                () -> assertEquals(ControladorVentas.COMPLETADA, ventas.obtenerVenta(ventaCerrada).estado),
                () -> assertEquals(8, stockProducto(producto), "el stock no cambia"));
    }

    @Test
    @DisplayName("CP-18 Motivo obligatorio: sin motivo, con un motivo fuera de la lista o con \"Otro\" y menos de 5 caracteres se rechaza; con 5 se acepta")
    void cp18_motivoObligatorio() throws SQLException {
        iniciarSesion("Personal1", "890");
        caja.abrirTurno(100.00);
        int producto = crearProducto("CP18", "10.00", 2, null);
        recibirLote(producto, "CP18-L1", LocalDate.now().plusMonths(6), 10);
        int ventaId = vender(producto, 1, 0, Datos.EFECTIVO, null, 20.0);

        String antes = huellaAnulacion();
        assertAll(
                () -> assertThrows(SQLException.class, () -> ventas.anularVenta(ventaId, null, null), "sin motivo"),
                () -> assertThrows(SQLException.class, () -> ventas.anularVenta(ventaId, "", "Detalle cualquiera"), "motivo vacío"),
                () -> assertThrows(SQLException.class, () -> ventas.anularVenta(ventaId, "Porque sí", null), "motivo fuera de la lista"),
                () -> assertThrows(SQLException.class, () -> ventas.anularVenta(ventaId, Datos.MOTIVO_OTRO, null), "Otro sin detalle"),
                () -> assertThrows(SQLException.class, () -> ventas.anularVenta(ventaId, Datos.MOTIVO_OTRO, "  abcd  "),
                        "Otro con 4 caracteres (los espacios no cuentan)"));
        assertEquals(antes, huellaAnulacion(), "los rechazos no dejan cambios");
        assertEquals(ControladorVentas.COMPLETADA, ventas.obtenerVenta(ventaId).estado);

        // Valor límite: "Otro" con exactamente 5 caracteres se acepta
        ventas.anularVenta(ventaId, Datos.MOTIVO_OTRO, "abcde");
        assertAll(
                () -> assertEquals(ControladorVentas.ANULADA, ventas.obtenerVenta(ventaId).estado),
                () -> assertEquals("abcde", texto("SELECT detalle FROM anulaciones WHERE venta_id = ?", ventaId)));
    }

    @Test
    @DisplayName("CP-19 Anulación por el administrador: anula una venta de un turno abierto de otro usuario y también queda registrada")
    void cp19_administradorAnulaYQuedaRegistrada() throws SQLException {
        iniciarSesion("Personal1", "890");
        caja.abrirTurno(100.00);
        int producto = crearProducto("CP19", "10.00", 2, null);
        recibirLote(producto, "CP19-L1", LocalDate.now().plusMonths(6), 10);
        int ventaId = vender(producto, 2, 0, Datos.YAPE_PLIN, null, null);

        iniciarSesion("admin", "123");
        ventas.anularVenta(ventaId, "Error en el cobro", null);

        int admin = (int) entero("SELECT id FROM usuarios WHERE usuario = 'admin'");
        assertAll(
                () -> assertEquals(ControladorVentas.ANULADA, ventas.obtenerVenta(ventaId).estado),
                () -> assertEquals("Error en el cobro", ventas.obtenerVenta(ventaId).motivoAnulacion, "el comprobante muestra el motivo"),
                () -> assertEquals(10, stockProducto(producto), "el stock vuelve"),
                () -> assertEquals(admin, entero("SELECT usuario_id FROM anulaciones WHERE venta_id = ?", ventaId), "usuario que anuló"),
                () -> assertEquals(Sesion.ADMINISTRADOR, texto("SELECT rol FROM anulaciones WHERE venta_id = ?", ventaId), "rol"),
                () -> assertEquals("Error en el cobro", texto("SELECT motivo FROM anulaciones WHERE venta_id = ?", ventaId)),
                () -> assertNull(texto("SELECT detalle FROM anulaciones WHERE venta_id = ?", ventaId), "el detalle es opcional"),
                () -> assertEquals(23.60, decimal("SELECT monto FROM anulaciones WHERE venta_id = ?", ventaId).doubleValue(), 0.001, "monto"),
                () -> assertTrue(ventas.listarAnulaciones(LocalDate.now(), LocalDate.now()).stream().anyMatch(x -> x.ventaId == ventaId),
                        "el administrador la ve en el registro"));
    }
}
