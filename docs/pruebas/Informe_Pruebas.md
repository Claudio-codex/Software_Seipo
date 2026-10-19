# Informe de pruebas de caja negra — SEIPO

**Curso:** Procesos de Software · **Sistema:** SEIPO, sistema de venta e inventario · **Fecha de ejecución:** 19/10/2026

**Resultado final (corrida 4): 15 de 15 casos aprobados (100 %).** Evidencias en [`evidencias/`](evidencias/) · Defectos en [`Defectos.md`](Defectos.md) · Tabla para Excel en [`Casos_de_Prueba.csv`](Casos_de_Prueba.csv) · Diapositivas en [`Resumen_para_PPT.md`](Resumen_para_PPT.md)

## 1. Objetivo y alcance

Verificar con pruebas automatizadas que SEIPO cumple las reglas de negocio de `Documentacion_Proyecto_Seipo_2.docx` en sus procesos principales: acceso y roles, turno de caja, venta con IGV y redondeo, descuento VIP, FEFO, cobro digital, comprobantes, anulación, mermas, compras, cierre de caja y reportes. Se ejecutaron 15 casos sobre la lógica de negocio (controladores) y la base de datos. La interfaz gráfica solo se usó para comprobar qué módulos ve cada rol.

## 2. Estrategia de pruebas

- **Tipo de prueba:** caja negra. Cada resultado esperado se fijó con la regla de negocio del documento, no con lo que hace el código. Si el código y el documento no coinciden, la diferencia se reporta como defecto. El código de producción solo se modificó para corregir DEF-01 (después de la corrida 1) y las observaciones OBS-02 a OBS-05 (antes de la corrida 4).
- **Técnicas:**
  - **Partición en clases de equivalencia (válidas e inválidas).** Credenciales correctas, clave errada y usuario inexistente; venta con y sin turno; cobro aprobado y rechazado; cliente con RUC, con DNI y sin documento; merma aprobada y rechazada; administrador y personal.
  - **Valores límite.** Stock exacto y stock + 1 (CP-08); umbral de compras del descuento VIP y una compra por debajo (CP-06); efectivo contado por debajo y por encima del esperado (CP-14); producto en el mínimo de stock (CP-13); clave con un espacio de más (CP-01); precio elegido para que actúe el redondeo a 10 céntimos (CP-05).
- **Independencia y repetibilidad:**
  - Antes de la primera prueba, la base `seipo_test` se crea desde `SEIPO/bd/seipo_db.sql`, y después de cada prueba se vuelve a cargar. Así todas empiezan con los mismos datos y la base queda como estaba.
  - Cada prueba crea sus propios productos (códigos `CP01`…`CP15`), lotes, clientes y turnos.
  - La base de producción `seipo_db` y su `config.properties` nunca se usan.

## 3. Entorno y ejecución

| Elemento | Detalle |
|---|---|
| Base de datos | MariaDB 12.3.3 (servicio de Windows), base `seipo_test`, usuario `seipo` (necesita `GRANT ALL PRIVILEGES ON seipo_test.*`) |
| Pruebas | JUnit 5.12.2 (Jupiter), jar en `SEIPO/lib-test/`; 15 casos en `SEIPO/test/seipo/pruebas/` |
| Java | JDK 25.0.3 |
| Usuarios de demostración | `admin` / `123` (administrador) y `Personal1` / `890` (personal de tienda) |
| Cómo ejecutar | IntelliJ: configuración **Pruebas SEIPO** → ▶. Consola: `pruebas.bat` en la raíz del repositorio. Duración: unos 15 segundos |

## 4. Casos de prueba (resultado final, corrida 4)

