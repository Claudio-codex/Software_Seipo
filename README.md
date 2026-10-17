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
