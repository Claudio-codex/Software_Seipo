# Informe de pruebas de caja negra — SEIPO

**Curso:** Procesos de Software · **Sistema:** SEIPO, sistema de venta e inventario · **Fecha de ejecución:** 19/10/2026 (corridas 1 a 5), 29/10/2026 (corridas 6 a 8) y 30/10/2026 (corridas 9 a 16)

**Resultado final (corrida 16): 24 de 24 casos aprobados (100 %).** Evidencias en [`evidencias/`](evidencias/) · Defectos en [`Defectos.md`](Defectos.md) · Tabla para Excel en [`Casos_de_Prueba.csv`](Casos_de_Prueba.csv) · Diapositivas en [`Resumen_para_PPT.md`](Resumen_para_PPT.md)

## 1. Objetivo y alcance

Verificar con pruebas automatizadas que SEIPO cumple las reglas de negocio de `Documentacion_Proyecto_Seipo_2.docx` en sus procesos principales: acceso y roles, turno de caja, venta con IGV, descuento VIP, FEFO, forma de pago, comprobantes, anulación (por el administrador y por el personal en su propio turno, con registro del motivo), mermas, compras, cierre de caja y reportes. Se ejecutaron 24 casos sobre la lógica de negocio (controladores) y la base de datos. La interfaz gráfica solo se usó para comprobar qué módulos ve cada rol.

## 2. Estrategia de pruebas

- **Tipo de prueba:** caja negra. Cada resultado esperado se fijó con la regla de negocio del documento, no con lo que hace el código. Si el código y el documento no coinciden, la diferencia se reporta como defecto. El código de producción solo se modificó para corregir DEF-01 (después de la corrida 1) y las observaciones OBS-02 a OBS-05 (antes de la corrida 4). Antes de la corrida 5 se quitó la pasarela de pago simulada por decisión del equipo (CAM-01), y CP-02, CP-09 y CP-11 se ajustaron a ese cambio. Antes de la corrida 6 se cifraron las contraseñas (OBS-06); ningún caso cambió. Antes de la corrida 9 se quitó el redondeo del total a 10 céntimos, a pedido del docente: el IGV es el 18 % de la base con 2 decimales y el total es base más IGV, para que el negocio declare exactamente el IGV cobrado. CP-05 se ajustó al nuevo resultado. Antes de la corrida 10, a pedido del docente, el personal pasó a poder anular las ventas que registró en su propio turno abierto, con un motivo de una lista y un registro de anulaciones (CAM-02, cambio de alcance). Se agregaron CP-16 a CP-19, y CP-11 usa la firma nueva de la anulación (con motivo de la lista). Antes de la corrida 12, a pedido del docente, se agregaron CP-20 a CP-23 para demostrar el orden de salida de los lotes (FEFO): desempate por orden de ingreso, lotes vencidos, productos sin vencimiento y ventas con varios lotes. El código ya cumplía esas reglas y no cambió. Antes de la corrida 13, `ControladorInventario.registrarRecepcion` pasó a rechazar los lotes ya vencidos (OBS-07) y se agregó CP-24. Antes de la corrida 16 se corrigió la lectura del PDF en CP-15 (OBS-08, defecto de la prueba, no del sistema).
- **Técnicas:**
  - **Partición en clases de equivalencia (válidas e inválidas).** Credenciales correctas, clave errada y usuario inexistente; venta con y sin turno; forma de pago admitida y no admitida; cliente con RUC, con DNI y sin documento; merma aprobada y rechazada; administrador y personal; venta propia y de otro usuario, turno abierto y cerrado (CP-17); motivo de la lista y fuera de ella (CP-18).
  - **Valores límite.** Stock exacto y stock + 1 (CP-08); umbral de compras del descuento VIP y una compra por debajo (CP-06); efectivo contado por debajo y por encima del esperado (CP-14); producto en el mínimo de stock (CP-13); clave con un espacio de más (CP-01); precio con céntimos sueltos para comprobar que el total no se redondea a 10 céntimos (CP-05); detalle de 4 y de 5 caracteres con el motivo "Otro" (CP-18); lote que vence hoy (se vende) y lote vencido ayer (no se vende) (CP-21); lote que vence hoy (se recibe) y lote vencido ayer (se rechaza) en una recepción (CP-24).
