# Defectos y observaciones — Pruebas de SEIPO

Registro de los hallazgos de las pruebas de caja negra (CP-01 a CP-15). Las evidencias de cada corrida están en [`evidencias/`](evidencias/). Volver al [informe de pruebas](Informe_Pruebas.md).

| ID | Caso | Severidad | Estado | Corrida en que se detectó | Corrida en que se cerró |
|---|---|---|---|---|---|
| DEF-01 | CP-02 | Alta | Corregido y verificado | Corrida 1 | Corrida 2 |
| OBS-01 | CP-06 | Media | Resuelto (aclaración del requerimiento) | Corrida 1 | Corrida 3 |
| OBS-02 | — | Media | Corregido y verificado | Revisión del código para los manuales | Corrida 4 |
| OBS-03 | — | Baja | Corregido y verificado | Revisión del código para los manuales | Corrida 4 |
| OBS-04 | — | Baja | Corregido y verificado | Revisión del código para los manuales | Corrida 4 |
| OBS-05 | — | Baja | Corregido y verificado | Revisión del código para los manuales | Corrida 4 |
| CAM-01 | CP-02, CP-09, CP-11 | — (cambio de alcance) | Aplicado y verificado | Decisión del equipo | Corrida 5 |

Las observaciones OBS-02 a OBS-05 no las detectó ninguno de los 15 casos automatizados. Se encontraron al leer el código de las pantallas para escribir los manuales de usuario, después de la corrida 3. Se corrigieron antes de la corrida 4. La corrida 4 confirmó que los 15 casos siguen aprobando (15/15), y cada corrección se verificó aparte como se indica en cada observación.

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
| `ControladorPasarela.listarTransacciones` | "Solo un administrador puede ver las transacciones de pago." (método eliminado con la pasarela en la corrida 5, CAM-01) |

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

---

## OBS-02 — Descuento manual sin tope ni control

- **Caso relacionado:** ninguno (revisión del código de `PanelVentas`)
- **Severidad:** Media (afecta el monto cobrado)
- **Estado:** Corregido y verificado (corrida 4)

**Descripción.** El botón **Descuento (F6)** de Ventas aceptaba cualquier porcentaje de 0 a 100 para cualquier usuario, también el personal, sin autorización ni tope. El documento solo describe el descuento VIP automático.

**Pasos para reproducir (antes de la corrección)**
1. Iniciar sesión como `Personal1`, abrir turno y agregar un producto.
2. Pulsar **Descuento (F6)** y escribir `100`.
3. Cobrar.

| | Resultado |
|---|---|
| **Esperado** | El personal no puede dar descuentos manuales sin límite |
| **Obtenido antes** | La venta se registraba con 100 % de descuento |
| **Obtenido después (corrida 4)** | Con más de 10 % se muestra "El personal puede aplicar como máximo 10 % de descuento manual. Para un descuento mayor, pida al administrador que lo aplique." |

**Corrección.** Se definió `Datos.DESCUENTO_MAX_PERSONAL = 10`. Se valida en dos lugares:
- En la pantalla (`PanelVentas.aplicarDescuento`), que muestra el mensaje.
- En el controlador (`ControladorVentas.cobrarVenta` → `validarDescuento`). Así no depende solo de la interfaz, que fue la lección de DEF-01.

El descuento VIP automático no cambia: si el cliente elegido tiene un VIP mayor (Platino, 15 %), ese porcentaje se respeta. El administrador no tiene tope.

**Verificación (contra `seipo_test`):**
- Personal con 10 %: se registra.
- Personal con 10,5 %: se rechaza.
- Personal con 15 % y cliente Platino: se registra.
- Personal con 20 % y cliente Platino: se rechaza.
- Administrador con 30 %: se registra.

---

## OBS-03 — Botones de los cuadros de diálogo en inglés

- **Caso relacionado:** ninguno (revisión de `Login` y `UI`)
- **Severidad:** Baja (usabilidad)
- **Estado:** Corregido y verificado (corrida 4)

**Descripción.** `Login.main` fija `Locale.US` para el formato de números. Por eso los cuadros de confirmación mostraban **OK**, **Cancel**, **Yes** y **No**, y los selectores de archivo **Save** y **Cancel**, aunque el resto del programa está en español.

| | Resultado |
|---|---|
| **Esperado** | Botones en español |
| **Obtenido antes** | OK, Cancel, Yes, No, Save |
| **Obtenido después** | Aceptar, Cancelar, Sí, No, Guardar |

**Corrección.** `UI.instalarTema()` define los textos de `UIManager` para `OptionPane` (Aceptar, Cancelar, Sí, No) y `FileChooser` (Guardar, Abrir, Cancelar y las etiquetas "Guardar en:", "Buscar en:", "Nombre de archivo:" y "Tipo de archivo:"). No depende del `Locale`, que se mantiene en US para los números.

**Verificación:** con el tema instalado y `Locale.US`, un `JOptionPane` de Sí/No/Cancelar muestra "Sí | No | Cancelar".

