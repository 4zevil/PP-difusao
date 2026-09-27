package com.example;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
// Estruturas usadas para guardar e ordenar os elementos que serao desenhados.
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Passeio aleatorio em 2D com a distribuicao gaussiana em uma superficie 3D. */
public class Simulador2 extends JPanel {
    private static final Color[] PARTICLE_COLORS = {
        new Color(31, 119, 180), new Color(255, 127, 14), new Color(44, 160, 44),
        new Color(214, 39, 40), new Color(148, 103, 189), new Color(140, 86, 75),
        new Color(227, 119, 194), new Color(127, 127, 127), new Color(188, 189, 34),
        new Color(23, 190, 207)
    };

    // Estes valores correspondem a numPart, numSteps, startPlot e N_Sigma do Python.
    private static final int NUM_PARTICLES = 150;
    private static final int NUM_STEPS = 500;
    private static final int START_PLOT = 50;
    private static final int N_SIGMA = 2;
    private static final int GRID_SIZE = 25;

    // Cada linha representa uma particula; cada coluna representa um instante.
    private final double[][] positionsX = new double[NUM_PARTICLES][NUM_STEPS];
    private final double[][] positionsY = new double[NUM_PARTICLES][NUM_STEPS];
    private final double[] meanX = new double[NUM_STEPS];
    private final double[] meanY = new double[NUM_STEPS];
    private final double[] rmsX = new double[NUM_STEPS];
    private final double[] rmsY = new double[NUM_STEPS];
    // Medias, desvios RMS e limites usados para desenhar a distribuicao.
    private double maxPosition;
    private int currentStep = START_PLOT;
    // Estado da camera: arraste gira o grafico e a roda do mouse controla o zoom.
    private double rotationX = Math.toRadians(25);
    private double rotationY = Math.toRadians(-35);
    private double zoom = 1.0;
    private Point lastMouse;

