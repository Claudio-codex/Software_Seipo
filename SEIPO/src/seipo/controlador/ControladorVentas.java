package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Datos;
import seipo.modelo.Datos.ProductoVenta;
import seipo.modelo.Sesion;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ControladorVentas {

    public static final String COMPLETADA = "COMPLETADA", ANULADA = "ANULADA";

    /**
     * Stock que se puede vender: lotes no vencidos menos las unidades reservadas por mermas pendientes
     * (los vencidos se dan de baja como merma).
     */
    static final String STOCK_DISPONIBLE =
            "(SELECT COALESCE(SUM(l.cantidad - " + ControladorMermas.RESERVADO_EN_LOTE + "), 0) " +
            "FROM lotes l WHERE l.producto_id = p.id " +
            "AND (l.fecha_vencimiento IS NULL OR l.fecha_vencimiento >= CURDATE())) AS stock";

    /** Venta del historial con su desglose de importes. */
    public static class VentaVO {
        public int id, turnoId;
        public LocalDateTime fecha;
        public String cliente, documentoCliente, vendedor, formaPago, estado, anuladoPor, motivoAnulacion;
        /** Número del comprobante (B001-00000001); null en ventas anteriores a los comprobantes. */
        public String comprobante;
        public double subtotal, descuento, total;
        public Double recibido;

        public double igv() { return Datos.igvVenta(subtotal, descuento); }

        /** Rebaja por redondear el total a 10 céntimos (0 o negativa). */
        public double redondeo() { return Datos.redondear(total - (subtotal - descuento) - igv()); }

        /** Vuelto entregado; null si no se registró el efectivo recibido. */
        public Double vuelto() { return recibido == null ? null : Datos.redondear(recibido - total); }

        public boolean anulada() { return ANULADA.equals(estado); }
    }

    public static class DetalleVO {
        public String producto;
        public int cantidad;
        public double precio, importe;
    }

    public List<ProductoVenta> filtrarSugerencias(String texto) throws SQLException {
        List<ProductoVenta> encontrados = new ArrayList<>();
        String sql = "SELECT p.id, p.codigo, p.nombre, p.categoria, p.precio, " + STOCK_DISPONIBLE +
                " FROM productos p WHERE p.codigo LIKE ? OR p.nombre LIKE ? OR p.categoria LIKE ? " +
                "ORDER BY p.codigo = ? DESC, p.nombre LIMIT 30";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, "%" + texto + "%");
            pst.setString(2, "%" + texto + "%");
            pst.setString(3, "%" + texto + "%");
            pst.setString(4, texto);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) encontrados.add(leerProducto(rs));
            }
        }
        return encontrados;
    }

    public ProductoVenta buscarProductoBD(String texto) throws SQLException {
        String sql = "SELECT p.id, p.codigo, p.nombre, p.categoria, p.precio, " + STOCK_DISPONIBLE +
                " FROM productos p WHERE p.codigo = ? OR p.nombre = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, texto);
            pst.setString(2, texto);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return leerProducto(rs);
            }
        }
        return null;
    }

    private static ProductoVenta leerProducto(ResultSet rs) throws SQLException {
        return new ProductoVenta(
                rs.getInt("id"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("categoria"),
                rs.getDouble("precio"),
                rs.getInt("stock")
        );
    }

    /**
     * Registra la venta en el turno abierto del usuario y devuelve su número.
     *
     * @param clienteId     cliente registrado, o null para "Clientes varios"
     * @param montoRecibido efectivo que entregó el cliente (solo en pagos en efectivo), o null
     * @param tokenPago     token del cobro aprobado por la pasarela (Yape o Plin), o null en efectivo
     */
    public int cobrarVenta(List<ProductoVenta> items, List<Integer> cantidades, double descuentoPct, String forma,
                           Integer clienteId, Double montoRecibido, String tokenPago) throws SQLException {
        if (items.isEmpty() || items.size() != cantidades.size()) throw new SQLException("La venta no tiene productos.");
        for (int c : cantidades) if (c <= 0) throw new SQLException("Las cantidades deben ser mayores que 0.");
        if (!Arrays.asList(Datos.FORMAS_PAGO).contains(forma)) throw new SQLException("Forma de pago no válida: " + forma + ".");
        double sub = 0;
        for (int i = 0; i < items.size(); i++) {
            sub += items.get(i).precio * cantidades.get(i);
        }
        sub = Datos.redondear(sub);
        double desc = Datos.redondear(sub * descuentoPct / 100);
        double totalVenta = Datos.totalVenta(sub, desc);
        if (montoRecibido != null && montoRecibido < totalVenta) {
            throw new SQLException("El monto recibido es menor que el total de la venta.");
        }
        if (montoRecibido != null && !Datos.esMontoEnMonedas(montoRecibido)) {
            throw new SQLException("El monto recibido no puede tener monedas de 1 ni 5 céntimos.");
        }

        String sqlTurno = "SELECT id FROM turnos WHERE usuario_id = ? AND estado = 'ABIERTO' FOR UPDATE";
        String sqlVenta = "INSERT INTO ventas (fecha, subtotal, descuento, total, forma_pago, cliente_id, usuario_id, turno_id, monto_recibido) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_ventas (venta_id, producto_id, producto_nombre, cantidad, precio_unitario, importe) VALUES (?, ?, ?, ?, ?, ?)";
        // FEFO: primero el lote que vence antes y al final los que no vencen. FOR UPDATE bloquea los lotes hasta el commit.
        // Las unidades reservadas por mermas pendientes no se venden
        String sqlLotes = "SELECT l.id, l.numero_lote, l.cantidad - " + ControladorMermas.RESERVADO_EN_LOTE + " AS cantidad " +
                "FROM lotes l WHERE l.producto_id = ? AND l.cantidad > 0 " +
                "AND (l.fecha_vencimiento IS NULL OR l.fecha_vencimiento >= CURDATE()) " +
                "ORDER BY l.fecha_vencimiento IS NULL, l.fecha_vencimiento, l.id FOR UPDATE";
        String sqlUpdateLote = "UPDATE lotes SET cantidad = cantidad - ? WHERE id = ?";
        String sqlVentaLote = "INSERT INTO venta_lotes (venta_id, lote_id, producto_id, cantidad) VALUES (?, ?, ?, ?)";
        String sqlStockActual = "SELECT stock FROM productos WHERE id = ? FOR UPDATE";
        String sqlUpdateStock = "UPDATE productos SET stock = stock - ? WHERE id = ?";
        String sqlMovimiento = "INSERT INTO movimientos (fecha, producto_nombre, tipo, lote, cantidad, stock_resultante) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                validarDescuento(con, descuentoPct, clienteId);
                int turnoId;
                try (PreparedStatement pst = con.prepareStatement(sqlTurno)) {
                    pst.setInt(1, Sesion.getId());
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) throw new SQLException("No tiene un turno de caja abierto. Abra su turno en la sección Caja.");
                        turnoId = rs.getInt(1);
                    }
                }

                // El cobro digital debe estar aprobado por la pasarela, por el mismo monto y sin otra venta asociada
                Integer transaccionId = null;
                if (!Datos.EFECTIVO.equals(forma)) {
                    transaccionId = validarCobroDigital(con, tokenPago, forma, totalVenta);
                }

                int ventaId = -1;
                try (PreparedStatement pstVenta = con.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                    pstVenta.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
                    pstVenta.setDouble(2, sub);
                    pstVenta.setDouble(3, desc);
                    pstVenta.setDouble(4, totalVenta);
                    pstVenta.setString(5, forma);
                    pstVenta.setObject(6, clienteId, Types.INTEGER);
                    pstVenta.setInt(7, Sesion.getId());
                    pstVenta.setInt(8, turnoId);
                    pstVenta.setObject(9, montoRecibido, Types.DECIMAL);
                    pstVenta.executeUpdate();
                    try (ResultSet rsKeys = pstVenta.getGeneratedKeys()) {
                        if (rsKeys.next()) ventaId = rsKeys.getInt(1);
                    }
                }
                if (transaccionId != null) {
                    try (PreparedStatement pst = con.prepareStatement("UPDATE transacciones_pago SET venta_id = ? WHERE id = ?")) {
                        pst.setInt(1, ventaId);
                        pst.setInt(2, transaccionId);
                        pst.executeUpdate();
                    }
                }

                try (PreparedStatement pstDetalle = con.prepareStatement(sqlDetalle);
                     PreparedStatement pstLotes = con.prepareStatement(sqlLotes);
                     PreparedStatement pstUpdLote = con.prepareStatement(sqlUpdateLote);
                     PreparedStatement pstVentaLote = con.prepareStatement(sqlVentaLote);
                     PreparedStatement pstStockActual = con.prepareStatement(sqlStockActual);
                     PreparedStatement pstStock = con.prepareStatement(sqlUpdateStock);
                     PreparedStatement pstMov = con.prepareStatement(sqlMovimiento)) {

                    for (int i = 0; i < items.size(); i++) {
                        ProductoVenta p = items.get(i);
                        int c = cantidades.get(i);
                        double importeLinea = p.precio * c;

                        pstDetalle.setInt(1, ventaId);
                        pstDetalle.setInt(2, p.id);
                        pstDetalle.setString(3, p.nombre);
                        pstDetalle.setInt(4, c);
                        pstDetalle.setDouble(5, p.precio);
                        pstDetalle.setDouble(6, importeLinea);
                        pstDetalle.executeUpdate();

                        int stock;
                        pstStockActual.setInt(1, p.id);
                        try (ResultSet rs = pstStockActual.executeQuery()) {
                            rs.next();
                            stock = rs.getInt(1);
                        }

                        // Descuenta lote por lote y registra una salida por cada lote usado
                        int pendiente = c;
                        pstLotes.setInt(1, p.id);
                        try (ResultSet rs = pstLotes.executeQuery()) {
                            while (pendiente > 0 && rs.next()) {
                                int enLote = rs.getInt("cantidad");
                                if (enLote <= 0) continue; // lote reservado completo por mermas pendientes
                                int tomar = Math.min(pendiente, enLote);
                                pendiente -= tomar;
                                stock -= tomar;

                                pstUpdLote.setInt(1, tomar);
                                pstUpdLote.setInt(2, rs.getInt("id"));
                                pstUpdLote.executeUpdate();

                                pstVentaLote.setInt(1, ventaId);
                                pstVentaLote.setInt(2, rs.getInt("id"));
                                pstVentaLote.setInt(3, p.id);
                                pstVentaLote.setInt(4, tomar);
                                pstVentaLote.executeUpdate();

                                pstMov.setString(1, LocalDate.now().format(Datos.FECHA));
                                pstMov.setString(2, p.nombre);
                                pstMov.setString(3, "Salida");
                                pstMov.setString(4, rs.getString("numero_lote"));
                                pstMov.setInt(5, tomar);
                                pstMov.setInt(6, stock);
                                pstMov.executeUpdate();
                            }
                        }
                        if (pendiente > 0) {
                            throw new SQLException("Stock disponible insuficiente para " + p.nombre
                                    + " (faltan " + pendiente + "). Los lotes vencidos no se pueden vender.");
                        }

                        pstStock.setInt(1, c);
                        pstStock.setInt(2, p.id);
                        pstStock.executeUpdate();
                    }
                }
                ControladorComprobantes.emitir(con, ventaId, clienteId, totalVenta);
                con.commit();
                return ventaId;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * El personal puede dar como máximo {@link Datos#DESCUENTO_MAX_PERSONAL} % de descuento manual; más solo si es el
     * descuento VIP que ya le corresponde al cliente. El administrador no tiene tope.
     */
    private static void validarDescuento(Connection con, double descuentoPct, Integer clienteId) throws SQLException {
        if (Sesion.esAdministrador() || descuentoPct <= Datos.DESCUENTO_MAX_PERSONAL) return;
        double vip = 0;
        if (clienteId != null) {
            try (PreparedStatement pst = con.prepareStatement("SELECT COUNT(*) FROM ventas WHERE cliente_id = ? AND estado = 'COMPLETADA'")) {
                pst.setInt(1, clienteId);
                try (ResultSet rs = pst.executeQuery()) {
                    rs.next();
                    vip = new ControladorClientes.ClienteVO(clienteId, null, null, null, null, rs.getInt(1)).porcentajeVIP;
                }
            }
        }
        if (descuentoPct > vip) throw new SQLException(mensajeTopeDescuento());
    }

    /** Mensaje cuando el personal intenta un descuento manual mayor al permitido. */
    public static String mensajeTopeDescuento() {
        return "El personal puede aplicar como máximo " + (int) Datos.DESCUENTO_MAX_PERSONAL
                + " % de descuento manual. Para un descuento mayor, pida al administrador que lo aplique.";
    }

    /** Comprueba el cobro digital dentro de la transacción de la venta y devuelve el id de la transacción de pago. */
    private static int validarCobroDigital(Connection con, String token, String forma, double total) throws SQLException {
        if (token == null) throw new SQLException("Falta la autorización de la pasarela de pago para cobrar con " + forma + ".");
        try (PreparedStatement pst = con.prepareStatement("SELECT id, estado, forma_pago, monto, venta_id, usuario_id " +
                "FROM transacciones_pago WHERE token = ? FOR UPDATE")) {
            pst.setString(1, token);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) throw new SQLException("El token de pago no existe.");
                if (!ControladorPasarela.APROBADA.equals(rs.getString("estado"))) {
                    throw new SQLException("El cobro no está aprobado por la pasarela (" + rs.getString("estado") + ").");
                }
                if (rs.getObject("venta_id") != null) throw new SQLException("Ese cobro ya pertenece a otra venta.");
                if (rs.getInt("usuario_id") != Sesion.getId() || !forma.equals(rs.getString("forma_pago"))
                        || Math.abs(rs.getDouble("monto") - total) > 0.001) {
                    throw new SQLException("El cobro aprobado no coincide con la venta (monto o forma de pago).");
                }
                return rs.getInt("id");
            }
        }
    }

    /** Una venta con su desglose, o null si no existe. */
    public VentaVO obtenerVenta(int ventaId) throws SQLException {
        List<VentaVO> l = listar("WHERE v.id = ?", pst -> pst.setInt(1, ventaId));
        return l.isEmpty() ? null : l.get(0);
    }

    /** Ventas de un turno, de la más reciente a la más antigua. */
    public List<VentaVO> listarVentasTurno(int turnoId) throws SQLException {
        return listar("WHERE v.turno_id = ?", pst -> pst.setInt(1, turnoId));
    }

    /** Ventas de un rango de fechas; el personal solo ve las suyas. */
    public List<VentaVO> listarVentas(LocalDate desde, LocalDate hasta) throws SQLException {
        boolean soloPropias = !Sesion.esAdministrador();
        return listar("WHERE v.fecha BETWEEN ? AND ?" + (soloPropias ? " AND v.usuario_id = ?" : ""), pst -> {
            pst.setDate(1, java.sql.Date.valueOf(desde));
            pst.setDate(2, java.sql.Date.valueOf(hasta));
            if (soloPropias) pst.setInt(3, Sesion.getId());
        });
    }

    private interface Parametros {
        void poner(PreparedStatement pst) throws SQLException;
    }

    private List<VentaVO> listar(String filtro, Parametros parametros) throws SQLException {
        String sql = "SELECT v.id, v.turno_id, v.creado_en, v.forma_pago, v.estado, v.motivo_anulacion, " +
                "COALESCE(v.subtotal, v.total / (1 + " + Datos.IGV + ")) AS subtotal, v.descuento, v.total, v.monto_recibido, " +
                "c.nombre AS cliente, c.documento, COALESCE(NULLIF(u.nombre, ''), u.usuario) AS vendedor, " +
                "COALESCE(NULLIF(a.nombre, ''), a.usuario) AS anulado_por, cp.serie, cp.numero " +
                "FROM ventas v LEFT JOIN clientes c ON c.id = v.cliente_id LEFT JOIN comprobantes cp ON cp.venta_id = v.id " +
                "LEFT JOIN usuarios u ON u.id = v.usuario_id LEFT JOIN usuarios a ON a.id = v.anulado_por " +
                filtro + " ORDER BY v.id DESC";
        List<VentaVO> lista = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            parametros.poner(pst);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    VentaVO v = new VentaVO();
                    v.id = rs.getInt("id");
                    v.turnoId = rs.getInt("turno_id");
                    v.fecha = rs.getTimestamp("creado_en").toLocalDateTime();
                    v.formaPago = rs.getString("forma_pago");
                    v.estado = rs.getString("estado");
                    v.motivoAnulacion = rs.getString("motivo_anulacion");
                    v.subtotal = rs.getDouble("subtotal");
                    v.descuento = rs.getDouble("descuento");
                    v.total = rs.getDouble("total");
                    v.recibido = ControladorCaja.decimal(rs, "monto_recibido");
                    v.cliente = rs.getString("cliente");
                    v.documentoCliente = rs.getString("documento");
                    v.vendedor = rs.getString("vendedor");
                    v.anuladoPor = rs.getString("anulado_por");
                    if (rs.getString("serie") != null) v.comprobante = rs.getString("serie") + "-" + String.format("%08d", rs.getInt("numero"));
                    lista.add(v);
                }
            }
        }
        return lista;
    }

    public List<DetalleVO> detalleVenta(int ventaId) throws SQLException {
        String sql = "SELECT producto_nombre, cantidad, COALESCE(precio_unitario, importe / cantidad) AS precio, importe " +
                "FROM detalle_ventas WHERE venta_id = ? ORDER BY id";
        List<DetalleVO> lista = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, ventaId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    DetalleVO d = new DetalleVO();
                    d.producto = rs.getString("producto_nombre");
                    d.cantidad = rs.getInt("cantidad");
                    d.precio = rs.getDouble("precio");
                    d.importe = rs.getDouble("importe");
                    lista.add(d);
                }
            }
        }
        return lista;
    }

    /**
     * Anula una venta (solo el administrador y mientras su turno siga abierto, para no alterar un cuadre ya cerrado).
     * Devuelve las unidades a los mismos lotes de los que salieron y registra un movimiento "Anulación" por lote.
     */
    public void anularVenta(int ventaId, String motivo) throws SQLException {
        if (!Sesion.esAdministrador()) throw new SQLException("Solo el administrador puede anular ventas.");
        String sqlVenta = "SELECT v.estado, t.estado AS estado_turno FROM ventas v LEFT JOIN turnos t ON t.id = v.turno_id " +
                "WHERE v.id = ? FOR UPDATE";
        String sqlLotes = "SELECT vl.lote_id, vl.producto_id, vl.cantidad, l.numero_lote, p.nombre " +
                "FROM venta_lotes vl JOIN lotes l ON l.id = vl.lote_id JOIN productos p ON p.id = vl.producto_id " +
                "WHERE vl.venta_id = ? ORDER BY vl.id";
        String sqlUpdateLote = "UPDATE lotes SET cantidad = cantidad + ? WHERE id = ?";
        String sqlStockActual = "SELECT stock FROM productos WHERE id = ? FOR UPDATE";
        String sqlUpdateStock = "UPDATE productos SET stock = stock + ? WHERE id = ?";
        String sqlMovimiento = "INSERT INTO movimientos (fecha, producto_nombre, tipo, lote, cantidad, stock_resultante) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlAnular = "UPDATE ventas SET estado = 'ANULADA', anulado_por = ?, anulado_en = ?, motivo_anulacion = ? WHERE id = ?";

        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement pst = con.prepareStatement(sqlVenta)) {
                    pst.setInt(1, ventaId);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) throw new SQLException("La venta no existe.");
                        if (ANULADA.equals(rs.getString("estado"))) throw new SQLException("La venta ya está anulada.");
                        if (!ControladorCaja.ABIERTO.equals(rs.getString("estado_turno"))) {
                            throw new SQLException("Solo se pueden anular ventas de un turno abierto: el cuadre de este turno ya se cerró.");
                        }
                    }
                }

                int devueltos = 0;
                try (PreparedStatement pstLotes = con.prepareStatement(sqlLotes);
                     PreparedStatement pstUpdLote = con.prepareStatement(sqlUpdateLote);
                     PreparedStatement pstStockActual = con.prepareStatement(sqlStockActual);
                     PreparedStatement pstStock = con.prepareStatement(sqlUpdateStock);
                     PreparedStatement pstMov = con.prepareStatement(sqlMovimiento)) {
                    pstLotes.setInt(1, ventaId);
                    try (ResultSet rs = pstLotes.executeQuery()) {
                        while (rs.next()) {
                            int productoId = rs.getInt("producto_id"), c = rs.getInt("cantidad");
                            pstUpdLote.setInt(1, c);
                            pstUpdLote.setInt(2, rs.getInt("lote_id"));
                            pstUpdLote.executeUpdate();

                            int stock;
                            pstStockActual.setInt(1, productoId);
                            try (ResultSet rsStock = pstStockActual.executeQuery()) {
                                rsStock.next();
                                stock = rsStock.getInt(1) + c;
                            }
                            pstStock.setInt(1, c);
                            pstStock.setInt(2, productoId);
                            pstStock.executeUpdate();

                            pstMov.setString(1, LocalDate.now().format(Datos.FECHA));
                            pstMov.setString(2, rs.getString("nombre"));
                            pstMov.setString(3, "Anulación");
                            pstMov.setString(4, rs.getString("numero_lote"));
                            pstMov.setInt(5, c);
                            pstMov.setInt(6, stock);
                            pstMov.executeUpdate();
                            devueltos++;
                        }
                    }
                }
                if (devueltos == 0) throw new SQLException("La venta no tiene lotes registrados; no se puede devolver su stock.");

                try (PreparedStatement pst = con.prepareStatement(sqlAnular)) {
                    pst.setInt(1, Sesion.getId());
                    pst.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    pst.setString(3, motivo);
                    pst.setInt(4, ventaId);
                    pst.executeUpdate();
                }
                // Si se pagó con Yape o Plin, el cobro queda reembolsado
                try (PreparedStatement pst = con.prepareStatement("UPDATE transacciones_pago SET estado = 'REEMBOLSADA', " +
                        "mensaje = 'Reembolsado por anulación de la venta' WHERE venta_id = ? AND estado = 'APROBADA'")) {
                    pst.setInt(1, ventaId);
                    pst.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }
}
