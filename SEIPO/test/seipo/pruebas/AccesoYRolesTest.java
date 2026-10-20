package seipo.pruebas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seipo.controlador.ControladorLogin;
import seipo.controlador.ControladorLogin.Resultado;
import seipo.controlador.ControladorUsuarios;
import seipo.modelo.Sesion;
import seipo.vista.Principal;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** Proceso: acceso al sistema y restricción por rol. */
class AccesoYRolesTest extends PruebaBase {

    private static final List<String> SOLO_ADMINISTRADOR = Arrays.asList("Reportes", "Negocio", "Usuarios");

    @Test
    @DisplayName("CP-01 Acceso al sistema: credencial válida entra como administrador; clave errada y usuario inexistente se rechazan igual")
    void cp01_acceso() throws SQLException {
        ControladorLogin login = new ControladorLogin();

        // Clase válida
        assertEquals(Resultado.CORRECTO, login.iniciarSesion("admin", "123"));
        assertTrue(Sesion.esAdministrador(), "admin debe entrar con rol ADMINISTRADOR");
        Sesion.cerrar();

        // Clases inválidas: misma respuesta, para no revelar cuál dato falló
        Resultado claveErrada = login.iniciarSesion("admin", "clave_incorrecta");
        Resultado usuarioInexistente = login.iniciarSesion("usuario_que_no_existe", "123");
        assertAll(
                () -> assertEquals(Resultado.INCORRECTO, claveErrada, "clave incorrecta"),
                () -> assertEquals(Resultado.INCORRECTO, usuarioInexistente, "usuario inexistente"),
                () -> assertEquals(claveErrada, usuarioInexistente, "ambos rechazos deben ser indistinguibles"),
                () -> assertEquals(0, Sesion.getId(), "no debe quedar ninguna sesión iniciada"));

        // Valor límite: un espacio de más al final (en SQL, la intercalación lo ignoraría)
        assertEquals(Resultado.INCORRECTO, login.iniciarSesion("admin", "123 "), "la clave con un espacio extra debe rechazarse");
    }

    @Test
    @DisplayName("CP-02 Restricción por rol: el personal no accede a Reportes, Negocio ni Usuarios; el administrador accede a todo")
    void cp02_restriccionPorRol() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Se necesita entorno gráfico para construir la ventana principal");

        iniciarSesion("Personal1", "890");
        List<String> modulosPersonal = modulosDelMenu();
        Sesion.cerrar();
        iniciarSesion("admin", "123");
        List<String> modulosAdmin = modulosDelMenu();
        Sesion.cerrar();

        // El documento también restringe la gestión de Usuarios al administrador: se prueba en el controlador
        iniciarSesion("Personal1", "890");
        SQLException rechazo = null;
        try {
            new ControladorUsuarios().registrarUsuario("cp02_intruso", "Creado por el personal", "x1", Sesion.ADMINISTRADOR);
        } catch (SQLException e) {
            rechazo = e;
        }
        final SQLException rechazoFinal = rechazo;
        long creados = entero("SELECT COUNT(*) FROM usuarios WHERE usuario = 'cp02_intruso'");

        assertAll(
                () -> assertTrue(modulosPersonal.containsAll(Arrays.asList("Ventas", "Caja", "Clientes", "Productos", "Inventario", "Mermas", "Compras")),
                        "el personal debe ver sus módulos de operación e inventario: " + modulosPersonal),
                () -> assertTrue(SOLO_ADMINISTRADOR.stream().noneMatch(modulosPersonal::contains),
                        "el personal no debe ver Reportes, Negocio ni Usuarios: " + modulosPersonal),
                () -> assertTrue(modulosAdmin.containsAll(modulosPersonal) && modulosAdmin.containsAll(SOLO_ADMINISTRADOR),
                        "el administrador debe ver todos los módulos: " + modulosAdmin),
                () -> assertNotNull(rechazoFinal, "el personal no debe poder crear cuentas de usuario (módulo Usuarios)"),
                () -> assertEquals(0, creados, "no debe quedar creada la cuenta de administrador hecha por el personal"));
    }

    /** Construye la ventana principal sin mostrarla y devuelve las secciones de su menú. */
    @SuppressWarnings("unchecked")
    private static List<String> modulosDelMenu() throws Exception {
        List<String> modulos = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                Principal p = new Principal();
                Field f = Principal.class.getDeclaredField("secciones");
                f.setAccessible(true);
                modulos.addAll(((Map<String, ?>) f.get(p)).keySet());
                p.dispose();
                // Cierra el aviso de alertas que el administrador recibe al ingresar
                SwingUtilities.invokeLater(() -> {
                    for (Window w : Window.getWindows()) if (w instanceof JDialog) w.dispose();
                });
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
        return modulos;
    }
}
