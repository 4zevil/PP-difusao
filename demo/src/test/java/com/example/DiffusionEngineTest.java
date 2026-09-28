package com.example;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DiffusionEngineTest {

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
}
