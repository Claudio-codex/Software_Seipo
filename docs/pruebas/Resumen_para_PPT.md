# Resumen para la presentación — Pruebas de caja negra de SEIPO

Detalle completo en [`Informe_Pruebas.md`](Informe_Pruebas.md) y [`Defectos.md`](Defectos.md).

---

## Diapositiva 1 — Objetivo
- Verificar que SEIPO cumple las reglas de negocio de su documentación
- 24 casos de prueba automatizados con JUnit 5
- Cubren acceso, caja, ventas, FEFO, forma de pago, comprobantes, mermas, compras y reportes
- Esperado = lo que dice el documento; si el código difiere, es un defecto
- Resultado final: 24 de 24 casos aprobados (100 %)

---

## Diapositiva 2 — Estrategia y técnicas
- Prueba de **caja negra**: entradas y salidas observables, sin mirar la implementación
- **Partición en clases de equivalencia**: válidas e inválidas (credenciales, turno, forma de pago, documento, rol)
- **Valores límite**: stock exacto y stock + 1, umbral VIP, faltante y sobrante de caja, stock mínimo
- Cada prueba crea sus propios datos y deja la base como la encontró
- Verificación directa en la base: ventas, lotes, stock, comprobantes y movimientos

---

## Diapositiva 3 — Entorno
- MariaDB 12.3 como servicio de Windows
- Base aislada **`seipo_test`**, creada desde `seipo_db.sql` y recargada después de cada prueba
- JUnit 5.12.2 (`SEIPO/lib-test/`) con JDK 25
- Usuarios de demostración: `admin` / `123` y `Personal1` / `890`
- Ejecución: botón **Pruebas SEIPO** en IntelliJ o `pruebas.bat` (unos 15 s)

---

## Diapositiva 4 — Casos de prueba (1/2)
- **CP-01 Acceso** y **CP-02 Roles:** credenciales y menú por rol; el personal ya no crea cuentas → Pasó
- **CP-03 Apertura de turno** y **CP-04 Venta sin turno** (bloqueada, sin registros) → Pasó
- **CP-05 Venta en efectivo:** S/ 12.35 + IGV S/ 2.22 = S/ 14.57 exacto, vuelto S/ 5.43 → Pasó
- **CP-06 Descuento VIP:** 5 % en la 3.ª compra y 10 % desde la 4.ª (criterio aclarado) → Pasó
- **CP-07 FEFO** (sale el lote que vence primero) y **CP-08 Stock límite** (5 rechazado, 4 aceptado) → Pasó

---

## Diapositiva 5 — Casos de prueba (2/2)
- **CP-09 Forma de pago:** tarjeta y otras formas rechazadas; Yape o Plin solo se registra y suma en el turno → Pasó
- **CP-10 Comprobante:** RUC → factura F001; DNI o sin documento → boleta B001; correlativo +1 → Pasó
- **CP-11 Anulación:** stock a los mismos lotes, venta anulada y fuera del cuadre del turno → Pasó
- **CP-12 Mermas** (pendiente, aprobada o rechazada) y **CP-13 Alertas y orden de compra** → Pasó
- **CP-14 Cierre de caja** (−S/ 1.80 y +S/ 0.50 exactos) y **CP-15 Reporte PDF y Excel = base de datos** → Pasó

---

## Diapositiva 6 — Casos nuevos: anulación por el personal (CAM-02)
- **CP-16:** el personal anula una venta propia de su turno abierto; stock a los lotes y registro con usuario, fecha, motivo y monto → Pasó
- **CP-17:** venta de otro usuario o de un turno cerrado: rechazada con mensaje claro y sin cambios → Pasó
- **CP-18:** sin motivo, fuera de la lista u "Otro" con 4 caracteres: rechazado; con 5 caracteres se acepta → Pasó
- **CP-19:** el administrador anula una venta y también queda registrada → Pasó
- La regla se valida en el controlador, no solo en la pantalla (como DEF-01)

---

## Diapositiva 7 — Casos nuevos: orden de salida de los lotes (FEFO)
- **CP-20:** mismo vencimiento → sale primero el lote que entró antes, aunque su número sea mayor → Pasó
- **CP-21:** el lote vencido no se vende ni se ofrece; el que vence hoy sí (valor límite) → Pasó
- **CP-22:** productos sin vencimiento → sus lotes salen por orden de ingreso → Pasó
- **CP-23:** venta con 4 lotes → los recorre en orden y registra cada lote en venta_lotes y movimientos → Pasó
- El código ya cumplía estas reglas: las pruebas lo dejan demostrado (CP-07 ya probaba el vencimiento más próximo)
- **CP-24:** una recepción con un lote vencido se rechaza en el controlador, sin registros; el que vence hoy se recibe → Pasó (OBS-07)

---

