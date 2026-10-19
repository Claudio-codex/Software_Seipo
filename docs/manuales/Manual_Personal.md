# SEIPO — Manual del Personal de Tienda

**Sistema de venta e inventario**

Versión 1.0 · 19 de octubre de 2026

Autores: Claudio Puente y Francesco Camarena Damián

Curso: Procesos de Software · Tarma, 2026

## Índice

<!-- INDICE:inicio -->
- [1. Introducción](#1-introducción)
- [2. Requisitos y cómo ingresar](#2-requisitos-y-cómo-ingresar)
  - [2.1 Iniciar sesión](#21-iniciar-sesión)
  - [2.2 Si olvidó su clave o no puede entrar](#22-si-olvidó-su-clave-o-no-puede-entrar)
- [3. La pantalla principal y el menú](#3-la-pantalla-principal-y-el-menú)
- [4. Procedimientos paso a paso](#4-procedimientos-paso-a-paso)
  - [4.1 Abrir el turno de caja](#41-abrir-el-turno-de-caja)
  - [4.2 Registrar una venta](#42-registrar-una-venta)
  - [4.3 Elegir al cliente y aplicar el descuento VIP](#43-elegir-al-cliente-y-aplicar-el-descuento-vip)
  - [4.4 Cobrar en efectivo y dar el vuelto](#44-cobrar-en-efectivo-y-dar-el-vuelto)
  - [4.5 Cobrar con Yape o con Plin](#45-cobrar-con-yape-o-con-plin)
  - [4.6 Emitir la boleta o la factura](#46-emitir-la-boleta-o-la-factura)
  - [4.7 Registrar una merma](#47-registrar-una-merma)
  - [4.8 Consultar el stock y las alertas](#48-consultar-el-stock-y-las-alertas)
  - [4.9 Recibir mercadería](#49-recibir-mercadería)
  - [4.10 Cerrar el turno y cuadrar la caja](#410-cerrar-el-turno-y-cuadrar-la-caja)
- [5. Reglas del negocio que debe conocer](#5-reglas-del-negocio-que-debe-conocer)
- [6. Mensajes de error frecuentes](#6-mensajes-de-error-frecuentes)
- [7. Preguntas frecuentes](#7-preguntas-frecuentes)
- [Lista de capturas pendientes](#lista-de-capturas-pendientes)
<!-- INDICE:fin -->

## 1. Introducción

SEIPO es el sistema de venta e inventario de la tienda. Con él se atiende al cliente en caja (ventas, cobro en efectivo o con Yape o Plin, boleta o factura), se controla el efectivo de cada turno y se lleva el inventario por lotes, para que primero se venda lo que vence antes.

Este manual está dirigido al **personal de tienda**: las personas que venden, cobran, reciben mercadería y registran productos dañados o vencidos. Explica, paso a paso, solo las tareas que su cuenta tiene permitidas. Las tareas del administrador (anular ventas, aprobar mermas y órdenes de compra, reportes, pagos y usuarios) están en el Manual del Administrador.

## 2. Requisitos y cómo ingresar

**Requisitos:** una computadora de la tienda con SEIPO instalado y conectada a la base de datos de la tienda, y una cuenta de usuario. Su usuario y clave se los entrega el administrador.

### 2.1 Iniciar sesión

1. Abra SEIPO. Aparece la ventana **SEIPO - Iniciar sesión**.
2. En **Usuario**, escriba su usuario.
3. En **Contraseña**, escriba su clave. Con el ícono del ojo puede verla mientras escribe.
4. Pulse **Ingresar** o la tecla `Enter`.

[CAPTURA 2.1.1: Inicio de sesión — ventana con los campos Usuario y Contraseña y el botón Ingresar]

**Resultado esperado:** se abre la pantalla principal en la sección **Ventas**.

> **Advertencia:** la contraseña distingue mayúsculas de minúsculas; el usuario no. Si el sistema dice **"El usuario está desactivado. Consulte con el administrador."**, su cuenta fue desactivada y solo el administrador puede activarla.

### 2.2 Si olvidó su clave o no puede entrar

- **Olvidó su clave:** el sistema no tiene la opción "olvidé mi clave". Pida al administrador que la restablezca; después ingrese con la clave nueva.
- **"Usuario o contraseña incorrectos."**: revise que escribió bien el usuario y la clave (mayúsculas incluidas). El mensaje es el mismo si el usuario no existe.
- **"No se pudo conectar con el servidor de base de datos."**: la computadora no se comunica con la base de datos. No intente repararlo; avise al administrador.

## 3. La pantalla principal y el menú

La pantalla principal tiene tres partes:

- **Menú lateral (izquierda):** las secciones del sistema agrupadas en **OPERACIÓN** (Ventas, Caja, Clientes) e **INVENTARIO** (Productos, Inventario, Mermas, Compras). En **Compras** aparece un número cuando hay productos agotados o con stock bajo.
- **Cabecera (arriba):** el nombre de la sección abierta, una breve descripción y la fecha de hoy.
- **Tarjeta de usuario (abajo a la izquierda):** su nombre, su rol (**Personal de tienda**) y el botón **Cerrar sesión**.

[CAPTURA 3.1: Pantalla principal — menú lateral del personal con OPERACIÓN e INVENTARIO y la sección Ventas abierta]

| Sección | Para qué sirve |
|---|---|
| Ventas | Busque productos, arme la venta, elija el cliente y cobre. |
| Caja | Apertura y cierre de turno, cuadre de caja y ventas del turno. |
| Clientes | Consulte y registre clientes con su DNI o RUC. |
| Productos | Catálogo de productos, precios y stock mínimo y máximo. |
| Inventario | Resumen del almacén, lotes en orden FEFO y recepción de mercadería. |
| Mermas | Bajas por productos vencidos o dañados y su autorización. |
| Compras | Alertas de stock, órdenes de compra y proveedores. |

> **Nota:** el personal de tienda no ve las secciones Reportes, Pagos ni Usuarios; son exclusivas del administrador. En las ventanas de confirmación los botones aparecen en inglés: **OK** (aceptar), **Cancel** (cancelar), **Yes** (sí) y **No**.

## 4. Procedimientos paso a paso

### 4.1 Abrir el turno de caja

**Objetivo:** registrar con cuánto efectivo empieza la caja. **Cuándo se usa:** al empezar su jornada, antes de la primera venta.

1. En el menú, entre a **Caja**, pestaña **Mi turno**. También puede pulsar **Abrir turno** en la barra amarilla de la sección Ventas.
2. En **Efectivo inicial (S/)**, escriba el efectivo que cuenta en la caja (sencillo para dar vuelto). Por ejemplo, `100.00`.
3. Pulse **Abrir turno**.

[CAPTURA 4.1.1: Caja, pestaña Mi turno — formulario "Abrir turno de caja" con el efectivo inicial]

**Resultado esperado:** aparecen las tarjetas **Efectivo inicial**, **Ventas en efectivo**, **Yape o Plin** y **Efectivo en caja**, y la línea "Turno N° … abierto el …". En Ventas, la barra superior se pone verde.

> **Advertencia:** cada usuario puede tener un solo turno abierto. El monto no acepta céntimos sueltos de 1 o 5 (use, por ejemplo, 100 o 85.50).

### 4.2 Registrar una venta

**Objetivo:** armar el detalle de lo que lleva el cliente. **Cuándo se usa:** en cada atención en caja.

1. Entre a **Ventas**. Pulse `F2` para ir al campo **Producto (F2)**.
2. Escanee el código de barras, o escriba el código, el nombre o la categoría. Aparece una lista con el nombre, el precio y el stock de cada producto.
3. Elija el producto con un clic, o con las flechas y `Enter`. Un código escaneado se agrega directamente. También puede pulsar **Agregar**.
4. Para varias unidades escriba, por ejemplo, `3*CODIGO` (agrega 3).
5. Para corregir el carrito: seleccione la fila y use **+** y **−** (o las teclas `+` y `−`), haga doble clic en **Cant** para escribir la cantidad, o pulse **Quitar ítem** (tecla `Supr`). **Vaciar venta** borra todo el carrito.

[CAPTURA 4.2.1: Ventas — carrito con dos productos y el resumen Subtotal, Descuento, IGV 18%, Redondeo y TOTAL]

**Resultado esperado:** el panel derecho muestra **Subtotal**, **Descuento**, **IGV 18%**, **Redondeo** y el **TOTAL**, y junto a COBRAR CON se ve cuántos productos y unidades lleva la venta.

> **Advertencia:** el sistema no deja agregar más unidades de las disponibles. Muestra "Stock insuficiente para … quedan N unidad(es) disponibles." o "… está agotado.". Puede armar la venta sin turno abierto, pero no cobrarla.

### 4.3 Elegir al cliente y aplicar el descuento VIP

**Objetivo:** asociar la venta a un cliente registrado para que reciba su descuento VIP. **Cuándo se usa:** cuando el cliente da su DNI o RUC.

1. En la tarjeta **CLIENTE (F4)** pulse **Buscar** (o `F4`). Se abre la ventana **Cliente de la venta**.
2. Escriba el nombre, DNI o RUC. Cada cliente muestra sus compras y su nivel VIP.
3. Seleccione al cliente y pulse **Usar cliente** (o doble clic o `Enter`).
4. Si el cliente no está registrado, pulse **+ Nuevo cliente** y complete **Nombre**, **DNI / RUC**, **Teléfono** y **Correo**. Pulse **OK**.

[CAPTURA 4.3.1: Ventana "Cliente de la venta" — lista de clientes con sus compras y nivel VIP]

**Resultado esperado:** el descuento VIP se aplica solo y se ve en la línea **Descuento**, por ejemplo "(10%)". **Quitar** saca al cliente y su descuento.

| Compras completadas del cliente | Nivel | Descuento |
|---|---|---|
| Ninguna | Sin Descuento | 0 % |
| 1 o 2 | Plata | 5 % |
| 3 o 4 | Oro | 10 % |
| 5 o más | Platino | 15 % |

> **Nota:** el botón **Descuento (F6)** permite escribir un porcentaje (0 a 100) para toda la venta. El sistema no pide autorización para usarlo; aplíquelo solo cuando la tienda lo haya autorizado.

### 4.4 Cobrar en efectivo y dar el vuelto

**Objetivo:** cobrar la venta en efectivo. **Cuándo se usa:** cuando el cliente paga con billetes o monedas.

1. Pulse **Efectivo** (`F9`). Se abre **Cobro en efectivo** con el **Total a cobrar**.
2. Escriba el **Monto recibido** o use los botones rápidos (**Exacto**, **S/ 20**, **S/ 50**…).
3. Verifique el **Vuelto** que muestra el sistema. Si el monto no alcanza, se ve "Faltan S/ …".
4. Pulse **Cobrar** (o `Enter`).

[CAPTURA 4.4.1: Ventana "Cobro en efectivo" — monto recibido y vuelto calculado]

**Resultado esperado:** aparece el comprobante con "Venta N° … registrada" y el vuelto a entregar, y el carrito queda vacío.

> **Advertencia:** el monto recibido no puede tener céntimos sueltos de 1 o 5. El total ya viene redondeado hacia abajo a 10 céntimos.

### 4.5 Cobrar con Yape o con Plin

**Objetivo:** cobrar con billetera digital a través de la pasarela de pago. **Cuándo se usa:** cuando el cliente paga con Yape o con Plin.

1. Pulse **Yape o Plin** (`F11`). Se abre **Cobro con Yape o Plin**.
2. En **Billetera**, marque **Yape** o **Plin**.
3. Escriba el **Celular del cliente** (9 dígitos, empieza con 9).
4. Escriba el **Código de aprobación** de 6 dígitos que el cliente ve en su aplicación.
5. Pulse **Enviar cobro** y espere la respuesta de la pasarela.

[CAPTURA 4.5.1: Ventana "Cobro con Yape o Plin" — billetera, celular y código, con el cobro aprobado]

**Resultado esperado:** se ve "Aprobada · código de autorización …", la ventana se cierra sola y aparece el comprobante con el medio, la autorización y el token de pago.

> **Advertencia:** si sale "Rechazada: …", corrija los datos y pulse **Reintentar**, pida otro medio de pago o pulse **Cancelar**. Si el botón está apagado, el cobro digital está desactivado; avise al administrador. Por ahora la pasarela está en **modo prueba**: no se hacen cargos reales.

### 4.6 Emitir la boleta o la factura

**Objetivo:** entregar el comprobante de la venta. **Cuándo se usa:** en cada venta; el sistema lo emite solo al cobrar.

1. Antes de cobrar, elija el cliente (punto 4.3). Si tiene **RUC** (11 dígitos), se emite **FACTURA ELECTRÓNICA** serie F001. Con **DNI**, o sin cliente ("Clientes varios"), se emite **BOLETA DE VENTA ELECTRÓNICA** serie B001.
2. Al terminar el cobro se abre el comprobante. Pulse **Imprimir** o **Guardar PDF**, y luego **Cerrar**.
3. Para volver a verlo después: **Caja** → **Mi turno**, seleccione la venta y pulse **Ver comprobante** (o doble clic).

[CAPTURA 4.6.1: Comprobante — factura electrónica F001 con los botones Imprimir, Guardar PDF y Cerrar]

**Resultado esperado:** el comprobante muestra su número correlativo, los datos del cliente, el detalle, el IGV, el total en letras y el código de verificación.

> **Advertencia:** después de cobrar no se puede cambiar el tipo de comprobante. Si el cliente pide factura, elíjalo **antes** de cobrar. Los comprobantes son documentos de prueba: no se envían a SUNAT.

### 4.7 Registrar una merma

**Objetivo:** dar de baja productos vencidos, dañados o deteriorados. **Cuándo se usa:** al encontrar un producto que ya no se puede vender.

1. Entre a **Mermas** y pulse **+ Registrar merma**. También puede hacerlo desde **Inventario** → **Lotes y vencimientos**: seleccione el lote y pulse **Registrar merma**.
2. En **Lote**, elija el lote (se ve el producto, el vencimiento y lo disponible).
3. Indique la **Cantidad** y la **Causal**: Vencido, Dañado, Deteriorado u Otro.
4. Escriba la **Observación** (qué ocurrió, dónde se encontró). Es obligatoria, salvo en "Vencido".
5. Pulse **OK**.

[CAPTURA 4.7.1: Ventana "Registrar merma" — lote, cantidad, causal y observación]

**Resultado esperado:** el mensaje "Merma registrada. Queda pendiente de autorización del administrador." y la merma en la lista como **Pendiente** (fila ámbar).

> **Advertencia:** mientras la merma está pendiente, esas unidades no se venden. El stock solo baja cuando el administrador la aprueba. El botón **Registrar lotes vencidos** crea de una vez una merma "Vencido" por cada lote vencido.

### 4.8 Consultar el stock y las alertas

**Objetivo:** saber cuánto hay de cada producto y qué falta reponer. **Cuándo se usa:** cuando un cliente pregunta por un producto o al revisar el almacén.

1. **Inventario** → **Resumen**: tarjetas **Productos totales**, **Stock bajo**, **Agotados**, **Lotes vencidos** y **Por vencer (30 días)**, y los **Movimientos recientes**.
2. **Inventario** → **Lotes y vencimientos**: busque un producto o lote. Con **Mostrar:** filtre Todos, Vencidos o Por vencer. Arriba está el lote que vence primero.
3. **Productos**: el catálogo con **Precio**, **Stock**, **Mín.**, **Máx.** y **Próx. vencimiento**.
4. **Compras** → **Alertas de stock**: productos con estado **Agotado**, **Stock bajo** o **Sobrestock** y la cantidad **Sugerido** a pedir.

[CAPTURA 4.8.1: Compras, pestaña Alertas de stock — productos agotados y con stock bajo con su cantidad sugerida]

**Resultado esperado:** ve el stock que se puede vender (sin lotes vencidos ni unidades en merma) y los productos por reponer.

> **Nota:** si falta mercadería, pulse **Crear orden de compra** en Alertas de stock. La orden queda **pendiente** hasta que el administrador la apruebe.

### 4.9 Recibir mercadería

**Objetivo:** ingresar al stock lo que llega del proveedor. **Cuándo se usa:** cuando llega un pedido con su guía de remisión.

1. Entre a **Inventario** → **Recepción de mercadería**.
2. Escriba la **Guía de remisión** y el **Proveedor**.
3. Para cada producto: escanee el **Código** y pulse `Enter`, y complete **Lote**, **Vencimiento (dd/mm/aaaa)** (vacío si no vence), **Cantidad** y **Ubicación**. Pulse **Agregar**.
4. Revise las líneas (**Quitar línea** borra una) y pulse **Registrar recepción**. Confirme con **Yes**.

[CAPTURA 4.9.1: Inventario, pestaña Recepción de mercadería — guía, proveedor y líneas con lote y vencimiento]

**Resultado esperado:** el mensaje "Recepción registrada. El stock fue actualizado.".

> **Advertencia:** no se puede recibir un lote ya vencido, ni registrar dos veces la misma guía del mismo proveedor. Si el código no existe, pida al administrador que registre el producto.

### 4.10 Cerrar el turno y cuadrar la caja

**Objetivo:** contar el efectivo y registrar si falta o sobra dinero. **Cuándo se usa:** al terminar su jornada.

1. Entre a **Caja** → **Mi turno** y pulse **Cerrar turno y cuadrar caja**.
2. Revise el resumen: **Efectivo inicial**, **Ventas en efectivo**, **Yape o Plin (no entra a caja)** y **Efectivo esperado**.
3. Cuente el efectivo y escríbalo en **Efectivo contado en caja (S/)**. El sistema muestra **Cuadrado**, **Falta S/ …** o **Sobra S/ …**.
4. Si hay diferencia, explique el motivo en **Observación (obligatoria si hay diferencia)**.
5. Pulse **OK**.

[CAPTURA 4.10.1: Ventana "Cerrar turno N°" — efectivo esperado, efectivo contado y diferencia]

**Resultado esperado:** el mensaje "Turno N° … cerrado." con lo esperado, lo contado y el resultado. En **Mis turnos**, la fila sale en rojo si faltó dinero y en ámbar si sobró.

> **Advertencia:** después de cerrar no podrá cobrar hasta abrir otro turno. Si pulsa **Cerrar sesión** con el turno abierto, el sistema se lo recuerda. Para anular una venta, pida autorización al administrador.

## 5. Reglas del negocio que debe conocer

| Regla | Qué significa para usted |
|---|---|
| Sin turno no hay cobro | Para cobrar debe tener su turno de caja abierto. Cada usuario tiene un solo turno abierto. |
| FEFO (lo que vence primero, sale primero) | Al vender, el sistema descuenta solo del lote que vence antes. Los lotes vencidos no se pueden vender. |
| Mermas pendientes | Las unidades de una merma pendiente quedan reservadas y no se venden hasta que el administrador decida. |
| Descuento VIP | Se aplica solo según las compras completadas del cliente: 5 % desde 1 compra, 10 % desde 3 y 15 % desde 5 (es decir, 10 % a partir de la 4.ª compra). Las ventas anuladas no cuentan. |
| IGV | El IGV es el 18 % de (subtotal − descuento) y se suma al total. |
| Redondeo | El total se redondea hacia abajo a 10 céntimos, porque no circulan monedas de 1 ni 5 céntimos. El redondeo favorece al cliente. |
| Montos en efectivo | Efectivo recibido, inicial y contado: sin céntimos sueltos de 1 o 5. |
| Comprobante | Cliente con RUC: factura F001. DNI o sin cliente: boleta B001. Numeración correlativa. |
| Cobro digital | La venta con Yape o Plin solo se registra si la pasarela aprobó el cobro. Del celular solo se guardan los 3 últimos dígitos. |
| Anulaciones | Solo el administrador anula ventas, y solo mientras el turno siga abierto. |

## 6. Mensajes de error frecuentes

| Mensaje del sistema | Qué hacer |
|---|---|
| Por favor, complete todos los campos. | Escriba su usuario y su contraseña. |
| Usuario o contraseña incorrectos. | Revise lo escrito, incluidas las mayúsculas de la clave. Si la olvidó, pida al administrador que la restablezca. |
| Abra su turno en la sección Caja antes de cobrar. | Abra su turno (punto 4.1) y vuelva a cobrar. |
| Ya tiene un turno abierto. Ciérrelo antes de abrir otro. | Siga trabajando con el turno abierto, o ciérrelo primero (punto 4.10). |
| Producto no encontrado. | Revise el código o búsquelo por nombre. Si no existe, avise al administrador. |
| Stock insuficiente para … quedan N unidad(es) disponibles. | Venda como máximo esa cantidad. |
| Ingrese un monto inicial válido, sin monedas de 1 ni 5 céntimos (por ejemplo 100 o 85.50). | Corrija el monto. |
| El celular debe tener 9 dígitos y empezar con 9. | Corrija el celular del cliente. |
| Rechazada: Código de aprobación incorrecto o vencido | Pida al cliente un código nuevo y pulse **Reintentar**, o cobre con otro medio. |
| Describa en la observación qué le ocurrió al producto. | Complete la observación de la merma. |
| Hay una diferencia de caja: explique el motivo en la observación. | Escriba el motivo del faltante o sobrante y pulse **OK**. |
| Nombre obligatorio y DNI (8 dígitos) o RUC (11 dígitos). | Corrija los datos del cliente. |

## 7. Preguntas frecuentes

**¿Puedo armar una venta sin turno abierto?** Sí, puede agregar productos y elegir al cliente, pero los botones de cobro se activan solo con el turno abierto.

**El cliente quiere factura, ¿qué hago?** Elija al cliente con su RUC (o regístrelo con **+ Nuevo cliente**) antes de cobrar. El sistema emitirá la factura F001.

**¿Por qué el total es unos céntimos menor que la suma?** Por el redondeo hacia abajo a 10 céntimos. La rebaja se ve en la línea **Redondeo**.

**Me equivoqué en una venta ya cobrada, ¿puedo anularla?** No con su cuenta. Pida al administrador que la anule mientras su turno siga abierto.

**¿Por qué no puedo vender un producto que veo en el almacén?** Puede estar en un lote vencido o tener unidades reservadas por una merma pendiente. Revíselo en **Inventario** → **Lotes y vencimientos**.

**¿Cuándo baja el stock por una merma?** Cuando el administrador la aprueba. Si la rechaza, las unidades vuelven a estar disponibles.

**¿El cobro con Yape o Plin cobra dinero real?** No. Por ahora la pasarela está en modo prueba y no hace cargos reales.

## Lista de capturas pendientes

<!-- CAPTURAS:inicio -->
Total: **12 capturas**. Tome cada una con datos de demostración y reemplace el marcador correspondiente.

| N° | Pantalla | Qué debe verse | Sección del manual |
|---|---|---|---|
| 2.1.1 | Inicio de sesión | Ventana con los campos Usuario y Contraseña y el botón Ingresar | 2.1 Iniciar sesión |
| 3.1 | Pantalla principal | Menú lateral del personal con OPERACIÓN e INVENTARIO y la sección Ventas abierta | 3. La pantalla principal y el menú |
| 4.1.1 | Caja, pestaña Mi turno | Formulario "Abrir turno de caja" con el efectivo inicial | 4.1 Abrir el turno de caja |
| 4.2.1 | Ventas | Carrito con dos productos y el resumen Subtotal, Descuento, IGV 18%, Redondeo y TOTAL | 4.2 Registrar una venta |
| 4.3.1 | Ventana "Cliente de la venta" | Lista de clientes con sus compras y nivel VIP | 4.3 Elegir al cliente y aplicar el descuento VIP |
| 4.4.1 | Ventana "Cobro en efectivo" | Monto recibido y vuelto calculado | 4.4 Cobrar en efectivo y dar el vuelto |
| 4.5.1 | Ventana "Cobro con Yape o Plin" | Billetera, celular y código, con el cobro aprobado | 4.5 Cobrar con Yape o con Plin |
| 4.6.1 | Comprobante | Factura electrónica F001 con los botones Imprimir, Guardar PDF y Cerrar | 4.6 Emitir la boleta o la factura |
| 4.7.1 | Ventana "Registrar merma" | Lote, cantidad, causal y observación | 4.7 Registrar una merma |
| 4.8.1 | Compras, pestaña Alertas de stock | Productos agotados y con stock bajo con su cantidad sugerida | 4.8 Consultar el stock y las alertas |
| 4.9.1 | Inventario, pestaña Recepción de mercadería | Guía, proveedor y líneas con lote y vencimiento | 4.9 Recibir mercadería |
| 4.10.1 | Ventana "Cerrar turno N°" | Efectivo esperado, efectivo contado y diferencia | 4.10 Cerrar el turno y cuadrar la caja |
<!-- CAPTURAS:fin -->