| ID | Proceso / requisito | Entrada | Resultado esperado | Resultado obtenido | Estado | Corregido en |
|---|---|---|---|---|---|---|
| CP-01 | Acceso al sistema (roles) | `admin`/`123`; `admin`/`clave_incorrecta`; usuario inexistente; `admin`/`123 ` | La credencial correcta entra con rol administrador; las inválidas se rechazan sin revelar qué dato falló | `admin`/`123` entra como ADMINISTRADOR. Clave errada y usuario inexistente dan la misma respuesta (INCORRECTO) y no queda sesión. La clave con un espacio de más se rechaza | Pasó | — |
| CP-02 | Restricción por rol | Sesión de `Personal1` y de `admin`: módulos del menú; `Personal1` intenta crear una cuenta en Usuarios | El personal no accede a Reportes, Pagos ni Usuarios; el administrador accede a todos | Menú del personal: 7 módulos, sin Reportes, Pagos ni Usuarios. Menú del administrador: los 10. El personal recibe "Solo un administrador puede crear cuentas de usuario." y no se crea la cuenta | Pasó | Corrida 2 (DEF-01) |
| CP-03 | Apertura de turno (proceso 01) | Abrir turno con S/ 100.00; abrir un segundo turno sin cerrar el primero | El primero queda abierto; el segundo se rechaza | Turno ABIERTO con S/ 100.00; el segundo se rechazó y quedó un solo turno abierto | Pasó | — |
| CP-04 | Venta sin turno | Cobrar 1 unidad sin turno abierto | La venta se bloquea, no se registra nada y no baja el stock | Rechazada con "No tiene un turno de caja abierto". Sin cambios en ventas, detalle, lotes, comprobantes, movimientos ni stock | Pasó | — |
| CP-05 | Venta en efectivo (proceso 08) | 1 unidad a S/ 12.35, paga S/ 20.00 | Total = (12.35 + IGV 18 %) redondeado hacia abajo a 10 céntimos = S/ 14.50; vuelto S/ 5.50; stock − 1 | Total S/ 14.50 (IGV S/ 2.22, redondeo −S/ 0.07), vuelto S/ 5.50, stock de 10 a 9 | Pasó | — |
| CP-06 | Descuento VIP | Cliente que hace su 4.ª compra (3 previas) y cliente que hace su 3.ª (2 previas), producto de S/ 100.00 | Criterio aclarado: 10 % después de 3 compras. 4.ª compra 10 % (S/ 10.00); 3.ª compra 5 % (S/ 5.00) | 4.ª compra: 10 % (S/ 10.00); 3.ª compra: 5 % (S/ 5.00) | Pasó | Corrida 3 (OBS-01, aclaración del requerimiento) |
| CP-07 | FEFO (proceso 03) | Lote que vence en 120 días (10 u.) y lote que vence en 20 días (10 u.); vender 3 | Se descuenta del lote que vence primero; el otro queda intacto | Lote de 20 días: 10 → 7; lote de 120 días: 10 (intacto). Un solo lote usado; stock = suma de lotes (17) | Pasó | — |
| CP-08 | Stock insuficiente (valor límite) | Stock 4 (2 lotes de 2): pedir 5 y luego 4 | Con 5 se rechaza sin registros parciales (rollback); con 4 procede | 5: rechazada, sin ningún registro. 4: registrada, stock 0 y ambos lotes en 0 | Pasó | — |
| CP-09 | Pago digital (proceso 15) | Tarjeta 4111 1111 1111 1111; Yape con celular 987654321 y código 123456; Yape con código 000000 | La tarjeta (retirada de esta versión) se rechaza. El cobro aprobado genera token y autorización. El cobro rechazado no confirma la venta | Tarjeta rechazada sin registros. Yape aprobado: token `tok_…`, autorización de 6 dígitos, venta vinculada y solo los 3 últimos dígitos del celular guardados. Código 000000: RECHAZADA con su motivo, sin venta | Pasó | — |
| CP-10 | Comprobante | Venta a cliente con RUC, con DNI y sin cliente | RUC: factura serie F; DNI o sin documento: boleta serie B; numeración correlativa de 1 en 1 | Factura F001-00001149 (última era 1148); boletas B001-00006694 y B001-00006695 (última era 6693); con código de verificación | Pasó | — |
| CP-11 | Anulación de venta | Como administrador, anular una venta en efectivo (5 u. de 2 lotes) y una con Plin (5 u.) | Stock vuelve a los mismos lotes; la venta queda anulada; el cobro digital queda reembolsado | Lotes de vuelta a 3 y 10 u., stock 13, ambas ventas ANULADA, cobro REEMBOLSADA, 3 movimientos de anulación (uno por lote) | Pasó | — |
| CP-12 | Mermas (proceso 13) | `Personal1` registra 2 mermas (3 u. y 2 u.); intenta aprobar una; `admin` aprueba la primera y rechaza la segunda | Nace pendiente sin descontar stock; al aprobar descuenta y registra al autorizador; al rechazar no descuenta | Nace PENDIENTE con stock y lote intactos (10). El personal no puede aprobarla. Al aprobar: stock y lote 7, con autorizador y fecha. Al rechazar: stock sigue en 7 | Pasó | — |
| CP-13 | Alertas y orden de compra (procesos 09-14) | Producto con mínimo 10, máximo 50 y stock 4; `Personal1` crea la orden; `admin` la aprueba | Aparece la alerta de reposición; la orden se crea y la aprueba el administrador | Alerta "Stock bajo" con cantidad sugerida. Orden PENDIENTE; el personal no puede aprobarla y el administrador sí (APROBADA). Las unidades pasan a "en pedido" | Pasó | — |
| CP-14 | Cierre de turno (proceso 12) | Turno 1: S/ 100.00 inicial + S/ 11.80, cuenta S/ 110.00. Turno 2: S/ 50.00 + S/ 11.80, cuenta S/ 62.30. Luego intentar vender | Faltante y sobrante por la diferencia exacta; el turno queda cerrado y no admite ventas | Turno 1: esperado S/ 111.80, faltante −S/ 1.80. Turno 2: esperado S/ 61.80, sobrante +S/ 0.50. Ambos CERRADO; la venta posterior se rechazó sin registros | Pasó | — |
| CP-15 | Reportes y exportación (proceso 16) | Mermas del día: 1 aprobada (4 u. × S/ 7.90), 1 rechazada, 1 pendiente; exportar a PDF y Excel | Se crean ambos archivos, no están vacíos y los totales coinciden con la base | PDF y Excel generados y no vacíos. Valor perdido S/ 31.60, 4 unidades y 3 mermas: iguales en el Excel, en el PDF y en la base. El PDF termina con las líneas de firma | Pasó | — |

