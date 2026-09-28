package com.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.stream.IntStream;

public final class DiffusionEngine {
    private DiffusionEngine() {}

    public enum BoundaryType { NONE, REFLECTIVE, PERIODIC, ABSORBING }

    public record SimConfig(
            int n,
            int s,
            double delta,
            double tau,
            Long seed,
            BoundaryType boundaryType,
            double boundaryLimit,
            double driftProbability,
            boolean hasObstacle,
            double obsX,
            double obsY,
            double obsRadius
    ) {
        public SimConfig {
            if (n < 1 || s < 0) throw new IllegalArgumentException("n must be positive and s non-negative");
            if (!Double.isFinite(delta) || delta <= 0.0) throw new IllegalArgumentException("delta must be positive and finite");
            if (!Double.isFinite(tau) || tau <= 0.0) throw new IllegalArgumentException("tau must be positive and finite");
            if (boundaryType == null) boundaryType = BoundaryType.NONE;
            if (boundaryType != BoundaryType.NONE && (!Double.isFinite(boundaryLimit) || boundaryLimit <= 0.0)) {
                throw new IllegalArgumentException("boundaryLimit must be positive for bounded domains");
            }
            if (!Double.isFinite(driftProbability) || driftProbability < 0.0 || driftProbability > 1.0) {
                throw new IllegalArgumentException("driftProbability must be between 0 and 1");
            }
            if (hasObstacle && (!Double.isFinite(obsX) || !Double.isFinite(obsY)
                    || !Double.isFinite(obsRadius) || obsRadius <= 0.0)) {
                throw new IllegalArgumentException("obstacle radius must be positive and finite");
            }
        }

        public SimConfig(int n, int s, double delta, Long seed) {
            this(n, s, delta, 1.0, seed, BoundaryType.NONE, 0.0, 0.5, false, 0.0, 0.0, 0.0);
        }

        public SimConfig(int n, int s, double delta, long seed) {
            this(n, s, delta, 1.0, Long.valueOf(seed), BoundaryType.NONE, 0.0, 0.5, false, 0.0, 0.0, 0.0);
        }

        public SimConfig(int n, int s, double delta, double tau) {
            this(n, s, delta, tau, null, BoundaryType.NONE, 0.0, 0.5, false, 0.0, 0.0, 0.0);
        }

        public SimConfig(int n, int s, double delta, double tau, Long seed) {
            this(n, s, delta, tau, seed, BoundaryType.NONE, 0.0, 0.5, false, 0.0, 0.0, 0.0);
        }

        public SimConfig(int n, int s, double delta) {
            this(n, s, delta, 1.0, null, BoundaryType.NONE, 0.0, 0.5, false, 0.0, 0.0, 0.0);
        }
    }

    public record Result1D(double[][] trajectories, double[] msd, double[] rmsd,
                           double meanFinalPos, HistogramData histogram) {}

    public record Result2D(double[][] x, double[][] y, double[] msd, double[] rmsd) {}

    public record Result3D(double[][] x, double[][] y, double[][] z, double[] msd, double[] rmsd) {}

    public record HistogramData(double[] binCenters, int[] counts, double[] density, double binWidth) {}

    public record MultiSeedResult(double[] meanMSD, double[] stdDevMSD,
                                  double[] confidence95, double dMean, double dStdDev) {}

    public record BenchmarkResult(long timeSequentialMs, long timeParallelMs, double speedup) {}

    public static Result1D run1D(SimConfig config) {
        Random random = config.seed() == null ? new Random() : new Random(config.seed());
        double[][] trajectories = new double[config.n()][config.s() + 1];
        for (int particle = 0; particle < config.n(); particle++) {
            simulateParticle1D(config, trajectories[particle], random);
        }
        return summarize1D(trajectories);
    }

