package com.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;

public class DiffusionEngine {

    public record SimConfig(int n, int s, double delta, double tau, Long seed) {
        public SimConfig(int n, int s, double delta, double tau) {
            this(n, s, delta, tau, null);
        }
    }

    public record Result1D(
            double[][] trajectories,
            double[] msd,
            double[] rmsd,
            double meanFinalPos,
            HistogramData histogram
    ) {}

    public record Result2D(
            double[][] x,
            double[][] y,
            double[] msd,
            double[] rmsd
    ) {}

    public record Result3D(
            double[][] x,
            double[][] y,
            double[][] z,
            double[] msd,
            double[] rmsd
    ) {}

    public record HistogramData(
            double[] binCenters,
            int[] counts,
            double[] density,
            double binWidth
    ) {}

    public static Result1D run1D(SimConfig config) {
        int n = Math.max(1, config.n());
        int s = Math.max(0, config.s());
        double delta = config.delta();
        Random random = config.seed() != null ? new Random(config.seed()) : new Random();

        double[][] trajectories = new double[n][s + 1];
        for (int i = 0; i < n; i++) {
            trajectories[i][0] = 0.0;
        }

        for (int i = 0; i < n; i++) {
            for (int step = 1; step <= s; step++) {
                double direction = random.nextBoolean() ? 1.0 : -1.0;
                trajectories[i][step] = trajectories[i][step - 1] + delta * direction;
            }
        }

        double[] msd = new double[s + 1];
        double[] rmsd = new double[s + 1];
        for (int step = 0; step <= s; step++) {
            double sumSquares = 0.0;
            for (int i = 0; i < n; i++) {
                double value = trajectories[i][step];
                sumSquares += value * value;
            }
            msd[step] = sumSquares / n;
            rmsd[step] = Math.sqrt(msd[step]);
        }

        double sumFinal = 0.0;
        for (int i = 0; i < n; i++) {
            sumFinal += trajectories[i][s];
        }
        double meanFinalPos = sumFinal / n;
        HistogramData histogram = calculateHistogram(getFinalPositions(trajectories), 20);

        return new Result1D(trajectories, msd, rmsd, meanFinalPos, histogram);
    }

    public static Result2D run2D(SimConfig config) {
        int n = Math.max(1, config.n());
        int s = Math.max(0, config.s());
        double delta = config.delta();
        Random random = config.seed() != null ? new Random(config.seed()) : new Random();

        double[][] x = new double[n][s + 1];
        double[][] y = new double[n][s + 1];

        for (int i = 0; i < n; i++) {
            x[i][0] = 0.0;
            y[i][0] = 0.0;
        }

        for (int i = 0; i < n; i++) {
            for (int step = 1; step <= s; step++) {
                x[i][step] = x[i][step - 1] + delta * (random.nextBoolean() ? 1.0 : -1.0);
                y[i][step] = y[i][step - 1] + delta * (random.nextBoolean() ? 1.0 : -1.0);
            }
        }

        double[] msd = new double[s + 1];
        double[] rmsd = new double[s + 1];
        for (int step = 0; step <= s; step++) {
            double sumSquares = 0.0;
            for (int i = 0; i < n; i++) {
                sumSquares += x[i][step] * x[i][step] + y[i][step] * y[i][step];
            }
            msd[step] = sumSquares / n;
            rmsd[step] = Math.sqrt(msd[step]);
        }

        return new Result2D(x, y, msd, rmsd);
    }

    public static Result3D run3D(SimConfig config) {
        int n = Math.max(1, config.n());
        int s = Math.max(0, config.s());
        double delta = config.delta();
        Random random = config.seed() != null ? new Random(config.seed()) : new Random();

        double[][] x = new double[n][s + 1];
        double[][] y = new double[n][s + 1];
        double[][] z = new double[n][s + 1];

        for (int i = 0; i < n; i++) {
            x[i][0] = 0.0;
            y[i][0] = 0.0;
            z[i][0] = 0.0;
        }

        for (int i = 0; i < n; i++) {
            for (int step = 1; step <= s; step++) {
                x[i][step] = x[i][step - 1] + delta * (random.nextBoolean() ? 1.0 : -1.0);
                y[i][step] = y[i][step - 1] + delta * (random.nextBoolean() ? 1.0 : -1.0);
                z[i][step] = z[i][step - 1] + delta * (random.nextBoolean() ? 1.0 : -1.0);
            }
        }

        double[] msd = new double[s + 1];
        double[] rmsd = new double[s + 1];
        for (int step = 0; step <= s; step++) {
            double sumSquares = 0.0;
            for (int i = 0; i < n; i++) {
                sumSquares += x[i][step] * x[i][step] + y[i][step] * y[i][step] + z[i][step] * z[i][step];
            }
            msd[step] = sumSquares / n;
            rmsd[step] = Math.sqrt(msd[step]);
        }

        return new Result3D(x, y, z, msd, rmsd);
    }