- **Independencia y repetibilidad:**
  - Antes de la primera prueba, la base `seipo_test` se crea desde `SEIPO/bd/seipo_db.sql`, y después de cada prueba se vuelve a cargar. Así todas empiezan con los mismos datos y la base queda como estaba.
  - Cada prueba crea sus propios productos (códigos `CP01`…`CP24`), lotes, clientes y turnos.
  - La base de producción `seipo_db` y su `config.properties` nunca se usan.

## 3. Entorno y ejecución

| Elemento | Detalle |
|---|---|
| Base de datos | MariaDB 12.3.3 (servicio de Windows), base `seipo_test`, usuario `seipo` (necesita `GRANT ALL PRIVILEGES ON seipo_test.*`) |
| Pruebas | JUnit 5.12.2 (Jupiter), jar en `SEIPO/lib-test/`; 24 casos en `SEIPO/test/seipo/pruebas/` |
| Java | JDK 25.0.3 |
| Usuarios de demostración | `admin` / `123` (administrador), `Personal1` / `890` y `personal2` / `456` (personal de tienda) |
| Cómo ejecutar | IntelliJ: configuración **Pruebas SEIPO** → ▶. Consola: `pruebas.bat` en la raíz del repositorio. Duración: unos 15 segundos |

## 4. Casos de prueba (resultado final, corrida 16)

