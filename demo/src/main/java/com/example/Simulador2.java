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
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Simulador2 extends JPanel {
    private final int nParticles = 150;
    private final int nSteps = 500;
    private DiffusionEngine.Result2D result;
    private int currentStep = 50;
    private double rotationX = Math.toRadians(25);
    private double rotationY = Math.toRadians(-35);
    private double zoom = 1.0;
    private Point lastMouse;

    public Simulador2() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1000, 760));
        setBackground(Color.WHITE);
        runSimulation();

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton playPause = new JButton("Pausar");
        JButton restart = new JButton("Reiniciar");
        JLabel stepLabel = new JLabel();
        JSpinner nField = new JSpinner(new SpinnerNumberModel(150, 10, 1000, 10));
        JSpinner sField = new JSpinner(new SpinnerNumberModel(500, 10, 1500, 10));
        JSpinner deltaField = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 5.0, 0.1));
        JSpinner tauField = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 5.0, 0.1));
        JButton simulate = new JButton("Simular");

        controls.add(new JLabel("N:"));
        controls.add(nField);
        controls.add(new JLabel("S:"));
        controls.add(sField);
        controls.add(new JLabel("δ:"));
        controls.add(deltaField);
        controls.add(new JLabel("τ:"));
        controls.add(tauField);
        controls.add(simulate);
        controls.add(playPause);
        controls.add(restart);
        controls.add(stepLabel);

        Timer timer = new Timer(120, e -> {
            if (currentStep < nSteps - 1) {
                currentStep++;
            }
            stepLabel.setText(String.format(Locale.US, "Passo: %d / %d", currentStep + 1, nSteps));
            repaint();
        });

        playPause.addActionListener(e -> {
            if (timer.isRunning()) {
                timer.stop();
                playPause.setText("Continuar");
            } else {
                timer.start();
                playPause.setText("Pausar");
            }
        });

        restart.addActionListener(e -> {
            currentStep = 50;
            stepLabel.setText(String.format(Locale.US, "Passo: %d / %d", currentStep + 1, nSteps));
            repaint();
        });

        simulate.addActionListener(e -> {
            int n = (int) nField.getValue();
            int s = (int) sField.getValue();
            double delta = ((Number) deltaField.getValue()).doubleValue();
            double tau = ((Number) tauField.getValue()).doubleValue();
            result = DiffusionEngine.run2D(new DiffusionEngine.SimConfig(n, s, delta, tau));
            currentStep = Math.min(50, s - 1);
            stepLabel.setText(String.format(Locale.US, "Passo: %d / %d", currentStep + 1, s));
            repaint();
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouse = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastMouse == null) return;
                rotationY += (e.getX() - lastMouse.x) * 0.01;
                rotationX += (e.getY() - lastMouse.y) * 0.01;
                rotationX = Math.max(-1.45, Math.min(1.45, rotationX));
                lastMouse = e.getPoint();
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoom *= e.getWheelRotation() < 0 ? 1.1 : 1 / 1.1;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        add(controls, BorderLayout.NORTH);
        add(this, BorderLayout.CENTER);
        timer.start();
    }

    private void runSimulation() {
        result = DiffusionEngine.run2D(new DiffusionEngine.SimConfig(nParticles, nSteps, 1.0, 1.0, 7L));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (result == null) return;

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2 + 20;
        double maxAbs = 1.0;
        for (int i = 0; i < result.x().length; i++) {
            for (int step = 0; step <= currentStep; step++) {
                maxAbs = Math.max(maxAbs, Math.abs(result.x()[i][step]));
                maxAbs = Math.max(maxAbs, Math.abs(result.y()[i][step]));
            }
        }
        maxAbs *= 1.5;
        double scale = Math.min(getWidth(), getHeight()) / (2.8 * maxAbs) * zoom;

        for (int particle = 0; particle < result.x().length; particle++) {
            int[] trailX = new int[currentStep + 1];
            int[] trailY = new int[currentStep + 1];
            for (int step = 0; step <= currentStep; step++) {
                Point3D p = project(result.x()[particle][step], result.y()[particle][step], 0.0, cx, cy, scale);
                trailX[step] = (int) p.x;
                trailY[step] = (int) p.y;
            }
            g.setColor(new Color(31 + particle % 10, 119 + particle % 50, 180 + particle % 30));
            g.setStroke(new BasicStroke(1.4f));
            g.drawPolyline(trailX, trailY, trailX.length);

            Point3D current = project(result.x()[particle][currentStep], result.y()[particle][currentStep], 0.0, cx, cy, scale);
            g.fillOval((int) current.x - 3, (int) current.y - 3, 6, 6);
        }

        double centerX = 0.0;
        double centerY = 0.0;
        double sigmaX = Math.sqrt(result.msd()[currentStep]) / 2.0;
        double sigmaY = sigmaX;
        for (int row = 0; row < 24; row++) {
            for (int col = 0; col < 24; col++) {
                double x1 = -maxAbs + col * (2 * maxAbs / 24.0);
                double x2 = -maxAbs + (col + 1) * (2 * maxAbs / 24.0);
                double y1 = -maxAbs + row * (2 * maxAbs / 24.0);
                double y2 = -maxAbs + (row + 1) * (2 * maxAbs / 24.0);
                double z11 = gaussianSurface(x1, y1, centerX, centerY, sigmaX, sigmaY);
                double z12 = gaussianSurface(x2, y1, centerX, centerY, sigmaX, sigmaY);
                double z21 = gaussianSurface(x1, y2, centerX, centerY, sigmaX, sigmaY);
                double z22 = gaussianSurface(x2, y2, centerX, centerY, sigmaX, sigmaY);

                Point3D a = project(x1, y1, z11, cx, cy, scale);
                Point3D b = project(x2, y1, z12, cx, cy, scale);
                Point3D c = project(x2, y2, z22, cx, cy, scale);
                Point3D d = project(x1, y2, z21, cx, cy, scale);

                int[] xs = {(int) a.x, (int) b.x, (int) c.x, (int) d.x};
                int[] ys = {(int) a.y, (int) b.y, (int) c.y, (int) d.y};
                g.setColor(new Color(30, 120, 220, 110));
                g.fillPolygon(xs, ys, 4);
                g.setColor(new Color(70, 90, 180, 120));
                g.drawPolygon(xs, ys, 4);
            }
        }

        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        String title = "Difusão em 2D";
        g.drawString(title, (getWidth() - g.getFontMetrics().stringWidth(title)) / 2, 24);
        g.dispose();
    }

    private double gaussianSurface(double x, double y, double meanX, double meanY, double sigmaX, double sigmaY) {
        double exponent = -((x - meanX) * (x - meanX) / (2.0 * sigmaX * sigmaX)
                + (y - meanY) * (y - meanY) / (2.0 * sigmaY * sigmaY));
        return Math.exp(exponent) / (2.0 * Math.PI * sigmaX * sigmaY);
    }

    private Point3D project(double x, double y, double z, int cx, int cy, double scale) {
        double x1 = x * Math.cos(rotationY) - y * Math.sin(rotationY);
        double y1 = x * Math.sin(rotationY) + y * Math.cos(rotationY);
        double y2 = y1 * Math.cos(rotationX) - z * Math.sin(rotationX);
        double depth = y1 * Math.sin(rotationX) + z * Math.cos(rotationX);
        return new Point3D(cx + x1 * scale, cy - y2 * scale, depth);
    }

    private record Point3D(double x, double y, double depth) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Difusão em 2D");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new Simulador2());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
