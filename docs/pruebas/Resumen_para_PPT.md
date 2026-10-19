# Resumen para la presentación — Pruebas de caja negra de SEIPO

Detalle completo en [`Informe_Pruebas.md`](Informe_Pruebas.md) y [`Defectos.md`](Defectos.md).

---

## Diapositiva 1 — Objetivo
- Verificar que SEIPO cumple las reglas de negocio de su documentación
- 15 casos de prueba automatizados con JUnit 5
- Cubren acceso, caja, ventas, FEFO, pagos, comprobantes, mermas, compras y reportes
- Esperado = lo que dice el documento; si el código difiere, es un defecto
- Resultado final: 15 de 15 casos aprobados (100 %)

---

## Diapositiva 2 — Estrategia y técnicas
- Prueba de **caja negra**: entradas y salidas observables, sin mirar la implementación
- **Partición en clases de equivalencia**: válidas e inválidas (credenciales, turno, pago, documento, rol)
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
- **CP-05 Venta en efectivo:** S/ 12.35 + IGV = S/ 14.50 con redondeo, vuelto S/ 5.50 → Pasó
- **CP-06 Descuento VIP:** 5 % en la 3.ª compra y 10 % desde la 4.ª (criterio aclarado) → Pasó
- **CP-07 FEFO** (sale el lote que vence primero) y **CP-08 Stock límite** (5 rechazado, 4 aceptado) → Pasó

---

## Diapositiva 5 — Casos de prueba (2/2)
- **CP-09 Pago digital:** tarjeta retirada rechazada; Yape aprobado con token; código 000000 rechazado → Pasó
- **CP-10 Comprobante:** RUC → factura F001; DNI o sin documento → boleta B001; correlativo +1 → Pasó
- **CP-11 Anulación:** stock a los mismos lotes, venta anulada y cobro reembolsado → Pasó
- **CP-12 Mermas** (pendiente, aprobada o rechazada) y **CP-13 Alertas y orden de compra** → Pasó
- **CP-14 Cierre de caja** (−S/ 1.80 y +S/ 0.50 exactos) y **CP-15 Reporte PDF y Excel = base de datos** → Pasó

---

## Diapositiva 6 — Historial de corridas y resultados
- **Corrida 1:** 13/15 (86,7 %); fallan CP-02 y CP-06
- **Corrida 2:** 14/15 (93,3 %); se corrige DEF-01 y pasa CP-02
- **Corrida 3:** **15/15 (100 %)**; se aclara el requerimiento VIP y pasa CP-06
- Cada corrida dura unos 15 segundos y se puede repetir
- Las tres salidas se guardan como evidencia en `docs/pruebas/evidencias/`

---

## Diapositiva 7 — Defectos y observaciones
- **DEF-01 (alta):** el rol solo se controlaba en el menú; el personal podía crear un administrador
- Corrección: validación de rol en 5 métodos de Usuarios, Reportes y Pagos (10 líneas en 3 controladores)
- **OBS-01 (media):** "10 % desde la tercera compra" era ambiguo
- Resolución: se aclaró como "10 % después de 3 compras (a partir de la 4.ª)", sin cambiar el código
- Ambos cerrados y verificados en las corridas 2 y 3

---

## Diapositiva 8 — Adaptaciones respecto al diseño original
- El documento retiró la tarjeta como forma de pago en esta versión
- **CP-09:** la tarjeta 4111 1111 1111 1111 se prueba como pago que debe rechazarse
- **CP-09:** el rechazo se prueba con el código 000000 de la pasarela (no existe la tarjeta 0002)
- **CP-11:** la venta "con tarjeta" se reemplazó por una venta con Plin
- Los casos siguen cubriendo autorización, rechazo y reembolso

---

## Diapositiva 9 — Conclusiones
- Los procesos centrales cumplen el documento: 15/15 casos aprobados
- Todas las operaciones rechazadas se revierten sin dejar registros parciales
- Las pruebas encontraron un defecto de seguridad real, que ya está corregido
- También revelaron un requerimiento ambiguo, que se aclaró en el documento
- Recomendación: ejecutar `pruebas.bat` antes de cada entrega
