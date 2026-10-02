# Relatório de Simulação: Difusão Estocástica

Data da execução: 2026-10-01 22:45:00

## Parâmetros da Simulação

- **População de partículas (N)**: 5000
- **Número de passos (S)**: 100
- **Tamanho do passo espacial (δ)**: 1.000000
- **Duração do passo temporal (τ)**: 1.000000 s
- **Semente pseudoaleatória (Seed)**: 42
- **Condição de fronteira**: NONE (Domínio livre infinito)
- **Limite da fronteira**: 0.00
- **Probabilidade de passo positivo**: 0.5000 (Passeio Simétrico Não Enviesado)
- **Obstáculo 2D**: Não

## Observáveis Estatísticos Finais

- **Deslocamento linear médio final ⟨x⟩**: -0.045600 (Teórico sem fronteiras: 0.000000)
- **Deslocamento Quadrático Médio (MSD)**: 99.920000 (Teórico: 100.000000)
- **Raiz do MSD (RMSD)**: 9.995999 (Teórico: 10.000000)
- **Coeficiente de difusão teórico (D)**: 0.500000
- **Coeficiente de difusão estimado por regressão linear**: 0.496812
- **Diferença relativa de D**: 0.64%

## Série Temporal de Referência

| Passo | Tempo (s) | MSD | RMSD |
|---:|---:|---:|---:|
| 0 | 0.0000 | 0.000000 | 0.000000 |
| 10 | 10.0000 | 9.985600 | 3.160000 |
| 20 | 20.0000 | 19.894400 | 4.460314 |
| 30 | 30.0000 | 29.840000 | 5.462600 |
| 40 | 40.0000 | 39.912000 | 6.317594 |
| 50 | 50.0000 | 49.952000 | 7.067673 |
| 60 | 60.0000 | 59.984000 | 7.744934 |
| 70 | 70.0000 | 69.896000 | 8.360383 |
| 80 | 80.0000 | 79.944000 | 8.941141 |
| 90 | 90.0000 | 89.920000 | 9.482616 |
| 100 | 100.0000 | 99.920000 | 9.995999 |
