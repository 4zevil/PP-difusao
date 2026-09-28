package com.example;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Simulador3 extends JPanel {
    private static final Color[] PARTICLE_COLORS = {
        new Color(31, 119, 180), new Color(255, 127, 14), new Color(44, 160, 44),
        new Color(214, 39, 40), new Color(148, 103, 189), new Color(140, 86, 75),
        new Color(23, 190, 207), new Color(188, 75, 62)
    };

    private int particleCount = 120;
    private int stepCount = 100;
    private int currentStep;
    private double delta = 1.0;
    private double tau = 1.0;
    private Long seed = 11L;
    private double extent = 1.0;
    private double rotationX = Math.toRadians(20);
    private double rotationY = Math.toRadians(-35);
    private double zoom = 1.0;
    private Point lastMouse;
    private DiffusionEngine.Result3D result;
    private Timer animationTimer;
    private JButton animateButton;
    private JLabel metricsLabel;

    public Simulador3() {
        setLayout(new BorderLayout());
        setBackground(new Color(248, 250, 251));
        setPreferredSize(new Dimension(850, 700));

        JSpinner nField = new JSpinner(new SpinnerNumberModel(particleCount, 1, 5000, 10));
        JSpinner sField = new JSpinner(new SpinnerNumberModel(stepCount, 1, 5000, 10));
        JSpinner deltaField = new JSpinner(new SpinnerNumberModel(delta, 0.1, 10.0, 0.1));
        JSpinner tauField = new JSpinner(new SpinnerNumberModel(tau, 0.1, 10.0, 0.1));
        JTextField seedField = new JTextField(seed.toString(), 7);
        JButton simulateButton = new JButton("Simular");
        animateButton = new JButton("Animar");
        JButton restartButton = new JButton("Reiniciar");

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        controls.add(new JLabel("N:"));
        controls.add(nField);
        controls.add(new JLabel("S:"));
        controls.add(sField);
        controls.add(new JLabel("δ:"));
        controls.add(deltaField);
        controls.add(new JLabel("τ:"));
        controls.add(tauField);
        controls.add(new JLabel("Seed:"));
        controls.add(seedField);
        controls.add(simulateButton);
        controls.add(animateButton);
        controls.add(restartButton);

        metricsLabel = new JLabel();
        metricsLabel.setOpaque(true);
        metricsLabel.setBackground(new Color(232, 241, 240));
        metricsLabel.setForeground(new Color(35, 58, 62));
        metricsLabel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        add(controls, BorderLayout.NORTH);
        add(metricsLabel, BorderLayout.SOUTH);

        animationTimer = new Timer(30, event -> {
            if (currentStep < stepCount) {
                currentStep++;
                updateMetrics();
                repaint();
            } else {
                animationTimer.stop();
                animateButton.setText("Animar");
            }
        });

        simulateButton.addActionListener(event -> {
            try {
                particleCount = (int) nField.getValue();
                stepCount = (int) sField.getValue();
                delta = ((Number) deltaField.getValue()).doubleValue();
                tau = ((Number) tauField.getValue()).doubleValue();
                seed = seedField.getText().isBlank() ? null : Long.parseLong(seedField.getText().trim());
                runSimulation();
                currentStep = stepCount;
                animationTimer.stop();
                animateButton.setText("Animar");
                updateMetrics();
                repaint();
            } catch (NumberFormatException exception) {
                JOptionPane.showMessageDialog(this, "Seed deve ser um número inteiro.",
                        "Parâmetro inválido", JOptionPane.ERROR_MESSAGE);
            }
        });

        animateButton.addActionListener(event -> {
            if (animationTimer.isRunning()) {
                animationTimer.stop();
                animateButton.setText("Continuar");
            } else {
                if (currentStep >= stepCount) currentStep = 0;
                animateButton.setText("Pausar");
                updateMetrics();
                repaint();
                animationTimer.start();
            }
        });

        restartButton.addActionListener(event -> {
            animationTimer.stop();
            currentStep = 0;
            animateButton.setText("Animar");
            updateMetrics();
            repaint();
        });

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                lastMouse = event.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (lastMouse == null) return;
                rotationY += (event.getX() - lastMouse.x) * 0.01;
                rotationX += (event.getY() - lastMouse.y) * 0.01;
                rotationX = Math.max(-1.45, Math.min(1.45, rotationX));
                lastMouse = event.getPoint();
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                zoom *= event.getWheelRotation() < 0 ? 1.1 : 1.0 / 1.1;
                repaint();
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);

        runSimulation();
        currentStep = stepCount;
        updateMetrics();
    }

    private void runSimulation() {
        result = DiffusionEngine.run3D(
                new DiffusionEngine.SimConfig(particleCount, stepCount, delta, tau, seed));
        extent = Math.max(1.0, delta * Math.sqrt(stepCount) * 2.5);
    }

    private void updateMetrics() {
        if (result == null) return;
        double[] visibleMsd = Arrays.copyOf(result.msd(), currentStep + 1);
        double dExperimental = DiffusionEngine.estimateD(visibleMsd, tau, 3);
        double dTheoretical = DiffusionEngine.theoreticalD(delta, tau);
        metricsLabel.setText(String.format(Locale.US,
                "Passo %d/%d  |  N = %d  |  MSD₃ = <x²+y²+z²> = %.3f  |  RMSD = %.3f  |  Dexp = %.4f  |  Dteo = %.4f",
                currentStep, stepCount, particleCount, result.msd()[currentStep], result.rmsd()[currentStep],
                dExperimental, dTheoretical));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (result == null) return;

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2 + 8;
        double scale = Math.min(getWidth(), getHeight()) / (2.8 * extent) * zoom;

        drawAxes(g, centerX, centerY, scale);
        drawEnvelope(g, centerX, centerY, scale);
        for (int particle = 0; particle < particleCount; particle++) {
            int[] trailX = new int[currentStep + 1];
            int[] trailY = new int[currentStep + 1];
            for (int step = 0; step <= currentStep; step++) {
                Point3D point = project(result.x()[particle][step], result.y()[particle][step],
                        result.z()[particle][step], centerX, centerY, scale);
                trailX[step] = (int) point.x;
                trailY[step] = (int) point.y;
            }
            Color color = PARTICLE_COLORS[particle % PARTICLE_COLORS.length];
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
            g.setStroke(new BasicStroke(1.1f));
            g.drawPolyline(trailX, trailY, trailX.length);

            Point3D position = project(result.x()[particle][currentStep], result.y()[particle][currentStep],
                    result.z()[particle][currentStep], centerX, centerY, scale);
            g.setColor(color);
            g.fillOval((int) position.x - 3, (int) position.y - 3, 6, 6);
        }

        g.setColor(new Color(35, 48, 58));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        String title = "Difusão tridimensional";
        g.drawString(title, (getWidth() - g.getFontMetrics().stringWidth(title)) / 2, 27);
        g.dispose();
    }

    private void drawAxes(Graphics2D g, int centerX, int centerY, double scale) {
        Point3D xAxis = project(extent, 0, 0, centerX, centerY, scale);
        Point3D yAxis = project(0, extent, 0, centerX, centerY, scale);
        Point3D zAxis = project(0, 0, extent, centerX, centerY, scale);
        g.setStroke(new BasicStroke(1.8f));
        g.setColor(new Color(180, 65, 55));
        g.drawLine(centerX, centerY, (int) xAxis.x, (int) xAxis.y);
        g.drawString("x", (int) xAxis.x + 5, (int) xAxis.y);
        g.setColor(new Color(39, 135, 88));
        g.drawLine(centerX, centerY, (int) yAxis.x, (int) yAxis.y);
        g.drawString("y", (int) yAxis.x + 5, (int) yAxis.y);
        g.setColor(new Color(48, 91, 170));
        g.drawLine(centerX, centerY, (int) zAxis.x, (int) zAxis.y);
        g.drawString("z", (int) zAxis.x + 5, (int) zAxis.y);
    }

    private void drawEnvelope(Graphics2D g, int centerX, int centerY, double scale) {
        double radiusX = 3.0 * coordinateRms(result.x(), currentStep);
        double radiusY = 3.0 * coordinateRms(result.y(), currentStep);
        double radiusZ = 3.0 * coordinateRms(result.z(), currentStep);
        if (radiusX == 0.0 || radiusY == 0.0 || radiusZ == 0.0) return;

        g.setColor(new Color(43, 129, 151, 105));
        g.setStroke(new BasicStroke(1.0f));
        int latitudes = 12;
        int longitudes = 24;
        for (int latitude = 1; latitude < latitudes; latitude++) {
            double phi = Math.PI * latitude / latitudes;
            int[] xs = new int[longitudes + 1];
            int[] ys = new int[longitudes + 1];
            for (int longitude = 0; longitude <= longitudes; longitude++) {
                double theta = 2.0 * Math.PI * longitude / longitudes;
                Point3D point = project(radiusX * Math.sin(phi) * Math.cos(theta),
                        radiusY * Math.sin(phi) * Math.sin(theta), radiusZ * Math.cos(phi),
                        centerX, centerY, scale);
                xs[longitude] = (int) point.x;
                ys[longitude] = (int) point.y;
            }
            g.drawPolyline(xs, ys, xs.length);
        }
    }

    private double coordinateRms(double[][] coordinate, int step) {
        double sumSquares = 0.0;
        for (double[] particle : coordinate) {
            sumSquares += particle[step] * particle[step];
        }
        return Math.sqrt(sumSquares / coordinate.length);
    }

    private Point3D project(double x, double y, double z, int centerX, int centerY, double scale) {
        double rotatedX = x * Math.cos(rotationY) - y * Math.sin(rotationY);
        double rotatedY = x * Math.sin(rotationY) + y * Math.cos(rotationY);
        double screenY = rotatedY * Math.cos(rotationX) - z * Math.sin(rotationX);
        double depth = rotatedY * Math.sin(rotationX) + z * Math.cos(rotationX);
        return new Point3D(centerX + rotatedX * scale, centerY - screenY * scale, depth);
    }

    private record Point3D(double x, double y, double depth) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Difusão em 3D");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new Simulador3());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}