    public static Result1D run1DParallel(SimConfig config) {
        long baseSeed = config.seed() == null ? new Random().nextLong() : config.seed();
        double[][] trajectories = new double[config.n()][config.s() + 1];
        IntStream.range(0, config.n()).parallel().forEach(particle -> {
            Random random = new Random(baseSeed + particle * 31L);
            simulateParticle1D(config, trajectories[particle], random);
        });
        return summarize1D(trajectories);
    }

    private static void simulateParticle1D(SimConfig config, double[] trajectory, Random random) {
        boolean[] absorbed = {false};
        for (int step = 1; step <= config.s(); step++) {
            double direction = random.nextDouble() < config.driftProbability() ? 1.0 : -1.0;
            double next = trajectory[step - 1] + direction * config.delta();
                trajectory[step] = applyBoundary(next, trajectory[step - 1], config.boundaryLimit(),
                    config.boundaryType(), absorbed);
        }
    }

    private static Result1D summarize1D(double[][] trajectories) {
        int n = trajectories.length;
        int s = trajectories[0].length - 1;
        double[] msd = new double[s + 1];
        double[] rmsd = new double[s + 1];
        for (int step = 0; step <= s; step++) {
            double sumSquares = 0.0;
            for (double[] trajectory : trajectories) sumSquares += trajectory[step] * trajectory[step];
            msd[step] = sumSquares / n;
            rmsd[step] = Math.sqrt(msd[step]);
        }
        double meanFinal = 0.0;
        for (double[] trajectory : trajectories) meanFinal += trajectory[s];
        meanFinal /= n;
        return new Result1D(trajectories, msd, rmsd, meanFinal,
                calculateHistogram(getFinalPositions(trajectories), 20));
    }

    public static Result2D run2D(SimConfig config) {
        Random random = config.seed() == null ? new Random() : new Random(config.seed());
        double[][] x = new double[config.n()][config.s() + 1];
        double[][] y = new double[config.n()][config.s() + 1];
        for (int particle = 0; particle < config.n(); particle++) {
            boolean[] absorbedX = {false};
            boolean[] absorbedY = {false};
            for (int step = 1; step <= config.s(); step++) {
                double previousX = x[particle][step - 1];
                double previousY = y[particle][step - 1];
                double nextX = previousX + randomStep(config, random);
                double nextY = previousY + randomStep(config, random);
                if (config.hasObstacle()) {
                    double dx = nextX - config.obsX();
                    double dy = nextY - config.obsY();
                    if (dx * dx + dy * dy < config.obsRadius() * config.obsRadius()) {
                        nextX = previousX;
                        nextY = previousY;
                    }
                }
                x[particle][step] = applyBoundary(nextX, previousX, config.boundaryLimit(),
                    config.boundaryType(), absorbedX);
                y[particle][step] = applyBoundary(nextY, previousY, config.boundaryLimit(),
                    config.boundaryType(), absorbedY);
            }
        }
        return new Result2D(x, y, calculateMsd(x, y), nullSafeRmsd(calculateMsd(x, y)));
    }

    public static Result3D run3D(SimConfig config) {
        Random random = config.seed() == null ? new Random() : new Random(config.seed());
        double[][] x = new double[config.n()][config.s() + 1];
        double[][] y = new double[config.n()][config.s() + 1];
        double[][] z = new double[config.n()][config.s() + 1];
        for (int particle = 0; particle < config.n(); particle++) {
            boolean[] absorbedX = {false};
            boolean[] absorbedY = {false};
            boolean[] absorbedZ = {false};
            for (int step = 1; step <= config.s(); step++) {
                x[particle][step] = applyBoundary(x[particle][step - 1] + randomStep(config, random),
                    x[particle][step - 1], config.boundaryLimit(), config.boundaryType(), absorbedX);
                y[particle][step] = applyBoundary(y[particle][step - 1] + randomStep(config, random),
                    y[particle][step - 1], config.boundaryLimit(), config.boundaryType(), absorbedY);
                z[particle][step] = applyBoundary(z[particle][step - 1] + randomStep(config, random),
                    z[particle][step - 1], config.boundaryLimit(), config.boundaryType(), absorbedZ);
            }
        }
        double[] msd = calculateMsd(x, y, z);
        return new Result3D(x, y, z, msd, nullSafeRmsd(msd));
    }

