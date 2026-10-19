package seipo.vista;

import seipo.controlador.ControladorPasarela;
import seipo.controlador.ControladorPasarela.TransaccionVO;
import seipo.modelo.Datos;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Cobro con Yape o Plin a través de la pasarela: envía la solicitud de cobro, muestra el token de pago
 * y espera la respuesta de autorización. Si se rechaza, se puede corregir los datos y reintentar.
 */
class DialogoPasarela extends JDialog {

    private final ControladorPasarela pasarela = new ControladorPasarela();
    private final String forma;
    private final double total;

    private final JTextField txtCelular = new JTextField(), txtCodigo = new JTextField();
    private final JRadioButton rbYape = new JRadioButton(ControladorPasarela.YAPE, true),
            rbPlin = new JRadioButton(ControladorPasarela.PLIN);
    private final JLabel lblError = new JLabel(" ");
    private final JPanel pasos = new JPanel(new GridLayout(3, 1, 0, 4));
    private final JButton enviar = UI.boton("Enviar cobro"), cancelar = UI.botonSecundario("Cancelar");
    private boolean procesando;
    private String tokenAprobado;

    /** Muestra el diálogo y devuelve el token del cobro aprobado, o null si se canceló. */
    static String cobrar(Component padre, String forma, double total, String proveedor) {
        DialogoPasarela d = new DialogoPasarela(SwingUtilities.getWindowAncestor(padre), forma, total, proveedor);
        d.setVisible(true);
        return d.tokenAprobado;
    }