## 5. Adaptaciones respecto al diseño original

- **CP-09:** el documento indica (Alcance de esta versión) que *"el cobro con tarjeta se retiró de esta versión"*. Por eso:
  - La tarjeta 4111 1111 1111 1111 se prueba como una forma de pago que debe rechazarse.
  - La autorización aprobada y el rechazo se prueban con Yape, que pasa por la pasarela simulada.
  - Para el rechazo se usa el código 000000 de la pasarela de prueba, porque la tarjeta terminada en 0002 ("fondos insuficientes") no existe en el sistema.
- **CP-11:** la venta "con tarjeta" se hace con Plin; la anulación debe dejar ese cobro reembolsado.

## 6. Historial de corridas

| Corrida | Fecha y hora | Aprobados | % | Fallidos | Causa del cambio respecto a la corrida anterior | Evidencia |
|---|---|---|---|---|---|---|
| 1 | 19/10/2026 14:57 | 13/15 | 86,7 % | CP-02, CP-06 | Primera ejecución | [`Resultados_Corrida_1.txt`](evidencias/Resultados_Corrida_1.txt) |
| 2 | 19/10/2026 15:04 | 14/15 | 93,3 % | CP-06 | Se corrigió DEF-01: validación de rol en 5 métodos de 3 controladores. CP-02 pasa. CP-06 queda pendiente de decisión | [`Resultados_Corrida_2.txt`](evidencias/Resultados_Corrida_2.txt) |
| 3 | 19/10/2026 15:13 | 15/15 | 100 % | — | El equipo aclaró el requerimiento VIP (OBS-01) y CP-06 se ajustó al criterio "10 % después de 3 compras". El código no cambió | [`Resultados_Corrida_3.txt`](evidencias/Resultados_Corrida_3.txt) |
| 4 | 19/10/2026 16:17 | **15/15** | **100 %** | — | Regresión tras corregir OBS-02 a OBS-05 (tope del descuento manual, diálogos en español, pausa de respaldo.bat, textos de ayuda y mensaje de registro). Los 15 casos siguen aprobando | [`Resultados_Corrida_4.txt`](evidencias/Resultados_Corrida_4.txt) |

Cada corrida duró unos 15 segundos (14,9 s, 14,5 s, 14,5 s y 15,3 s), incluida la recarga de `seipo_test` antes de la primera prueba y después de cada una.

## 7. Defectos y observaciones

El detalle completo, con pasos para reproducir y esperado contra obtenido, está en [`Defectos.md`](Defectos.md).

