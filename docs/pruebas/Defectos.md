# Defectos y observaciones — Pruebas de SEIPO

Registro de los hallazgos de las pruebas de caja negra (CP-01 a CP-15). Las evidencias de cada corrida están en [`evidencias/`](evidencias/). Volver al [informe de pruebas](Informe_Pruebas.md).

| ID | Caso | Severidad | Estado | Corrida en que se detectó | Corrida en que se cerró |
|---|---|---|---|---|---|
| DEF-01 | CP-02 | Alta | Corregido y verificado | Corrida 1 | Corrida 2 |
| OBS-01 | CP-06 | Media | Resuelto (aclaración del requerimiento) | Corrida 1 | Corrida 3 |

---

## DEF-01 — La restricción por rol existía solo en la interfaz

- **Caso relacionado:** CP-02 (Restricción por rol)
- **Severidad:** Alta (escalada de privilegios: el personal podía darse acceso de administrador)
- **Estado:** Corregido y verificado

**Descripción.** El menú de la aplicación ocultaba Reportes, Pagos y Usuarios al personal de tienda, pero los controladores no comprobaban el rol del usuario en sesión. Un usuario del personal podía crear cuentas, incluso de administrador, porque `ControladorUsuarios.registrarUsuario` aceptaba la operación. Por lectura del código, lo mismo ocurría en `actualizarUsuario`, `cambiarPassword`, `ControladorReportes.obtenerReporte` y `ControladorPasarela.listarTransacciones`. Esto contradice la regla del documento: *"el administrador además … gestiona Reportes, Pagos y Usuarios"*.

**Pasos para reproducir (con el código de la corrida 1)**
1. Iniciar sesión como `Personal1` / `890` (rol PERSONAL).
2. Invocar `ControladorUsuarios.registrarUsuario("cp02_intruso", "Creado por el personal", "x1", "ADMINISTRADOR")`. Es lo que hace la prueba CP-02.
3. Consultar `SELECT COUNT(*) FROM usuarios WHERE usuario = 'cp02_intruso'`.

| | Resultado |
|---|---|
| **Esperado** | La operación se rechaza y no se crea ninguna cuenta (0 filas) |
| **Obtenido en la corrida 1** | No hubo rechazo y se creó 1 cuenta con rol ADMINISTRADOR (ver [`Resultados_Corrida_1.txt`](evidencias/Resultados_Corrida_1.txt)) |
| **Obtenido en las corridas 2 y 3** | Rechazo con "Solo un administrador puede crear cuentas de usuario."; 0 cuentas creadas. CP-02 pasa |

**Corrección (corrida 2).** Se agregó la validación del rol del usuario en sesión, `Sesion.esAdministrador()`, al inicio de 5 métodos. Son 10 líneas en 3 controladores, con la misma forma que ya usaban `ControladorMermas` y `ControladorCompras`:

| Método | Mensaje para el personal |
|---|---|
| `ControladorUsuarios.registrarUsuario` | "Solo un administrador puede crear cuentas de usuario." |
| `ControladorUsuarios.actualizarUsuario` | "Solo un administrador puede modificar cuentas de usuario." |
| `ControladorUsuarios.cambiarPassword` | "Solo un administrador puede restablecer contraseñas." |
| `ControladorReportes.obtenerReporte` | "Solo un administrador puede consultar los reportes." |
| `ControladorPasarela.listarTransacciones` | "Solo un administrador puede ver las transacciones de pago." |

**Verificación**
- **Pruebas automatizadas:** CP-02 pasa en las corridas 2 y 3.
- **Verificación adicional en la corrida 2**, contra `seipo_test`:
  - Personal: los 5 métodos lo rechazan con su mensaje, y `personal2` conserva su rol y su clave.
  - Administrador: los 5 siguen funcionando. Creó, editó y cambió la clave de una cuenta, que luego entró con la clave nueva. El reporte de 2025 trajo 4 332 ventas y la lista de transacciones trajo 1 669.

---

## OBS-01 — Ambigüedad del requerimiento del descuento VIP

- **Caso relacionado:** CP-06 (Descuento VIP)
- **Severidad:** Media (afecta el monto cobrado si se interpreta distinto)
- **Estado:** Resuelto (aclaración del requerimiento). El código no se modificó.

**Descripción.** El documento decía: *"el cliente registrado obtiene 5 % desde su primera compra, 10 % desde la tercera y 15 % desde la quinta"*. La prueba de la corrida 1 interpretó "desde la tercera" como "la tercera compra ya recibe 10 %". El código (`ControladorClientes.ClienteVO`) cuenta las compras **ya completadas** (`numCompras >= 3`), así que da 10 % a partir de la 4.ª compra.

**Pasos para reproducir**
1. Registrar un cliente con 2 compras completadas.
2. Consultar su `porcentajeVIP` con `ControladorClientes.buscarClientes` y cobrarle una venta de S/ 100.00 con ese porcentaje (su 3.ª compra).

| | Resultado |
|---|---|
| **Esperado en la corrida 1** (lectura literal) | 3.ª compra: 10 % (S/ 10.00) |
| **Obtenido en la corrida 1** | 3.ª compra: 5 % (S/ 5.00). CP-06 falló (ver [`Resultados_Corrida_1.txt`](evidencias/Resultados_Corrida_1.txt)) |
| **Criterio aclarado por el equipo** | "10 % después de 3 compras (a partir de la 4.ª compra)": 3.ª compra 5 % y 4.ª compra 10 % |
| **Obtenido en la corrida 3** | 4.ª compra (3 previas): 10 % (S/ 10.00); 3.ª compra (2 previas): 5 % (S/ 5.00). CP-06 pasa |

**Resolución.** El equipo aclaró que el comportamiento del código es el correcto:
- La prueba CP-06 se ajustó al criterio aclarado, con un comentario que explica que es una aclaración del requerimiento y no un cambio de código.
- La frase del documento del proyecto se corrigió a "10 % después de 3 compras (a partir de la 4.ª compra)".