    private DialogoPasarela(Window duenio, String forma, double total, String proveedor) {
        super(duenio, "Cobro con " + forma, ModalityType.APPLICATION_MODAL);
        this.forma = forma;
        this.total = total;

        JPanel c = UI.base();
        JPanel norte = new JPanel(new GridLayout(0, 1, 0, 2));
        norte.setOpaque(false);
        JLabel titulo = new JLabel("Cobrar " + Datos.soles(total) + " con " + forma);
        titulo.setFont(UI.fuente(Font.BOLD, 20));
        norte.add(titulo);
        norte.add(UI.ayuda("Pasarela: " + proveedor + "  ·  Modo prueba: no se hacen cargos reales"));
        c.add(norte, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);
        centro.add(formularioBilletera(), BorderLayout.NORTH);
        lblError.setForeground(UI.PELIGRO);
        lblError.setFont(UI.fuente(Font.PLAIN, 13));
        centro.add(lblError, BorderLayout.CENTER);
        pasos.setBackground(UI.SUPERFICIE);
        pasos.setBorder(new CompoundBorder(new LineBorder(UI.BORDE, 1, true), new EmptyBorder(10, 14, 10, 14)));
        pasos.setPreferredSize(new Dimension(0, 96));
        paso("Complete los datos y pulse Enviar cobro.", UI.TEXTO_SECUNDARIO);
        centro.add(pasos, BorderLayout.SOUTH);
        c.add(centro, BorderLayout.CENTER);

        JPanel sur = new JPanel(new BorderLayout(0, 10));
        sur.setOpaque(false);
        sur.add(UI.ayuda("<html>Prueba: cualquier código de 6 dígitos se aprueba, salvo " + ControladorPasarela.CODIGO_RECHAZADO + "</html>"), BorderLayout.NORTH);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.setOpaque(false);
        botones.add(cancelar);
        botones.add(enviar);
        sur.add(botones, BorderLayout.SOUTH);
        c.add(sur, BorderLayout.SOUTH);

        enviar.addActionListener(e -> enviar());
        cancelar.addActionListener(e -> cerrar());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { cerrar(); }
        });
        getRootPane().setDefaultButton(enviar);
        getRootPane().registerKeyboardAction(e -> cerrar(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setContentPane(c);
        pack();
        setSize(Math.max(getWidth(), 520), getHeight());
        setLocationRelativeTo(duenio);
    }

    private JPanel formularioBilletera() {
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbYape);
        grupo.add(rbPlin);
        JPanel billeteras = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        billeteras.setOpaque(false);
        billeteras.add(rbYape);
        billeteras.add(Box.createHorizontalStrut(14));
        billeteras.add(rbPlin);
        txtCelular.putClientProperty("JTextField.placeholderText", "987 654 321");
        txtCodigo.putClientProperty("JTextField.placeholderText", "6 dígitos");
        txtCelular.setFont(UI.fuente(Font.BOLD, 16));
        txtCodigo.setFont(UI.fuente(Font.BOLD, 16));
        JPanel f = formulario();
        fila(f, 0, "Billetera", billeteras, 3);
        fila(f, 1, "Celular del cliente", txtCelular, 3);
        fila(f, 2, "Código de aprobación", txtCodigo, 3);
        GridBagConstraints g = restricciones(1, 3);
        g.gridwidth = 3;
        f.add(UI.ayuda("El cliente ve el código de aprobación en su aplicación."), g);
        return f;
    }

    private static JPanel formulario() {
        JPanel f = new JPanel(new GridBagLayout());
        f.setOpaque(false);
        return f;
    }

    private static void fila(JPanel f, int y, String etiqueta, JComponent campo, int ancho) {
        GridBagConstraints g = restricciones(0, y);
        g.weightx = 0;
        f.add(new JLabel(etiqueta), g);
        g = restricciones(1, y);
        g.gridwidth = ancho;
        f.add(campo, g);
    }

    private static GridBagConstraints restricciones(int x, int y) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(4, 0, 4, 10);
        return g;
    }

    /** Error del formulario antes de enviar, o null si los datos tienen el formato correcto. */
    private String validar() {
        String error = ControladorPasarela.validarCelular(txtCelular.getText());
        return error != null ? error : ControladorPasarela.validarCodigo(txtCodigo.getText());
    }

    private void enviar() {
        if (procesando || tokenAprobado != null) return;
        String error = validar();
        lblError.setText(error == null ? " " : error);
        if (error != null) return;

        procesando = true;
        habilitar(false);
        pasos.removeAll();
        paso("Enviando solicitud de cobro por " + Datos.soles(total) + "…", UI.TEXTO_SECUNDARIO);

        String billetera = rbPlin.isSelected() ? ControladorPasarela.PLIN : ControladorPasarela.YAPE;
        String celular = txtCelular.getText(), codigo = txtCodigo.getText();
        new SwingWorker<TransaccionVO, String>() {
            @Override protected TransaccionVO doInBackground() throws Exception {
                TransaccionVO t = pasarela.solicitarCobro(forma, total);
                publish(t.token);
                return pasarela.autorizarBilletera(t.token, billetera, celular, codigo);
            }

            @Override protected void process(List<String> tokens) {
                pasos.removeAll();
                paso("Solicitud enviada · token de pago " + tokens.get(0), UI.TEXTO);
                paso("Esperando la autorización de la pasarela…", UI.TEXTO_SECUNDARIO);
            }

            @Override protected void done() {
                procesando = false;
                // Se deja la línea del token y se reemplaza "Esperando la autorización…" por la respuesta
                if (pasos.getComponentCount() > 1) pasos.remove(pasos.getComponentCount() - 1);
                try {
                    TransaccionVO t = get();
                    if (t.aprobada()) {
                        paso("Aprobada · código de autorización " + t.codigoAutorizacion, UI.EXITO);
                        paso(t.medio + " · registrando la venta…", UI.TEXTO_SECUNDARIO);
                        tokenAprobado = t.token;
                        Timer cierre = new Timer(1100, e -> dispose());
                        cierre.setRepeats(false);
                        cierre.start();
                        return;
                    }
                    paso("Rechazada: " + t.mensaje, UI.PELIGRO);
                    lblError.setText("Corrija los datos, pida otro medio de pago o cancele.");
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    pasos.removeAll();
                    paso("No se pudo completar el cobro: " + causa.getMessage(), UI.PELIGRO);
                }
                enviar.setText("Reintentar");
                habilitar(true);
            }
        }.execute();
    }

    private void paso(String texto, Color color) {
        JLabel l = new JLabel("●  " + texto);
        l.setForeground(color);
        l.setFont(UI.fuente(Font.PLAIN, 13));
        pasos.add(l);
        pasos.revalidate();
        pasos.repaint();
    }

    private void habilitar(boolean si) {
        for (JComponent comp : new JComponent[]{txtCelular, txtCodigo, rbYape, rbPlin, enviar, cancelar}) {
            comp.setEnabled(si);
        }
    }

    private void cerrar() {
        if (!procesando && tokenAprobado == null) dispose();
    }
}
