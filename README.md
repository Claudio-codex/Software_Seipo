# Software_Seipo
Sistema de venta e inventario

1. Requisitos Previos
* Tener instalado un servidor de base de datos local compatible con MySQL (como **XAMPP** o **MySQL Server**).
* Asegurarse de que el servicio de **MySQL esté encendido** y activo en el puerto `3306`.

2. Inicializar la Base de Datos Automáticamente
* Abre tu proyecto en tu entorno de desarrollo (como NetBeans, IntelliJ IDEA, etc.).
* Busca y ejecuta por primera vez la clase **`ConexionBD.java`** (ubicada en el paquete `seipo.modelo`).
* Al ejecutarla, el sistema detectará automáticamente si la base de datos `seipo_db` existe; como aún no está creada, **la creará junto con todas las tablas y el usuario administrador por defecto de forma automática**. Verás un mensaje en consola confirmando que todo está listo.

3. Ejecutar la Aplicación

* Una vez creada la base de datos con el paso anterior, ve al paquete `seipo.vista` y busca la clase **`Login.java`**.
* Ejecuta esa clase (`Login.java`) para abrir la ventana de inicio de sesión de la aplicación.
* Puedes iniciar sesión con el usuario administrador por defecto (`admin`) y contraseña (`123`).

4. Abrir el proyecto en el IDE
* Las librerías necesarias (conector MySQL, FlatLaf para el tema visual oscuro, OpenPDF y Apache POI) ya vienen en la carpeta `SEIPO/lib`, no hay que descargar nada.
* **IntelliJ IDEA:** abre la carpeta raíz del repositorio. La librería `seipo-lib` ya apunta a `SEIPO/lib`.
* **NetBeans:** abre la carpeta `SEIPO` como proyecto. Las librerías y la clase principal (`seipo.vista.Login`) ya están configuradas; basta con *Run Project*.
* Se requiere JDK 11 o superior.

5. Usuarios y roles
* **Administrador:** accede a todas las secciones y además autoriza mermas, aprueba órdenes de compra y gestiona Reportes, Pagos y Usuarios.
* **Personal de tienda:** accede a Ventas, Caja, Clientes, Productos, Inventario, Mermas y Compras.
* En **Mermas**, el botón **Exportar** genera el reporte de mermas de un período (por fecha de solicitud) en PDF o Excel: resumen, desglose por causal y detalle con quién solicitó y quién autorizó. El valor perdido cuenta solo las mermas aprobadas. El PDF termina con las líneas de firma "Elaborado por" y "Autorizado por".
* Las cuentas del personal se crean desde el módulo **Usuarios** (entrando como `admin`), donde también se edita el rol, se activa/desactiva y se restablece la contraseña.
* Si ya tenías la base de datos creada con una versión anterior, no hace falta borrarla: al iniciar, el sistema agrega las columnas nuevas y los usuarios existentes quedan como administradores.

6. Caja y ventas
* Para cobrar, cada usuario debe **abrir su turno** en la sección **Caja** indicando el efectivo inicial. Al terminar, **cierra el turno** contando el efectivo: el sistema calcula lo esperado (inicial + ventas en efectivo) y registra si sobra o falta dinero (con una observación obligatoria si hay diferencia).
* En **Ventas** se agregan productos escaneando el código o escribiendo el nombre (`3*CODIGO` agrega 3 unidades). La cantidad se cambia en la tabla con doble clic o con las teclas `+` y `-`; `Supr` quita el ítem.
* Se puede elegir un **cliente** registrado (o registrarlo ahí mismo): su descuento VIP se aplica solo. En efectivo se ingresa el monto recibido y se muestra el **vuelto**.
* El total se **redondea hacia abajo a 10 céntimos** (no se usan monedas de 1 ni de 5 céntimos) y la rebaja aparece como "Redondeo". Los montos en efectivo (recibido, inicial y contado) tampoco aceptan céntimos sueltos.
* Atajos: `F2` buscar producto, `F4` cliente, `F6` descuento, `F9` efectivo, `F10` tarjeta, `F11` Yape o Plin.
* Solo el **administrador** puede **anular** una venta (desde Caja), y solo mientras el turno siga abierto; el stock vuelve a los mismos lotes. Si se pagó con tarjeta o billetera, el cobro queda reembolsado.

7. Pagos digitales y comprobantes
* Los cobros con **Tarjeta** y **Yape o Plin** pasan por una **pasarela de pago simulada**: se envía la solicitud de cobro, la pasarela genera un **token de pago** y responde si autoriza el cobro (con su código de autorización) o lo rechaza (por ejemplo, por fondos insuficientes). Recién con el cobro aprobado se registra la venta. No se conecta a ningún servicio real ni se hacen cargos.
* Datos de prueba: la tarjeta `4111 1111 1111 1111` (o cualquier número válido) se aprueba; si termina en `0002` se rechaza por fondos insuficientes y si termina en `0069`, por tarjeta bloqueada. Para Yape o Plin, cualquier celular de 9 dígitos que empiece con 9 y cualquier código de aprobación de 6 dígitos se aprueba, salvo `000000`.
* No se guarda el número de tarjeta ni el CVV: solo la marca y los últimos 4 dígitos.
* Cada venta emite su **comprobante digital**: **boleta** (B001) si el cliente tiene DNI o es "Clientes varios", y **factura** (F001) si tiene RUC. Se puede ver, imprimir o guardar en PDF al cobrar y después desde Caja. Incluye un código de verificación. Es un documento de prueba: no se envía a SUNAT.
* En la sección **Pagos** (solo administrador) se revisan todas las transacciones (aprobadas, rechazadas, revertidas y reembolsadas) y se configuran la pasarela y los datos del negocio que salen en el comprobante. La clave secreta de la pasarela se guarda sin cifrar, igual que las contraseñas.

8. Cargar los datos de ejemplo (opcional)
El archivo `SEIPO/bd/seipo_db.sql` es una copia de la base de datos con datos de demostración de un bazar (útiles escolares, limpieza, ferretería, cocina, accesorios, juguetes, perfumería y relojería): 55 productos, unas 7 800 ventas desde enero de 2025 con sus temporadas (campaña escolar, Día de la Madre, Navidad), más de 1 200 turnos de caja con su cuadre, sus comprobantes y los cobros de la pasarela, lotes vencidos y por vencer, mermas, proveedores, órdenes de compra y usuarios. Sirve para ver el sistema funcionando sin cargar datos a mano.

> ⚠️ Al importarlo se **reemplaza por completo** la base `seipo_db` que tengas en tu computadora.

* **Con phpMyAdmin (XAMPP):** abre `http://localhost/phpmyadmin`, entra a la pestaña **Importar**, elige `SEIPO/bd/seipo_db.sql` y pulsa **Importar**. No hace falta crear la base antes: el archivo la crea.
* **Por consola:** desde la carpeta del repositorio, ejecuta
  ```
  C:\xampp\mysql\bin\mysql.exe -u root < SEIPO\bd\seipo_db.sql
  ```
* Luego ejecuta `Login` y entra con `admin` / `123`. También hay dos cuentas de personal: `Personal1` / `890` y `personal2` / `456`.
* Las contraseñas se guardan sin cifrar, así que quedan visibles dentro del archivo `.sql`: no uses contraseñas reales en la base de ejemplo.
* La copia es una foto del momento en que se generó; lo que cada uno registre después queda solo en su propia computadora.