| ID | Proceso / requisito | Entrada | Resultado esperado | Resultado obtenido | Estado | Corregido en |
|---|---|---|---|---|---|---|
| CP-01 | Acceso al sistema (roles) | `admin`/`123`; `admin`/`clave_incorrecta`; usuario inexistente; `admin`/`123 ` | La credencial correcta entra con rol administrador; las inválidas se rechazan sin revelar qué dato falló | `admin`/`123` entra como ADMINISTRADOR. Clave errada y usuario inexistente dan la misma respuesta (INCORRECTO) y no queda sesión. La clave con un espacio de más se rechaza | Pasó | — |
| CP-02 | Restricción por rol | Sesión de `Personal1` y de `admin`: módulos del menú; `Personal1` intenta crear una cuenta en Usuarios | El personal no accede a Reportes, Negocio ni Usuarios; el administrador accede a todos | Menú del personal: 7 módulos, sin Reportes, Negocio ni Usuarios. Menú del administrador: los 10. El personal recibe "Solo un administrador puede crear cuentas de usuario." y no se crea la cuenta | Pasó | Corrida 2 (DEF-01) |
| CP-03 | Apertura de turno (proceso 01) | Abrir turno con S/ 100.00; abrir un segundo turno sin cerrar el primero | El primero queda abierto; el segundo se rechaza | Turno ABIERTO con S/ 100.00; el segundo se rechazó y quedó un solo turno abierto | Pasó | — |
| CP-04 | Venta sin turno | Cobrar 1 unidad sin turno abierto | La venta se bloquea, no se registra nada y no baja el stock | Rechazada con "No tiene un turno de caja abierto". Sin cambios en ventas, detalle, lotes, comprobantes, movimientos ni stock | Pasó | — |
| CP-05 | Venta en efectivo (proceso 08) | 1 unidad a S/ 12.35, paga S/ 20.00 | Total = 12.35 + IGV 18 % (2.22) = S/ 14.57, sin redondeo a 10 céntimos; vuelto S/ 5.43; stock − 1 | Total S/ 14.57 (IGV S/ 2.22, redondeo S/ 0.00), vuelto S/ 5.43, stock de 10 a 9 | Pasó | Corrida 9 (IGV sin redondeo) |
| CP-06 | Descuento VIP | Cliente que hace su 4.ª compra (3 previas) y cliente que hace su 3.ª (2 previas), producto de S/ 100.00 | Criterio aclarado: 10 % después de 3 compras. 4.ª compra 10 % (S/ 10.00); 3.ª compra 5 % (S/ 5.00) | 4.ª compra: 10 % (S/ 10.00); 3.ª compra: 5 % (S/ 5.00) | Pasó | Corrida 3 (OBS-01, aclaración del requerimiento) |
| CP-07 | FEFO (proceso 03) | Lote que vence en 120 días (10 u.) y lote que vence en 20 días (10 u.); vender 3 | Se descuenta del lote que vence primero; el otro queda intacto | Lote de 20 días: 10 → 7; lote de 120 días: 10 (intacto). Un solo lote usado; stock = suma de lotes (17) | Pasó | — |
| CP-08 | Stock insuficiente (valor límite) | Stock 4 (2 lotes de 2): pedir 5 y luego 4 | Con 5 se rechaza sin registros parciales (rollback); con 4 procede | 5: rechazada, sin ningún registro. 4: registrada, stock 0 y ambos lotes en 0 | Pasó | — |
| CP-09 | Forma de pago (proceso 15) | Tarjeta 4111 1111 1111 1111; forma de pago "Transferencia"; 1 unidad de S/ 10.00 con Yape o Plin | La tarjeta (retirada de esta versión) y cualquier forma que no sea Efectivo o Yape o Plin se rechazan sin registros. Yape o Plin se registra como forma de pago, sin efectivo recibido, con comprobante, y suma en el turno sin cambiar el efectivo en caja | Tarjeta y "Transferencia" rechazadas sin registros. Venta con Yape o Plin por S/ 11.80, sin efectivo recibido y con comprobante. Turno: Yape o Plin S/ 11.80, efectivo en caja S/ 100.00. No existe la tabla de la pasarela | Pasó | Corrida 5 (CAM-01) |
| CP-10 | Comprobante | Venta a cliente con RUC, con DNI y sin cliente | RUC: factura serie F; DNI o sin documento: boleta serie B; numeración correlativa de 1 en 1 | Factura F001-00001149 (última era 1148); boletas B001-00006694 y B001-00006695 (última era 6693); con código de verificación | Pasó | — |
| CP-11 | Anulación de venta | Como administrador, anular una venta en efectivo (5 u. de 2 lotes) y una con Plin (5 u.) | Stock vuelve a los mismos lotes; la venta queda anulada y sale del cuadre del turno | Lotes de vuelta a 3 y 10 u., stock 13, ambas ventas ANULADA, total del turno y Yape o Plin en S/ 0.00, 3 movimientos de anulación (uno por lote) | Pasó | Corrida 5 (CAM-01); corrida 10 (CAM-02, motivo de la lista) |
| CP-12 | Mermas (proceso 13) | `Personal1` registra 2 mermas (3 u. y 2 u.); intenta aprobar una; `admin` aprueba la primera y rechaza la segunda | Nace pendiente sin descontar stock; al aprobar descuenta y registra al autorizador; al rechazar no descuenta | Nace PENDIENTE con stock y lote intactos (10). El personal no puede aprobarla. Al aprobar: stock y lote 7, con autorizador y fecha. Al rechazar: stock sigue en 7 | Pasó | — |
| CP-13 | Alertas y orden de compra (procesos 09-14) | Producto con mínimo 10, máximo 50 y stock 4; `Personal1` crea la orden; `admin` la aprueba | Aparece la alerta de reposición; la orden se crea y la aprueba el administrador | Alerta "Stock bajo" con cantidad sugerida. Orden PENDIENTE; el personal no puede aprobarla y el administrador sí (APROBADA). Las unidades pasan a "en pedido" | Pasó | — |
| CP-14 | Cierre de turno (proceso 12) | Turno 1: S/ 100.00 inicial + S/ 11.80, cuenta S/ 110.00. Turno 2: S/ 50.00 + S/ 11.80, cuenta S/ 62.30. Luego intentar vender | Faltante y sobrante por la diferencia exacta; el turno queda cerrado y no admite ventas | Turno 1: esperado S/ 111.80, faltante −S/ 1.80. Turno 2: esperado S/ 61.80, sobrante +S/ 0.50. Ambos CERRADO; la venta posterior se rechazó sin registros | Pasó | — |
| CP-15 | Reportes y exportación (proceso 16) | Mermas del día: 1 aprobada (4 u. × S/ 7.90), 1 rechazada, 1 pendiente; exportar a PDF y Excel | Se crean ambos archivos, no están vacíos y los totales coinciden con la base | PDF y Excel generados y no vacíos. Valor perdido S/ 31.60, 4 unidades y 3 mermas: iguales en el Excel, en el PDF y en la base. El PDF termina con las líneas de firma | Pasó | Corrida 16 (OBS-08, lectura del PDF en la prueba) |
| CP-16 | Anulación por el personal (CAM-02) | `Personal1` vende 5 u. de 2 lotes (S/ 59.00) en su turno abierto y la anula con "Cliente desistió" y un detalle | El stock vuelve a los mismos lotes, la venta queda anulada y sale del cuadre; queda un registro con usuario, rol, turno, fecha y hora, motivo, detalle y monto | Lotes de vuelta a 3 y 10 u., stock 13, venta ANULADA, total del turno S/ 0.00. Un registro con Personal1, rol PERSONAL, su turno, la hora de la anulación, "Cliente desistió", el detalle y S/ 59.00 | Pasó | Corrida 10 (CAM-02, caso nuevo) |
| CP-17 | Anulación no permitida (CAM-02) | `personal2` intenta anular una venta de `Personal1`; `Personal1` intenta anular una venta propia de su turno ya cerrado | Los dos intentos se rechazan con un mensaje claro y sin cambios | Rechazos con "Solo puede anular las ventas que usted registró en su propio turno de caja." y "Solo se pueden anular ventas de un turno abierto…". Las dos ventas siguen COMPLETADA, stock 8 y ningún registro nuevo | Pasó | Corrida 10 (CAM-02, caso nuevo) |
| CP-18 | Motivo obligatorio (CAM-02) | Anular sin motivo, con motivo vacío, con "Porque sí" (fuera de la lista), con "Otro" sin detalle y con "Otro" y 4 caracteres; luego con "Otro" y 5 caracteres | Los cinco primeros intentos se rechazan sin cambios; con 5 caracteres se acepta (valor límite) | Cinco rechazos sin cambios. Con "abcde" la venta queda ANULADA y el registro guarda el detalle | Pasó | Corrida 10 (CAM-02, caso nuevo) |
| CP-19 | Anulación por el administrador (CAM-02) | `Personal1` vende 2 u. con Yape o Plin (S/ 23.60) en su turno abierto; `admin` la anula con "Error en el cobro" sin detalle | La venta queda anulada y también queda registrada, con el rol de administrador | Venta ANULADA con el motivo en el comprobante, stock 10. Registro con admin, rol ADMINISTRADOR, "Error en el cobro", sin detalle y S/ 23.60; aparece en el registro del administrador | Pasó | Corrida 10 (CAM-02, caso nuevo) |
| CP-20 | FEFO con el mismo vencimiento (proceso 08) | Dos lotes con la misma fecha de vencimiento: entra primero CP20-Z y después CP20-A (4 u. cada uno); dos ventas de 3 u. | Sale primero el lote que entró antes, sin importar su número; al agotarse, sigue con el siguiente | Primera venta: 3 u. de CP20-Z. Segunda: 1 u. de CP20-Z y 2 de CP20-A. Quedan 0 y 2; stock 2 | Pasó | Corrida 12 (caso nuevo) |
| CP-21 | Lotes vencidos (proceso 08) | Lote vencido ayer (5 u.), lote que vence hoy (2 u.) y lote vigente (3 u.); venta de 4 u. y luego de 2 u. | El lote vencido no se vende ni se ofrece; el que vence hoy sí (valor límite); la venta que necesitaría el vencido se rechaza sin cambios | Ventas ofrece 5 u. (sin el vencido). La venta de 4 u. toma 2 del que vence hoy y 2 del vigente; la de 2 u. se rechaza ("Los lotes vencidos no se pueden vender") sin registros. El lote vencido sigue con 5 | Pasó | Corrida 12 (caso nuevo) |
| CP-22 | Productos sin vencimiento (proceso 08) | Tres lotes sin fecha de vencimiento, recibidos en el orden CP22-SEGUNDO, CP22-PRIMERO, CP22-TERCERO (3 u. cada uno); venta de 5 u. | Los lotes salen por orden de ingreso, no por su nombre | 3 u. de CP22-SEGUNDO y 2 de CP22-PRIMERO; CP22-TERCERO queda intacto; stock 4 | Pasó | Corrida 12 (caso nuevo) |
| CP-23 | Venta con varios lotes (proceso 08) | Cuatro lotes recibidos en desorden: C (vence en 90 días, 5 u.), A (10 días, 2 u.), sin vencimiento (4 u.) y B (40 días, 2 u.); venta de 10 u. | Recorre los lotes por vencimiento y deja al final el que no vence; registra de qué lote salió cada unidad en venta_lotes y en movimientos | venta_lotes: A 2, B 2, C 5 y sin vencimiento 1, en ese orden. Movimientos: una salida por lote con stock resultante 11, 9, 4 y 3. Stock 3, igual a la suma de los lotes | Pasó | Corrida 12 (caso nuevo) |
| CP-24 | Recepción de un lote vencido (proceso 03) | Guía con dos líneas: un lote vigente (6 u.) y uno vencido ayer (4 u.); luego la misma guía con un lote que vence hoy (3 u.) | La guía con el lote vencido se rechaza completa en el controlador, con mensaje claro y sin registros; el lote que vence hoy se recibe (valor límite) | Rechazo "El lote CP24-VENCIDO de … ya está vencido (venció el …). No se puede recibir." sin recepción, lotes, movimientos ni stock (tampoco la línea vigente). Luego se recibe CP24-HOY con 3 u.; stock 3 | Pasó | Corrida 13 (OBS-07, caso nuevo) |

