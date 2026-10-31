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
* **Administrador:** accede a todas las secciones y además autoriza mermas, aprueba órdenes de compra y gestiona Reportes, Negocio y Usuarios.
* **Personal de tienda:** accede a Ventas, Caja, Clientes, Productos, Inventario, Mermas y Compras.
* En **Mermas**, el botón **Exportar** genera el reporte de mermas de un período (por fecha de solicitud) en PDF o Excel: resumen, desglose por causal y detalle con quién solicitó y quién autorizó. El valor perdido cuenta solo las mermas aprobadas. El PDF termina con las líneas de firma "Elaborado por" y "Autorizado por".
* Las cuentas del personal se crean desde el módulo **Usuarios** (entrando como `admin`), donde también se edita el rol, se activa/desactiva y se restablece la contraseña.
* Si ya tenías la base de datos creada con una versión anterior, no hace falta borrarla: al iniciar, el sistema agrega las columnas nuevas y los usuarios existentes quedan como administradores.

7. Caja y ventas
* Para cobrar, cada usuario debe **abrir su turno** en la sección **Caja** indicando el efectivo inicial. Al terminar, **cierra el turno** contando el efectivo: el sistema calcula lo esperado (inicial + ventas en efectivo) y registra si sobra o falta dinero (con una observación obligatoria si hay diferencia).
* En **Ventas** se agregan productos escaneando el código o escribiendo el nombre (`3*CODIGO` agrega 3 unidades). La cantidad se cambia en la tabla con doble clic o con las teclas `+` y `-`; `Supr` quita el ítem.
* Se puede elegir un **cliente** registrado (o registrarlo ahí mismo): su descuento VIP se aplica solo. En efectivo se ingresa el monto recibido y se muestra el **vuelto**.
* **IGV:** El IGV es el 18 % de la base, con 2 decimales y sin redondeo a 10 céntimos; el total es base más IGV. El sistema no redondea el total porque el negocio debe declarar exactamente el IGV cobrado. El efectivo (recibido, inicial y contado) acepta céntimos y el vuelto es exacto. Las ventas registradas antes de este cambio conservan su línea "Redondeo" en el comprobante.
* Formas de pago: **Efectivo** y **Yape o Plin**. El cobro con tarjeta se retiró del sistema por ahora.
* Atajos: `F2` buscar producto, `F4` cliente, `F6` descuento, `F9` efectivo, `F11` Yape o Plin.
* Solo el **administrador** puede **anular** una venta (desde Caja), y solo mientras el turno siga abierto; el stock vuelve a los mismos lotes. Si se pagó con Yape o Plin, el cajero devuelve el dinero desde el celular del negocio.

8. Formas de pago y comprobantes
* El sistema **solo registra la forma de pago** de cada venta; no procesa cobros. Con **Yape o Plin**, el cliente paga desde su celular y el cajero, después de ver el pago en el celular del negocio, confirma la venta. Esas ventas no entran al efectivo de caja.
* Cada venta emite su **comprobante digital**: **boleta** (B001) si el cliente tiene DNI o es "Clientes varios", y **factura** (F001) si tiene RUC. Se puede ver, imprimir o guardar en PDF al cobrar y después desde Caja. Incluye un código de verificación. Es un documento de prueba: no se envía a SUNAT.
* En la sección **Negocio** (solo administrador) se cambian los datos del negocio que salen en el comprobante (nombre, RUC y dirección).

9. Datos de ejemplo
El archivo `SEIPO/bd/seipo_db.sql` es una copia de la base de datos con datos de demostración de un bazar (útiles escolares, limpieza, ferretería, cocina, accesorios, juguetes, perfumería y relojería): 55 productos, unas 7 800 ventas desde enero de 2025 con sus temporadas (campaña escolar, Día de la Madre, Navidad), más de 1 200 turnos de caja con su cuadre, sus comprobantes, lotes vencidos y por vencer, mermas, proveedores, órdenes de compra y usuarios. Sirve para ver el sistema funcionando sin cargar datos a mano.

> ⚠️ Al importarlo se **reemplaza por completo** la base `seipo_db` que tengas en tu computadora.

* Se importa por consola con `mariadb.exe` (o `mysql.exe` en XAMPP), como se explica en la sección 5. No hace falta crear la base antes: el archivo la crea. HeidiSQL falla con este archivo.
* Luego ejecuta `Login` y entra con `admin` / `123`. También hay dos cuentas de personal: `Personal1` / `890` y `personal2` / `456`.
* Las contraseñas se guardan cifradas (PBKDF2 con sal), así que el archivo `.sql` no las muestra. Aun así, las de las cuentas de ejemplo están escritas arriba: no las uses para cuentas reales. Si importas una base de una versión anterior, con las contraseñas sin cifrar, el programa las cifra la primera vez que se conecta.
* La copia es una foto del momento en que se generó; lo que cada uno registre después queda solo en su propia computadora.