    public Simulador2() {
        // Configuracao inicial do painel e geracao dos dados antes da primeira pintura.
        setPreferredSize(new Dimension(1000, 760));
        setBackground(Color.WHITE);
        gerarSimulacao();

        // Eventos equivalentes ao view_init e a uma interacao manual com o grafico.
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                lastMouse = event.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                // A diferenca entre os pontos do mouse vira um novo angulo da camera.
                if (lastMouse == null) return;
                rotationY += (event.getX() - lastMouse.x) * 0.01;
                rotationX += (event.getY() - lastMouse.y) * 0.01;
                rotationX = Math.max(-1.45, Math.min(1.45, rotationX));
                lastMouse = event.getPoint();
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                // Roda para cima aproxima; roda para baixo afasta.
                zoom *= event.getWheelRotation() < 0 ? 1.1 : 1 / 1.1;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    public static void main(String[] args) {
        // Swing deve criar a janela na Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            Simulador2 panel = new Simulador2();
            JFrame frame = new JFrame("Diffusion in 2D");
            JButton playPause = new JButton("Pausar");
            JButton restart = new JButton("Reiniciar");
            JLabel stepLabel = new JLabel();

            // O Timer substitui plt.pause: a cada 200 ms avancamos 10 passos.
            Timer timer = new Timer(200, event -> {
                panel.avancar();
                stepLabel.setText(panel.textoDoPasso());
            });
            playPause.addActionListener(event -> {
                // Alterna entre executar e pausar a animacao.
                if (timer.isRunning()) {
                    timer.stop();
                    playPause.setText("Continuar");
                } else {
                    timer.start();
                    playPause.setText("Pausar");
                }
            });
            restart.addActionListener(event -> {
                // Reinicia a visualizacao no mesmo ponto de startPlot do Python.
                panel.currentStep = START_PLOT;
                panel.repaint();
                stepLabel.setText(panel.textoDoPasso());
                if (!timer.isRunning()) timer.start();
                playPause.setText("Pausar");
            });

            // Barra inferior com os controles da animacao.
            JPanel controls = new JPanel();
            controls.add(playPause);
            controls.add(restart);
            controls.add(stepLabel);
            frame.add(panel);
            frame.add(controls, java.awt.BorderLayout.SOUTH);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            stepLabel.setText(panel.textoDoPasso());
            timer.start();
        });
    }

    private void gerarSimulacao() {
        // Equivalente a criar os arrays NumPy e aplicar np.cumsum.
        Random random = new Random();
        for (int particle = 0; particle < NUM_PARTICLES; particle++) {
            for (int step = 0; step < NUM_STEPS; step++) {
                // A posicao atual e a posicao anterior mais um deslocamento +/-1.
                double previousX = step == 0 ? 0 : positionsX[particle][step - 1];
                double previousY = step == 0 ? 0 : positionsY[particle][step - 1];
                positionsX[particle][step] = previousX + (random.nextBoolean() ? 1 : -1);
                positionsY[particle][step] = previousY + (random.nextBoolean() ? 1 : -1);
                maxPosition = Math.max(maxPosition, Math.max(Math.abs(positionsX[particle][step]),
                        Math.abs(positionsY[particle][step])));
            }
        }
        // Margem visual equivalente a maxV_X e maxV_Y multiplicados por 1.25.
        maxPosition *= 1.25;

        for (int step = 0; step < NUM_STEPS; step++) {
            // Para cada instante, calculamos media e raiz da media dos quadrados.
            double sumX = 0, sumY = 0, squareX = 0, squareY = 0;
            for (int particle = 0; particle < NUM_PARTICLES; particle++) {
                double x = positionsX[particle][step];
                double y = positionsY[particle][step];
                sumX += x;
                sumY += y;
                squareX += x * x;
                squareY += y * y;
            }
            meanX[step] = sumX / NUM_PARTICLES;
            meanY[step] = sumY / NUM_PARTICLES;
            rmsX[step] = Math.sqrt(squareX / NUM_PARTICLES);
            rmsY[step] = Math.sqrt(squareY / NUM_PARTICLES);
        }
    }

    private void avancar() {
        // O passo visual avanca de 10 em 10, como range(startPlot, numSteps, 10).
        currentStep = Math.min(currentStep + 10, NUM_STEPS - 1);
        repaint();
    }

    private String textoDoPasso() {
        return "Passo: " + (currentStep + 1) + " / " + NUM_STEPS;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        // Este metodo e chamado novamente sempre que o Timer ou o mouse pede repaint.
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2 + 25;
        double scale = Math.min(getWidth(), getHeight()) / (2.7 * maxPosition) * zoom;
        // Os objetos sao ordenados por profundidade para simular a sobreposicao 3D.
        List<Drawable> objects = new ArrayList<>();

        adicionarSuperficie(objects, centerX, centerY, scale);
        adicionarParticulas(objects, centerX, centerY, scale);
        adicionarCirculoSigma(objects, centerX, centerY, scale);
        objects.sort(Comparator.comparingDouble(object -> object.depth));
        for (Drawable object : objects) object.draw(g);

        // O inset corresponde ao segundo grafico 2D criado com add_axes no Python.
        desenharInset(g);
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("Serif", Font.BOLD, 19));
        String title = "Difusao em 2D: " + NUM_PARTICLES + " particulas durante " + NUM_STEPS
                + " passos (passo " + (currentStep + 1) + ")";
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(title, (getWidth() - metrics.stringWidth(title)) / 2, 28);
        g.dispose();
    }

    private void adicionarParticulas(List<Drawable> objects, int centerX, int centerY, double scale) {
        // Desenha o caminho percorrido e a posicao atual de cada particula no plano z = 0.
        for (int particle = 0; particle < NUM_PARTICLES; particle++) {
            int[] trailX = new int[currentStep + 1];
            int[] trailY = new int[currentStep + 1];
            double depth = 0;
            for (int step = 0; step <= currentStep; step++) {
                ScreenPoint trailPoint = project(
                        new Point3(positionsX[particle][step], positionsY[particle][step], 0),
                        centerX, centerY, scale);
                trailX[step] = (int) trailPoint.x;
                trailY[step] = (int) trailPoint.y;
                depth += trailPoint.depth;
            }
            double averageDepth = depth / (currentStep + 1);
            Color particleColor = PARTICLE_COLORS[particle % PARTICLE_COLORS.length];
            objects.add(new Drawable(averageDepth, g -> {
                g.setColor(particleColor);
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolyline(trailX, trailY, trailX.length);
            }));

            Point3 point = new Point3(positionsX[particle][currentStep], positionsY[particle][currentStep], 0);
            ScreenPoint projected = project(point, centerX, centerY, scale);
            objects.add(new Drawable(projected.depth, g -> {
                g.setColor(particleColor);
                g.fillOval((int) projected.x - 3, (int) projected.y - 3, 6, 6);
            }));
        }
    }

    private void adicionarCirculoSigma(List<Drawable> objects, int centerX, int centerY, double scale) {
        // Aproxima o plt.Circle por 80 pontos e o projeta no plano z = 0.
        double radius = N_SIGMA * rmsX[currentStep];
        double centerXPosition = meanX[currentStep];
        double centerYPosition = meanY[currentStep];
        int samples = 80;
        int[] xs = new int[samples];
        int[] ys = new int[samples];
        double depth = 0;
        for (int index = 0; index < samples; index++) {
            double angle = 2 * Math.PI * index / (samples - 1);
            ScreenPoint point = project(new Point3(centerXPosition + radius * Math.cos(angle),
                    centerYPosition + radius * Math.sin(angle), 0), centerX, centerY, scale);
            xs[index] = (int) point.x;
            ys[index] = (int) point.y;
            depth += point.depth;
        }
        double averageDepth = depth / samples;
        objects.add(new Drawable(averageDepth, g -> {
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1, new float[]{8, 6}, 0));
            g.drawPolyline(xs, ys, samples);
        }));
    }

    private void adicionarSuperficie(List<Drawable> objects, int centerX, int centerY, double scale) {
        // Cria uma malha XY e calcula a altura gaussiana em cada vertice.
        double xMean = meanX[currentStep];
        double yMean = meanY[currentStep];
        double sigmaX = Math.max(rmsX[currentStep], 0.001);
        double sigmaY = Math.max(rmsY[currentStep], 0.001);
        double step = 2 * maxPosition / (GRID_SIZE - 1);
        for (int row = 0; row < GRID_SIZE - 1; row++) {
            for (int col = 0; col < GRID_SIZE - 1; col++) {
            // Cada celula da malha vira um quadrilatero transluscro.
                double x1 = -maxPosition + col * step;
                double x2 = x1 + step;
                double y1 = -maxPosition + row * step;
                double y2 = y1 + step;
                ScreenPoint a = project(new Point3(x1, y1, gaussiana(x1, y1, xMean, yMean, sigmaX, sigmaY)), centerX, centerY, scale);
                ScreenPoint b = project(new Point3(x2, y1, gaussiana(x2, y1, xMean, yMean, sigmaX, sigmaY)), centerX, centerY, scale);
                ScreenPoint c = project(new Point3(x2, y2, gaussiana(x2, y2, xMean, yMean, sigmaX, sigmaY)), centerX, centerY, scale);
                ScreenPoint d = project(new Point3(x1, y2, gaussiana(x1, y2, xMean, yMean, sigmaX, sigmaY)), centerX, centerY, scale);
                int[] xs = {(int) a.x, (int) b.x, (int) c.x, (int) d.x};
                int[] ys = {(int) a.y, (int) b.y, (int) c.y, (int) d.y};
                int shade = (int) Math.min(255, 80 + 280 * Math.max(a.z, Math.max(b.z, Math.max(c.z, d.z))));
                objects.add(new Drawable((a.depth + b.depth + c.depth + d.depth) / 4, g -> {
                    g.setColor(new Color(30, Math.max(40, shade - 60), shade, 105));
                    g.fillPolygon(xs, ys, 4);
                    g.setColor(new Color(70, 100, 180, 55));
                    g.drawPolygon(xs, ys, 4);
                }));
            }
        }
    }

    private double gaussiana(double x, double y, double xMean, double yMean, double sigmaX, double sigmaY) {
        // Formula da superficie gaussiana usada no Zga do codigo Python.
        return NUM_PARTICLES * Math.exp(-Math.pow(x - xMean, 2) / (2 * sigmaX * sigmaX)
                - Math.pow(y - yMean, 2) / (2 * sigmaY * sigmaY)) / (sigmaX * sigmaY);
    }

    private void desenharInset(Graphics2D g) {
        // Desenha uma versao 2D simples das mesmas particulas e do circulo de sigma.
        int left = 24, top = 52, width = 190, height = 150;
        g.setColor(new Color(255, 255, 255, 225));
        g.fillRect(left, top, width, height);
        g.setColor(Color.GRAY);
        g.drawRect(left, top, width, height);
        double radius = N_SIGMA * rmsX[currentStep];
        for (int particle = 0; particle < NUM_PARTICLES; particle++) {
            int previousX = 0;
            int previousY = 0;
            g.setColor(PARTICLE_COLORS[particle % PARTICLE_COLORS.length]);
            for (int step = 0; step <= currentStep; step++) {
                int x = left + (int) ((positionsX[particle][step] + maxPosition) / (2 * maxPosition) * width);
                int y = top + height - (int) ((positionsY[particle][step] + maxPosition) / (2 * maxPosition) * height);
                if (step > 0) g.drawLine(previousX, previousY, x, y);
                previousX = x;
                previousY = y;
            }
            int x = left + (int) ((positionsX[particle][currentStep] + maxPosition) / (2 * maxPosition) * width);
            int y = top + height - (int) ((positionsY[particle][currentStep] + maxPosition) / (2 * maxPosition) * height);
            g.fillOval(x - 2, y - 2, 4, 4);
        }
        int cx = left + (int) ((meanX[currentStep] + maxPosition) / (2 * maxPosition) * width);
        int cy = top + height - (int) ((meanY[currentStep] + maxPosition) / (2 * maxPosition) * height);
        int diameter = (int) (radius / maxPosition * width);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawOval(cx - diameter / 2, cy - diameter / 2, diameter, diameter);
    }

    private ScreenPoint project(Point3 point, int centerX, int centerY, double scale) {
        // Rotaciona um ponto 3D e converte suas coordenadas para pixels da tela.
        double x1 = point.x * Math.cos(rotationY) - point.y * Math.sin(rotationY);
        double y1 = point.x * Math.sin(rotationY) + point.y * Math.cos(rotationY);
        double y2 = y1 * Math.cos(rotationX) - point.z * Math.sin(rotationX);
        double depth = y1 * Math.sin(rotationX) + point.z * Math.cos(rotationX);
        return new ScreenPoint(centerX + x1 * scale, centerY - y2 * scale, depth, point.z);
    }

    // Pequenos tipos imutaveis para transportar pontos e elementos desenhaveis.
    private record Point3(double x, double y, double z) { }
    private record ScreenPoint(double x, double y, double depth, double z) { }
    private record Drawable(double depth, Painter painter) {
        void draw(Graphics2D graphics) { painter.paint(graphics); }
    }
    private interface Painter { void paint(Graphics2D graphics); }
}