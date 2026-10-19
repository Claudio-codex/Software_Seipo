# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language

Always respond to the user in Spanish (siempre responde en español), including explanations, summaries, questions, and commit messages.

## Overview

SEIPO is a Java Swing desktop app for sales and inventory ("Sistema de venta e inventario") backed by a local MySQL database. Code, identifiers, UI text, and docs are in Spanish. Money is in Peruvian soles (`Datos.soles(...)`), and IGV (18% tax) is applied to every sale.

## Build & Run

No build tool (Maven/Gradle). The project is built from an IDE: IntelliJ (`Software_Seipo.iml` at the repo root, source root `SEIPO/src`) or NetBeans/Ant (`SEIPO/build.xml`, `SEIPO/nbproject/`). There are no tests and no linter.

- **Prerequisite:** MySQL running on `localhost:3306`, user `root`, empty password (hard-coded in `seipo.modelo.ConexionBD`).- **Entry point:** `seipo.vista.Login` (`main`). Default login: `admin` / `123` (seeded by `ConexionBD`; `LEEME.txt` wrongly says `1234`).
- `seipo.modelo.ConexionBD#main` checks the connection and creates the schema.
- The required jars (MySQL Connector/J, FlatLaf, OpenPDF, Apache POI and its dependencies) are in `SEIPO/lib`. IntelliJ uses them through the `seipo-lib` library, and NetBeans lists them in `javac.classpath` in `nbproject/project.properties`. If you add a jar, update both.
- Command-line compile/run (connector jar path required):
  ```
  javac -d out -cp <mysql-connector-j.jar> $(find SEIPO/src -name "*.java")
  java -cp "out;<mysql-connector-j.jar>" seipo.vista.Login   # ':' separator on Unix
  ```
- `out/` and `SEIPO/out/` contain IDE build output (some of it is committed). Don't edit them.

## Architecture

The app is a simple three-package MVC under `SEIPO/src/seipo/`:

- **`modelo`**
  - `ConexionBD.obtenerConexion()` is the only way code gets a DB connection. **On every call** it runs `inicializarBaseDatos()`, which runs `CREATE DATABASE/TABLE IF NOT EXISTS` and seeds the admin user. This makes the schema self-bootstrapping, so schema changes go in that SQL array. `CREATE TABLE IF NOT EXISTS` does not alter tables that already exist, so a new column on an existing table also needs an `ALTER TABLE ... ADD COLUMN` through `agregarColumna` (it ignores MySQL error 1060, duplicate column). See `actualizarUsuarios` for an example.
  - `Sesion` holds the logged-in user (id, name, role). There are two roles, `ADMINISTRADOR` and `PERSONAL`. `Principal` only builds `PanelReportes` and `PanelUsuarios` for administrators.
  - `Datos` holds shared constants (`IGV`, `STOCK_BAJO` low-stock threshold, the `dd/MM/yy` date format), the `ProductoVenta` value object, and the currency formatter.
- **`controlador`**: one controller per screen (`ControladorVentas`, `ControladorProductos`, etc.). Each one opens its own connection with try-with-resources, uses plain JDBC `PreparedStatement`s, throws `SQLException` up to the view, and returns small nested `*VO` classes with public fields.
- **`vista`**: Swing views. `Login` opens `Principal`, a `JFrame` with a side menu and a `CardLayout` of `Panel*` screens. `Principal.mostrar()` calls each panel's `refrescar()` when that panel is shown, so new panels need a `refrescar()` and a case in that method. Each section is registered in `Principal` with a menu group (OPERACIÓN, INVENTARIO, GESTIÓN) and a description, which the header shows. `UI` holds the visual theme and the widget factories:
  - Dark theme: FlatLaf, installed by `UI.instalarTema()` in `Login.main`, before any window is created.
  - Palette constants (`FONDO`, `SUPERFICIE`, `TEXTO`, `TEXTO_SECUNDARIO`, `ACENTO`, `BORDE`, `FILA_*` row tints). Use these instead of hard-coded colors, and never use light colors such as `Color.WHITE` for backgrounds.
  - Fonts via `UI.fuente(estilo, tamaño)`.
  - Buttons: `boton` (main action), `botonSecundario`, and `botonPeligro` (reject/remove).
  - Tables: `modelo` (read-only model), `tabla`, `colorearFilas` (tints rows that need attention), `columnaAncha`.
  - Layout: `tarjeta`, `ayuda`, `base`.

  `Graficos` uses the same palette. `ExportadorReporte` keeps its own light colors for PDF and Excel.