## 5. Adaptaciones respecto al diseño original

- **CP-09:** el documento indica (Alcance de esta versión) que *"el cobro con tarjeta se retiró de esta versión"*. Por eso la tarjeta 4111 1111 1111 1111 se prueba como una forma de pago que debe rechazarse. Desde la corrida 5 el sistema solo registra la forma de pago (CAM-01): ya no hay pasarela, token ni autorización que probar, y la tarjeta terminada en 0002 ("fondos insuficientes") no aplica.
- **CP-11:** la venta "con tarjeta" se hace con Plin. Desde la corrida 5 ya no hay cobro que reembolsar: se verifica que la venta anulada salga del cuadre del turno. El dinero de Yape o Plin lo devuelve el cajero desde el celular del negocio.
- **CP-11:** desde la corrida 10 la anulación pide un motivo de la lista. CP-11 anula con "Error de registro" y "Cliente desistió" y verifica lo mismo que antes.
- **CP-16 a CP-19 (CAM-02):** prueban la regla nueva que pidió el docente: el personal anula solo las ventas que registró en su propio turno abierto, con motivo obligatorio, y cada anulación (también las del administrador) queda en la tabla `anulaciones`.

## 6. Historial de corridas

| Corrida | Fecha y hora | Aprobados | % | Fallidos | Causa del cambio respecto a la corrida anterior | Evidencia |
|---|---|---|---|---|---|---|
| 1 | 19/10/2026 14:57 | 13/15 | 86,7 % | CP-02, CP-06 | Primera ejecución | [`Resultados_Corrida_1.txt`](evidencias/Resultados_Corrida_1.txt) |
| 2 | 19/10/2026 15:04 | 14/15 | 93,3 % | CP-06 | Se corrigió DEF-01: validación de rol en 5 métodos de 3 controladores. CP-02 pasa. CP-06 queda pendiente de decisión | [`Resultados_Corrida_2.txt`](evidencias/Resultados_Corrida_2.txt) |
| 3 | 19/10/2026 15:13 | 15/15 | 100 % | — | El equipo aclaró el requerimiento VIP (OBS-01) y CP-06 se ajustó al criterio "10 % después de 3 compras". El código no cambió | [`Resultados_Corrida_3.txt`](evidencias/Resultados_Corrida_3.txt) |
| 4 | 19/10/2026 16:17 | 15/15 | 100 % | — | Regresión tras corregir OBS-02 a OBS-05 (tope del descuento manual, diálogos en español, pausa de respaldo.bat, textos de ayuda y mensaje de registro). Los 15 casos siguen aprobando | [`Resultados_Corrida_4.txt`](evidencias/Resultados_Corrida_4.txt) |
| 5 | 19/10/2026 23:55 | 15/15 | 100 % | — | Regresión tras quitar la pasarela de pago simulada (CAM-01): Yape o Plin solo se registra como forma de pago. CP-02 (la sección Pagos pasó a llamarse Negocio), CP-09 y CP-11 se ajustaron al cambio | [`Resultados_Corrida_5.txt`](evidencias/Resultados_Corrida_5.txt) |
| 6 | 29/10/2026 23:07 | 14/15 | 93,3 % | CP-15 (entorno) | Regresión tras cifrar las contraseñas (OBS-06). Las verificaciones de los 15 casos pasaron, pero JUnit marcó CP-15 como fallido al no poder borrar su carpeta temporal: otro proceso de Windows tenía abierto `mermas.pdf`. El sistema cierra el archivo; es un fallo del entorno y se repitió la corrida | [`Resultados_Corrida_6.txt`](evidencias/Resultados_Corrida_6.txt) |
| 7 | 29/10/2026 23:08 | 15/15 | 100 % | — | Repetición de la corrida 6 sin cambios en el código ni en las pruebas | [`Resultados_Corrida_7.txt`](evidencias/Resultados_Corrida_7.txt) |
| 8 | 29/10/2026 23:40 | 15/15 | 100 % | — | Corrida 8: regresión tras el cambio visual (tema claro con acento naranja, menú con iconos, tarjetas e insignias de estado). Solo cambió la capa de vista; ningún caso cambió | [`Resultados_Corrida_8.txt`](evidencias/Resultados_Corrida_8.txt) |
| 9 | 30/10/2026 19:39 | 15/15 | 100 % | — | Corrida 9: regresión tras quitar el redondeo del IGV. El total es base más IGV con 2 decimales, sin redondeo a 10 céntimos; el efectivo acepta céntimos y el vuelto es exacto. CP-05 se ajustó al nuevo resultado (total S/ 14.57, vuelto S/ 5.43); CP-09, CP-10, CP-11 y CP-14 siguen pasando con totales exactos | [`Resultados_Corrida_9.txt`](evidencias/Resultados_Corrida_9.txt) |
| 10 | 30/10/2026 20:22 | 19/19 | 100 % | — | Corrida 10: el personal puede anular las ventas de su propio turno abierto, con motivo de una lista y registro de anulaciones (CAM-02). Se agregaron CP-16 a CP-19 y CP-11 usa la firma nueva; los 15 casos anteriores siguen aprobando | [`Resultados_Corrida_10.txt`](evidencias/Resultados_Corrida_10.txt) |
| 11 | 30/10/2026 20:52 | 19/19 | 100 % | — | Corrida 11: regresión tras corregir el texto de Caja (la descripción de la sección en la cabecera menciona las anulaciones). Solo cambió un texto de la vista; ningún caso cambió. También se corrigió la advertencia sobre las claves en el Manual del Administrador | [`Resultados_Corrida_11.txt`](evidencias/Resultados_Corrida_11.txt) |
| 12 | 30/10/2026 20:59 | 23/23 | 100 % | — | Corrida 12: se agregaron CP-20 a CP-23 para demostrar el orden de salida de los lotes (mismo vencimiento por orden de ingreso, lotes vencidos, productos sin vencimiento y venta con varios lotes). El código ya cumplía las reglas y no cambió; los 19 casos anteriores siguen aprobando | [`Resultados_Corrida_12.txt`](evidencias/Resultados_Corrida_12.txt) |
| 13 | 30/10/2026 21:13 | 23/24 | 95,8 % | CP-15 (limpieza) | El controlador rechaza la recepción de lotes vencidos (OBS-07) y se agrega CP-24, que pasa. CP-15 pasó sus verificaciones, pero JUnit no pudo borrar su carpeta temporal porque `mermas.pdf` seguía abierto | [`Resultados_Corrida_13.txt`](evidencias/Resultados_Corrida_13.txt) |
| 14 | 30/10/2026 21:14 | 23/24 | 95,8 % | CP-15 (limpieza) | Repetición de la corrida 13 sin cambios; el mismo fallo de CP-15 | [`Resultados_Corrida_14.txt`](evidencias/Resultados_Corrida_14.txt) |
| 15 | 30/10/2026 21:15 | 23/24 | 95,8 % | CP-15 (limpieza) | CP-15 reintentó borrar sus archivos durante 10 s y el PDF siguió bloqueado: no era un bloqueo breve de otro proceso. Se investigó la causa (OBS-08) | [`Resultados_Corrida_15.txt`](evidencias/Resultados_Corrida_15.txt) |
| 16 | 30/10/2026 21:17 | **24/24** | **100 %** | — | Corrida 16: CP-15 lee el PDF desde sus bytes (OBS-08) y se quitan los reintentos. Los 24 casos aprueban, incluido CP-24 | [`Resultados_Corrida_16.txt`](evidencias/Resultados_Corrida_16.txt) |

