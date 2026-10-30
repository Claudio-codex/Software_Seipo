package seipo.modelo;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Guarda las contraseñas con PBKDF2-HMAC-SHA512 y una sal aleatoria por usuario, nunca en texto plano.
 * El valor guardado tiene la forma {@code pbkdf2$iteraciones$sal$hash} (sal y hash en Base64), así que
 * cada contraseña lleva sus propios parámetros y se pueden subir las iteraciones sin invalidar las anteriores.
 */
public final class Contrasenas {

    private static final String PREFIJO = "pbkdf2$";
    private static final String ALGORITMO = "PBKDF2WithHmacSHA512";
    /** Iteraciones recomendadas por OWASP para PBKDF2-HMAC-SHA512. */
    private static final int ITERACIONES = 210_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Contrasenas() {
    }

    /** Devuelve el valor que se guarda en usuarios.password para la contraseña dada. */
    public static String cifrar(String password) {
        byte[] sal = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIJO + ITERACIONES + "$" + b64.encodeToString(sal) + "$"
                + b64.encodeToString(derivar(password, sal, ITERACIONES));
    }

    /** Indica si el valor guardado ya está cifrado (las bases anteriores guardaban la contraseña tal cual). */
    public static boolean estaCifrada(String guardado) {
        return guardado != null && guardado.startsWith(PREFIJO);
    }

    /** Compara la contraseña escrita con el valor guardado. Un valor mal formado nunca coincide. */
    public static boolean verificar(String password, String guardado) {
        if (!estaCifrada(guardado)) return false;
        String[] partes = guardado.split("\\$");
        if (partes.length != 4) return false;
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            // Comparación en tiempo constante, para no revelar cuántos bytes coinciden
            return MessageDigest.isEqual(esperado, derivar(password, sal, iteraciones));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(String password, byte[] sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), sal, iteraciones, BITS_HASH);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("La JVM no tiene " + ALGORITMO, e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Uso: java seipo.modelo.Contrasenas &lt;contraseña&gt; — imprime el valor cifrado (para preparar datos a mano). */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java seipo.modelo.Contrasenas <contraseña>");
            return;
        }
        System.out.println(cifrar(args[0]));
    }
}