10. Respaldos
* `respaldo.bat` (en la raíz del repositorio) guarda una copia completa de la base en `respaldos\seipo_db_AAAA-MM-DD_HHMM.sql`. Basta con hacerle doble clic o ejecutarlo en `cmd`. Al terminar muestra el resultado y espera una tecla (`Presione una tecla para continuar`).
* Lee el host, el puerto, la base, el usuario y la clave de `config.properties`.
* Usa `mariadb-dump.exe` o `mysqldump.exe`. Los busca en `C:\Program Files\MariaDB 12.3\bin`, luego en `C:\xampp\mysql\bin` y después en el PATH. Si están en otra carpeta, indica la ruta en `config.properties`:
  ```
  respaldo.dump=D:\MariaDB\bin\mariadb-dump.exe
  ```
* La carpeta `respaldos` no se sube al repositorio, porque los respaldos contienen todos los datos del negocio y las contraseñas cifradas. Cópialos a otro disco o a la nube de vez en cuando.
* Para restaurar un respaldo, impórtalo igual que la copia de ejemplo: `mariadb.exe -u seipo -p < respaldos\seipo_db_AAAA-MM-DD_HHMM.sql` (reemplaza la base actual).

**Programarlo con el Programador de tareas de Windows**
* **Por la ventana:** abre el **Programador de tareas** → **Crear tarea básica…** → nombre `SEIPO respaldo` → **Diariamente** a la hora que prefieras (por ejemplo 21:00, al cierre) → **Iniciar un programa** → en **Programa o script** elige `respaldo.bat` del repositorio y en **Agregar argumentos** escribe `/sinpausa` (sin esto la tarea quedaría esperando una tecla) → **Finalizar**.
* **Por consola** (`cmd`, cambia la ruta por la de tu repositorio):
  ```
  schtasks /create /tn "SEIPO respaldo" /tr "\"C:\ruta\a\Software_Seipo\respaldo.bat\" /sinpausa" /sc daily /st 21:00
  ```
* La computadora debe estar encendida a esa hora. En la columna **Resultado de la última ejecución** se ve `0x0` si salió bien y `0x1` si falló.

11. Problemas frecuentes
Si el programa no puede conectarse, la ventana de inicio de sesión indica qué revisar:
* **No se pudo conectar con el servidor:** revisa que el servicio de MariaDB esté iniciado (Servicios de Windows → `MariaDB`, o MySQL en el panel de XAMPP) y que `db.host` y `db.puerto` sean correctos.
* **Usuario o contraseña rechazados:** revisa `db.usuario` y `db.clave` en `config.properties`.
* **Sin acceso a la base:** el usuario necesita permisos sobre la base (`GRANT ALL PRIVILEGES ON seipo_db.* TO 'seipo'@'localhost';`).
* **Método de autenticación no admitido:** en MariaDB para Windows, `root` puede entrar con la autenticación de Windows, que el programa no admite. Usa el usuario `seipo`.

## Pruebas
**Qué se probó:** 15 casos de caja negra con JUnit 5 (CP-01 a CP-15) sobre acceso y roles, turno y cierre de caja, ventas con IGV, descuento VIP, FEFO, stock límite, pago digital, comprobantes, anulación, mermas, compras y el reporte de mermas.

**Cómo ejecutarlas**
* Usan su propia base, **`seipo_test`**: la crean desde `SEIPO/bd/seipo_db.sql` y la vuelven a cargar después de cada prueba. No tocan `seipo_db` ni `config.properties`. Antes de la primera ejecución, dale permisos al usuario `seipo` (como `root`):
  ```sql
  GRANT ALL PRIVILEGES ON seipo_test.* TO 'seipo'@'localhost';
  ```
* **IntelliJ IDEA:** elige la configuración **Pruebas SEIPO** (arriba a la derecha) y pulsa ▶.
* **Consola:** ejecuta `pruebas.bat` en la raíz del repositorio (unos 15 segundos). Los reportes XML quedan en `out-pruebas\reportes`.
* La prueba CP-02 abre la ventana principal sin mostrarla, así que necesita un entorno gráfico. Puede aparecer un instante el aviso de alertas del administrador, que se cierra solo.

**Resultados finales:** **15 de 15 casos aprobados (100 %)** en las corridas 3 y 4.
* La corrida 1 dio 13/15. Se corrigió el defecto DEF-01: el control de roles existía solo en la interfaz.
* La corrida 2 dio 14/15. Se aclaró el requerimiento del descuento VIP (OBS-01) y la prueba CP-06 se ajustó a ese criterio.
* La corrida 4 (15/15) confirmó que las correcciones menores OBS-02 a OBS-05 no rompieron nada: tope de 10 % al descuento manual del personal, diálogos en español, pausa de `respaldo.bat` y textos corregidos.

Informe completo: [docs/pruebas/Informe_Pruebas.md](docs/pruebas/Informe_Pruebas.md).

## Manuales
Manuales de usuario en lenguaje simple, con los procedimientos paso a paso, las reglas del negocio, los mensajes de error y las preguntas frecuentes:
* [Manual del Personal de Tienda](docs/manuales/Manual_Personal.md) ([Word](docs/manuales/Manual_Personal.docx))
* [Manual del Administrador](docs/manuales/Manual_Administrador.md) ([Word](docs/manuales/Manual_Administrador.docx))

Las capturas de pantalla están marcadas como `[CAPTURA …]`; al final de cada manual hay una lista de las capturas pendientes.