    private static double randomStep(SimConfig config, Random random) {
        return (random.nextDouble() < config.driftProbability() ? 1.0 : -1.0) * config.delta();
    }

    private static double[] calculateMsd(double[][]... coordinates) {
        int steps = coordinates[0][0].length;
        int n = coordinates[0].length;
        double[] msd = new double[steps];
        for (int step = 0; step < steps; step++) {
            double sum = 0.0;
            for (double[][] coordinate : coordinates) {
                for (int particle = 0; particle < n; particle++) {
                    double value = coordinate[particle][step];
                    sum += value * value;
                }
            }
            msd[step] = sum / n;
        }
        return msd;
    }

    private static double[] nullSafeRmsd(double[] msd) {
        double[] rmsd = new double[msd.length];
        for (int i = 0; i < msd.length; i++) rmsd[i] = Math.sqrt(msd[i]);
        return rmsd;
    }

    private static double applyBoundary(double value, double previousValue, double limit,
                                        BoundaryType type, boolean[] absorbed) {
        if (type == BoundaryType.NONE) return value;
        if (type == BoundaryType.ABSORBING) {
            if (absorbed[0]) return previousValue;
            if (value >= limit) {
                absorbed[0] = true;
                return limit;
            }
            if (value <= -limit) {
                absorbed[0] = true;
                return -limit;
            }
            return value;
        }
        if (type == BoundaryType.PERIODIC) {
            double width = 2.0 * limit;
            return ((value + limit) % width + width) % width - limit;
        }
        double period = 4.0 * limit;
        double folded = ((value + limit) % period + period) % period;
        return folded <= 2.0 * limit ? -limit + folded : 3.0 * limit - folded;
    }

    public static HistogramData calculateHistogram(double[] values, int numBins) {
        int bins = Math.max(1, numBins);
        if (values == null || values.length == 0) {
            return new HistogramData(new double[0], new int[0], new double[0], 0.0);
        }
        double min = Arrays.stream(values).min().orElse(0.0);
        double max = Arrays.stream(values).max().orElse(min);
        double binWidth = max > min ? (max - min) / bins : 1.0;
        int[] counts = new int[bins];
        double[] centers = new double[bins];
        for (int bin = 0; bin < bins; bin++) centers[bin] = min + (bin + 0.5) * binWidth;
        for (double value : values) {
            int index = max > min ? (int) ((value - min) / binWidth) : 0;
            counts[Math.max(0, Math.min(bins - 1, index))]++;
        }
        double[] density = new double[bins];
        for (int bin = 0; bin < bins; bin++) density[bin] = counts[bin] / (values.length * binWidth);
        return new HistogramData(centers, counts, density, binWidth);
    }

    public static HistogramData computeHistogram(double[] values, int numBins) {
        return calculateHistogram(values, numBins);
    }

    public static double gaussian1D(double x, double t, double diffusion, double n0) {
        if (t <= 0.0 || diffusion <= 0.0) return 0.0;
        return n0 / Math.sqrt(4.0 * Math.PI * diffusion * t)
                * Math.exp(-(x * x) / (4.0 * diffusion * t));
    }

    public static double gaussianDensity(double x, int s, double delta) {
        double variance = s * delta * delta;
        if (variance <= 0.0) return 0.0;
        return Math.exp(-(x * x) / (2.0 * variance)) / Math.sqrt(2.0 * Math.PI * variance);
    }

    public static double theoreticalD(double delta, double tau) {
        if (tau <= 0.0) return 0.0;
        return delta * delta / (2.0 * tau);
    }

    public static double estimateD(double[] msd, double tau, int dimension) {
        return estimateDiffusionCoefficient(msd, tau, dimension);
    }