Cada corrida duró unos 15 segundos (14,9 s, 14,5 s, 14,5 s, 15,3 s, 15 s, 16,1 s, 14,3 s, 16,1 s, 14,9 s, 18,8 s, 18,4 s, 22,4 s, 24,6 s, 29,8 s, 36,1 s y 26,6 s; las corridas 10 y 11 tienen 19 casos, la 12 tiene 23 y de la 13 en adelante 24; la 15 incluye 10 s de reintentos), incluida la recarga de `seipo_test` antes de la primera prueba y después de cada una.

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
| CAM-01 | CP-02, CP-09, CP-11 | Cambio de alcance, no defecto: el equipo decidió quitar la pasarela de pago simulada, porque el sistema solo registra la forma de pago. El cajero confirma el pago con Yape o Plin en el celular del negocio; la sección Pagos pasó a ser Negocio (solo los datos del comprobante) | — | Aplicado y verificado (corrida 5) |
| OBS-06 | CP-01 | Las contraseñas se guardaban en texto plano y quedaban visibles en la base, en los respaldos y en el dump. Ahora se guardan con PBKDF2-HMAC-SHA512 y sal aleatoria; las claves antiguas se cifran solas y siguen entrando | Media | Corregido y verificado (corridas 6 y 7) |
| CAM-02 | CP-11, CP-16 a CP-19 | Cambio de alcance pedido por el docente, no defecto: el personal puede anular las ventas que registró en su propio turno abierto (antes solo el administrador). El motivo se elige de una lista (con "Otro", detalle obligatorio) y cada anulación queda en la tabla `anulaciones` con usuario, rol, turno, fecha y hora, motivo, detalle y monto. La regla se valida en el controlador | — | Aplicado y verificado (corrida 10) |
| OBS-07 | CP-24 | Recepción de lotes vencidos validada solo en la pantalla: `ControladorInventario.registrarRecepcion` aceptaba un lote vencido si se llamaba sin pasar por Recepción o Compras. Ahora el controlador lo rechaza antes de grabar nada | Baja | Corregido y verificado (corrida 16) |
| OBS-08 | CP-15 | Fallo intermitente de CP-15 atribuido a otro proceso de Windows (corridas 6, 13 y 14). La causa era la propia prueba: `new PdfReader(ruta)` mapea el PDF en memoria y Windows no deja borrarlo hasta que se libera el mapeo, aunque se cierre el lector. La prueba ahora lee el PDF desde sus bytes. Defecto de la prueba, no del sistema | Baja | Corregido y verificado (corrida 16) |

