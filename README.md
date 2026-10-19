# Software_Seipo
Sistema de venta e inventario

1. Clonar y abrir el proyecto
* Se requiere **JDK 11 o superior** y **Git**.
* Clona el repositorio:
  ```
  git clone https://github.com/Claudio-codex/Software_Seipo.git
  ```
* Las librerías necesarias (conector MySQL, FlatLaf para el tema visual oscuro, OpenPDF y Apache POI) ya vienen en la carpeta `SEIPO/lib`, no hay que descargar nada.
* **IntelliJ IDEA:** abre la carpeta raíz del repositorio. La librería `seipo-lib` ya apunta a `SEIPO/lib`.
* **NetBeans:** abre la carpeta `SEIPO` como proyecto. Las librerías y la clase principal (`seipo.vista.Login`) ya están configuradas, y el programa se ejecuta desde la raíz del repositorio para usar el mismo `config.properties`.

2. Instalar la base de datos
Elige una de las dos opciones:
* **MariaDB 12.3 (recomendado):** descárgalo de https://mariadb.org/download/ (Windows, paquete MSI) e instálalo con las opciones por defecto: queda como **servicio de Windows** en el puerto `3306`. Durante la instalación define una contraseña para `root`. Las herramientas quedan en `C:\Program Files\MariaDB 12.3\bin`.
* **XAMPP:** inicia **MySQL** desde el panel de control de XAMPP. Las herramientas están en `C:\xampp\mysql\bin` (en los comandos de abajo usa `mysql.exe` en lugar de `mariadb.exe`).

3. Crear el usuario `seipo`
El programa no usa `root`: se conecta con un usuario propio que solo tiene permisos sobre la base `seipo_db`. Abre una consola (`cmd`) y entra como `root`:
```
"C:\Program Files\MariaDB 12.3\bin\mariadb.exe" -u root -p
```
y ejecuta:
```sql
CREATE USER 'seipo'@'localhost' IDENTIFIED BY 'Seipo2026';
GRANT ALL PRIVILEGES ON seipo_db.* TO 'seipo'@'localhost';
```
`Seipo2026` es una **clave de práctica**: si el sistema se usa con datos reales, cámbiala aquí y en `config.properties`.

4. Configurar la conexión
* En la raíz del repositorio, copia `config.ejemplo.properties` como **`config.properties`** (`copy config.ejemplo.properties config.properties`). Ya trae el usuario `seipo` y su clave; cámbialos si usaste otros.
* `config.properties` guarda el host, el puerto, la base, el usuario y la clave. **No se sube al repositorio**: cada computadora tiene el suyo.
* Si el archivo no existe, el programa lo crea al iniciar con los valores de un XAMPP recién instalado (`localhost`, `3306`, `seipo_db`, `root` sin contraseña).

5. Cargar los datos e iniciar
* Importa la copia de la base con datos de ejemplo (ver la sección 9) **por consola**, desde la carpeta del repositorio en `cmd`:
  ```
  "C:\Program Files\MariaDB 12.3\bin\mariadb.exe" -u seipo -p < SEIPO\bd\seipo_db.sql
  ```
  Escribe la clave (`Seipo2026`) cuando la pida. Con XAMPP: `C:\xampp\mysql\bin\mysql.exe -u seipo -p < SEIPO\bd\seipo_db.sql`.
  * En **PowerShell** el `<` no funciona: usa `cmd`, o antepone `cmd /c "..."` al comando.
  * **No uses HeidiSQL** para importar este archivo: falla con él.
* Si prefieres empezar con la base vacía, sáltate la importación: al iniciar, el programa crea la base, las tablas y el usuario administrador.
* Ejecuta la clase **`seipo.vista.Login`** y entra con `admin` / `123`.

6. Usuarios y roles
* **Administrador:** accede a todas las secciones y además autoriza mermas, aprueba órdenes de compra y gestiona Reportes, Pagos y Usuarios.
* **Personal de tienda:** accede a Ventas, Caja, Clientes, Productos, Inventario, Mermas y Compras.
* En **Mermas**, el botón **Exportar** genera el reporte de mermas de un período (por fecha de solicitud) en PDF o Excel: resumen, desglose por causal y detalle con quién solicitó y quién autorizó. El valor perdido cuenta solo las mermas aprobadas. El PDF termina con las líneas de firma "Elaborado por" y "Autorizado por".
* Las cuentas del personal se crean desde el módulo **Usuarios** (entrando como `admin`), donde también se edita el rol, se activa/desactiva y se restablece la contraseña.
* Si ya tenías la base de datos creada con una versión anterior, no hace falta borrarla: al iniciar, el sistema agrega las columnas nuevas y los usuarios existentes quedan como administradores.

7. Caja y ventas
* Para cobrar, cada usuario debe **abrir su turno** en la sección **Caja** indicando el efectivo inicial. Al terminar, **cierra el turno** contando el efectivo: el sistema calcula lo esperado (inicial + ventas en efectivo) y registra si sobra o falta dinero (con una observación obligatoria si hay diferencia).
* En **Ventas** se agregan productos escaneando el código o escribiendo el nombre (`3*CODIGO` agrega 3 unidades). La cantidad se cambia en la tabla con doble clic o con las teclas `+` y `-`; `Supr` quita el ítem.
* Se puede elegir un **cliente** registrado (o registrarlo ahí mismo): su descuento VIP se aplica solo. En efectivo se ingresa el monto recibido y se muestra el **vuelto**.
* El total se **redondea hacia abajo a 10 céntimos** (no se usan monedas de 1 ni de 5 céntimos) y la rebaja aparece como "Redondeo". Los montos en efectivo (recibido, inicial y contado) tampoco aceptan céntimos sueltos.
* Formas de pago: **Efectivo** y **Yape o Plin**. El cobro con tarjeta se retiró del sistema por ahora.
* Atajos: `F2` buscar producto, `F4` cliente, `F6` descuento, `F9` efectivo, `F11` Yape o Plin.
* Solo el **administrador** puede **anular** una venta (desde Caja), y solo mientras el turno siga abierto; el stock vuelve a los mismos lotes. Si se pagó con Yape o Plin, el cobro queda reembolsado.

