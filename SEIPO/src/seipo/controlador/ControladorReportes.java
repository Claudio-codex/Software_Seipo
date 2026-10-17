package seipo.controlador;

import seipo.modelo.ConexionBD;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;

public class ControladorReportes {

    public static class DatosReporteVO {
        public double totalHoy;
        public double precioPromedio;
        public int numeroVentas;
        public int unidadesTotal;
        public double[] porDia = new double[7];
        public Map<String, double[]> ranking = new LinkedHashMap<>(); // nombre -> {unidades, importe}
    }

    public DatosReporteVO obtenerReporte() throws SQLException {
        DatosReporteVO vo = new DatosReporteVO();
        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        double totalGeneral = 0;

        String sqlVentas = "SELECT id, fecha, total FROM ventas";
        String sqlDetalle = "SELECT producto_nombre, cantidad, importe FROM detalle_ventas WHERE venta_id = ?";

        try (Connection con = ConexionBD.obtenerConexion();
             Statement st = con.createStatement();
             ResultSet rsVentas = st.executeQuery(sqlVentas)) {

            while (rsVentas.next()) {
                long ventaId = rsVentas.getLong("id");
                LocalDate fechaVenta = rsVentas.getDate("fecha").toLocalDate();
                double totalVenta = rsVentas.getDouble("total");

                vo.numeroVentas++;
                totalGeneral += totalVenta;

                if (fechaVenta.equals(hoy)) {
                    vo.totalHoy += totalVenta;
                }

                long d = java.time.temporal.ChronoUnit.DAYS.between(lunes, fechaVenta);
                if (d >= 0 && d < 7) {
                    vo.porDia[(int) d] += totalVenta;
                }

                try (PreparedStatement pstDetalle = con.prepareStatement(sqlDetalle)) {
                    pstDetalle.setLong(1, ventaId);
                    try (ResultSet rsDetalle = pstDetalle.executeQuery()) {
                        while (rsDetalle.next()) {
                            String nombreProd = rsDetalle.getString("producto_nombre");
                            int cantidad = rsDetalle.getInt("cantidad");
                            double importe = rsDetalle.getDouble("importe");

                            vo.unidadesTotal += cantidad;
                            double[] r = vo.ranking.computeIfAbsent(nombreProd, k -> new double[2]);
                            r[0] += cantidad;
                            r[1] += importe;
                        }
                    }
                }
            }
        }

        vo.precioPromedio = (vo.unidadesTotal == 0) ? 0 : totalGeneral / vo.unidadesTotal;
        return vo;
    }
}