| ID | Caso | Descripción | Severidad | Estado |
|---|---|---|---|---|
| DEF-01 | CP-02 | La restricción por rol existía solo en la interfaz; el personal podía crear una cuenta de administrador. Se corrigió en la corrida 2 validando el rol en `ControladorUsuarios.registrarUsuario`, `actualizarUsuario` y `cambiarPassword`, `ControladorReportes.obtenerReporte` y `ControladorPasarela.listarTransacciones` (10 líneas en 3 controladores) | Alta | Corregido y verificado |
| OBS-01 | CP-06 | Ambigüedad del requerimiento: "10 % desde la tercera compra" se podía leer como la 3.ª compra, mientras el sistema lo da desde la 4.ª. Se resolvió aclarando el documento ("10 % después de 3 compras, a partir de la 4.ª compra"); el código no cambió | Media | Resuelto (aclaración del requerimiento) |
| OBS-02 | — | Descuento manual (F6) sin tope para el personal. Ahora el personal puede aplicar como máximo 10 %; se valida en la pantalla y en el controlador, y el VIP automático no cambia | Media | Corregido y verificado (corrida 4) |
| OBS-03 | — | Botones de los diálogos en inglés (OK, Cancel, Yes, No, Save). Ahora aparecen en español mediante `UIManager` | Baja | Corregido y verificado (corrida 4) |
| OBS-04 | — | `respaldo.bat` se cerraba sin mostrar el resultado. Ahora pausa al final; el Programador de tareas lo ejecuta con `/sinpausa` | Baja | Corregido y verificado (corrida 4) |
| OBS-05 | — | Textos inexactos: a la ayuda de Usuarios le faltaban Caja y Pagos, y el mensaje de registro de clientes mencionaba una "categoría inicial" | Baja | Corregido y verificado (corrida 4) |

OBS-02 a OBS-05 se encontraron al revisar el código para escribir los manuales de usuario, no con los casos automatizados. Las limitaciones que se dejan como están (creación de productos y proveedores por el personal, exportación del reporte de mermas y falta de recuperación de contraseña) se describen en [`Defectos.md`](Defectos.md#limitaciones-conocidas).

### Verificaciones adicionales

**Corrida 2 (DEF-01)**
- Los 5 métodos protegidos rechazan al personal con un mensaje claro y siguen funcionando para el administrador.
- La aplicación compila. Se compiló `SEIPO/src` sin JUnit y sin errores.
- El login y la venta en efectivo funcionan con `Personal1` y con `admin`: ambos iniciaron sesión, abrieron turno y cobraron una venta.
- `seipo_db` quedó intacta: 7 841 ventas y 3 usuarios.

**Corrida 4 (OBS-02 a OBS-05)**
- **Tope del descuento (contra `seipo_test`):** personal con 10 %, se registra; personal con 10,5 %, se rechaza; personal con 15 % y cliente Platino, se registra; personal con 20 % y cliente Platino, se rechaza; administrador con 30 %, se registra.
- **Diálogos:** con `Locale.US`, un diálogo de Sí/No/Cancelar muestra "Sí | No | Cancelar", y el selector de archivos muestra "Guardar".
- **`respaldo.bat`:** sin argumento pausa y sale con 0; con `/sinpausa` no pausa y sale con 0; sin `config.properties` muestra el error, pausa y sale con 1.
- La aplicación compila y `seipo_db` sigue intacta (7 841 ventas y 3 usuarios).

## 8. Conclusiones y recomendaciones

**Conclusiones**
- **Resultado final:** los 15 casos aprueban (100 %). Los procesos centrales funcionan según el documento: venta con IGV y redondeo, FEFO, rollback por stock insuficiente, bloqueo sin turno, comprobantes correlativos, anulación con devolución al mismo lote, mermas con autorización, alertas y órdenes de compra, cuadre de caja y reporte de mermas.
- **Integridad transaccional:** en todos los rechazos probados (sin turno, stock insuficiente, cobro rechazado, turno cerrado y tarjeta) no quedaron registros parciales.
- **Defecto de seguridad:** el control de acceso dependía solo de la interfaz (DEF-01). Se corrigió en el controlador y quedó cubierto por CP-02.
- **Requerimiento ambiguo:** las pruebas detectaron uno (OBS-01), que se resolvió aclarando el documento sin cambiar el código.
- **Hallazgos menores:** la revisión del código para los manuales encontró cuatro observaciones (OBS-02 a OBS-05), que se corrigieron. La corrida 4 confirmó que no rompieron nada (15/15).

**Recomendaciones**
1. Mantener la validación de rol en cada controlador nuevo que gestione información administrativa.
2. Alinear la redacción de los otros tramos del descuento VIP (5 % y 15 %) con el criterio aclarado ("después de N compras").
3. Actualizar la sección "Diseño de la Interfaz de Usuario" del documento, que aún menciona la tarjeta como forma de pago.
4. Guardar las contraseñas con hash; hoy están en texto plano. Esto quedó fuera del alcance de estas pruebas.
5. Ejecutar `pruebas.bat` antes de cada entrega para detectar regresiones.
6. Decidir con el equipo las limitaciones conocidas (ver `Defectos.md`) y mantener al menos dos administradores activos.
