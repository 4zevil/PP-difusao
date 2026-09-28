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
import java.awt.geom.Path2D;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

public class PainelMelhorias extends JPanel {
    private final Map<String, DiffusionEngine.SimConfig> presets = DiffusionEngine.getPresets();
    private final JComboBox<String> presetSelector = new JComboBox<>(presets.keySet().toArray(String[]::new));
    private final JSpinner nField = new JSpinner(new SpinnerNumberModel(500, 1, 100000, 10));
    private final JSpinner sField = new JSpinner(new SpinnerNumberModel(100, 1, 5000, 10));
    private final JSpinner deltaField = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));
    private final JSpinner tauField = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));
    private final JComboBox<DiffusionEngine.BoundaryType> boundarySelector =
            new JComboBox<>(DiffusionEngine.BoundaryType.values());
    private final JSpinner boundaryLimitField = new JSpinner(new SpinnerNumberModel(15.0, 0.1, 10000.0, 1.0));
    private final JSpinner driftField = new JSpinner(new SpinnerNumberModel(0.5, 0.0, 1.0, 0.05));
    private final JCheckBox obstacleEnabled = new JCheckBox("Obstáculo 2D");
    private final JSpinner obstacleXField = new JSpinner(new SpinnerNumberModel(5.0, -1000.0, 1000.0, 1.0));
    private final JSpinner obstacleYField = new JSpinner(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 1.0));
    private final JSpinner obstacleRadiusField = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 1000.0, 0.5));
    private final JSpinner seedCountField = new JSpinner(new SpinnerNumberModel(10, 2, 1000, 1));
    private final JTextField seedField = new JTextField("42", 8);
    private final JLabel status = new JLabel("Selecione parâmetros e execute uma análise.");
    private final ConfidencePlot plot = new ConfidencePlot();
    private JButton multiSeedButton;
    private JButton benchmarkButton;
    private JButton reportButton;
    private DiffusionEngine.MultiSeedResult latestMultiSeed;
    private DiffusionEngine.SimConfig latestConfig;

    public PainelMelhorias() {
        setLayout(new BorderLayout(0, 8));
        setBackground(new Color(248, 250, 251));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));

        JPanel controls = new JPanel(new BorderLayout(0, 5));
        controls.setOpaque(false);
        JPanel presetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        presetRow.setOpaque(false);
        presetRow.add(new JLabel("Cenário:"));
        presetRow.add(presetSelector);
        JButton loadPreset = new JButton("Carregar");
        JButton loadScenario = new JButton("Abrir cenário...");
        presetRow.add(loadPreset);
        presetRow.add(loadScenario);
        presetRow.add(new JLabel("N:"));
        presetRow.add(nField);
        presetRow.add(new JLabel("S:"));
        presetRow.add(sField);
        presetRow.add(new JLabel("δ:"));
        presetRow.add(deltaField);
        presetRow.add(new JLabel("τ:"));
        presetRow.add(tauField);
        presetRow.add(new JLabel("Seed:"));
        presetRow.add(seedField);

        JPanel physicsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        physicsRow.setOpaque(false);
        physicsRow.add(new JLabel("Fronteira:"));
        physicsRow.add(boundarySelector);
        physicsRow.add(new JLabel("L:"));
        physicsRow.add(boundaryLimitField);
        physicsRow.add(new JLabel("p(+δ):"));
        physicsRow.add(driftField);
        physicsRow.add(obstacleEnabled);
        physicsRow.add(new JLabel("x₀:"));
        physicsRow.add(obstacleXField);
        physicsRow.add(new JLabel("y₀:"));
        physicsRow.add(obstacleYField);
        physicsRow.add(new JLabel("R:"));
        physicsRow.add(obstacleRadiusField);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        actions.setOpaque(false);
        multiSeedButton = new JButton("Executar multi-sementes");
        actions.add(new JLabel("Réplicas:"));
        actions.add(seedCountField);
        actions.add(multiSeedButton);
        benchmarkButton = new JButton("Benchmark 100k");
        actions.add(benchmarkButton);
        reportButton = new JButton("Gerar relatório .md");
        actions.add(reportButton);
        JButton saveScenario = new JButton("Salvar cenário...");
        JButton obstacleRunButton = new JButton("Executar 2D com obstáculo");
        actions.add(saveScenario);
        actions.add(obstacleRunButton);

        controls.add(presetRow, BorderLayout.NORTH);
        controls.add(physicsRow, BorderLayout.CENTER);
        controls.add(actions, BorderLayout.SOUTH);
        add(controls, BorderLayout.NORTH);

        plot.setPreferredSize(new Dimension(900, 450));
        add(plot, BorderLayout.CENTER);
        status.setOpaque(true);
        status.setBackground(new Color(232, 241, 240));
        status.setForeground(new Color(35, 58, 62));
        status.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        add(status, BorderLayout.SOUTH);

        loadPreset.addActionListener(event -> applySelectedPreset());
        loadScenario.addActionListener(event -> loadScenario());
        saveScenario.addActionListener(event -> saveScenario());
        obstacleRunButton.addActionListener(event -> runObstacleSimulation());
        multiSeedButton.addActionListener(event -> runMultiSeedAnalysis());
        benchmarkButton.addActionListener(event -> runBenchmark());
        reportButton.addActionListener(event -> generateReport());
        boundarySelector.addActionListener(event -> boundaryLimitField.setEnabled(
                boundarySelector.getSelectedItem() != DiffusionEngine.BoundaryType.NONE));
        obstacleEnabled.addActionListener(event -> setObstacleFieldsEnabled(obstacleEnabled.isSelected()));
        setObstacleFieldsEnabled(false);
        boundaryLimitField.setEnabled(false);
    }

    private void applySelectedPreset() {
        DiffusionEngine.SimConfig config = presets.get(presetSelector.getSelectedItem());
        if (config == null) return;
        applyConfig(config);
        status.setText("Cenário carregado. Multi-semente é 1D; o obstáculo é aplicado na execução 2D.");
    }

    private void applyConfig(DiffusionEngine.SimConfig config) {
        nField.setValue(config.n());
        sField.setValue(config.s());
        deltaField.setValue(config.delta());
        tauField.setValue(config.tau());
        seedField.setText(config.seed() == null ? "" : config.seed().toString());
        boundarySelector.setSelectedItem(config.boundaryType());
        boundaryLimitField.setValue(Math.max(0.1, config.boundaryLimit()));
        driftField.setValue(config.driftProbability());
        obstacleEnabled.setSelected(config.hasObstacle());
        obstacleXField.setValue(config.obsX());
        obstacleYField.setValue(config.obsY());
        obstacleRadiusField.setValue(Math.max(0.1, config.obsRadius()));
        boundaryLimitField.setEnabled(config.boundaryType() != DiffusionEngine.BoundaryType.NONE);
        setObstacleFieldsEnabled(config.hasObstacle());
    }

    private void loadScenario() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            applyConfig(DiffusionEngine.loadScenario(chooser.getSelectedFile()));
            status.setText("Cenário carregado de " + chooser.getSelectedFile().getAbsolutePath());
        } catch (Exception exception) {
            showError("Não foi possível carregar o cenário: " + exception.getMessage());
        }
    }

    private void saveScenario() {
        try {
            DiffusionEngine.SimConfig config = readConfig();
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("cenario-difusao.properties"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            DiffusionEngine.saveScenario(chooser.getSelectedFile(), config);
            status.setText("Cenário salvo em " + chooser.getSelectedFile().getAbsolutePath());
        } catch (Exception exception) {
            showError("Não foi possível salvar o cenário: " + exception.getMessage());
        }
    }

    private void runObstacleSimulation() {
        try {
            DiffusionEngine.SimConfig config = readConfig();
            DiffusionEngine.Result2D result = DiffusionEngine.run2D(config);
            int insideCount = 0;
            if (config.hasObstacle()) {
                double radiusSquared = config.obsRadius() * config.obsRadius();
                for (int particle = 0; particle < config.n(); particle++) {
                    for (int step = 1; step <= config.s(); step++) {
                        double dx = result.x()[particle][step] - config.obsX();
                        double dy = result.y()[particle][step] - config.obsY();
                        if (dx * dx + dy * dy < radiusSquared) insideCount++;
                    }
                }
            }
            status.setText("Simulação 2D concluída. Posições amostradas dentro do obstáculo: " + insideCount
                    + (config.hasObstacle() ? "." : " (nenhum obstáculo configurado)."));
        } catch (IllegalArgumentException exception) {
            showError("Revise os parâmetros: " + exception.getMessage());
        }
    }

    private void setObstacleFieldsEnabled(boolean enabled) {
        obstacleXField.setEnabled(enabled);
        obstacleYField.setEnabled(enabled);
        obstacleRadiusField.setEnabled(enabled);
    }

    private DiffusionEngine.SimConfig readConfig() {
        Long seed = seedField.getText().isBlank() ? null : Long.parseLong(seedField.getText().trim());
        DiffusionEngine.BoundaryType boundary = (DiffusionEngine.BoundaryType) boundarySelector.getSelectedItem();
        return new DiffusionEngine.SimConfig((int) nField.getValue(), (int) sField.getValue(),
                ((Number) deltaField.getValue()).doubleValue(), ((Number) tauField.getValue()).doubleValue(), seed,
                boundary, ((Number) boundaryLimitField.getValue()).doubleValue(),
                ((Number) driftField.getValue()).doubleValue(), obstacleEnabled.isSelected(),
                ((Number) obstacleXField.getValue()).doubleValue(),
                ((Number) obstacleYField.getValue()).doubleValue(),
                ((Number) obstacleRadiusField.getValue()).doubleValue());
    }

    private void runMultiSeedAnalysis() {
        final DiffusionEngine.SimConfig config;
        try {
            config = readConfig();
        } catch (IllegalArgumentException exception) {
            showError("Revise a semente e os parâmetros: " + exception.getMessage());
            return;
        }
        int replicates = (int) seedCountField.getValue();
        setBusy(true, "Executando " + replicates + " réplicas...");
        new SwingWorker<DiffusionEngine.MultiSeedResult, Void>() {
            @Override
            protected DiffusionEngine.MultiSeedResult doInBackground() {
                return DiffusionEngine.runMultiSeed(config, replicates);
            }

            @Override
            protected void done() {
                try {
                    latestMultiSeed = get();
                    latestConfig = config;
                    plot.setData(latestMultiSeed, config);
                    status.setText(String.format(Locale.US,
                            "Multi-sementes concluído: D médio = %.5f ± %.5f (DP amostral); IC 95%% exibido por passo.",
                            latestMultiSeed.dMean(), latestMultiSeed.dStdDev()));
                } catch (Exception exception) {
                    showError("Falha na análise: " + exception.getMessage());
                } finally {
                    setBusy(false, status.getText());
                }
            }
        }.execute();
    }

    private void runBenchmark() {
        setBusy(true, "Executando benchmark serial/paralelo (100.000 partículas × 100 passos)...");
        new SwingWorker<DiffusionEngine.BenchmarkResult, Void>() {
            @Override
            protected DiffusionEngine.BenchmarkResult doInBackground() {
                return DiffusionEngine.runBenchmark(100_000, 100);
            }

            @Override
            protected void done() {
                try {
                    DiffusionEngine.BenchmarkResult result = get();
                    status.setText(String.format(Locale.US,
                            "Benchmark: serial %d ms | paralelo %d ms | speedup %.2fx",
                            result.timeSequentialMs(), result.timeParallelMs(), result.speedup()));
                } catch (Exception exception) {
                    showError("Falha no benchmark: " + exception.getMessage());
                } finally {
                    setBusy(false, status.getText());
                }
            }
        }.execute();
    }

    private void generateReport() {
        DiffusionEngine.SimConfig config;
        try {
            config = readConfig();
        } catch (IllegalArgumentException exception) {
            showError("Revise a semente e os parâmetros: " + exception.getMessage());
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("relatorio-difusao.md"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        setBusy(true, "Gerando relatório Markdown...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DiffusionEngine.Result1D result = DiffusionEngine.run1D(config);
                DiffusionEngine.generateMarkdownReport(result, config, file);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    status.setText("Relatório salvo em " + file.getAbsolutePath());
                } catch (Exception exception) {
                    showError("Falha ao gerar relatório: " + exception.getMessage());
                } finally {
                    setBusy(false, status.getText());
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy, String message) {
        multiSeedButton.setEnabled(!busy);
        benchmarkButton.setEnabled(!busy);
        reportButton.setEnabled(!busy);
        status.setText(message);
    }

    private void showError(String message) {
        status.setText(message);
        JOptionPane.showMessageDialog(this, message, "Falha na operação", JOptionPane.ERROR_MESSAGE);
    }

    private static final class ConfidencePlot extends JPanel {
        private DiffusionEngine.MultiSeedResult result;
        private DiffusionEngine.SimConfig config;

        ConfidencePlot() {
            setBackground(new Color(248, 250, 251));
        }

        void setData(DiffusionEngine.MultiSeedResult result, DiffusionEngine.SimConfig config) {
            this.result = result;
            this.config = config;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 76;
            int right = getWidth() - 28;
            int top = 46;
            int bottom = getHeight() - 54;
            int width = right - left;
            int height = bottom - top;
            g.setColor(new Color(35, 48, 58));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
            g.drawString("MSD por passo com intervalo de confiança de 95%", left, 27);
            if (result == null) {
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                g.drawString("A curva e o intervalo aparecem após executar a análise multi-semente.", left, top + 28);
                g.dispose();
                return;
            }

            double maximum = 0.0;
            for (int i = 0; i < result.meanMSD().length; i++) {
                maximum = Math.max(maximum, result.meanMSD()[i] + result.confidence95()[i]);
            }
            maximum = Math.max(maximum, 1e-12) * 1.08;
            for (int tick = 0; tick <= 5; tick++) {
                int y = bottom - tick * height / 5;
                g.setColor(new Color(222, 229, 233));
                g.drawLine(left, y, right, y);
                g.setColor(new Color(85, 101, 110));
                g.drawString(String.format(Locale.US, "%.2f", maximum * tick / 5.0), 12, y + 4);
            }

            int count = result.meanMSD().length;
            Path2D confidenceArea = new Path2D.Double();
            for (int i = 0; i < count; i++) {
                double x = left + i * width / (double) Math.max(1, count - 1);
                double y = bottom - (result.meanMSD()[i] + result.confidence95()[i]) * height / maximum;
                if (i == 0) confidenceArea.moveTo(x, y); else confidenceArea.lineTo(x, y);
            }
            for (int i = count - 1; i >= 0; i--) {
                double x = left + i * width / (double) Math.max(1, count - 1);
                double y = bottom - Math.max(0.0, result.meanMSD()[i] - result.confidence95()[i]) * height / maximum;
                confidenceArea.lineTo(x, y);
            }
            confidenceArea.closePath();
            g.setColor(new Color(36, 139, 132, 48));
            g.fill(confidenceArea);

            g.setColor(new Color(36, 139, 132));
            g.setStroke(new BasicStroke(2.3f));
            drawSeries(g, result.meanMSD(), left, bottom, width, height, maximum);
            if (config.boundaryType() == DiffusionEngine.BoundaryType.NONE) {
                double drift = (2.0 * config.driftProbability() - 1.0) * config.delta();
                double[] theory = new double[count];
                for (int step = 0; step < count; step++) {
                    theory[step] = step * config.delta() * config.delta()
                            + step * Math.max(0, step - 1) * drift * drift;
                }
                g.setColor(new Color(196, 74, 54));
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                        1.0f, new float[]{7.0f, 5.0f}, 0.0f));
                drawSeries(g, theory, left, bottom, width, height, maximum);
            }
            g.setColor(new Color(55, 66, 74));
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(left, top, left, bottom);
            g.drawLine(left, bottom, right, bottom);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.drawString("Passo k", left + width / 2 - 20, getHeight() - 18);
            g.drawString("MSD", 18, top - 8);
            g.setColor(new Color(36, 139, 132));
            g.fillRect(right - 235, top + 2, 12, 12);
            g.setColor(new Color(55, 66, 74));
            g.drawString("Média multi-semente", right - 218, top + 13);
            g.setColor(new Color(196, 74, 54));
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    1.0f, new float[]{7.0f, 5.0f}, 0.0f));
            g.drawLine(right - 92, top + 8, right - 76, top + 8);
            g.setColor(new Color(55, 66, 74));
            g.drawString("teoria livre", right - 71, top + 13);
            g.dispose();
        }

        private void drawSeries(Graphics2D g, double[] values, int left, int bottom,
                                int width, int height, double maximum) {
            Path2D path = new Path2D.Double();
            for (int i = 0; i < values.length; i++) {
                double x = left + i * width / (double) Math.max(1, values.length - 1);
                double y = bottom - values[i] * height / maximum;
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            g.draw(path);
        }
    }
}