Domain notes:
- Stock is tracked per batch in `lotes` (FEFO). `productos.stock` is a cached total that **must always equal** `SUM(lotes.cantidad)` for that product, so any code that changes stock updates both in the same transaction. Stock only comes in through `ControladorInventario.registrarRecepcion` (receipt with a delivery guide, `recepciones`). New products start at 0. Batches with no expiry date have `fecha_vencimiento NULL` and sort last.
- A sale (`ControladorVentas.cobrarVenta`) runs as one transaction: it requires an open cash shift (`turnos`) for the logged-in user, inserts into `ventas` (subtotal, discount, total, client, user, shift, cash received) and `detalle_ventas`, takes stock from batches in FEFO order (soonest expiry first, `FOR UPDATE`), skips expired batches, decrements `productos.stock`, records each batch used in `venta_lotes`, and logs one `movimientos` row per batch used. If the non-expired stock is not enough, it rolls back. The stock shown in Ventas is the sellable stock (non-expired batches) and does not include expired units.
- Cash shifts (`ControladorCaja`, `PanelCaja`): each user has at most one `ABIERTO` shift, opened with the starting cash. Closing stores `efectivo_esperado` (starting cash + non-voided cash sales) and `efectivo_contado`; the difference is shortage/surplus. A user closes their own shift; an administrator can close anyone's. Ventas totals per payment method come from `ventas.forma_pago` (`Datos.FORMAS_PAGO`, cash is `Datos.EFECTIVO`).
- Voiding (`ControladorVentas.anularVenta`): administrator only, and only while the sale's shift is still open (so a closed cash count never changes). It sets `ventas.estado = 'ANULADA'`, returns the units to the same batches via `venta_lotes`, and logs an `Anulación` movement. Every query that sums sales (reports, shift totals, client purchase counts) must filter `estado = 'COMPLETADA'`.
- Client VIP discount (`ControladorClientes.ClienteVO.porcentajeVIP`) depends on that client's completed purchases; Ventas applies it automatically when the client is chosen.
- Write-offs (`mermas`, `ControladorMermas`) are tied to a batch. Anyone can register one, and it starts as `PENDIENTE`. While pending, its units are reserved: `RESERVADO_EN_LOTE` is subtracted from the sellable stock in `ControladorVentas` and from what is left to write off. Only an administrator can move a write-off to `APROBADA`, which in one transaction decrements the batch and `productos.stock` and logs a `Merma` movement, or to `RECHAZADA`, which releases the reservation. The write-off stores `precio_unitario`, so the value lost doesn't change if the price changes later.
- Stock alerts (`ControladorCompras.listarAlertas`) use the sellable stock compared against `productos.stock_minimo` and `stock_maximo` (NULL means no limit): Agotado, Stock bajo, or Sobrestock. The Inventario summary counts the same alerts. Purchase orders (`ordenes_compra` + `detalle_ordenes`) go `PENDIENTE` → `APROBADA` (administrator) → `RECIBIDA`, or to `RECHAZADA`. Units on pending or approved orders count as "en pedido", and the suggested quantity subtracts them. An order is received through `ControladorInventario.registrarRecepcion(..., ordenId)`, which creates the batches and marks the order in the same transaction. `Principal` refreshes the alert count on the Compras button with a `javax.swing.Timer` and when switching screens. The sale total is `Datos.totalVenta(subtotal, discount)` = `(subtotal - discount) * (1 + IGV)` rounded to cents; both `ControladorVentas` and `PanelVentas` use it.
- `detalle_ventas` and `movimientos` store the product **name** (`producto_nombre`) rather than a foreign key to `productos`. `movimientos.fecha` is a `VARCHAR` formatted with `Datos.FECHA`.
- Passwords are stored and compared in plaintext (`ControladorLogin`, `ControladorUsuarios`). Deactivated users (`usuarios.activo = FALSE`) can't log in. `ControladorUsuarios.actualizarUsuario` stops the logged-in user from demoting or deactivating themselves, and keeps at least one active administrator.

The demo data in `SEIPO/bd/seipo_db.sql` is for a general store (bazar) that sells school supplies, cleaning products, hardware, kitchenware, accessories, toys, perfumes and watches. It sells no food and no clothing, so keep demo data and examples within those categories.

## Docs

`docs/Documentacion_Proyecto_Seipo_2.docx` is the project documentation (university course project).
