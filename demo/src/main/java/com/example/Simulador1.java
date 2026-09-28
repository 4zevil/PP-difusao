package com.example;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

public class Simulador1 extends JPanel {
    private static final Color[] PARTICLE_COLORS = {
        new Color(31, 119, 180), new Color(255, 127, 14), new Color(44, 160, 44),
        new Color(214, 39, 40), new Color(148, 103, 189), new Color(140, 86, 75),
        new Color(227, 119, 194), new Color(127, 127, 127), new Color(188, 189, 34),
        new Color(23, 190, 207)
    };

    private DiffusionEngine.Result1D result;
    private int currentN = 100;
    private int currentS = 100;
    private double currentDelta = 1.0;
    private double currentTau = 1.0;
    private Long currentSeed = null;

    public Simulador1() {
        this(new DiffusionEngine.SimConfig(100, 100, 1.0, 1.0));
    }

    public Simulador1(DiffusionEngine.SimConfig config) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(900, 650));
        setBackground(Color.WHITE);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JSpinner nField = new JSpinner(new SpinnerNumberModel(100, 1, 5000, 10));
        JSpinner sField = new JSpinner(new SpinnerNumberModel(100, 1, 5000, 10));
        JSpinner deltaField = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));
        JSpinner tauField = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));
        JTextField seedField = new JTextField(8);
        JButton simulate = new JButton("Simular");
        JButton export = new JButton("Exportar CSV");

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
        controls.add(simulate);
        controls.add(export);

        JLabel metrics = new JLabel();
        metrics.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        controls.add(metrics);

        simulate.addActionListener(event -> {
            Long seed = seedField.getText() == null || seedField.getText().isBlank() ? null : Long.parseLong(seedField.getText());
            currentN = (int) nField.getValue();
            currentS = (int) sField.getValue();
            currentDelta = ((Number) deltaField.getValue()).doubleValue();
            currentTau = ((Number) tauField.getValue()).doubleValue();
            currentSeed = seed;
            runSimulation();
            updateMetrics(metrics);
            repaint();
        });

        export.addActionListener(event -> {
            if (result == null) {
                return;
            }
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Salvar CSV");
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    DiffusionEngine.exportToCSV(file, result, new DiffusionEngine.SimConfig(currentN, currentS, currentDelta, currentTau, currentSeed));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        add(controls, BorderLayout.NORTH);
        add(new PlotPanel(), BorderLayout.CENTER);

        runSimulation();
        updateMetrics(metrics);
    }

    private void runSimulation() {
        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(currentN, currentS, currentDelta, currentTau, currentSeed);
        result = DiffusionEngine.run1D(config);
    }

    private void updateMetrics(JLabel metrics) {
        if (result == null) return;
        double msdFinal = result.msd()[currentS];
        double rmsdFinal = result.rmsd()[currentS];
        double dEstimate = DiffusionEngine.estimateDiffusionCoefficient(result.msd(), currentTau, 1);
        metrics.setText(String.format(Locale.US,
                "x̄_final=%.3f | MSD_final=%.3f | RMSD_final=%.3f | D_est=%.4f",
                result.meanFinalPos(), msdFinal, rmsdFinal, dEstimate));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Difusão em 1D");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new Simulador1());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private class PlotPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (result == null) {
                return;
            }

            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int left = 80;
            int right = getWidth() - 30;
            int top = 50;
            int bottom = getHeight() - 60;
            int plotWidth = right - left;
            int plotHeight = bottom - top;

            double minX = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            for (double[] row : result.trajectories()) {
                for (double value : row) {
                    if (value < minX) minX = value;
                    if (value > maxX) maxX = value;
                }
            }
            if (maxX == minX) {
                maxX = minX + 1.0;
            }
            double xRange = Math.max(1.0, (maxX - minX) * 0.6);
            double xScale = plotWidth / (2.0 * xRange);
            double centerX = (maxX + minX) / 2.0;

            drawAxes(g, left, right, top, bottom, plotWidth, plotHeight, minX, maxX, currentS, xScale, centerX);

            for (int particle = 0; particle < result.trajectories().length; particle++) {
                g.setColor(PARTICLE_COLORS[particle % PARTICLE_COLORS.length]);
                g.setStroke(new BasicStroke(1.5f));
                for (int step = 0; step <= currentS; step++) {
                    double x = result.trajectories()[particle][step];
                    int px = left + (int) Math.round((x - centerX + xRange) * xScale);
                    int py = bottom - (int) Math.round(step * plotHeight / (double) currentS);
                    if (step > 0) {
                        double prevX = result.trajectories()[particle][step - 1];
                        int prevPx = left + (int) Math.round((prevX - centerX + xRange) * xScale);
                        int prevPy = bottom - (int) Math.round((step - 1) * plotHeight / (double) currentS);
                        g.drawLine(prevPx, prevPy, px, py);
                    }
                    g.fillOval(px - 2, py - 2, 4, 4);
                }
            }

            DiffusionEngine.HistogramData histogram = result.histogram();
            int histX = getWidth() / 2 + 40;
            int histY = 50;
            int histW = getWidth() - histX - 30;
            int histH = getHeight() - 120;
            drawHistogram(g, histogram, histX, histY, histW, histH);

            String title = "Difusão em 1D: N=" + currentN + ", S=" + currentS;
            g.setColor(Color.DARK_GRAY);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g.drawString(title, (getWidth() - g.getFontMetrics().stringWidth(title)) / 2, 24);
            g.dispose();
        }

        private void drawAxes(Graphics2D g, int left, int right, int top, int bottom,
                              int plotWidth, int plotHeight, double minX, double maxX,
                              int stepCount, double xScale, double centerX) {
            g.setColor(new Color(70, 70, 70));
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(left, top, left, bottom);
            g.drawLine(left, bottom, right, bottom);

            int ticks = 6;
            for (int i = 0; i <= ticks; i++) {
                double value = minX + (maxX - minX) * i / ticks;
                int x = left + (int) Math.round(((value - centerX) + (maxX - minX) * 0.6) * xScale);
                g.setColor(new Color(225, 230, 235));
                g.drawLine(x, top, x, bottom);
                g.setColor(Color.DARK_GRAY);
                g.drawString(String.format(Locale.US, "%.1f", value), x - 10, bottom + 18);
            }

            for (int i = 0; i <= 5; i++) {
                int step = (int) Math.round((stepCount / 5.0) * i);
                int y = bottom - (int) Math.round(step * plotHeight / (double) stepCount);
                g.setColor(new Color(225, 230, 235));
                g.drawLine(left, y, right, y);
                g.setColor(Color.DARK_GRAY);
                g.drawString(String.valueOf(step), left - 18, y + 4);
            }
        }

        private void drawHistogram(Graphics2D g, DiffusionEngine.HistogramData histogram, int x, int y, int width, int height) {
            g.setColor(new Color(245, 245, 245));
            g.fillRect(x, y, width, height);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y, width, height);

            if (histogram == null || histogram.binCenters() == null || histogram.counts() == null || histogram.density() == null) {
                return;
            }

            int maxCount = 1;
            for (int value : histogram.counts()) {
                maxCount = Math.max(maxCount, value);
            }

            for (int i = 0; i < histogram.binCenters().length; i++) {
                int barWidth = Math.max(2, width / histogram.binCenters().length - 2);
                int barHeight = (int) ((histogram.counts()[i] / (double) maxCount) * (height - 30));
                int barX = x + i * (width / histogram.binCenters().length) + 2;
                int barY = y + height - 20 - barHeight;
                g.setColor(new Color(80, 120, 220));
                g.fillRect(barX, barY, barWidth, barHeight);
            }

            int[] xs = new int[histogram.binCenters().length];
            int[] ys = new int[histogram.binCenters().length];
            double maxDensity = 1.0;
            for (double density : histogram.density()) {
                maxDensity = Math.max(maxDensity, density);
            }
            for (int i = 0; i < histogram.binCenters().length; i++) {
                double density = histogram.density()[i];
                xs[i] = x + (int) ((i + 0.5) * (width / (double) histogram.binCenters().length));
                ys[i] = y + height - 20 - (int) ((density / maxDensity) * (height - 30));
            }
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(2.0f));
            g.drawPolyline(xs, ys, xs.length);
        }
    }
}
