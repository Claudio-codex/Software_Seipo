package seipo.controlador;

import seipo.modelo.ConexionBD;
import seipo.modelo.Sesion;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Alertas de stock bajo y sobrestock, proveedores y órdenes de compra. */
public class ControladorCompras {

    public static final String PENDIENTE = "PENDIENTE", APROBADA = "APROBADA",
            RECHAZADA = "RECHAZADA", RECIBIDA = "RECIBIDA";
    public static final String AGOTADO = "Agotado", STOCK_BAJO = "Stock bajo", SOBRESTOCK = "Sobrestock";

    /** Unidades del producto pedidas en órdenes pendientes o aprobadas que aún no llegan. */
    private static final String EN_PEDIDO =
            "(SELECT COALESCE(SUM(d.cantidad), 0) FROM detalle_ordenes d JOIN ordenes_compra o ON o.id = d.orden_id " +
            "WHERE d.producto_id = p.id AND o.estado IN ('PENDIENTE', 'APROBADA')) AS en_pedido";

    public static class AlertaVO {
        public int productoId, disponible, minimo, enPedido;
        public Integer maximo; // null: sin límite
        public String codigo, nombre, estado;

        public boolean esReposicion() { return !SOBRESTOCK.equals(estado); }

        /** Cantidad a pedir para llegar al máximo (o al doble del mínimo si no hay máximo), descontando lo ya pedido. */
        public int sugerido() {
            if (!esReposicion()) return 0;
            int objetivo = maximo != null ? maximo : minimo * 2;
            return Math.max(0, objetivo - disponible - enPedido);
        }
    }

    public static class ProveedorVO {
        public int id;
        public String ruc, razonSocial, contacto, telefono, correo;
        public boolean activo;

        @Override
        public String toString() { return razonSocial + " (RUC " + ruc + ")"; }
    }

    public static class OrdenVO {
        public int id, items, unidades;
        public String proveedor, estado, observacion, creadoPor, aprobadoPor, motivoRechazo, guia;
        public LocalDateTime creadoEn, aprobadoEn;
    }

    public static class DetalleOrdenVO {
        public int productoId, cantidad;
        public String codigo, nombre;

        public DetalleOrdenVO(int productoId, String codigo, String nombre, int cantidad) {
            this.productoId = productoId;
            this.codigo = codigo;
            this.nombre = nombre;
            this.cantidad = cantidad;
        }
    }

    // ---------------------------------------------------------------- Alertas

