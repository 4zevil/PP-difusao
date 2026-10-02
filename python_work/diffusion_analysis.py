# -*- coding: utf-8 -*-
"""
Script de trabalho instrumentado (python_work) para execução dos casos de referência,
cálculo de MSD, RMSD, estimativa de D e exportação de séries temporais em CSV.
"""

import numpy as np
import os

def run_simulation(n, s, delta=1.0, tau=1.0, seed=42):
    if seed is not None:
        np.random.seed(seed)
    steps = np.random.choice([-1.0, 1.0], size=(n, s)) * delta
    trajectories = np.cumsum(steps, axis=1)
    trajectories = np.hstack([np.zeros((n, 1)), trajectories])
    
    msd = np.mean(trajectories**2, axis=0)
    rmsd = np.sqrt(msd)
    mean_positions = np.mean(trajectories, axis=0)
    final_mean = mean_positions[-1]
    
    # Linear regression on MSD(t) to estimate D (MSD = 2 * D * t)
    time_vect = np.arange(s + 1) * tau
    # Fit line through origin / standard linear fit
    slope, _ = np.polyfit(time_vect, msd, 1)
    d_estimated = slope / 2.0
    
    return {
        'n': n,
        's': s,
        'delta': delta,
        'tau': tau,
        'seed': seed,
        'trajectories': trajectories,
        'time': time_vect,
        'msd': msd,
        'rmsd': rmsd,
        'mean_positions': mean_positions,
        'final_mean': final_mean,
        'final_msd': msd[-1],
        'final_rmsd': rmsd[-1],
        'd_estimated': d_estimated,
        'd_theoretical': (delta**2) / (2.0 * tau)
    }

def export_csv(result, filename):
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    header = "passo,tempo,msd,rmsd,media_posicoes\n"
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(header)
        for k in range(result['s'] + 1):
            f.write(f"{k},{result['time'][k]:.4f},{result['msd'][k]:.6f},{result['rmsd'][k]:.6f},{result['mean_positions'][k]:.6f}\n")
    print(f"Exportado com sucesso para: {filename}")

if __name__ == "__main__":
    print("Executando casos de referência...")
    cases = [
        ("A1", 5, 10),
        ("A2", 5, 100),
        ("A3", 5, 1000),
        ("B1", 100, 10),
        ("B2", 100, 100),
        ("B3", 100, 1000),
        ("B_large", 5000, 100)
    ]
    for name, n, s in cases:
        res = run_simulation(n, s, seed=42)
        print(f"Caso {name:7s} (N={n:4d}, S={s:4d}) -> <x>={res['final_mean']:+8.4f}, MSD={res['final_msd']:9.4f}, RMSD={res['final_rmsd']:7.4f}, D_est={res['d_estimated']:.4f}")
        
    # Export sample
    sample_res = run_simulation(100, 100, seed=42)
    export_csv(sample_res, os.path.join("..", "relatorios", "simulacao_exemplo.csv"))