    public static HistogramData calculateHistogram(double[] values, int numBins) {
        int bins = Math.max(1, numBins);
        if (values == null || values.length == 0) {
            double[] empty = new double[0];
            return new HistogramData(empty, new int[0], new double[0], 0.0);
        }

        double min = values[0];
        double max = values[0];
        for (double value : values) {
            if (value < min) min = value;
            if (value > max) max = value;
        }

        double binWidth = (max - min) / bins;
        if (Double.isNaN(binWidth) || !Double.isFinite(binWidth) || binWidth <= 0.0) {
            binWidth = 1.0;
        }

        int[] counts = new int[bins];
        double[] centers = new double[bins];
        for (int b = 0; b < bins; b++) {
            centers[b] = min + (b + 0.5) * binWidth;
        }

        for (double value : values) {
            int index = bins - 1;
            if (max > min) {
                int rawIndex = (int) ((value - min) / binWidth);
                if (rawIndex < 0) {
                    index = 0;
                } else if (rawIndex >= bins) {
                    index = bins - 1;
                } else {
                    index = rawIndex;
                }
            }
            counts[index]++;
        }

        double[] density = new double[bins];
        double total = values.length;
        for (int b = 0; b < bins; b++) {
            density[b] = counts[b] / (total * binWidth);
        }

        return new HistogramData(centers, counts, density, binWidth);
    }

    public static double gaussian1D(double x, double t, double D, double n0) {
        if (t <= 0.0 || D <= 0.0) {
            return 0.0;
        }
        return n0 / Math.sqrt(4.0 * Math.PI * D * t) * Math.exp(-(x * x) / (4.0 * D * t));
    }

    public static double estimateDiffusionCoefficient(double[] msd, double tau, int dimension) {
        if (msd == null || msd.length < 2 || tau <= 0.0 || dimension <= 0) {
            return 0.0;
        }

        double[] times = new double[msd.length];
        for (int i = 0; i < msd.length; i++) {
            times[i] = i * tau;
        }

        double meanTime = 0.0;
        double meanMsd = 0.0;
        for (int i = 0; i < msd.length; i++) {
            meanTime += times[i];
            meanMsd += msd[i];
        }
        meanTime /= msd.length;
        meanMsd /= msd.length;

        double numerator = 0.0;
        double denominator = 0.0;
        for (int i = 0; i < msd.length; i++) {
            double dt = times[i] - meanTime;
            double dm = msd[i] - meanMsd;
            numerator += dt * dm;
            denominator += dt * dt;
        }

        if (denominator == 0.0) {
            return 0.0;
        }

        double slope = numerator / denominator;
        return slope / (2.0 * dimension);
    }

    public static void exportToCSV(File file, Result1D result, SimConfig config) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            Files.createDirectories(parent.toPath());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("passo,tempo,msd,rmsd,media_posicoes");
            writer.newLine();

            int steps = config.s();
            for (int step = 0; step <= steps; step++) {
                double meanPos = 0.0;
                for (int i = 0; i < config.n(); i++) {
                    meanPos += result.trajectories()[i][step];
                }
                meanPos /= config.n();

                writer.write(step + "," + (step * config.tau()) + "," + result.msd()[step] + ","
                        + result.rmsd()[step] + "," + meanPos);
                writer.newLine();
            }
        }
    }

    private static double[] getFinalPositions(double[][] trajectories) {
        if (trajectories == null || trajectories.length == 0) {
            return new double[0];
        }
        int lastStep = trajectories[0].length - 1;
        double[] values = new double[trajectories.length];
        for (int i = 0; i < trajectories.length; i++) {
            values[i] = trajectories[i][lastStep];
        }
        return values;
    }
}
