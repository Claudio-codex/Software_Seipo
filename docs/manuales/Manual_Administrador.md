# SEIPO — Manual del Administrador

**Sistema de venta e inventario**

Versión 1.2 · 19 de octubre de 2026

Autores: Claudio Puente y Francesco Camarena Damián

Curso: Procesos de Software · Tarma, 2026

## Índice

<!-- INDICE:inicio -->
- [1. Introducción](#1-introducción)
- [2. Requisitos y cómo ingresar](#2-requisitos-y-cómo-ingresar)
  - [2.1 Iniciar sesión](#21-iniciar-sesión)
  - [2.2 Si se olvida una clave o falla el acceso](#22-si-se-olvida-una-clave-o-falla-el-acceso)
- [3. La pantalla principal y el menú](#3-la-pantalla-principal-y-el-menú)
- [4. Procedimientos paso a paso](#4-procedimientos-paso-a-paso)
  - [4.1 Abrir el turno de caja](#41-abrir-el-turno-de-caja)
  - [4.2 Registrar una venta y aplicar el descuento VIP](#42-registrar-una-venta-y-aplicar-el-descuento-vip)
  - [4.3 Cobrar en efectivo, con Yape o con Plin](#43-cobrar-en-efectivo-con-yape-o-con-plin)
  - [4.4 Emitir la boleta o la factura](#44-emitir-la-boleta-o-la-factura)
  - [4.5 Cerrar un turno y cuadrar la caja](#45-cerrar-un-turno-y-cuadrar-la-caja)
  - [4.6 Anular una venta](#46-anular-una-venta)
  - [4.7 Gestionar usuarios y roles](#47-gestionar-usuarios-y-roles)
  - [4.8 Gestionar productos](#48-gestionar-productos)
  - [4.9 Recibir mercadería y editar lotes](#49-recibir-mercadería-y-editar-lotes)
  - [4.10 Registrar, aprobar o rechazar mermas](#410-registrar-aprobar-o-rechazar-mermas)
  - [4.11 Alertas de reposición y órdenes de compra](#411-alertas-de-reposición-y-órdenes-de-compra)
  - [4.12 Consultar los reportes](#412-consultar-los-reportes)
  - [4.13 Exportar a PDF y a Excel](#413-exportar-a-pdf-y-a-excel)
  - [4.14 Configurar los datos del negocio](#414-configurar-los-datos-del-negocio)
  - [4.15 Respaldar la base de datos](#415-respaldar-la-base-de-datos)
- [5. Reglas del negocio que debe conocer](#5-reglas-del-negocio-que-debe-conocer)
- [6. Mensajes de error frecuentes](#6-mensajes-de-error-frecuentes)
- [7. Preguntas frecuentes](#7-preguntas-frecuentes)
- [Lista de capturas pendientes](#lista-de-capturas-pendientes)
<!-- INDICE:fin -->

## 1. Introducción

SEIPO es el sistema de venta e inventario de la tienda. Cubre la venta en caja con turnos y cuadre de efectivo, el cobro en efectivo o con Yape o Plin, la boleta o factura de cada venta, el inventario por lotes en orden FEFO (lo que vence primero, sale primero), las mermas, las compras y los reportes del negocio.

Este manual está dirigido al **administrador**. El administrador hace todo lo que hace el personal de tienda y, además, autoriza mermas y órdenes de compra, anula ventas, gestiona los productos, las cuentas de usuario, los datos del negocio y los reportes, y respalda la base de datos. Las tareas que comparte con el personal se explican aquí de forma resumida; el detalle para la caja está en el Manual del Personal de Tienda.

## 2. Requisitos y cómo ingresar

**Requisitos:** una computadora con SEIPO instalado, el servicio de base de datos (MariaDB) iniciado y una cuenta con rol **Administrador**. La cuenta de administrador se entrega al instalar el sistema; use su usuario y clave.

### 2.1 Iniciar sesión

1. Abra SEIPO. En la ventana **SEIPO - Iniciar sesión**, escriba su **Usuario** y su **Contraseña**.
2. Pulse **Ingresar** o `Enter`.

[CAPTURA 2.1.1: Inicio de sesión — ventana con los campos Usuario y Contraseña]

**Resultado esperado:** se abre la pantalla principal. Si hay pendientes, aparece la ventana **Alertas** con los productos agotados, con stock bajo o con sobrestock, las órdenes de compra por aprobar y las mermas por autorizar.

[CAPTURA 2.1.2: Ventana "Alertas" — resumen de pendientes al ingresar como administrador]

### 2.2 Si se olvida una clave o falla el acceso

- **Un usuario olvidó su clave:** el sistema no tiene la opción "olvidé mi clave". Restablézcala en **Usuarios** (punto 4.7).
- **Usted olvidó su propia clave:** otra cuenta de administrador puede restablecerla. Por eso conviene tener al menos dos administradores.
- **"El usuario está desactivado. Consulte con el administrador."**: active la cuenta en **Usuarios** → **Editar** → **Usuario activo**.
- **"No se pudo conectar con el servidor de base de datos."**: revise que el servicio de MariaDB esté iniciado (Servicios de Windows) y los datos del archivo `config.properties` de la carpeta del programa. Si el mensaje dice que se rechazó el usuario o la contraseña, revise `db.usuario` y `db.clave` en ese archivo.

## 3. La pantalla principal y el menú

- **Menú lateral:** **OPERACIÓN** (Ventas, Caja, Clientes), **INVENTARIO** (Productos, Inventario, Mermas, Compras) y **GESTIÓN** (Reportes, Negocio, Usuarios). El grupo GESTIÓN solo lo ve el administrador. En **Compras** aparece el número de productos por reponer; se actualiza al cambiar de sección y cada 5 minutos.
- **Cabecera:** el nombre y la descripción de la sección, y la fecha de hoy.
- **Tarjeta de usuario:** su nombre, el rol **Administrador** y el botón **Cerrar sesión**.

[CAPTURA 3.1: Pantalla principal del administrador — menú con los grupos OPERACIÓN, INVENTARIO y GESTIÓN]

| Sección | Qué puede hacer el administrador (además de lo del personal) |
|---|---|
| Caja | Ver el historial de turnos de todos, cerrar el turno de otro usuario y anular ventas. |
| Productos | Crear y **editar** productos (precio y stock mínimo y máximo). |
| Inventario | Editar el vencimiento y la ubicación de un lote. |
| Mermas | **Aprobar** o **rechazar** mermas y exportar su reporte. |
| Compras | **Aprobar** o **rechazar** órdenes de compra y editar proveedores. |
| Reportes | Indicadores de ventas e inventario, gráficos y exportación. |
| Negocio | Datos del negocio que aparecen en la boleta y la factura. |
| Usuarios | Cuentas de acceso y roles del personal. |

> **Nota:** las ventanas de confirmación tienen los botones **Aceptar**, **Cancelar**, **Sí** y **No**; al guardar archivos, el botón es **Guardar**.

## 4. Procedimientos paso a paso

### 4.1 Abrir el turno de caja

**Objetivo:** registrar el efectivo inicial de la caja. **Cuándo se usa:** antes de cobrar; el administrador también necesita turno para vender.

1. Entre a **Caja** → **Mi turno**.
2. Escriba el **Efectivo inicial (S/)** y pulse **Abrir turno**.

**Resultado esperado:** aparecen las tarjetas del turno (**Efectivo inicial**, **Ventas en efectivo**, **Yape o Plin**, **Efectivo en caja**).

> **Advertencia:** cada usuario tiene un solo turno abierto. Los montos en efectivo aceptan céntimos.

### 4.2 Registrar una venta y aplicar el descuento VIP

**Objetivo:** armar la venta con el cliente y su descuento. **Cuándo se usa:** en cada atención en caja.

1. En **Ventas**, escanee o escriba el producto en **Producto (F2)** y elíjalo de la lista (`3*CODIGO` agrega 3 unidades). Corrija las cantidades con **+**, **−**, doble clic en **Cant** o **Quitar ítem**.
2. En **CLIENTE (F4)** pulse **Buscar**, elija al cliente y pulse **Usar cliente**, o regístrelo con **+ Nuevo cliente**.
3. El descuento VIP se aplica solo: **5 %** con 1 compra completada, **10 %** con 3 y **15 %** con 5. Con **Descuento (F6)** puede escribir otro porcentaje (0 a 100); el administrador no tiene tope.

[CAPTURA 4.2.1: Ventas — carrito con cliente VIP y el desglose con Descuento, IGV 18% y TOTAL]

**Resultado esperado:** el panel derecho muestra **Subtotal**, **Descuento**, **IGV 18%** y **TOTAL**.

> **Advertencia:** el personal puede aplicar como máximo **10 %** de descuento manual; el sistema se lo impide en la pantalla y al registrar la venta. El descuento VIP del cliente siempre se respeta, aunque sea mayor (Platino, 15 %). Si corresponde un descuento manual mayor, cobre usted esa venta desde su sesión y su turno.

### 4.3 Cobrar en efectivo, con Yape o con Plin

**Objetivo:** registrar el pago. **Cuándo se usa:** al cerrar cada venta.

1. **Efectivo** (`F9`): escriba el **Monto recibido** (o use **Exacto**, **S/ 20**…), verifique el **Vuelto** y pulse **Cobrar**.
2. **Yape o Plin** (`F11`): cuando vea en el celular del negocio que llegó el pago por el total, pulse **Yape o Plin** y responda **Sí** a "¿El cliente ya pagó S/ … con Yape o Plin?".

[CAPTURA 4.3.1: Confirmación "Cobro con Yape o Plin" — pregunta si el cliente ya pagó el total]

**Resultado esperado:** aparece el comprobante con "Venta N° … registrada".

> **Advertencia:** el sistema solo registra la forma de pago; no cobra ni verifica el pago con Yape o Plin. Confirme siempre el pago en el celular del negocio antes de responder **Sí**.

### 4.4 Emitir la boleta o la factura

**Objetivo:** entregar el comprobante. **Cuándo se usa:** en cada venta; se emite solo al cobrar.

1. Antes de cobrar, elija al cliente. Con **RUC** (11 dígitos) se emite **FACTURA ELECTRÓNICA** F001. Con **DNI** o sin cliente ("Clientes varios") se emite **BOLETA DE VENTA ELECTRÓNICA** B001.
2. En el comprobante, pulse **Imprimir**, **Guardar PDF** o **Cerrar**.
3. Para verlo después: **Caja**, seleccione la venta y pulse **Ver comprobante**.

**Resultado esperado:** comprobante con número correlativo, total en letras y código de verificación.

> **Nota:** los datos del negocio que salen en el comprobante (nombre, RUC y dirección) se cambian en **Negocio** (punto 4.14). Los comprobantes son de prueba y no se envían a SUNAT.

### 4.5 Cerrar un turno y cuadrar la caja

**Objetivo:** registrar el efectivo contado y la diferencia. **Cuándo se usa:** al final de cada turno, o cuando un usuario dejó su turno abierto.

1. Para su propio turno: **Caja** → **Mi turno** → **Cerrar turno y cuadrar caja**. Para el turno de otro usuario: **Caja** → **Historial de turnos**, seleccione la fila celeste (abierta) y pulse **Cerrar turno seleccionado**.
2. Revise el **Efectivo esperado** (inicial + ventas en efectivo no anuladas; Yape o Plin no entra a caja).
3. Escriba el **Efectivo contado en caja (S/)**. Si hay diferencia, complete la **Observación** y pulse **Aceptar**.

[CAPTURA 4.5.1: Caja, pestaña Historial de turnos — turnos con faltante (rojo), sobrante (ámbar) y abierto (celeste)]

**Resultado esperado:** "Turno N° … cerrado." con **Cuadrado**, **Falta S/ …** o **Sobra S/ …**. En el historial, la columna **Diferencia** guarda el resultado. Con **Período** se filtra el historial (Hoy, Últimos 7 días, Últimos 30 días, Este año) y **Ver ventas del turno** muestra sus ventas.

### 4.6 Anular una venta

**Objetivo:** dejar sin efecto una venta mal registrada. **Cuándo se usa:** solo mientras el turno de esa venta siga abierto.

1. Entre a **Caja** → **Mi turno**, o abra el turno en **Historial de turnos** → **Ver ventas del turno**.
2. Seleccione la venta y pulse **Anular venta**.
3. Lea el aviso, escriba el **Motivo de la anulación** y pulse **Aceptar**.

[CAPTURA 4.6.1: Caja — ventana "Anular venta" con el motivo de la anulación]

**Resultado esperado:** la venta queda **Anulada** (fila gris), el stock vuelve a los mismos lotes, y el monto sale del cuadre del turno. Su comprobante muestra **ANULADO**. Si se pagó con Yape o Plin, el sistema no devuelve el dinero: devuélvalo al cliente desde el celular del negocio.

> **Advertencia:** si el turno ya se cerró, el sistema responde "Solo se pueden anular ventas de un turno abierto: el cuadre de este turno ya se cerró.". La anulación no se puede deshacer.

### 4.7 Gestionar usuarios y roles

**Objetivo:** crear cuentas, cambiar roles, desactivar usuarios y restablecer claves. **Cuándo se usa:** cuando ingresa o se va alguien, o cuando un usuario olvida su clave.

1. Entre a **Usuarios**. La tabla muestra **Usuario**, **Nombre**, **Rol** y **Estado**.
2. **Crear:** pulse **+ Nuevo Usuario** y complete **Usuario**, **Nombre completo**, **Rol** (Personal de tienda o Administrador), **Contraseña** y **Repetir contraseña**. Pulse **Aceptar**.
3. **Cambiar rol o desactivar:** seleccione la fila, pulse **Editar** y cambie **Nombre completo**, **Rol** o la casilla **Usuario activo**.
4. **Clave olvidada:** seleccione la fila, pulse **Restablecer contraseña** y escriba la **Nueva contraseña** dos veces.

[CAPTURA 4.7.1: Usuarios — ventana "Registrar Nuevo Usuario" con el rol elegido]

**Resultado esperado:** "¡Usuario registrado exitosamente!" o "Contraseña actualizada.". Un usuario desactivado ya no puede ingresar.

> **Advertencia:** no puede quitarse el rol de administrador ni desactivarse a sí mismo, y siempre debe quedar al menos un administrador activo. Las claves se guardan sin cifrar en la base de datos: no use claves personales que se repitan en otros servicios.

### 4.8 Gestionar productos

**Objetivo:** mantener el catálogo y los parámetros de stock. **Cuándo se usa:** al incorporar un producto o cambiar su precio o sus límites.

1. Entre a **Productos**. Use **Buscar:** para filtrar.
2. **Nuevo:** pulse **+ Nuevo Producto** y complete **Código**, **Nombre**, **Categoría**, **Precio**, **Stock mínimo (alerta de stock bajo)** y **Stock máximo (alerta de sobrestock, opcional)**.
3. **Editar:** seleccione el producto y pulse **Editar**. El código no se puede cambiar; deje vacío el stock máximo si no tiene límite.

[CAPTURA 4.8.1: Productos — ventana "Editar producto" con precio y stock mínimo y máximo]

**Resultado esperado:** "Producto registrado exitosamente." El producto nuevo empieza con stock 0.

> **Advertencia:** el stock no se escribe aquí: entra solo por **Inventario** → **Recepción de mercadería**. El precio debe ser mayor que 0 y el stock máximo mayor que el mínimo. El personal también puede crear productos, pero solo el administrador puede editarlos.

### 4.9 Recibir mercadería y editar lotes

**Objetivo:** ingresar mercadería por lotes y corregir sus datos. **Cuándo se usa:** cuando llega mercadería, o si un lote tiene mal la fecha o la ubicación.

1. **Recibir sin orden:** **Inventario** → **Recepción de mercadería**. Escriba la **Guía de remisión** y el **Proveedor**. Para cada producto, complete **Código**, **Lote**, **Vencimiento (dd/mm/aaaa)**, **Cantidad** y **Ubicación** y pulse **Agregar**. Luego pulse **Registrar recepción**. Para recibir una orden de compra, vea el punto 4.11.
2. **Editar un lote:** **Inventario** → **Lotes y vencimientos**, seleccione el lote y pulse **Editar lote**. Cambie el **Vencimiento** (vacío si no vence) o la **Ubicación**.

[CAPTURA 4.9.1: Inventario, pestaña Lotes y vencimientos — lotes en orden FEFO con su estado y el botón Editar lote]

**Resultado esperado:** "Recepción registrada. El stock fue actualizado." La tabla de lotes muestra arriba el que vence primero, con su estado: Vencido, Por vencer (30 días), Vigente o Sin vencimiento.

### 4.10 Registrar, aprobar o rechazar mermas

**Objetivo:** autorizar la baja de productos vencidos o dañados. **Cuándo se usa:** cuando hay mermas pendientes (la ventana **Alertas** se lo avisa al ingresar).

1. **Registrar:** en **Mermas**, pulse **+ Registrar merma** y elija **Lote**, **Cantidad**, **Causal** y **Observación**; o pulse **Registrar lotes vencidos**.
2. **Revisar:** con **Mostrar:** en **Pendientes**, revise las filas ámbar. Puede seleccionar varias.
3. **Aprobar:** pulse **Aprobar** y confirme con **Sí** ("¿Aprobar N merma(s)? Se darán de baja … unidad(es) por S/ …").
4. **Rechazar:** pulse **Rechazar**, escriba el **Motivo del rechazo** y pulse **Aceptar**.

[CAPTURA 4.10.1: Mermas — tarjetas Pendientes de autorizar, Unidades dadas de baja y Valor perdido, con mermas pendientes seleccionadas]

**Resultado esperado:** al aprobar, el stock del lote baja, se registra quién autorizó y la tarjeta **Valor perdido** aumenta. Al rechazar, las unidades vuelven a estar a la venta.

> **Advertencia:** el valor perdido se calcula con el precio que tenía el producto al registrar la merma. Solo cuentan las mermas aprobadas.

### 4.11 Alertas de reposición y órdenes de compra

**Objetivo:** reponer lo que falta y controlar los pedidos a proveedores. **Cuándo se usa:** cuando **Compras** muestra un número en el menú.

1. **Alertas:** en **Compras** → **Alertas de stock**, revise los productos **Agotado**, **Stock bajo** o **Sobrestock**. **Sugerido** es lo que falta para llegar al máximo (o al doble del mínimo), sin contar lo que ya está **En pedido**.
2. **Crear la orden:** seleccione los productos (o ninguno, para todos) y pulse **Crear orden de compra**; o vaya a **Órdenes de compra** → **+ Nueva orden**. Elija el **Proveedor**, ajuste las cantidades y pulse **Aceptar**.
3. **Aprobar o rechazar:** en **Órdenes de compra**, con **Mostrar:** en Pendientes, seleccione la orden y pulse **Aprobar** (confirme con **Sí**) o **Rechazar** (escriba el motivo).
4. **Recibir:** cuando llegue el pedido, seleccione la orden **Aprobada** y pulse **Recibir mercadería**. Escriba la **Guía de remisión** y, por producto, **Recibido**, **Lote**, **Vence (dd/mm/aaaa)** y **Ubicación** (Recibido = 0 si no llegó).
5. **Proveedores:** en la pestaña **Proveedores**, **+ Nuevo proveedor** pide **RUC (11 dígitos)**, **Razón social**, **Contacto**, **Teléfono** y **Correo**. **Editar** permite desactivarlo con **Proveedor activo**.

[CAPTURA 4.11.1: Compras, pestaña Órdenes de compra — orden pendiente seleccionada con los botones Aprobar y Rechazar]

**Resultado esperado:** "Orden de compra N° … creada. Queda pendiente de aprobación." y, al recibirla, "Mercadería recibida. La orden N° … quedó como Recibida y el stock fue actualizado.".

> **Nota:** el estado de una orden avanza de **Pendiente** a **Aprobada** y de ahí a **Recibida**, o pasa a **Rechazada**. El sobrestock señala productos que conviene promocionar.

### 4.12 Consultar los reportes

**Objetivo:** analizar las ventas y el inventario. **Cuándo se usa:** para decidir compras, promociones y precios.

1. Entre a **Reportes** y elija el **Período:** Hoy, Últimos 7 días, Últimos 30 días, Este mes, Mes anterior, Este año o Personalizado (con **Desde** y **Hasta**).
2. Lea los indicadores: **Ingresos**, **Nº ventas**, **Ticket promedio**, **Productos vendidos** y **Precio prom. por unidad**, cada uno comparado con el período anterior ("% vs anterior").
3. Recorra las pestañas: **Resumen**, **Productos** (ordenar por Unidades o Importe), **Categorías**, **Formas de pago**, **Tendencia** e **Inventario**.

[CAPTURA 4.12.1: Reportes — indicadores comparados con el período anterior y el gráfico de ventas del período]

**Resultado esperado:** los gráficos y las tablas del período elegido. Arriba se ve "Hoy: S/ … en N ventas". Solo se suman las ventas completadas, no las anuladas.

### 4.13 Exportar a PDF y a Excel

**Objetivo:** obtener un archivo del reporte para imprimir o compartir. **Cuándo se usa:** para informes periódicos o para archivar.

1. **Reporte de ventas:** en **Reportes**, elija el período y pulse **Exportar PDF** o **Exportar Excel**.
2. **Reporte de mermas:** en **Mermas**, pulse **Exportar** y elija **Exportar PDF** o **Exportar Excel**. Elija el **Período** (Este mes, Mes anterior, Últimos 30 días o Este año) o escriba **Desde** y **Hasta** (dd/mm/aaaa) y pulse **Aceptar**.
3. Elija la carpeta y el nombre del archivo y pulse **Guardar**.

[CAPTURA 4.13.1: Mermas — menú Exportar con las opciones Exportar PDF y Exportar Excel]

**Resultado esperado:** "Reporte exportado:" con la ruta del archivo. El PDF del reporte de mermas termina con las firmas "Elaborado por" y "Autorizado por".

> **Advertencia:** si sale "No se pudo exportar el reporte (¿está abierto en otro programa?)", cierre el archivo en Excel o en el lector de PDF y vuelva a exportar.

### 4.14 Configurar los datos del negocio

**Objetivo:** mantener los datos que aparecen como emisor en la boleta y la factura. **Cuándo se usa:** al instalar el sistema o si cambian los datos del negocio.

1. Entre a **Negocio**.
2. En **Datos del negocio**, escriba **Nombre o razón social**, **RUC** y **Dirección**.
3. Pulse **Guardar cambios**.

[CAPTURA 4.14.1: Negocio — datos del negocio para el comprobante]

**Resultado esperado:** "Datos guardados. Los comprobantes nuevos usan estos datos; los ya emitidos no cambian."

> **Advertencia:** el RUC del negocio debe tener 11 dígitos, y el nombre y la dirección son obligatorios.

### 4.15 Respaldar la base de datos

**Objetivo:** guardar una copia de todos los datos para recuperarlos ante una falla. **Cuándo se usa:** al cierre de cada día, o antes de cualquier cambio importante.

1. En la carpeta del programa, haga doble clic en **respaldo.bat**. Se abre una ventana negra.
2. Lea el resultado: "Respaldo guardado en …" con la ruta del archivo, o un mensaje "ERROR: …". Pulse cualquier tecla para cerrar la ventana ("Presione una tecla para continuar").
3. El archivo queda en la carpeta **respaldos**, con la fecha y la hora en el nombre, por ejemplo `seipo_db_2026-10-19_2100.sql`. Copie esa carpeta a otro disco o a la nube de vez en cuando.
4. **Programarlo:** abra el **Programador de tareas** de Windows → **Crear tarea básica…** → nombre "SEIPO respaldo" → **Diariamente** a una hora fija (por ejemplo, 21:00) → **Iniciar un programa** → elija **respaldo.bat** y en **Agregar argumentos** escriba `/sinpausa` → **Finalizar**.

[CAPTURA 4.15.1: Ventana de respaldo.bat — mensaje "Respaldo guardado en" y "Presione una tecla para continuar"]

**Resultado esperado:** un archivo `.sql` nuevo en **respaldos**. En el Programador de tareas, **Resultado de la última ejecución** muestra `0x0` si salió bien y `0x1` si falló.

> **Advertencia:** sin el argumento `/sinpausa`, la tarea programada quedaría esperando una tecla y no terminaría. El respaldo contiene todos los datos, incluidas las claves de los usuarios: guárdelo en un lugar seguro. Para restaurar un respaldo se reemplaza la base completa; pida apoyo al responsable técnico (el README explica cómo). La computadora debe estar encendida a la hora programada.

## 5. Reglas del negocio que debe conocer

| Regla | Detalle |
|---|---|
| Turno de caja | Para cobrar se necesita un turno abierto; uno por usuario. Efectivo esperado = inicial + ventas en efectivo no anuladas. |
| FEFO | Cada venta descuenta del lote que vence antes. Los lotes vencidos no se venden y se dan de baja como merma. |
| Stock por lotes | El stock de un producto es la suma de sus lotes. Entra solo por recepción de mercadería. |
| Mermas | Nacen pendientes y reservan sus unidades. Solo el administrador las aprueba (descuenta el stock) o las rechaza. |
| Descuento VIP | 5 % desde 1 compra completada, 10 % desde 3 (a partir de la 4.ª compra) y 15 % desde 5. Las anuladas no cuentan. |
| Descuento manual | El personal puede aplicar como máximo 10 % con **Descuento (F6)**; el administrador no tiene tope. El VIP del cliente siempre se respeta. |
| IGV | La base es (subtotal − descuento). El IGV es el 18 % de la base, con 2 decimales y sin redondeo a 10 céntimos; el total es base más IGV. El sistema no redondea el total porque el negocio debe declarar exactamente el IGV cobrado. Las ventas registradas antes de este cambio conservan su línea **Redondeo** en el comprobante. |
| Comprobante | RUC: factura F001. DNI o sin cliente: boleta B001. Numeración correlativa por serie. |
| Yape o Plin | El sistema solo registra la forma de pago; el cajero confirma el pago en el celular del negocio. No entra al efectivo de caja. |
| Anulación | Solo el administrador, y solo con el turno abierto. El stock vuelve a los mismos lotes. El pago con Yape o Plin se devuelve desde el celular del negocio. |
| Alertas | Agotado: 0 disponibles. Stock bajo: disponible menor o igual al mínimo. Sobrestock: disponible mayor que el máximo. |
| Órdenes de compra | Pendiente → Aprobada (solo el administrador) → Recibida, o Rechazada. |
| Roles | El personal no ve Reportes, Negocio ni Usuarios, y el sistema también le rechaza esas operaciones. |

## 6. Mensajes de error frecuentes

| Mensaje del sistema | Qué hacer |
|---|---|
| No se pudo conectar con el servidor de base de datos. | Inicie el servicio de MariaDB y revise el host y el puerto en `config.properties`. |
| El servidor rechazó el usuario o la contraseña. | Revise `db.usuario` y `db.clave` en `config.properties`. |
| Solo se pueden anular ventas de un turno abierto: el cuadre de este turno ya se cerró. | Ya no se puede anular en el sistema; deje constancia del caso según el procedimiento de la tienda. |
| No puede desactivarse ni quitarse el rol de administrador a sí mismo. | Pida a otro administrador que haga el cambio. |
| Debe quedar al menos un administrador activo. | Cree o active otro administrador antes. |
| Ya existe un producto con ese código. | Use otro código o edite el producto existente. |
| El stock máximo debe ser mayor que el mínimo. | Corrija los límites o deje vacío el máximo. |
| La guía … de … ya fue registrada. | Esa recepción ya se ingresó; revise **Movimientos recientes**. |
| El personal puede aplicar como máximo 10 % de descuento manual. … | Lo ve el personal. Si corresponde un descuento mayor, cobre usted la venta desde su sesión. |
| El lote ya está vencido. No se puede recibir. | No ingrese ese lote; coordine la devolución con el proveedor. |
| Seleccione una o más mermas pendientes. | Marque filas en estado Pendiente antes de aprobar o rechazar. |
| No hay productos por reponer en la selección (o ya están en pedido). | Esos productos ya tienen una orden pendiente o aprobada. |
| El RUC del negocio debe tener 11 dígitos. | Corrija el RUC en **Negocio**. |
| No se pudo exportar el reporte (¿está abierto en otro programa?) | Cierre el archivo abierto y vuelva a exportar. |

## 7. Preguntas frecuentes

**Un vendedor se fue sin cerrar su turno, ¿qué hago?** En **Caja** → **Historial de turnos**, seleccione su turno (celeste) y pulse **Cerrar turno seleccionado**.

**¿Puedo anular una venta de ayer?** Solo si su turno sigue abierto. Si ya se cerró el cuadre, el sistema no lo permite.

**¿Por qué el valor perdido no incluye todas las mermas?** Solo cuentan las aprobadas; las pendientes aún pueden rechazarse.

**¿Cómo cambio el precio de un producto?** En **Productos**, selecciónelo y pulse **Editar**. Las ventas anteriores conservan su precio.

**¿Cómo doy de baja a un trabajador que se fue?** En **Usuarios** → **Editar**, desmarque **Usuario activo**. Su historial se conserva.

**¿Cada cuánto debo respaldar?** Al menos una vez al día; lo más seguro es programarlo con el Programador de tareas.

**¿Dónde cambio el nombre y el RUC que salen en la boleta?** En **Negocio** → **Datos del negocio**.

**¿El sistema cobra los pagos con Yape o Plin?** No. El cliente paga desde su celular y el cajero lo confirma en el celular del negocio; el sistema solo registra la forma de pago.

## Lista de capturas pendientes

<!-- CAPTURAS:inicio -->
Total: **16 capturas**. Tome cada una con datos de demostración y reemplace el marcador correspondiente.

| N° | Pantalla | Qué debe verse | Sección del manual |
|---|---|---|---|
| 2.1.1 | Inicio de sesión | Ventana con los campos Usuario y Contraseña | 2.1 Iniciar sesión |
| 2.1.2 | Ventana "Alertas" | Resumen de pendientes al ingresar como administrador | 2.1 Iniciar sesión |
| 3.1 | Pantalla principal del administrador | Menú con los grupos OPERACIÓN, INVENTARIO y GESTIÓN | 3. La pantalla principal y el menú |
| 4.2.1 | Ventas | Carrito con cliente VIP y el desglose con Descuento, IGV 18% y TOTAL | 4.2 Registrar una venta y aplicar el descuento VIP |
| 4.3.1 | Confirmación "Cobro con Yape o Plin" | Pregunta si el cliente ya pagó el total | 4.3 Cobrar en efectivo, con Yape o con Plin |
| 4.5.1 | Caja, pestaña Historial de turnos | Turnos con faltante (rojo), sobrante (ámbar) y abierto (celeste) | 4.5 Cerrar un turno y cuadrar la caja |
| 4.6.1 | Caja | Ventana "Anular venta" con el motivo de la anulación | 4.6 Anular una venta |
| 4.7.1 | Usuarios | Ventana "Registrar Nuevo Usuario" con el rol elegido | 4.7 Gestionar usuarios y roles |
| 4.8.1 | Productos | Ventana "Editar producto" con precio y stock mínimo y máximo | 4.8 Gestionar productos |
| 4.9.1 | Inventario, pestaña Lotes y vencimientos | Lotes en orden FEFO con su estado y el botón Editar lote | 4.9 Recibir mercadería y editar lotes |
| 4.10.1 | Mermas | Tarjetas Pendientes de autorizar, Unidades dadas de baja y Valor perdido, con mermas pendientes seleccionadas | 4.10 Registrar, aprobar o rechazar mermas |
| 4.11.1 | Compras, pestaña Órdenes de compra | Orden pendiente seleccionada con los botones Aprobar y Rechazar | 4.11 Alertas de reposición y órdenes de compra |
| 4.12.1 | Reportes | Indicadores comparados con el período anterior y el gráfico de ventas del período | 4.12 Consultar los reportes |
| 4.13.1 | Mermas | Menú Exportar con las opciones Exportar PDF y Exportar Excel | 4.13 Exportar a PDF y a Excel |
| 4.14.1 | Negocio | Datos del negocio para el comprobante | 4.14 Configurar los datos del negocio |
| 4.15.1 | Ventana de respaldo.bat | Mensaje "Respaldo guardado en" y "Presione una tecla para continuar" | 4.15 Respaldar la base de datos |
<!-- CAPTURAS:fin -->