    /**
     * Productos agotados, con stock bajo (disponible ≤ mínimo) o con sobrestock (disponible > máximo).
     * Se usa el stock vendible: sin lotes vencidos ni unidades reservadas por mermas.
     */
    public List<AlertaVO> listarAlertas() throws SQLException {
        List<AlertaVO> lista = new ArrayList<>();
        String sql = "SELECT p.id, p.codigo, p.nombre, p.stock_minimo, p.stock_maximo, " +
                ControladorVentas.STOCK_DISPONIBLE + ", " + EN_PEDIDO + " FROM productos p ORDER BY p.nombre";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                AlertaVO a = new AlertaVO();
                a.productoId = rs.getInt("id");
                a.codigo = rs.getString("codigo");
                a.nombre = rs.getString("nombre");
                a.minimo = rs.getInt("stock_minimo");
                int max = rs.getInt("stock_maximo");
                a.maximo = rs.wasNull() ? null : max;
                a.disponible = rs.getInt("stock");
                a.enPedido = rs.getInt("en_pedido");
                if (a.disponible == 0) a.estado = AGOTADO;
                else if (a.disponible <= a.minimo) a.estado = STOCK_BAJO;
                else if (a.maximo != null && a.disponible > a.maximo) a.estado = SOBRESTOCK;
                else continue;
                lista.add(a);
            }
        }
        // Primero lo más urgente: agotados, luego stock bajo, al final sobrestock
        lista.sort((x, y) -> Integer.compare(prioridad(x.estado), prioridad(y.estado)));
        return lista;
    }

    private static int prioridad(String estado) {
        return AGOTADO.equals(estado) ? 0 : STOCK_BAJO.equals(estado) ? 1 : 2;
    }

    public int contarOrdenesPendientes() throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement("SELECT COUNT(*) FROM ordenes_compra WHERE estado = 'PENDIENTE'");
             ResultSet rs = pst.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ------------------------------------------------------------- Proveedores

    public List<ProveedorVO> listarProveedores(boolean soloActivos) throws SQLException {
        List<ProveedorVO> lista = new ArrayList<>();
        String sql = "SELECT id, ruc, razon_social, contacto, telefono, correo, activo FROM proveedores " +
                (soloActivos ? "WHERE activo = TRUE " : "") + "ORDER BY razon_social";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                ProveedorVO p = new ProveedorVO();
                p.id = rs.getInt("id");
                p.ruc = rs.getString("ruc");
                p.razonSocial = rs.getString("razon_social");
                p.contacto = rs.getString("contacto");
                p.telefono = rs.getString("telefono");
                p.correo = rs.getString("correo");
                p.activo = rs.getBoolean("activo");
                lista.add(p);
            }
        }
        return lista;
    }

    public void registrarProveedor(String ruc, String razonSocial, String contacto, String telefono, String correo) throws SQLException {
        String sql = "INSERT INTO proveedores (ruc, razon_social, contacto, telefono, correo) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, ruc);
            pst.setString(2, razonSocial);
            pst.setString(3, contacto);
            pst.setString(4, telefono);
            pst.setString(5, correo);
            pst.executeUpdate();
        }
    }

    public void actualizarProveedor(int id, String razonSocial, String contacto, String telefono, String correo, boolean activo) throws SQLException {
        exigirAdministrador("modificar proveedores");
        String sql = "UPDATE proveedores SET razon_social = ?, contacto = ?, telefono = ?, correo = ?, activo = ? WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, razonSocial);
            pst.setString(2, contacto);
            pst.setString(3, telefono);
            pst.setString(4, correo);
            pst.setBoolean(5, activo);
            pst.setInt(6, id);
            pst.executeUpdate();
        }
    }

    // ------------------------------------------------------- Órdenes de compra

    /** Crea una orden de compra PENDIENTE de aprobación con sus líneas. Devuelve su número. */
    public int crearOrden(int proveedorId, List<DetalleOrdenVO> lineas, String observacion) throws SQLException {
        if (lineas.isEmpty()) throw new SQLException("La orden debe tener al menos un producto.");
        String sqlOrden = "INSERT INTO ordenes_compra (proveedor_id, observacion, creado_por) VALUES (?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_ordenes (orden_id, producto_id, cantidad) VALUES (?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                int ordenId;
                try (PreparedStatement pst = con.prepareStatement(sqlOrden, Statement.RETURN_GENERATED_KEYS)) {
                    pst.setInt(1, proveedorId);
                    pst.setString(2, observacion);
                    pst.setInt(3, Sesion.getId());
                    pst.executeUpdate();
                    try (ResultSet rs = pst.getGeneratedKeys()) {
                        rs.next();
                        ordenId = rs.getInt(1);
                    }
                }
                try (PreparedStatement pst = con.prepareStatement(sqlDetalle)) {
                    for (DetalleOrdenVO d : lineas) {
                        if (d.cantidad <= 0) throw new SQLException("La cantidad de " + d.nombre + " debe ser mayor que 0.");
                        pst.setInt(1, ordenId);
                        pst.setInt(2, d.productoId);
                        pst.setInt(3, d.cantidad);
                        pst.executeUpdate();
                    }
                }
                con.commit();
                return ordenId;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Lista las órdenes, opcionalmente filtradas por estado (null = todas), de la más reciente a la más antigua. */
    public List<OrdenVO> listarOrdenes(String estado) throws SQLException {
        List<OrdenVO> lista = new ArrayList<>();
        String sql = "SELECT o.id, pr.razon_social, o.estado, o.observacion, o.creado_en, o.aprobado_en, o.motivo_rechazo, " +
                "uc.usuario AS creo, ua.usuario AS aprobo, r.guia_remision, " +
                "(SELECT COUNT(*) FROM detalle_ordenes d WHERE d.orden_id = o.id) AS items, " +
                "(SELECT COALESCE(SUM(d.cantidad), 0) FROM detalle_ordenes d WHERE d.orden_id = o.id) AS unidades " +
                "FROM ordenes_compra o JOIN proveedores pr ON pr.id = o.proveedor_id " +
                "JOIN usuarios uc ON uc.id = o.creado_por LEFT JOIN usuarios ua ON ua.id = o.aprobado_por " +
                "LEFT JOIN recepciones r ON r.id = o.recepcion_id " +
                (estado == null ? "" : "WHERE o.estado = ? ") +
                "ORDER BY o.id DESC";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            if (estado != null) pst.setString(1, estado);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    OrdenVO o = new OrdenVO();
                    o.id = rs.getInt("id");
                    o.proveedor = rs.getString("razon_social");
                    o.estado = rs.getString("estado");
                    o.observacion = rs.getString("observacion");
                    Timestamp creado = rs.getTimestamp("creado_en"), aprobado = rs.getTimestamp("aprobado_en");
                    o.creadoEn = creado == null ? null : creado.toLocalDateTime();
                    o.aprobadoEn = aprobado == null ? null : aprobado.toLocalDateTime();
                    o.motivoRechazo = rs.getString("motivo_rechazo");
                    o.creadoPor = rs.getString("creo");
                    o.aprobadoPor = rs.getString("aprobo");
                    o.guia = rs.getString("guia_remision");
                    o.items = rs.getInt("items");
                    o.unidades = rs.getInt("unidades");
                    lista.add(o);
                }
            }
        }
        return lista;
    }

    public List<DetalleOrdenVO> detalleOrden(int ordenId) throws SQLException {
        List<DetalleOrdenVO> lista = new ArrayList<>();
        String sql = "SELECT p.id, p.codigo, p.nombre, d.cantidad FROM detalle_ordenes d " +
                "JOIN productos p ON p.id = d.producto_id WHERE d.orden_id = ? ORDER BY d.id";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, ordenId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    lista.add(new DetalleOrdenVO(rs.getInt("id"), rs.getString("codigo"), rs.getString("nombre"), rs.getInt("cantidad")));
                }
            }
        }
        return lista;
    }

    public void aprobarOrden(int id) throws SQLException {
        cambiarEstado(id, APROBADA, null);
    }

    public void rechazarOrden(int id, String motivo) throws SQLException {
        cambiarEstado(id, RECHAZADA, motivo);
    }

    private void cambiarEstado(int id, String estado, String motivo) throws SQLException {
        exigirAdministrador("aprobar o rechazar órdenes de compra");
        String sql = "UPDATE ordenes_compra SET estado = ?, aprobado_por = ?, aprobado_en = NOW(), motivo_rechazo = ? " +
                "WHERE id = ? AND estado = 'PENDIENTE'";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, estado);
            pst.setInt(2, Sesion.getId());
            pst.setString(3, motivo);
            pst.setInt(4, id);
            if (pst.executeUpdate() == 0) throw new SQLException("La orden N° " + id + " ya fue procesada.");
        }
    }

    private static void exigirAdministrador(String accion) throws SQLException {
        if (!Sesion.esAdministrador()) throw new SQLException("Solo un administrador puede " + accion + ".");
    }
}