8. Pagos digitales y comprobantes
* Los cobros con **Yape o Plin** pasan por una **pasarela de pago simulada**: se envía la solicitud de cobro, la pasarela genera un **token de pago** y responde si autoriza el cobro (con su código de autorización) o lo rechaza (por ejemplo, por un código de aprobación incorrecto). Recién con el cobro aprobado se registra la venta. No se conecta a ningún servicio real ni se hacen cargos.
* Datos de prueba: cualquier celular de 9 dígitos que empiece con 9 y cualquier código de aprobación de 6 dígitos se aprueba, salvo `000000`.
* Del celular del cliente solo se guardan los últimos 3 dígitos.
* Cada venta emite su **comprobante digital**: **boleta** (B001) si el cliente tiene DNI o es "Clientes varios", y **factura** (F001) si tiene RUC. Se puede ver, imprimir o guardar en PDF al cobrar y después desde Caja. Incluye un código de verificación. Es un documento de prueba: no se envía a SUNAT.
* En la sección **Pagos** (solo administrador) se revisan todas las transacciones (aprobadas, rechazadas, revertidas y reembolsadas) y se configuran la pasarela y los datos del negocio que salen en el comprobante. La clave secreta de la pasarela se guarda sin cifrar, igual que las contraseñas.

9. Datos de ejemplo
El archivo `SEIPO/bd/seipo_db.sql` es una copia de la base de datos con datos de demostración de un bazar (útiles escolares, limpieza, ferretería, cocina, accesorios, juguetes, perfumería y relojería): 55 productos, unas 7 800 ventas desde enero de 2025 con sus temporadas (campaña escolar, Día de la Madre, Navidad), más de 1 200 turnos de caja con su cuadre, sus comprobantes y los cobros de la pasarela, lotes vencidos y por vencer, mermas, proveedores, órdenes de compra y usuarios. Sirve para ver el sistema funcionando sin cargar datos a mano.

> ⚠️ Al importarlo se **reemplaza por completo** la base `seipo_db` que tengas en tu computadora.

* Se importa por consola con `mariadb.exe` (o `mysql.exe` en XAMPP), como se explica en la sección 5. No hace falta crear la base antes: el archivo la crea. HeidiSQL falla con este archivo.
* Luego ejecuta `Login` y entra con `admin` / `123`. También hay dos cuentas de personal: `Personal1` / `890` y `personal2` / `456`.
* Las contraseñas se guardan sin cifrar, así que quedan visibles dentro del archivo `.sql`: no uses contraseñas reales en la base de ejemplo.
* La copia es una foto del momento en que se generó; lo que cada uno registre después queda solo en su propia computadora.

10. Respaldos
* `respaldo.bat` (en la raíz del repositorio) guarda una copia completa de la base en `respaldos\seipo_db_AAAA-MM-DD_HHMM.sql`. Basta con hacerle doble clic o ejecutarlo en `cmd`.
* Lee el host, el puerto, la base, el usuario y la clave de `config.properties`.
* Usa `mariadb-dump.exe` o `mysqldump.exe`. Los busca en `C:\Program Files\MariaDB 12.3\bin`, luego en `C:\xampp\mysql\bin` y después en el PATH. Si están en otra carpeta, indica la ruta en `config.properties`:
  ```
  respaldo.dump=D:\MariaDB\bin\mariadb-dump.exe
  ```
* La carpeta `respaldos` no se sube al repositorio, porque los respaldos contienen todos los datos y contraseñas. Cópialos a otro disco o a la nube de vez en cuando.
* Para restaurar un respaldo, impórtalo igual que la copia de ejemplo: `mariadb.exe -u seipo -p < respaldos\seipo_db_AAAA-MM-DD_HHMM.sql` (reemplaza la base actual).

**Programarlo con el Programador de tareas de Windows**
* **Por la ventana:** abre el **Programador de tareas** → **Crear tarea básica…** → nombre `SEIPO respaldo` → **Diariamente** a la hora que prefieras (por ejemplo 21:00, al cierre) → **Iniciar un programa** → en **Programa o script** elige `respaldo.bat` del repositorio → **Finalizar**.
* **Por consola** (`cmd`, cambia la ruta por la de tu repositorio):
  ```
  schtasks /create /tn "SEIPO respaldo" /tr "\"C:\ruta\a\Software_Seipo\respaldo.bat\"" /sc daily /st 21:00
  ```
* La computadora debe estar encendida a esa hora. En la columna **Resultado de la última ejecución** se ve `0x0` si salió bien y `0x1` si falló.

11. Problemas frecuentes
Si el programa no puede conectarse, la ventana de inicio de sesión indica qué revisar:
* **No se pudo conectar con el servidor:** revisa que el servicio de MariaDB esté iniciado (Servicios de Windows → `MariaDB`, o MySQL en el panel de XAMPP) y que `db.host` y `db.puerto` sean correctos.
* **Usuario o contraseña rechazados:** revisa `db.usuario` y `db.clave` en `config.properties`.
* **Sin acceso a la base:** el usuario necesita permisos sobre la base (`GRANT ALL PRIVILEGES ON seipo_db.* TO 'seipo'@'localhost';`).
* **Método de autenticación no admitido:** en MariaDB para Windows, `root` puede entrar con la autenticación de Windows, que el programa no admite. Usa el usuario `seipo`.