    public static double estimateDiffusionCoefficient(double[] msd, double tau, int dimension) {
        if (msd == null || msd.length < 2 || tau <= 0.0 || dimension <= 0) return 0.0;
        double meanTime = tau * (msd.length - 1) / 2.0;
        double meanMsd = Arrays.stream(msd).average().orElse(0.0);
        double numerator = 0.0;
        double denominator = 0.0;
        for (int i = 0; i < msd.length; i++) {
            double centeredTime = i * tau - meanTime;
            numerator += centeredTime * (msd[i] - meanMsd);
            denominator += centeredTime * centeredTime;
        }
        return denominator == 0.0 ? 0.0 : numerator / denominator / (2.0 * dimension);
    }

    public static MultiSeedResult runMultiSeed(SimConfig baseConfig, int numSeeds) {
        if (numSeeds < 1) throw new IllegalArgumentException("numSeeds must be positive");
        long baseSeed = baseConfig.seed() == null ? new Random().nextLong() : baseConfig.seed();
        double[][] allMSD = new double[numSeeds][];
        double[] allD = new double[numSeeds];
        for (int run = 0; run < numSeeds; run++) {
            SimConfig config = withSeed(baseConfig, baseSeed + run * 10007L);
            Result1D result = run1D(config);
            allMSD[run] = result.msd();
            allD[run] = estimateD(result.msd(), config.tau(), 1);
        }

        int steps = baseConfig.s() + 1;
        double[] mean = new double[steps];
        double[] stdDev = new double[steps];
        double[] confidence95 = new double[steps];
        for (int step = 0; step < steps; step++) {
            for (int run = 0; run < numSeeds; run++) mean[step] += allMSD[run][step];
            mean[step] /= numSeeds;
            for (int run = 0; run < numSeeds; run++) {
                double difference = allMSD[run][step] - mean[step];
                stdDev[step] += difference * difference;
            }
            stdDev[step] = Math.sqrt(stdDev[step] / Math.max(1, numSeeds - 1));
            confidence95[step] = 1.96 * stdDev[step] / Math.sqrt(numSeeds);
        }
        double meanD = Arrays.stream(allD).average().orElse(0.0);
        double varianceD = 0.0;
        for (double diffusion : allD) varianceD += (diffusion - meanD) * (diffusion - meanD);
        double stdD = Math.sqrt(varianceD / Math.max(1, numSeeds - 1));
        return new MultiSeedResult(mean, stdDev, confidence95, meanD, stdD);
    }

    public static BenchmarkResult runBenchmark(int n, int s) {
        SimConfig config = new SimConfig(n, s, 1.0, 1.0, 42L);
        long startSequential = System.nanoTime();
        run1D(config);
        long sequentialNanos = System.nanoTime() - startSequential;
        long startParallel = System.nanoTime();
        run1DParallel(config);
        long parallelNanos = System.nanoTime() - startParallel;
        long sequentialMs = Math.max(0L, sequentialNanos / 1_000_000L);
        long parallelMs = Math.max(0L, parallelNanos / 1_000_000L);
        return new BenchmarkResult(sequentialMs, parallelMs,
                (double) sequentialNanos / Math.max(1L, parallelNanos));
    }

    public static Map<String, SimConfig> getPresets() {
        Map<String, SimConfig> presets = new LinkedHashMap<>();
        presets.put("Caso A: Flutuações (N=5, S=100)", new SimConfig(5, 100, 1.0, 42L));
        presets.put("Caso B: Alta Estatística (N=1000, S=100)", new SimConfig(1000, 100, 1.0, 42L));
        presets.put("Caso C: Drift positivo (p=0.6)",
                new SimConfig(500, 100, 1.0, 1.0, 42L, BoundaryType.NONE, 0.0, 0.6, false, 0, 0, 0));
        presets.put("Caso D: Caixa refletora (L=15)",
                new SimConfig(500, 200, 1.0, 1.0, 42L, BoundaryType.REFLECTIVE, 15.0, 0.5, false, 0, 0, 0));
        presets.put("Caso E: Caixa periódica (L=20)",
                new SimConfig(500, 200, 1.0, 1.0, 42L, BoundaryType.PERIODIC, 20.0, 0.5, false, 0, 0, 0));
        presets.put("Caso F: Obstáculo circular 2D", new SimConfig(500, 200, 1.0, 1.0, 42L,
                BoundaryType.NONE, 0.0, 0.5, true, 5.0, 0.0, 3.0));
        return Collections.unmodifiableMap(presets);
    }

