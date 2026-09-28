package com.example;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DiffusionEngineTest {

    @Test
    public void testReprodutibilidadeSemente() {
        DiffusionEngine.SimConfig config = new DiffusionEngine.SimConfig(50, 100, 1.0, 1.0, 12345L);
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
        assertEquals(100.0, msdFinal, 5.0, "MSD no passo 100 deve ser aproximadamente 100");
    }
}
