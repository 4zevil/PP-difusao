package com.example;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DiffusionEngineTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    public void testReprodutibilidadeSemente() {
        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(100, 50, 1.0, 12345L);
        DiffusionEngine.Result1D r1 = DiffusionEngine.run1D(config);
        DiffusionEngine.Result1D r2 = DiffusionEngine.run1D(config);

        assertArrayEquals(r1.trajectories()[0], r2.trajectories()[0], 1e-9);
        assertArrayEquals(r1.msd(), r2.msd(), 1e-9);
    }

    @Test
    public void testSimetriaDeslocamentoMedio() {
        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(5000, 100, 1.0, 1.0, 42L);
        DiffusionEngine.Result1D r = DiffusionEngine.run1D(config);

        assertEquals(0.0, r.meanFinalPos(), 1.0, "O deslocamento médio final deve ser próximo de zero");
    }

    @Test
    public void testLeiDeEscalaMSD() {
        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(5000, 100, 1.0, 1.0, 42L);
        DiffusionEngine.Result1D r = DiffusionEngine.run1D(config);

        double msdFinal = r.msd()[100];
        assertEquals(100.0, msdFinal, 6.0, "MSD no passo 100 deve ser aproximadamente 100");
    }

    @Test
    public void testEstimativaCoeficienteDifusao() {
        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(5000, 100, 1.0, 42L);
        DiffusionEngine.Result1D result = DiffusionEngine.run1D(config);

        double experimentalD = DiffusionEngine.estimateD(result.msd(), 1.0, 1);
        assertEquals(0.5, experimentalD, 0.05);
        assertEquals(0.5, DiffusionEngine.theoreticalD(1.0, 1.0), 1e-12);
        assertEquals(DiffusionEngine.gaussian1D(2.0, 100.0, 0.5, 1.0),
                DiffusionEngine.gaussianDensity(2.0, 100, 1.0), 1e-12);
    }

    @Test
    public void testDimensoesECondicoesIniciais2D3D() {
        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(12, 8, 0.5, 2.0, 9L);
        DiffusionEngine.Result2D result2D = DiffusionEngine.run2D(config);
        DiffusionEngine.Result3D result3D = DiffusionEngine.run3D(config);

        assertEquals(12, result2D.x().length);
        assertEquals(9, result2D.x()[0].length);
        assertEquals(0.0, result2D.x()[0][0], 0.0);
        assertEquals(0.0, result2D.y()[0][0], 0.0);
        assertEquals(0.0, result2D.msd()[0], 0.0);

        assertEquals(12, result3D.z().length);
        assertEquals(9, result3D.z()[0].length);
        assertEquals(0.0, result3D.x()[0][0], 0.0);
        assertEquals(0.0, result3D.y()[0][0], 0.0);
        assertEquals(0.0, result3D.z()[0][0], 0.0);
        assertEquals(0.0, result3D.msd()[0], 0.0);
    }

    @Test
    public void testFronteiraRefletora() {
        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(100, 250, 1.0, 1.0, 19L,
                DiffusionEngine.BoundaryType.REFLECTIVE, 6.0, 0.5, false, 0.0, 0.0, 0.0);
        DiffusionEngine.Result1D result = DiffusionEngine.run1D(config);

        for (double[] trajectory : result.trajectories()) {
            for (double position : trajectory) assertTrue(Math.abs(position) <= 6.0);
        }
    }

    @Test
    public void testDriftPositivo() {
        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(3000, 100, 1.0, 1.0, 42L,
                DiffusionEngine.BoundaryType.NONE, 0.0, 0.8, false, 0.0, 0.0, 0.0);
        assertTrue(DiffusionEngine.run1D(config).meanFinalPos() > 0.0);
    }

    @Test
    public void testObstaculo2DImpedeEntrada() {
        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(20, 25, 1.0, 1.0, 7L,
            DiffusionEngine.BoundaryType.NONE, 0.0, 0.5, true, 2.0, 0.0, 1.5);
        DiffusionEngine.Result2D result = DiffusionEngine.run2D(config);

        for (int particle = 0; particle < config.n(); particle++) {
            for (int step = 0; step <= config.s(); step++) {
                assertTrue(result.x()[particle][step] * result.x()[particle][step]
                    - 4.0 * result.x()[particle][step] + 4.0
                    + result.y()[particle][step] * result.y()[particle][step] >= 2.25);
            }
        }
    }

    @Test
    public void testMultiSeedReprodutivelEComIntervalosValidos() {
        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(500, 40, 1.0, 42L);
        DiffusionEngine.MultiSeedResult first = DiffusionEngine.runMultiSeed(config, 8);
        DiffusionEngine.MultiSeedResult second = DiffusionEngine.runMultiSeed(config, 8);

        assertArrayEquals(first.meanMSD(), second.meanMSD(), 0.0);
        assertEquals(config.s() + 1, first.confidence95().length);
        for (double margin : first.confidence95()) assertTrue(margin >= 0.0);
    }

    @Test
    public void testParaleloConvergeParaFisicaSerial() {
        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(12000, 50, 1.0, 42L);
        DiffusionEngine.Result1D serial = DiffusionEngine.run1D(config);
        DiffusionEngine.Result1D parallel = DiffusionEngine.run1DParallel(config);

        assertEquals(serial.msd()[50], parallel.msd()[50], 2.0);
        assertArrayEquals(parallel.trajectories()[0],
                DiffusionEngine.run1DParallel(config).trajectories()[0], 0.0);
    }

    @Test
    public void testFronteirasPeriodicaEAbsorvente() {
        DiffusionEngine.SimConfig periodic = new DiffusionEngine.SimConfig(20, 100, 1.0, 1.0, 31L,
                DiffusionEngine.BoundaryType.PERIODIC, 4.0, 0.5, false, 0.0, 0.0, 0.0);
        for (double[] trajectory : DiffusionEngine.run1D(periodic).trajectories()) {
            for (double position : trajectory) assertTrue(position >= -4.0 && position < 4.0);
        }

        DiffusionEngine.SimConfig absorbing = new DiffusionEngine.SimConfig(10, 10, 1.0, 1.0, 31L,
                DiffusionEngine.BoundaryType.ABSORBING, 3.0, 1.0, false, 0.0, 0.0, 0.0);
        for (double[] trajectory : DiffusionEngine.run1D(absorbing).trajectories()) {
            assertEquals(3.0, trajectory[3], 0.0);
            for (int step = 3; step < trajectory.length; step++) assertEquals(3.0, trajectory[step], 0.0);
        }
    }

    @Test
    public void testPresetsPersistenciaRelatorioEBenchmark() throws Exception {
        assertTrue(DiffusionEngine.getPresets().containsKey("Caso D: Caixa refletora (L=15)"));
        assertTrue(DiffusionEngine.getPresets().containsKey("Caso F: Obstáculo circular 2D"));

        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(80, 20, 0.75, 0.5, 51L,
                DiffusionEngine.BoundaryType.REFLECTIVE, 8.0, 0.65, true, 3.0, -1.0, 1.5);
        File scenarioFile = temporaryDirectory.resolve("scenario.properties").toFile();
        DiffusionEngine.saveScenario(scenarioFile, config);
        assertEquals(config, DiffusionEngine.loadScenario(scenarioFile));

        DiffusionEngine.Result1D result = DiffusionEngine.run1D(config);
        File reportFile = temporaryDirectory.resolve("report.md").toFile();
        DiffusionEngine.generateMarkdownReport(result, config, reportFile);
        String report = Files.readString(reportFile.toPath());
        assertTrue(report.contains("Observáveis finais"));
        assertTrue(report.contains("D estimado por regressão"));

        DiffusionEngine.BenchmarkResult benchmark = DiffusionEngine.runBenchmark(100, 10);
        assertTrue(benchmark.timeSequentialMs() >= 0);
        assertTrue(benchmark.timeParallelMs() >= 0);
        assertTrue(benchmark.speedup() > 0.0);
    }
}
