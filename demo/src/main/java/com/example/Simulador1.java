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
import java.util.Arrays;
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
import javax.swing.Timer;

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
    private int currentStep;
    private Timer animationTimer;
    private JLabel metrics;

    public Simulador1() {
        this(new DiffusionEngine.SimConfig(100, 100, 1.0, 1.0));
    }

    public Simulador1(DiffusionEngine.SimConfig config) {
        currentN = config.n();
        currentS = config.s();
        currentDelta = config.delta();
        currentTau = config.tau();
        currentSeed = config.seed();
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(900, 650));
        setBackground(Color.WHITE);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JSpinner nField = new JSpinner(new SpinnerNumberModel(currentN, 1, 5000, 10));
        JSpinner sField = new JSpinner(new SpinnerNumberModel(currentS, 1, 5000, 10));
        JSpinner deltaField = new JSpinner(new SpinnerNumberModel(currentDelta, 0.1, 10.0, 0.1));
        JSpinner tauField = new JSpinner(new SpinnerNumberModel(currentTau, 0.1, 10.0, 0.1));
        JTextField seedField = new JTextField(8);
        seedField.setText(currentSeed == null ? "" : currentSeed.toString());
        JButton simulate = new JButton("Simular");
        JButton export = new JButton("Exportar CSV");
        JButton animate = new JButton("Animar");

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
        controls.add(animate);

        metrics = new JLabel();
        metrics.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        metrics.setOpaque(true);
        metrics.setBackground(new Color(232, 241, 240));
        metrics.setForeground(new Color(35, 58, 62));

        simulate.addActionListener(event -> {
            try {
                animationTimer.stop();
                currentSeed = seedField.getText().isBlank() ? null : Long.parseLong(seedField.getText().trim());
                currentN = (int) nField.getValue();
                currentS = (int) sField.getValue();
                currentDelta = ((Number) deltaField.getValue()).doubleValue();
                currentTau = ((Number) tauField.getValue()).doubleValue();
                runSimulation();
                currentStep = currentS;
                updateMetrics();
                repaint();
            } catch (NumberFormatException exception) {
                javax.swing.JOptionPane.showMessageDialog(this, "Seed deve ser um número inteiro.",
                        "Parâmetro inválido", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });

        animate.addActionListener(event -> startAnimation());

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
                    javax.swing.JOptionPane.showMessageDialog(this, "Não foi possível exportar o CSV: " + ex.getMessage(),
                            "Falha na exportação", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        add(controls, BorderLayout.NORTH);
        add(new PlotPanel(), BorderLayout.CENTER);
        add(metrics, BorderLayout.SOUTH);

        animationTimer = new Timer(30, event -> {
            if (currentStep < currentS) {
                currentStep++;
                updateMetrics();
                repaint();
            } else {
                animationTimer.stop();
            }
        });

        runSimulation();
        currentStep = currentS;
        updateMetrics();
    }

    private void runSimulation() {
        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(currentN, currentS, currentDelta, currentTau, currentSeed);
        result = DiffusionEngine.run1D(config);
    }

    private void startAnimation() {
        animationTimer.stop();
        currentStep = 0;
        updateMetrics();
        repaint();
        animationTimer.start();
    }

    private void updateMetrics() {
        if (result == null) return;
        double meanPosition = 0.0;
        for (double[] trajectory : result.trajectories()) {
            meanPosition += trajectory[currentStep];
        }
        meanPosition /= result.trajectories().length;
        double[] visibleMsd = Arrays.copyOf(result.msd(), currentStep + 1);
        double dExperimental = DiffusionEngine.estimateD(visibleMsd, currentTau, 1);
        double dTheoretical = DiffusionEngine.theoreticalD(currentDelta, currentTau);
        metrics.setText(String.format(Locale.US,
                "Passo %d/%d  |  <x> = %.3f  |  MSD = %.3f  |  RMSD = %.3f  |  Dexp = %.4f  |  Dteo = %.4f",
                currentStep, currentS, meanPosition, result.msd()[currentStep], result.rmsd()[currentStep],
                dExperimental, dTheoretical));
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
            int separator = getWidth() / 2;
            int top = 70;
            int bottom = getHeight() - 55;
            int leftPlotX = 58;
            int leftPlotRight = separator - 24;
            int rightPlotX = separator + 54;
            int rightPlotRight = getWidth() - 28;

            g.setColor(new Color(248, 250, 251));
            g.fillRect(leftPlotX, top, leftPlotRight - leftPlotX, bottom - top);
            g.fillRect(rightPlotX, top, rightPlotRight - rightPlotX, bottom - top);
            g.setColor(new Color(222, 228, 232));
            g.drawLine(separator, top, separator, bottom);

            g.setColor(new Color(35, 48, 58));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            g.drawString("Trajetórias", leftPlotX, 48);
            g.drawString("Distribuição final", rightPlotX, 48);

            drawTrajectoryPlot(g, leftPlotX, top, leftPlotRight - leftPlotX, bottom - top);
            drawHistogram(g, rightPlotX, top, rightPlotRight - rightPlotX, bottom - top);

            g.setColor(new Color(35, 48, 58));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            String title = "Passeio aleatório unidimensional";
            g.drawString(title, (getWidth() - g.getFontMetrics().stringWidth(title)) / 2, 27);
            g.dispose();
        }

        private void drawTrajectoryPlot(Graphics2D g, int x, int y, int width, int height) {
            int left = x + 42;
            int right = x + width - 12;
            int top = y + 12;
            int bottom = y + height - 34;
            double maxAbs = 1.0;
            for (double[] trajectory : result.trajectories()) {
                for (double position : trajectory) {
                    maxAbs = Math.max(maxAbs, Math.abs(position));
                }
            }
            maxAbs *= 1.12;
            double plotWidth = right - left;
            double plotHeight = bottom - top;
            int visibleSteps = Math.max(1, currentStep);

            for (int tick = 0; tick <= 4; tick++) {
                int gridY = top + (int) Math.round(tick * plotHeight / 4.0);
                g.setColor(new Color(225, 231, 235));
                g.drawLine(left, gridY, right, gridY);
                g.setColor(new Color(91, 104, 113));
                String label = Integer.toString(currentStep - (int) Math.round(tick * currentStep / 4.0));
                g.drawString(label, x + 5, gridY + 4);
            }
            g.setColor(new Color(91, 104, 113));
            for (int tick = 0; tick <= 4; tick++) {
                double value = -maxAbs + 2.0 * maxAbs * tick / 4.0;
                int gridX = left + (int) Math.round(tick * plotWidth / 4.0);
                g.setColor(new Color(225, 231, 235));
                g.drawLine(gridX, top, gridX, bottom);
                g.setColor(new Color(91, 104, 113));
                g.drawString(String.format(Locale.US, "%.1f", value), gridX - 12, bottom + 18);
            }

            for (int particle = 0; particle < result.trajectories().length; particle++) {
                g.setColor(new Color(PARTICLE_COLORS[particle % PARTICLE_COLORS.length].getRed(),
                        PARTICLE_COLORS[particle % PARTICLE_COLORS.length].getGreen(),
                        PARTICLE_COLORS[particle % PARTICLE_COLORS.length].getBlue(), 100));
                g.setStroke(new BasicStroke(1.0f));
                for (int step = 1; step <= currentStep; step++) {
                    int prevX = left + (int) Math.round((result.trajectories()[particle][step - 1] + maxAbs) * plotWidth / (2.0 * maxAbs));
                    int currX = left + (int) Math.round((result.trajectories()[particle][step] + maxAbs) * plotWidth / (2.0 * maxAbs));
                    int prevY = bottom - (int) Math.round((step - 1) * plotHeight / visibleSteps);
                    int currY = bottom - (int) Math.round(step * plotHeight / visibleSteps);
                    g.drawLine(prevX, prevY, currX, currY);
                }
            }
            g.setColor(new Color(55, 66, 74));
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(left, top, left, bottom);
            g.drawLine(left, bottom, right, bottom);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.drawString("posição x", left + (right - left) / 2 - 22, bottom + 33);
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

        private void drawHistogram(Graphics2D g, int x, int y, int width, int height) {
            double[] positions = new double[result.trajectories().length];
            for (int i = 0; i < positions.length; i++) {
                positions[i] = result.trajectories()[i][currentStep];
            }
            DiffusionEngine.HistogramData histogram = DiffusionEngine.calculateHistogram(positions, 20);
            if (histogram == null || histogram.binCenters() == null || histogram.counts() == null || histogram.density() == null) {
                return;
            }
            int left = x + 14;
            int right = x + width - 10;
            int top = y + 12;
            int bottom = y + height - 34;
            int plotWidth = right - left;
            int plotHeight = bottom - top;
            double[] centers = histogram.binCenters();
            double binWidth = histogram.binWidth();
            double minX = centers[0] - binWidth / 2.0;
            double maxX = centers[centers.length - 1] + binWidth / 2.0;
            int plottedStep = Math.max(1, currentStep);
            double maxDensity = 0.0;
            for (double value : histogram.density()) maxDensity = Math.max(maxDensity, value);
            for (int i = 0; i <= plotWidth; i++) {
                double value = minX + i * (maxX - minX) / plotWidth;
                maxDensity = Math.max(maxDensity, DiffusionEngine.gaussianDensity(value, plottedStep, currentDelta));
            }
            maxDensity = Math.max(maxDensity, 1e-12) * 1.12;

            for (int tick = 0; tick <= 4; tick++) {
                int gridY = top + (int) Math.round(tick * plotHeight / 4.0);
                g.setColor(new Color(225, 231, 235));
                g.drawLine(left, gridY, right, gridY);
            }

            for (int i = 0; i < centers.length; i++) {
                int barLeft = left + (int) Math.round((centers[i] - binWidth / 2.0 - minX) * plotWidth / (maxX - minX));
                int barRight = left + (int) Math.round((centers[i] + binWidth / 2.0 - minX) * plotWidth / (maxX - minX));
                int barHeight = (int) Math.round(histogram.density()[i] * plotHeight / maxDensity);
                g.setColor(new Color(36, 139, 132, 190));
                g.fillRect(barLeft + 1, bottom - barHeight, Math.max(1, barRight - barLeft - 2), barHeight);
            }

            int previousX = left;
            int previousY = bottom;
            g.setColor(new Color(196, 74, 54));
            g.setStroke(new BasicStroke(2.2f));
            for (int i = 0; i <= plotWidth; i++) {
                double value = minX + i * (maxX - minX) / plotWidth;
                double density = currentStep == 0 ? 0.0
                    : DiffusionEngine.gaussianDensity(value, currentStep, currentDelta);
                int pointY = bottom - (int) Math.round(density * plotHeight / maxDensity);
                if (i > 0) g.drawLine(previousX, previousY, left + i, pointY);
                previousX = left + i;
                previousY = pointY;
            }

            g.setColor(new Color(55, 66, 74));
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(left, top, left, bottom);
            g.drawLine(left, bottom, right, bottom);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.drawString("densidade", x + 2, top + 4);
            g.drawString("posição final x", left + plotWidth / 2 - 42, bottom + 32);
            g.setColor(new Color(36, 139, 132));
            g.fillRect(left + 8, top + 8, 10, 10);
            g.setColor(new Color(55, 66, 74));
            g.drawString("simulação", left + 23, top + 17);
            g.setColor(new Color(196, 74, 54));
            g.setStroke(new BasicStroke(2.2f));
            g.drawLine(left + 94, top + 13, left + 108, top + 13);
            g.setColor(new Color(55, 66, 74));
            g.drawString("Fick", left + 113, top + 17);
        }
    }
}