OBS-02 a OBS-05 se encontraron al revisar el código para escribir los manuales de usuario, no con los casos automatizados. OBS-06 venía de la recomendación 4 de este informe. Las limitaciones que se dejan como están (creación de productos y proveedores por el personal, exportación del reporte de mermas y falta de recuperación de contraseña) se describen en [`Defectos.md`](Defectos.md#limitaciones-conocidas).

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

**Corrida 5 (CAM-01)**
- La aplicación compila sin `ControladorPasarela` ni `DialogoPasarela`.
- Al iniciar, `ConexionBD` borra la tabla `transacciones_pago` y las claves `pasarela_*` de `configuracion` en las bases creadas con versiones anteriores. El dump `SEIPO/bd/seipo_db.sql` ya no las incluye.

**Corridas 6 y 7 (OBS-06)**
- **Cifrado:** la clave correcta entra; se rechazan la clave con un espacio de más y con otras mayúsculas; dos cifrados de la misma clave son distintos (sal) y un valor mal formado se rechaza sin error. Cada verificación tarda unos 140 ms.
- **Base de una versión anterior (contra `seipo_test`):** con `admin` y `Personal1` en texto plano, la primera conexión los cifra y no queda ninguna clave sin cifrar; ambos entran con su clave de siempre. Una clave plana restaurada a mitad de la ejecución entra una vez y queda cifrada; un intento con la clave errada no cambia nada.
- **Usuarios:** el usuario nuevo y la clave restablecida se guardan cifrados; después de restablecerla, la clave anterior ya no entra.
- **Instalación nueva:** con `seipo_test` borrada, `ConexionBD` la crea y el `admin` inicial queda cifrado; `admin` / `123` entra.
- El dump `SEIPO/bd/seipo_db.sql` solo cambió en las tres claves de los usuarios de demostración, que siguen siendo `123`, `890` y `456`.

**Corrida 10 (CAM-02)**
- **Pantalla (contra `seipo_test`, como `Personal1`):** en **Caja** → **Mi turno**, **Anular venta** se habilita sobre su venta completada y queda desactivado sobre la anulada. La pestaña **Anulaciones** muestra solo sus anulaciones, con período, motivo como insignia, detalle y monto, y las tarjetas de número y monto anulado.
- **Bases existentes:** `ConexionBD` crea la tabla `anulaciones` al iniciar ("crear si no existe"). El dump `SEIPO/bd/seipo_db.sql` la incluye vacía y se importa sin errores (las pruebas lo cargan antes de cada caso). Las 48 ventas anuladas antes del cambio no tienen registro y su comprobante sigue mostrando ANULADO y el motivo.

## 8. Conclusiones y recomendaciones

**Conclusiones**
- **Resultado final:** los 24 casos aprueban (100 %). Los procesos centrales funcionan según el documento: venta con IGV exacto, FEFO, rollback por stock insuficiente, bloqueo sin turno, comprobantes correlativos, anulación con devolución al mismo lote, mermas con autorización, alertas y órdenes de compra, cuadre de caja y reporte de mermas.
- **Integridad transaccional:** en todos los rechazos probados (sin turno, stock insuficiente, forma de pago no admitida, turno cerrado y tarjeta) no quedaron registros parciales.
- **Defecto de seguridad:** el control de acceso dependía solo de la interfaz (DEF-01). Se corrigió en el controlador y quedó cubierto por CP-02.
- **Requerimiento ambiguo:** las pruebas detectaron uno (OBS-01), que se resolvió aclarando el documento sin cambiar el código.
- **Hallazgos menores:** la revisión del código para los manuales encontró cuatro observaciones (OBS-02 a OBS-05), que se corrigieron. La corrida 4 confirmó que no rompieron nada (15/15).
- **Cambio de alcance:** la corrida 5 confirmó que quitar la pasarela simulada (CAM-01) no afectó a los demás procesos (15/15).
- **Contraseñas:** desde la corrida 6 se guardan cifradas (OBS-06). La corrida 7 confirmó que el acceso y los demás procesos siguen funcionando (15/15).
- **IGV sin redondeo:** desde la corrida 9 el total es base más IGV con 2 decimales, sin redondeo a 10 céntimos, para que el negocio declare exactamente el IGV cobrado. La corrida 9 confirmó que los 15 casos siguen aprobando (15/15).
- **Anulación por el personal:** desde la corrida 10 el personal anula las ventas de su propio turno abierto, con motivo obligatorio y registro de cada anulación (CAM-02). La regla se valida en el controlador; los intentos no permitidos se rechazan sin cambios (CP-17 y CP-18). La corrida 10 dio 19/19.
- **Orden de salida de los lotes:** la corrida 12 demostró con CP-20 a CP-23 que las ventas salen por vencimiento, desempatan por orden de ingreso, no venden lotes vencidos (el que vence hoy sí), dejan al final los lotes sin vencimiento y registran cada lote usado en `venta_lotes` y en `movimientos`. El código ya lo cumplía.
- **Recepción de lotes vencidos:** la regla existía solo en las pantallas; desde la corrida 13 también la valida el controlador (OBS-07, CP-24).
- **Fallo intermitente de CP-15:** no era otro proceso de Windows, sino la propia prueba, que mapeaba el PDF en memoria al leerlo. Se corrigió la prueba (OBS-08) y la corrida 16 dio 24/24. Las corridas 6, 13, 14 y 15 quedan como evidencia del problema.

**Recomendaciones**
1. Mantener la validación de rol en cada controlador nuevo que gestione información administrativa.
2. Alinear la redacción de los otros tramos del descuento VIP (5 % y 15 %) con el criterio aclarado ("después de N compras").
3. Actualizar la sección "Diseño de la Interfaz de Usuario" del documento, que aún menciona la tarjeta como forma de pago.
4. ~~Guardar las contraseñas con hash~~: aplicada en la corrida 6 (OBS-06).
5. Ejecutar `pruebas.bat` antes de cada entrega para detectar regresiones.
6. Decidir con el equipo las limitaciones conocidas (ver `Defectos.md`) y mantener al menos dos administradores activos.
