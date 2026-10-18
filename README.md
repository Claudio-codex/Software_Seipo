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
* **Administrador:** accede a todas las secciones y además autoriza mermas, aprueba órdenes de compra y gestiona Reportes y Usuarios.
* **Personal de tienda:** accede a Ventas, Clientes, Productos, Inventario, Mermas y Compras.
* Las cuentas del personal se crean desde el módulo **Usuarios** (entrando como `admin`), donde también se edita el rol, se activa/desactiva y se restablece la contraseña.
* Si ya tenías la base de datos creada con una versión anterior, no hace falta borrarla: al iniciar, el sistema agrega las columnas nuevas y los usuarios existentes quedan como administradores.

6. Cargar los datos de ejemplo (opcional)
El archivo `SEIPO/bd/seipo_db.sql` es una copia de la base de datos con datos de demostración: productos, unas 7 000 ventas, lotes vencidos y por vencer, mermas, proveedores, órdenes de compra y usuarios. Sirve para ver el sistema funcionando sin cargar datos a mano.

> ⚠️ Al importarlo se **reemplaza por completo** la base `seipo_db` que tengas en tu computadora.

* **Con phpMyAdmin (XAMPP):** abre `http://localhost/phpmyadmin`, entra a la pestaña **Importar**, elige `SEIPO/bd/seipo_db.sql` y pulsa **Importar**. No hace falta crear la base antes: el archivo la crea.
* **Por consola:** desde la carpeta del repositorio, ejecuta
  ```
  C:\xampp\mysql\bin\mysql.exe -u root < SEIPO\bd\seipo_db.sql
  ```
* Luego ejecuta `Login` y entra con `admin` / `123`.
* La copia es una foto del momento en que se generó; lo que cada uno registre después queda solo en su propia computadora.