---

## OBS-04 — respaldo.bat se cerraba sin mostrar el resultado

- **Caso relacionado:** ninguno (revisión de `respaldo.bat`)
- **Severidad:** Baja (usabilidad)
- **Estado:** Corregido y verificado (corrida 4)

**Descripción.** Con doble clic, la ventana del respaldo se cerraba al terminar y no se alcanzaba a leer "Respaldo guardado en …" ni los errores.

**Corrección.** Antes de salir, tanto si sale bien como si falla, el script ejecuta `pause`. Para que la tarea del Programador de tareas no quede esperando una tecla, se ejecuta con el argumento `/sinpausa`. El README y el Manual del Administrador lo indican. Los códigos de salida (0 si sale bien y 1 si falla) no cambian.

**Verificación:**
- Sin argumento: muestra "Presione una tecla para continuar" y sale con 0.
- Con `/sinpausa`: no pausa y sale con 0.
- Sin `config.properties`: muestra el error, pausa y sale con 1.

---

## OBS-05 — Textos de ayuda y mensaje de registro inexactos

- **Caso relacionado:** ninguno (revisión de `PanelUsuarios` y `PanelClientes`)
- **Severidad:** Baja (información confusa)
- **Estado:** Corregido y verificado (corrida 4)

**Descripción**
- **Ayuda de Usuarios:** decía que el personal accede a "Ventas, Productos, Inventario, Mermas, Compras y Clientes" (faltaba **Caja**) y que el administrador gestiona "Reportes y Usuarios" (faltaba **Pagos**).
- **Registro de clientes:** mostraba "¡Cliente registrado exitosamente con categoría inicial!", aunque el cliente nuevo no tiene descuento VIP.

**Corrección**
- **Ayuda de Usuarios:** "El personal de tienda accede a Ventas, Caja, Clientes, Productos, Inventario, Mermas y Compras. El administrador además autoriza mermas y órdenes de compra y gestiona Reportes, Pagos y Usuarios."
- **Registro de clientes:** "¡Cliente registrado exitosamente! Aún no tiene descuento VIP: lo obtiene según sus compras completadas."

**Verificación:** la aplicación compila y los 15 casos pasan en la corrida 4. Son cambios solo de texto.

---

## CAM-01 — Se quita la pasarela de pago simulada

- **Tipo:** cambio de alcance decidido por el equipo, no es un defecto.
- **Casos relacionados:** CP-02, CP-09 y CP-11.

**Motivo.** El sistema solo registra con qué forma de pago se cobró la venta; no hace la transacción. La pasarela simulada (solicitud de cobro, token, autorización, rechazo, reversión y reembolso) no correspondía a lo que hace el negocio: el cliente paga con Yape o Plin desde su celular y el cajero lo confirma a la vista.

**Cambio**
- Se eliminaron `ControladorPasarela` y `DialogoPasarela`. `ControladorVentas.cobrarVenta` ya no recibe un token de pago y `anularVenta` ya no marca reembolsos.
- Al cobrar con Yape o Plin, Ventas pide confirmar que el cliente ya pagó y registra la forma de pago en `ventas.forma_pago`.
- Al anular una venta con Yape o Plin, el aviso indica que el cajero devuelve el pago desde el celular del negocio.
- La sección **Pagos** pasó a ser **Negocio** y solo conserva los datos del emisor del comprobante (nombre, RUC y dirección), en `ControladorComprobantes.leerEmisor` y `guardarEmisor` (solo administrador).
- `ConexionBD` borra la tabla `transacciones_pago` y las claves `pasarela_*` de `configuracion` en las bases creadas con versiones anteriores. El dump `SEIPO/bd/seipo_db.sql` ya no las incluye.

**Ajuste de las pruebas.** CP-02 espera la sección Negocio en lugar de Pagos. CP-09 comprueba que se rechacen la tarjeta y cualquier forma de pago no admitida, y que Yape o Plin se registre y sume en el turno. CP-11 comprueba que la venta anulada salga del cuadre del turno en lugar del reembolso.

**Verificación:** la aplicación compila y los 15 casos pasan en la corrida 5.

---

## Limitaciones conocidas

Comportamientos del sistema que se dejan como están en esta versión. Se documentan en los manuales.

| Limitación | Detalle | Recomendación |
|---|---|---|
| El personal puede crear productos y proveedores | **+ Nuevo Producto** (con precio y stock mínimo y máximo) y **+ Nuevo proveedor** están disponibles para el personal; solo el administrador puede **editarlos** | Si la tienda lo requiere, restringir también la creación al administrador |
| El personal puede exportar el reporte de mermas | El botón **Exportar** de Mermas (PDF o Excel) no está restringido por rol | Decidir si ese reporte es solo para el administrador |
| No existe recuperación de contraseña | No hay opción "olvidé mi clave"; solo un administrador puede restablecer claves | Mantener al menos **dos administradores** activos |