## Diapositiva 8 — Historial de corridas y resultados
- **Corrida 1:** 13/15 (86,7 %); fallan CP-02 y CP-06
- **Corrida 2:** 14/15 (93,3 %); se corrige DEF-01 y pasa CP-02
- **Corrida 3:** 15/15 (100 %); se aclara el requerimiento VIP y pasa CP-06
- **Corrida 4:** 15/15 (100 %); regresión tras corregir OBS-02 a OBS-05, sin fallas
- **Corrida 5:** 15/15 (100 %); regresión tras quitar la pasarela de pago simulada (CAM-01)
- **Corrida 6:** 14/15; contraseñas cifradas (OBS-06). CP-15 falló por el entorno: otro proceso de Windows tenía abierto el PDF temporal
- **Corrida 7:** 15/15 (100 %); repetición de la corrida 6 sin cambios
- **Corrida 8:** 15/15 (100 %); regresión tras el cambio visual (tema claro)
- **Corrida 9:** 15/15 (100 %); regresión tras quitar el redondeo del IGV (total = base + IGV, con 2 decimales)
- **Corrida 10:** 19/19 (100 %); el personal anula ventas de su turno con registro del motivo (CAM-02), con CP-16 a CP-19
- **Corrida 11:** 19/19 (100 %); regresión tras corregir el texto de Caja (solo cambió un texto de la vista)
- **Corrida 12:** 23/23 (100 %); se agregan CP-20 a CP-23 sobre el orden de salida de los lotes (sin cambios en el código)
- **Corridas 13 a 15:** 23/24; el controlador rechaza lotes vencidos (OBS-07, CP-24 pasa), pero CP-15 no pudo borrar su PDF temporal
- **Corrida 16:** 24/24 (100 %); la causa era la propia prueba, que mapeaba el PDF en memoria al leerlo (OBS-08, corregido)
- **Corrida 17:** 24/24 (100 %); regresión tras ajustar la tipografía y los tamaños de la interfaz (misma suite, solo cambió la vista)
- **Corrida 18:** **24/24 (100 %)**; regresión tras pasar al español los títulos y textos de los diálogos estándar (misma suite, solo cambió la vista)
- Cada corrida dura unos 15 s; las dieciocho salidas están en `docs/pruebas/evidencias/`

---

## Diapositiva 9 — Defectos y observaciones
- **DEF-01 (alta):** el rol solo se controlaba en el menú; el personal podía crear un administrador
- Corrección: validación de rol en 5 métodos de Usuarios, Reportes y Pagos (10 líneas en 3 controladores; Pagos se quitó en la corrida 5)
- **OBS-01 (media):** "10 % desde la tercera compra" era ambiguo
- Resolución: se aclaró como "10 % después de 3 compras (a partir de la 4.ª)", sin cambiar el código
- Ambos cerrados y verificados en las corridas 2 y 3

---

## Diapositiva 10 — Observaciones menores (OBS-02 a OBS-06)
- Se encontraron al revisar el código para escribir los manuales de usuario
- **OBS-02 (media):** descuento manual sin tope; ahora el personal aplica como máximo 10 % (el VIP no cambia)
- **OBS-03 (baja):** botones de los diálogos en inglés; ahora dicen Aceptar, Cancelar, Sí, No y Guardar
- **OBS-04 y OBS-05 (bajas):** pausa al final de respaldo.bat y textos de ayuda y de registro corregidos
- Todas corregidas y verificadas; la corrida 4 sigue en 15/15
- **OBS-06 (media):** las contraseñas estaban en texto plano; ahora se guardan cifradas con PBKDF2 y sal (corridas 6 y 7)
- **OBS-07 (baja):** la recepción de lotes vencidos solo se impedía en la pantalla; ahora también en el controlador (CP-24)
- **OBS-08 (baja):** CP-15 fallaba a veces al borrar su PDF; era la propia prueba (lectura con mapeo en memoria), no otro proceso

---

## Diapositiva 11 — Limitaciones conocidas
- El personal puede crear productos y proveedores, aunque solo el administrador los edita
- El personal puede exportar el reporte de mermas
- No existe recuperación de contraseña: solo un administrador restablece claves
- Recomendación: mantener al menos dos administradores activos
- Se dejan documentadas para que el equipo decida si las restringe

---

## Diapositiva 12 — Adaptaciones respecto al diseño original
- El documento retiró la tarjeta como forma de pago en esta versión
- **CP-09:** la tarjeta 4111 1111 1111 1111 se prueba como pago que debe rechazarse
- **CP-11:** la venta "con tarjeta" se reemplazó por una venta con Plin
- **CAM-01 (corrida 5):** el equipo quitó la pasarela simulada; el sistema solo registra la forma de pago
- CP-02, CP-09 y CP-11 se ajustaron: la sección Pagos pasó a ser Negocio y ya no hay token ni reembolso
- **CAM-02 (corrida 10):** a pedido del docente, el personal anula las ventas de su propio turno abierto con motivo y registro; CP-11 usa el motivo de la lista

---

## Diapositiva 13 — Conclusiones
- Los procesos centrales cumplen el documento: 15/15 casos aprobados en las corridas 3, 4, 5, 7, 8 y 9, y 19/19 en las corridas 10 y 11, y 23/23 en la corrida 12, y 24/24 en las corridas 16 a 18
- Todas las operaciones rechazadas se revierten sin dejar registros parciales
- Las pruebas encontraron un defecto de seguridad real, que ya está corregido
- También aclararon un requerimiento ambiguo y, con los manuales, cuatro observaciones menores
- Las contraseñas ya no se guardan en texto plano (OBS-06)
- Recomendación: ejecutar `pruebas.bat` antes de cada entrega