    public static void saveScenario(File file, SimConfig config) throws IOException {
        if (file == null || config == null) throw new IllegalArgumentException("file and config are required");
        Properties properties = new Properties();
        properties.setProperty("n", Integer.toString(config.n()));
        properties.setProperty("s", Integer.toString(config.s()));
        properties.setProperty("delta", Double.toString(config.delta()));
        properties.setProperty("tau", Double.toString(config.tau()));
        properties.setProperty("seed", config.seed() == null ? "" : config.seed().toString());
        properties.setProperty("boundaryType", config.boundaryType().name());
        properties.setProperty("boundaryLimit", Double.toString(config.boundaryLimit()));
        properties.setProperty("driftProbability", Double.toString(config.driftProbability()));
        properties.setProperty("hasObstacle", Boolean.toString(config.hasObstacle()));
        properties.setProperty("obsX", Double.toString(config.obsX()));
        properties.setProperty("obsY", Double.toString(config.obsY()));
        properties.setProperty("obsRadius", Double.toString(config.obsRadius()));
        File parent = file.getParentFile();
        if (parent != null) Files.createDirectories(parent.toPath());
        try (var output = Files.newOutputStream(file.toPath())) {
            properties.store(output, "Diffusion simulation scenario");
        }
    }

    public static SimConfig loadScenario(File file) throws IOException {
        if (file == null) throw new IllegalArgumentException("file is required");
        Properties properties = new Properties();
        try (var input = Files.newInputStream(file.toPath())) {
            properties.load(input);
        }
        String seedText = properties.getProperty("seed", "").trim();
        Long seed = seedText.isEmpty() ? null : Long.parseLong(seedText);
        return new SimConfig(
                Integer.parseInt(properties.getProperty("n")),
                Integer.parseInt(properties.getProperty("s")),
                Double.parseDouble(properties.getProperty("delta")),
                Double.parseDouble(properties.getProperty("tau")),
                seed,
                BoundaryType.valueOf(properties.getProperty("boundaryType", "NONE")),
                Double.parseDouble(properties.getProperty("boundaryLimit", "0")),
                Double.parseDouble(properties.getProperty("driftProbability", "0.5")),
                Boolean.parseBoolean(properties.getProperty("hasObstacle", "false")),
                Double.parseDouble(properties.getProperty("obsX", "0")),
                Double.parseDouble(properties.getProperty("obsY", "0")),
                Double.parseDouble(properties.getProperty("obsRadius", "0")));
    }

