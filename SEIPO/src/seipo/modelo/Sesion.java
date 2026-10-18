package seipo.modelo;

/** Usuario que inició sesión en la aplicación. */
public class Sesion {
    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String PERSONAL = "PERSONAL";

    private static int id;
    private static String usuario, nombre, rol;

    public static void iniciar(int id, String usuario, String nombre, String rol) {
        Sesion.id = id;
        Sesion.usuario = usuario;
        Sesion.nombre = nombre;
        Sesion.rol = rol;
    }

    public static void cerrar() {
        id = 0;
        usuario = nombre = rol = null;
    }

    public static int getId() { return id; }
    public static String getUsuario() { return usuario; }
    public static String getNombre() { return nombre == null || nombre.isEmpty() ? usuario : nombre; }
    public static String getRol() { return rol; }
    public static boolean esAdministrador() { return ADMINISTRADOR.equals(rol); }

    /** Texto para mostrar el rol en pantalla. */
    public static String textoRol(String rol) {
        return ADMINISTRADOR.equals(rol) ? "Administrador" : "Personal de tienda";
    }
}
