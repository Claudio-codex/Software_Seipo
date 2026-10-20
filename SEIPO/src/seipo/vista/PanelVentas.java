package seipo.vista;

import seipo.controlador.ControladorCaja;
import seipo.controlador.ControladorClientes;
import seipo.controlador.ControladorClientes.ClienteVO;
import seipo.controlador.ControladorVentas;
import seipo.modelo.Datos;
import seipo.modelo.Datos.ProductoVenta;
import seipo.modelo.Sesion;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public class PanelVentas extends JPanel {

    private static final int COL_CANTIDAD = 2;
    private static final DecimalFormat PORCENTAJE = new DecimalFormat("#.##");

    private final JTextField txtBuscar = new JTextField();
    // Solo la cantidad se edita directamente en la tabla
    private final DefaultTableModel modelo = new DefaultTableModel(new String[]{"Código", "Descripción", "Cant", "P.Unit", "Importe"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return c == COL_CANTIDAD; }
        @Override public Class<?> getColumnClass(int c) { return c == COL_CANTIDAD ? Integer.class : Object.class; }
        @Override public void setValueAt(Object v, int r, int c) {
            if (c == COL_CANTIDAD && v instanceof Integer) cambiarCantidad(r, (Integer) v);
        }
    };
    private final JTable tabla = UI.tabla(modelo);
    private final List<ProductoVenta> items = new ArrayList<>();
    private final List<Integer> cantidades = new ArrayList<>();
    private final JLabel lblSub = new JLabel(), lblDesc = new JLabel(), lblIgv = new JLabel(), lblRedondeo = new JLabel(), lblTotal = new JLabel();
    private final JLabel lblArticulos = UI.ayuda(" ");
    private double descuentoPct = 0;

    private ClienteVO cliente;
    private final JLabel lblCliente = new JLabel(), lblClienteDetalle = UI.ayuda(" ");
    private final JButton quitarCliente = UI.botonSecundario("Quitar");

    private ControladorCaja.TurnoVO turno;
    private final JPanel barraTurno = new JPanel(new BorderLayout(12, 0));
    private final JLabel lblTurno = new JLabel(), lblTurnoIcono = new JLabel("●");
    private final JButton irCaja = UI.botonSecundario("Ir a Caja");
    private final List<JButton> botonesCobro = new ArrayList<>();

    private final JPopupMenu popupSugerencias = new JPopupMenu();
    private final JList<ProductoVenta> listaSugerencias = new JList<>();
    private final DefaultListModel<ProductoVenta> modeloSugerencias = new DefaultListModel<>();
    // Espera a que se deje de escribir (o de escanear) para consultar las sugerencias una sola vez
    private final Timer esperaSugerencias = new Timer(150, e -> filtrarSugerencias());
    private final ControladorVentas controlador = new ControladorVentas();
    private final ControladorCaja caja = new ControladorCaja();
    private final ControladorClientes clientes = new ControladorClientes();

    /** @param abrirCaja muestra la sección Caja (para abrir el turno) */
    public PanelVentas(Runnable abrirCaja) {
        setLayout(new BorderLayout());
        JPanel p = UI.base();
        add(p);

        // Arriba: estado del turno y buscador de productos
        JPanel norte = new JPanel(new BorderLayout(0, 12));
        norte.setOpaque(false);
        barraTurno.setBorder(new CompoundBorder(new LineBorder(UI.BORDE, 1, true), new EmptyBorder(4, 14, 4, 6)));
        lblTurno.setFont(UI.fuente(Font.PLAIN, 13));
        lblTurnoIcono.setFont(UI.fuente(Font.BOLD, 14));
        irCaja.setMargin(new Insets(3, 14, 3, 14));
        barraTurno.add(lblTurnoIcono, BorderLayout.WEST);
        barraTurno.add(lblTurno, BorderLayout.CENTER);
        barraTurno.add(irCaja, BorderLayout.EAST);
        irCaja.addActionListener(e -> abrirCaja.run());
        norte.add(barraTurno, BorderLayout.NORTH);

        JPanel buscador = new JPanel(new BorderLayout(8, 0));
        buscador.setOpaque(false);
        JButton agregar = UI.boton("Agregar");
        txtBuscar.putClientProperty("JTextField.placeholderText",
                "Escanee el código o escriba el nombre o la categoría  ·  3*CODIGO agrega 3 unidades");
        txtBuscar.setFont(UI.fuente(Font.PLAIN, 15));
        JLabel lblProducto = new JLabel("Producto (F2)  ");
        lblProducto.setForeground(UI.TEXTO_SECUNDARIO);
        buscador.add(lblProducto, BorderLayout.WEST);
        buscador.add(txtBuscar, BorderLayout.CENTER);
        buscador.add(agregar, BorderLayout.EAST);
        norte.add(buscador, BorderLayout.CENTER);
        p.add(norte, BorderLayout.NORTH);

        // Centro: carrito con sus acciones
        int[] anchos = {75, 360, 50, 80, 90};
        for (int i = 0; i < anchos.length; i++) UI.columnaAncha(tabla, i, anchos[i]);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JPanel carrito = new JPanel(new BorderLayout(0, 8));
        carrito.setOpaque(false);
        carrito.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel accionesCarrito = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        accionesCarrito.setOpaque(false);
        JButton menos = UI.botonSecundario("−"), mas = UI.botonSecundario("+");
        JButton quitar = UI.botonPeligro("Quitar ítem"), vaciar = UI.botonSecundario("Vaciar venta");
        menos.setToolTipText("Restar una unidad al ítem seleccionado (tecla −)");
        mas.setToolTipText("Sumar una unidad al ítem seleccionado (tecla +)");
        quitar.setToolTipText("Quitar el ítem seleccionado (tecla Supr)");
        for (JButton b : new JButton[]{menos, mas, quitar, vaciar}) accionesCarrito.add(b);
        tabla.setToolTipText("Doble clic en Cant para escribir la cantidad · + / − cambian la cantidad · Supr quita el ítem");
        carrito.add(accionesCarrito, BorderLayout.SOUTH);
        p.add(carrito, BorderLayout.CENTER);

        // Derecha: cliente, totales y formas de pago
        // Los botones de cobro van abajo para que se vean aunque la ventana sea baja
        JPanel lateral = new JPanel(new BorderLayout(0, 10));
        lateral.setOpaque(false);
        lateral.setPreferredSize(new Dimension(330, 0));
        lateral.add(tarjetaCliente(), BorderLayout.NORTH);
        JPanel envolturaTotales = new JPanel(new BorderLayout());
        envolturaTotales.setOpaque(false);
        envolturaTotales.add(tarjetaTotales(), BorderLayout.NORTH);
        lateral.add(envolturaTotales, BorderLayout.CENTER);
        lateral.add(botonesPago(), BorderLayout.SOUTH);
        p.add(lateral, BorderLayout.EAST);

        configurarSugerencias();
        configurarTeclado();

        agregar.addActionListener(e -> agregar());
        menos.addActionListener(e -> sumar(-1));
        mas.addActionListener(e -> sumar(1));
        quitar.addActionListener(e -> quitar());
        vaciar.addActionListener(e -> {
            if (!items.isEmpty() && JOptionPane.showConfirmDialog(this, "¿Vaciar la venta actual?", "Vaciar venta",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) limpiarVenta();
        });
        quitarCliente.addActionListener(e -> ponerCliente(null));
        ponerCliente(null);
        refrescarTabla();
    }

    private JPanel tarjetaCliente() {
        JPanel c = new JPanel(new BorderLayout(10, 0));
        c.setBackground(UI.SUPERFICIE);
        c.setBorder(new CompoundBorder(new LineBorder(UI.BORDE, 1, true), new EmptyBorder(8, 14, 8, 10)));
        JLabel t = UI.ayuda("CLIENTE (F4)");
        t.setFont(UI.fuente(Font.BOLD, 11));
        lblCliente.setFont(UI.fuente(Font.BOLD, 14));
        JPanel textos = new JPanel(new GridLayout(0, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(t);
        textos.add(lblCliente);
        textos.add(lblClienteDetalle);
        c.add(textos, BorderLayout.CENTER);
        JPanel botones = new JPanel(new GridLayout(0, 1, 0, 6));
        botones.setOpaque(false);
        JButton buscar = UI.botonSecundario("Buscar");
        buscar.setToolTipText("Buscar o registrar el cliente de la venta (F4)");
        buscar.addActionListener(e -> elegirCliente());
        for (JButton b : new JButton[]{buscar, quitarCliente}) {
            b.setFont(UI.fuente(Font.BOLD, 12));
            b.setMargin(new Insets(2, 12, 2, 12));
            botones.add(b);
        }
        JPanel derecha = new JPanel(new GridBagLayout());
        derecha.setOpaque(false);
        derecha.add(botones);
        c.add(derecha, BorderLayout.EAST);
        return c;
    }

    // Resumen de importes: desglose a la izquierda, monto a la derecha y el total destacado
    private JPanel tarjetaTotales() {
        JPanel totales = new JPanel(new GridBagLayout());
        totales.setBackground(UI.SUPERFICIE);
        totales.setBorder(new CompoundBorder(new LineBorder(UI.BORDE, 1, true), new EmptyBorder(12, 16, 12, 16)));
        totales.setAlignmentX(LEFT_ALIGNMENT);
        totales.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(2, 0, 2, 0);
        String[] conceptos = {"Subtotal", "Descuento", "IGV 18%", "Redondeo"};
        JLabel[] montos = {lblSub, lblDesc, lblIgv, lblRedondeo};
        for (int i = 0; i < conceptos.length; i++) {
            g.gridy = i;
            g.gridx = 0; g.weightx = 1;
            if (i == 1) {
                // El concepto Descuento es un botón para cambiar el porcentaje
                JButton c = UI.botonSecundario("Descuento (F6)");
                c.setFont(UI.fuente(Font.PLAIN, 12));
                c.setMargin(new Insets(1, 8, 1, 8));
                c.setToolTipText("Aplicar un porcentaje de descuento a toda la venta");
                c.addActionListener(e -> aplicarDescuento());
                JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                izq.setOpaque(false);
                izq.add(c);
                totales.add(izq, g);
            } else {
                JLabel c = new JLabel(conceptos[i]);
                c.setForeground(UI.TEXTO_SECUNDARIO);
                totales.add(c, g);
            }
            g.gridx = 1; g.weightx = 0;
            montos[i].setHorizontalAlignment(SwingConstants.RIGHT);
            totales.add(montos[i], g);
        }
        g.gridy = 4; g.gridx = 0; g.gridwidth = 2;
        JSeparator sep = new JSeparator();
        sep.setForeground(UI.BORDE);
        g.insets = new Insets(8, 0, 6, 0);
        totales.add(sep, g);
        g.gridy = 5; g.gridwidth = 1; g.insets = new Insets(0, 0, 0, 0);
        JLabel t = new JLabel("TOTAL");
        t.setFont(UI.fuente(Font.BOLD, 16));
        totales.add(t, g);
        g.gridx = 1;
        lblTotal.setFont(UI.fuente(Font.BOLD, 28));
        lblTotal.setForeground(UI.ACENTO);
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        totales.add(lblTotal, g);
        return totales;
    }

    /** Cobrar con: una fila de tres botones, cada uno con su tecla de atajo debajo. */
    private JPanel botonesPago() {
        JPanel pago = new JPanel(new BorderLayout(0, 6));
        pago.setOpaque(false);
        pago.setAlignmentX(LEFT_ALIGNMENT);
        JLabel cobrarCon = new JLabel("COBRAR CON");
        cobrarCon.setFont(UI.fuente(Font.BOLD, 11));
        cobrarCon.setForeground(UI.TEXTO_SECUNDARIO);
        // Junto al título va el conteo de productos y unidades de la venta
        JPanel titulo = new JPanel(new BorderLayout());
        titulo.setOpaque(false);
        titulo.add(cobrarCon, BorderLayout.WEST);
        titulo.add(lblArticulos, BorderLayout.EAST);
        pago.add(titulo, BorderLayout.NORTH);
        JPanel fila = new JPanel(new GridLayout(1, 0, 8, 0));
        fila.setOpaque(false);
        for (String f : Datos.FORMAS_PAGO) {
            JButton b = UI.boton("<html><center>" + f + "<br><span style='font-size:9px'>" + teclaDe(f) + "</span></center></html>");
            b.setFont(UI.fuente(Font.BOLD, 14));
            b.setMargin(new Insets(8, 4, 8, 4));
            b.addActionListener(e -> cobrar(f));
            botonesCobro.add(b);
            fila.add(b);
        }
        pago.add(fila, BorderLayout.CENTER);
        pago.setMaximumSize(new Dimension(Integer.MAX_VALUE, pago.getPreferredSize().height));
        return pago;
    }

    private void configurarSugerencias() {
        listaSugerencias.setModel(modeloSugerencias);
        listaSugerencias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        esperaSugerencias.setRepeats(false);
        listaSugerencias.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JPanel fila = new JPanel(new BorderLayout(16, 0));
            fila.setBorder(new EmptyBorder(6, 10, 6, 12));
            fila.setBackground(isSelected ? UI.ACENTO_OSCURO : UI.SUPERFICIE);
            JLabel nombre = new JLabel("<html><b>" + value.nombre + "</b> &nbsp;<span style='color:#94A3B8'>"
                    + value.codigo + " · " + (value.categoria == null ? "" : value.categoria) + "</span></html>");
            nombre.setForeground(UI.TEXTO);
            JLabel datos = new JLabel(Datos.soles(value.precio) + "   " + (value.stock > 0 ? "Stock " + value.stock : "Agotado"));
            datos.setForeground(value.stock > 0 ? UI.TEXTO_SECUNDARIO : UI.PELIGRO);
            fila.add(nombre, BorderLayout.CENTER);
            fila.add(datos, BorderLayout.EAST);
            return fila;
        });
        JScrollPane sp = new JScrollPane(listaSugerencias);
        sp.setBorder(null);
        popupSugerencias.add(sp);
        popupSugerencias.setFocusable(false);

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { esperaSugerencias.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { esperaSugerencias.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { esperaSugerencias.restart(); }
        });

        listaSugerencias.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) seleccionarSugerencia();
            }
        });

        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                // Si quedó una búsqueda pendiente, se hace ya para decidir con las sugerencias del texto completo
                if (esperaSugerencias.isRunning() && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_DOWN)) {
                    esperaSugerencias.stop();
                    filtrarSugerencias();
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN && popupSugerencias.isVisible()) {
                    listaSugerencias.requestFocus();
                    listaSugerencias.setSelectedIndex(0);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popupSugerencias.setVisible(false);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // Un código exacto (lector de barras) se agrega directo; si no, se toma la primera sugerencia
                    if (popupSugerencias.isVisible() && !modeloSugerencias.isEmpty()
                            && !modeloSugerencias.get(0).codigo.equalsIgnoreCase(codigoBuscado())) {
                        listaSugerencias.setSelectedIndex(0);
                        seleccionarSugerencia();
                    } else {
                        agregar();
                    }
                }
            }
        });

        listaSugerencias.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) seleccionarSugerencia();
                else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popupSugerencias.setVisible(false);
                    txtBuscar.requestFocusInWindow();
                }
            }
        });
    }

    private void configurarTeclado() {
        atajo(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "buscar", () -> txtBuscar.requestFocusInWindow());
        atajo(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "cliente", this::elegirCliente);
        atajo(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "descuento", this::aplicarDescuento);
        atajo(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "efectivo", () -> cobrar(Datos.EFECTIVO));
        atajo(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "yape", () -> cobrar(Datos.YAPE_PLIN));

        // Teclas sobre el carrito: + y − cambian la cantidad, Supr quita el ítem
        InputMap im = tabla.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = tabla.getActionMap();
        for (int k : new int[]{KeyEvent.VK_ADD, KeyEvent.VK_PLUS}) im.put(KeyStroke.getKeyStroke(k, 0), "mas");
        for (int k : new int[]{KeyEvent.VK_SUBTRACT, KeyEvent.VK_MINUS}) im.put(KeyStroke.getKeyStroke(k, 0), "menos");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "quitar");
        am.put("mas", accion(() -> sumar(1)));
        am.put("menos", accion(() -> sumar(-1)));
        am.put("quitar", accion(this::quitar));
    }

    /** Tecla de atajo que se muestra en el botón de cada forma de pago. */
    private static String teclaDe(String forma) {
        return Datos.EFECTIVO.equals(forma) ? "F9" : "F11";
    }

    private void atajo(KeyStroke tecla, String nombre, Runnable r) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(tecla, nombre);
        getActionMap().put(nombre, accion(r));
    }

    private static Action accion(Runnable r) {
        return new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { r.run(); }
        };
    }

    /** Se llama al mostrar la pantalla: actualiza el estado del turno de caja. */
    public void refrescar() {
        try {
            turno = caja.turnoAbierto();
        } catch (SQLException ex) {
            turno = null;
            ex.printStackTrace();
        }
        if (turno != null) {
            barraTurno.setBackground(UI.FILA_VERDE);
            lblTurnoIcono.setForeground(UI.EXITO);
            lblTurno.setText("Turno abierto desde " + turno.abiertoEn.format(Datos.FECHA_HORA) + "   ·   "
                    + turno.numVentas + " venta(s) por " + Datos.soles(turno.totalVendido())
                    + "   ·   Efectivo en caja: " + Datos.soles(turno.efectivoEnCaja()));
            irCaja.setText("Ver caja");
        } else {
            barraTurno.setBackground(UI.FILA_AMBAR);
            lblTurnoIcono.setForeground(UI.ALERTA);
            lblTurno.setText("No tiene un turno de caja abierto. Puede armar la venta, pero para cobrar debe abrir su turno.");
            irCaja.setText("Abrir turno");
        }
        for (JButton b : botonesCobro) {
            b.setEnabled(turno != null);
            b.setToolTipText(turno == null ? "Abra su turno en la sección Caja para cobrar" : null);
        }
        SwingUtilities.invokeLater(txtBuscar::requestFocusInWindow);
    }

    /** Cantidad pedida con el formato N*CODIGO (1 si no se indica). */
    private int cantidadBuscada() {
        String t = txtBuscar.getText().trim();
        int i = t.indexOf('*');
        if (i <= 0) return 1;
        try {
            return Integer.parseInt(t.substring(0, i).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Texto buscado sin el prefijo de cantidad N*. */
    private String codigoBuscado() {
        String t = txtBuscar.getText().trim();
        int i = t.indexOf('*');
        return i < 0 ? t : t.substring(i + 1).trim();
    }

    private void filtrarSugerencias() {
        String texto = codigoBuscado();
        modeloSugerencias.clear();

        if (texto.isEmpty()) {
            popupSugerencias.setVisible(false);
            return;
        }

        try {
            List<ProductoVenta> encontrados = controlador.filtrarSugerencias(texto);
            if (encontrados.isEmpty()) {
                popupSugerencias.setVisible(false);
            } else {
                for (ProductoVenta p : encontrados) modeloSugerencias.addElement(p);
                listaSugerencias.setVisibleRowCount(Math.min(encontrados.size(), 8));
                popupSugerencias.setPopupSize(txtBuscar.getWidth(), popupSugerencias.getPreferredSize().height);
                popupSugerencias.show(txtBuscar, 0, txtBuscar.getHeight());
                txtBuscar.requestFocusInWindow();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void seleccionarSugerencia() {
        ProductoVenta pr = listaSugerencias.getSelectedValue();
        if (pr != null) {
            int c = cantidadBuscada();
            txtBuscar.setText((c > 1 ? c + "*" : "") + pr.codigo);
            popupSugerencias.setVisible(false);
            agregar();
        }
    }

    private void agregar() {
        popupSugerencias.setVisible(false);
        String textoBuscado = codigoBuscado();
        if (textoBuscado.isEmpty()) return;
        int c = cantidadBuscada();
        if (c <= 0) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida. Use, por ejemplo, 3*CODIGO.");
            return;
        }

        try {
            ProductoVenta pr = controlador.buscarProductoBD(textoBuscado);
            if (pr == null) {
                JOptionPane.showMessageDialog(this, "Producto no encontrado.");
                return;
            }

            int i = indiceDe(pr.codigo);
            int yaEnCarrito = i != -1 ? cantidades.get(i) : 0;
            if (c + yaEnCarrito > pr.stock) {
                JOptionPane.showMessageDialog(this, pr.stock <= 0 ? pr.nombre + " está agotado."
                        : "Stock insuficiente para " + pr.nombre + ": quedan " + pr.stock + " unidad(es) disponibles.");
                return;
            }

            if (i != -1) {
                items.set(i, pr); // stock actualizado
                cantidades.set(i, yaEnCarrito + c);
            } else {
                items.add(pr);
                cantidades.add(c);
                i = items.size() - 1;
            }
            txtBuscar.setText("");
            refrescarTabla();
            tabla.setRowSelectionInterval(i, i);
            tabla.scrollRectToVisible(tabla.getCellRect(i, 0, true));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al buscar el producto:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int indiceDe(String codigo) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).codigo.equalsIgnoreCase(codigo)) return i;
        }
        return -1;
    }

    /** Suma (o resta) unidades al ítem seleccionado. */
    private void sumar(int delta) {
        int f = tabla.getSelectedRow();
        if (f < 0 || tabla.isEditing()) return;
        cambiarCantidad(f, cantidades.get(f) + delta);
    }

    /** Cambia la cantidad de una fila validando el stock disponible; con 0 o menos se quita el ítem. */
    private void cambiarCantidad(int fila, int nueva) {
        ProductoVenta p = items.get(fila);
        if (nueva <= 0) {
            items.remove(fila);
            cantidades.remove(fila);
            fila = Math.min(fila, items.size() - 1);
        } else if (nueva > p.stock) {
            JOptionPane.showMessageDialog(this, "Stock insuficiente para " + p.nombre + ": quedan " + p.stock + " unidad(es) disponibles.");
            return;
        } else {
            cantidades.set(fila, nueva);
        }
        int seleccion = fila;
        SwingUtilities.invokeLater(() -> {
            refrescarTabla();
            if (seleccion >= 0 && seleccion < items.size()) tabla.setRowSelectionInterval(seleccion, seleccion);
        });
    }

    private void aplicarDescuento() {
        String s = JOptionPane.showInputDialog(this, "Descuento (%):", PORCENTAJE.format(descuentoPct));
        if (s == null) return;
        try {
            double d = Double.parseDouble(s.trim().replace(',', '.'));
            if (d < 0 || d > 100) throw new NumberFormatException();
            // El personal tiene un tope de descuento manual; el VIP del cliente elegido siempre se respeta
            if (!Sesion.esAdministrador() && d > Datos.DESCUENTO_MAX_PERSONAL && (cliente == null || d > cliente.porcentajeVIP)) {
                JOptionPane.showMessageDialog(this, ControladorVentas.mensajeTopeDescuento(), "Descuento no permitido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            descuentoPct = d;
            recalcular();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Porcentaje inválido (0-100).");
        }
    }

    private void quitar() {
        int f = tabla.getSelectedRow();
        if (f >= 0) cambiarCantidad(f, 0);
    }

    /** Busca un cliente por nombre o DNI/RUC (o lo registra) y lo asigna a la venta. */
    private void elegirCliente() {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "Cliente de la venta", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel c = UI.base();
        JTextField filtro = new JTextField();
        filtro.putClientProperty("JTextField.placeholderText", "Nombre, DNI o RUC");
        DefaultListModel<ClienteVO> encontrados = new DefaultListModel<>();
        JList<ClienteVO> lista = new JList<>(encontrados);
        lista.setCellRenderer((l, v, i, sel, foco) -> {
            JLabel lbl = new JLabel("<html><b>" + v.nombre + "</b><br><span style='color:#94A3B8'>" + v.documento
                    + " · " + v.numCompras + " compra(s) · " + v.descuentoVIP + "</span></html>");
            lbl.setBorder(new EmptyBorder(6, 10, 6, 10));
            lbl.setOpaque(true);
            lbl.setBackground(sel ? UI.ACENTO_OSCURO : UI.SUPERFICIE);
            lbl.setForeground(UI.TEXTO);
            return lbl;
        });
        Runnable buscar = () -> {
            encontrados.clear();
            try {
                for (ClienteVO cl : clientes.buscarClientes(filtro.getText().trim())) encontrados.addElement(cl);
                if (!encontrados.isEmpty()) lista.setSelectedIndex(0);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        };
        filtro.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { buscar.run(); }
            @Override public void removeUpdate(DocumentEvent e) { buscar.run(); }
            @Override public void changedUpdate(DocumentEvent e) { buscar.run(); }
        });
        Runnable elegir = () -> {
            if (lista.getSelectedValue() == null) return;
            ponerCliente(lista.getSelectedValue());
            d.dispose();
        };
        filtro.addActionListener(e -> elegir.run());
        filtro.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                int i = lista.getSelectedIndex();
                if (e.getKeyCode() == KeyEvent.VK_DOWN && i < encontrados.size() - 1) lista.setSelectedIndex(i + 1);
                else if (e.getKeyCode() == KeyEvent.VK_UP && i > 0) lista.setSelectedIndex(i - 1);
                lista.ensureIndexIsVisible(lista.getSelectedIndex());
            }
        });
        lista.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) elegir.run();
            }
        });

        JButton usar = UI.boton("Usar cliente"), nuevo = UI.botonSecundario("+ Nuevo cliente"), cancelar = UI.botonSecundario("Cancelar");
        usar.addActionListener(e -> elegir.run());
        cancelar.addActionListener(e -> d.dispose());
        nuevo.addActionListener(e -> {
            String doc = registrarCliente(d);
            if (doc != null) filtro.setText(doc);
        });
        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(nuevo, BorderLayout.WEST);
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        derecha.setOpaque(false);
        derecha.add(cancelar);
        derecha.add(usar);
        sur.add(derecha, BorderLayout.EAST);

        c.add(filtro, BorderLayout.NORTH);
        c.add(new JScrollPane(lista), BorderLayout.CENTER);
        c.add(sur, BorderLayout.SOUTH);
        d.setContentPane(c);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        buscar.run();
        d.setSize(520, 460);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    /** Registra un cliente nuevo; devuelve su documento, o null si se canceló. */
    private String registrarCliente(Component padre) {
        JTextField n = new JTextField(), doc = new JTextField(), t = new JTextField(), c = new JTextField();
        Object[] campos = {"Nombre:", n, "DNI / RUC:", doc, "Teléfono:", t, "Correo:", c};
        if (JOptionPane.showConfirmDialog(padre, campos, "Registrar nuevo cliente", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return null;
        }
        String documento = doc.getText().trim();
        if (n.getText().trim().isEmpty() || !(documento.matches("\\d{8}") || documento.matches("\\d{11}"))) {
            JOptionPane.showMessageDialog(padre, "Nombre obligatorio y DNI (8 dígitos) o RUC (11 dígitos).");
            return null;
        }
        try {
            clientes.registrarCliente(n.getText().trim(), documento, t.getText().trim(), c.getText().trim());
            return documento;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(padre, "Error al registrar (es posible que el DNI/RUC ya esté registrado):\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /** Asigna el cliente y aplica su descuento VIP (al quitarlo se retira ese descuento). */
    private void ponerCliente(ClienteVO nuevo) {
        if (cliente != null && descuentoPct == cliente.porcentajeVIP) descuentoPct = 0;
        cliente = nuevo;
        if (cliente == null) {
            lblCliente.setText("Clientes varios");
            lblClienteDetalle.setText("Sin cliente registrado");
        } else {
            if (cliente.porcentajeVIP > descuentoPct) descuentoPct = cliente.porcentajeVIP;
            lblCliente.setText(cliente.nombre);
            lblClienteDetalle.setText(cliente.documento + "  ·  " + cliente.descuentoVIP);
        }
        quitarCliente.setEnabled(cliente != null);
        recalcular();
    }

    private void refrescarTabla() {
        if (tabla.isEditing()) tabla.getCellEditor().cancelCellEditing();
        modelo.setRowCount(0);
        for (int i = 0; i < items.size(); i++) {
            ProductoVenta p = items.get(i);
            int c = cantidades.get(i);
            modelo.addRow(new Object[]{p.codigo, p.nombre, c, Datos.soles(p.precio), Datos.soles(p.precio * c)});
        }
        recalcular();
    }

    private double subtotal() {
        double s = 0;
        for (int i = 0; i < items.size(); i++) {
            s += items.get(i).precio * cantidades.get(i);
        }
        return Datos.redondear(s);
    }

    private double descuento() {
        return Datos.redondear(subtotal() * descuentoPct / 100);
    }

    private void recalcular() {
        double sub = subtotal();
        double desc = descuento();
        double total = Datos.totalVenta(sub, desc);
        lblSub.setText(Datos.soles(sub));
        lblDesc.setText((descuentoPct > 0 ? "− " : "") + Datos.soles(desc) + (descuentoPct > 0 ? "  (" + PORCENTAJE.format(descuentoPct) + "%)" : ""));
        lblIgv.setText(Datos.soles(Datos.igvVenta(sub, desc)));
        double redondeo = Datos.redondeoVenta(sub, desc);
        lblRedondeo.setText((redondeo < 0 ? "− " : "") + Datos.soles(Math.abs(redondeo)));
        lblTotal.setText(Datos.soles(total));
        int unidades = cantidades.stream().mapToInt(Integer::intValue).sum();
        lblArticulos.setText(items.isEmpty() ? "Sin productos" : items.size() + " producto(s) · " + unidades + " unidad(es)");
    }

    private void cobrar(String forma) {
        if (turno == null) {
            JOptionPane.showMessageDialog(this, "Abra su turno en la sección Caja antes de cobrar.");
            return;
        }
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay productos en la venta.");
            return;
        }
        double total = Datos.totalVenta(subtotal(), descuento());

        Double recibido = null;
        if (Datos.EFECTIVO.equals(forma)) {
            recibido = pedirEfectivo(total);
            if (recibido == null) return;
        }

        // Yape o Plin: el sistema solo registra la forma de pago; el cajero confirma que llegó el pago al celular del negocio
        if (!Datos.EFECTIVO.equals(forma) && JOptionPane.showConfirmDialog(this,
                "¿El cliente ya pagó " + Datos.soles(total) + " con " + forma + "?\nConfirme el pago en el celular del negocio antes de registrar la venta.",
                "Cobro con " + forma, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int ventaId = controlador.cobrarVenta(items, cantidades, descuentoPct, forma,
                    cliente == null ? null : cliente.id, recibido);
            limpiarVenta();
            refrescar();
            DialogoComprobante.mostrar(this, ventaId, true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar la venta en la base de datos:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Pide el efectivo recibido y muestra el vuelto; devuelve null si se cancela. */
    private Double pedirEfectivo(double total) {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "Cobro en efectivo", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel c = UI.base();
        JLabel lblCobrar = new JLabel("Total a cobrar: " + Datos.soles(total));
        lblCobrar.setFont(UI.fuente(Font.BOLD, 20));
        JTextField txtRecibido = new JTextField();
        txtRecibido.setFont(UI.fuente(Font.BOLD, 22));
        txtRecibido.putClientProperty("JTextField.placeholderText", "Monto recibido");
        JLabel lblVuelto = new JLabel(" ");
        lblVuelto.setFont(UI.fuente(Font.BOLD, 22));
        Double[] resultado = {null};

        Runnable actualizar = () -> {
            Double r = leerMonto(txtRecibido.getText());
            if (r == null) {
                lblVuelto.setForeground(UI.TEXTO_SECUNDARIO);
                lblVuelto.setText("Ingrese el monto recibido (sin monedas de 1 ni 5 céntimos)");
            } else if (r < total) {
                lblVuelto.setForeground(UI.PELIGRO);
                lblVuelto.setText("Faltan " + Datos.soles(total - r));
            } else {
                lblVuelto.setForeground(UI.EXITO);
                lblVuelto.setText("Vuelto: " + Datos.soles(r - total));
            }
        };
        txtRecibido.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { actualizar.run(); }
            @Override public void removeUpdate(DocumentEvent e) { actualizar.run(); }
            @Override public void changedUpdate(DocumentEvent e) { actualizar.run(); }
        });
        Runnable confirmar = () -> {
            Double r = leerMonto(txtRecibido.getText());
            if (r == null || r < total) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            resultado[0] = r;
            d.dispose();
        };
        txtRecibido.addActionListener(e -> confirmar.run());

        // Montos rápidos: exacto y billetes comunes que alcanzan para el total
        JPanel rapidos = new JPanel(new GridLayout(1, 0, 6, 0));
        rapidos.setOpaque(false);
        JButton exacto = UI.botonSecundario("Exacto");
        exacto.addActionListener(e -> txtRecibido.setText(String.format(java.util.Locale.US, "%.2f", total)));
        rapidos.add(exacto);
        // Redondeos al sol y a la decena, y billetes que alcanzan para el total (hasta 4 opciones)
        java.util.TreeSet<Integer> montos = new java.util.TreeSet<>();
        montos.add((int) Math.ceil(total));
        montos.add((int) Math.ceil(total / 10) * 10);
        for (int billete : new int[]{20, 50, 100, 200}) if (billete >= total) montos.add(billete);
        int opciones = 0;
        for (int billete : montos) {
            if (billete == total || ++opciones > 4) continue;
            JButton b = UI.botonSecundario("S/ " + billete);
            b.addActionListener(e -> txtRecibido.setText(String.valueOf(billete)));
            rapidos.add(b);
        }

        JPanel centro = new JPanel(new GridLayout(0, 1, 0, 10));
        centro.setOpaque(false);
        centro.add(txtRecibido);
        centro.add(rapidos);
        centro.add(lblVuelto);
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        sur.setOpaque(false);
        JButton cancelar = UI.botonSecundario("Cancelar"), cobrar = UI.boton("Cobrar");
        cancelar.addActionListener(e -> d.dispose());
        cobrar.addActionListener(e -> confirmar.run());
        sur.add(cancelar);
        sur.add(cobrar);
        c.add(lblCobrar, BorderLayout.NORTH);
        c.add(centro, BorderLayout.CENTER);
        c.add(sur, BorderLayout.SOUTH);
        d.setContentPane(c);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        actualizar.run();
        d.pack();
        d.setSize(Math.max(d.getWidth(), 460), d.getHeight());
        d.setLocationRelativeTo(this);
        d.setVisible(true);
        return resultado[0];
    }

    private static Double leerMonto(String texto) {
        return PanelCaja.leerMonto(texto);
    }

    private void limpiarVenta() {
        items.clear();
        cantidades.clear();
        descuentoPct = 0;
        cliente = null;
        ponerCliente(null);
        refrescarTabla();
        txtBuscar.requestFocusInWindow();
    }
}