    public static void exportToCSV(File file, Result1D result, SimConfig config) throws IOException {
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        File parent = file.getParentFile();
        if (parent != null) Files.createDirectories(parent.toPath());
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("passo,tempo,msd,rmsd,media_posicoes");
            writer.newLine();
            for (int step = 0; step <= config.s(); step++) {
                double meanPosition = 0.0;
                for (double[] trajectory : result.trajectories()) meanPosition += trajectory[step];
                meanPosition /= result.trajectories().length;
                writer.write(step + "," + step * config.tau() + "," + result.msd()[step] + ","
                        + result.rmsd()[step] + "," + meanPosition);
                writer.newLine();
            }
        }
    }

    public static void generateMarkdownReport(Result1D result, SimConfig config, File file) throws IOException {
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        double dTheoretical = theoreticalD(config.delta(), config.tau());
        double dExperimental = estimateD(result.msd(), config.tau(), 1);
        double relativeError = dTheoretical == 0.0 ? 0.0
                : Math.abs(dExperimental - dTheoretical) / dTheoretical * 100.0;
        double meanTheory = config.s() * (2.0 * config.driftProbability() - 1.0) * config.delta();
        double driftStep = (2.0 * config.driftProbability() - 1.0) * config.delta();
        double msdTheory = config.s() * config.delta() * config.delta()
                + config.s() * Math.max(0, config.s() - 1) * driftStep * driftStep;

        StringBuilder report = new StringBuilder();
        report.append("# Relatório de Simulação: Difusão Estocástica\n\n")
                .append("Data da execução: ").append(LocalDateTime.now()).append("\n\n")
                .append("## Parâmetros\n\n")
                .append("- N: ").append(config.n()).append("\n")
                .append("- S: ").append(config.s()).append("\n")
                .append("- δ: ").append(config.delta()).append("\n")
                .append("- τ: ").append(config.tau()).append(" s\n")
                .append("- Seed: ").append(config.seed() == null ? "aleatória" : config.seed()).append("\n")
                .append("- Fronteira: ").append(config.boundaryType()).append("\n")
                .append("- Limite da fronteira: ").append(config.boundaryLimit()).append("\n")
                .append("- Probabilidade de passo positivo: ").append(config.driftProbability()).append("\n")
                .append("- Obstáculo circular 2D: ").append(config.hasObstacle() ? "sim (não aplicado neste relatório 1D)" : "não").append("\n\n")
                .append("## Observáveis finais\n\n")
                .append(String.format(java.util.Locale.ROOT, "- Deslocamento médio: %.5f (teórico sem fronteiras: %.5f)\n",
                        result.meanFinalPos(), meanTheory))
                .append(String.format(java.util.Locale.ROOT, "- MSD: %.5f (teórico livre: %.5f)\n",
                        result.msd()[config.s()], msdTheory))
                .append(String.format(java.util.Locale.ROOT, "- RMSD: %.5f\n", result.rmsd()[config.s()]))
                .append(String.format(java.util.Locale.ROOT, "- D teórico: %.6f\n", dTheoretical))
                .append(String.format(java.util.Locale.ROOT, "- D estimado por regressão: %.6f\n", dExperimental))
                .append(String.format(java.util.Locale.ROOT, "- Diferença relativa de D: %.2f%%\n\n", relativeError))
                .append("## Série temporal\n\n")
                .append("| Passo | Tempo (s) | MSD | RMSD |\n|---:|---:|---:|---:|\n");
        int interval = Math.max(1, config.s() / 10);
        for (int step = 0; step <= config.s(); step += interval) {
            appendReportRow(report, result, config, step);
        }
        if (config.s() % interval != 0) appendReportRow(report, result, config, config.s());

        File parent = file.getParentFile();
        if (parent != null) Files.createDirectories(parent.toPath());
        Files.writeString(file.toPath(), report, StandardCharsets.UTF_8);
    }

    private static void appendReportRow(StringBuilder report, Result1D result, SimConfig config, int step) {
        report.append(String.format(java.util.Locale.ROOT, "| %d | %.4f | %.6f | %.6f |%n",
                step, step * config.tau(), result.msd()[step], result.rmsd()[step]));
    }

    private static SimConfig withSeed(SimConfig config, long seed) {
        return new SimConfig(config.n(), config.s(), config.delta(), config.tau(), seed,
                config.boundaryType(), config.boundaryLimit(), config.driftProbability(),
                config.hasObstacle(), config.obsX(), config.obsY(), config.obsRadius());
    }

    private static double[] getFinalPositions(double[][] trajectories) {
        double[] values = new double[trajectories.length];
        int finalStep = trajectories[0].length - 1;
        for (int i = 0; i < trajectories.length; i++) values[i] = trajectories[i][finalStep];
        return values;
    }
}