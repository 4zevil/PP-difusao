package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random; 

public class Simulador3 extends JPanel {
    private static final Color[] PARTICLE_COLORS = {
        new Color(31, 119, 180), new Color(255, 127, 14), new Color(44, 160, 44),
        new Color(214, 39, 40), new Color(148, 103, 189), new Color(140, 86, 75),
        new Color(227, 119, 194), new Color(127, 127, 127), new Color(188, 189, 34),
        new Color(23, 190, 207)
    };


    // Parametros do experimento
    static final int numPart = 120;
    static final int numSteps = 100;
    static final int nSigma = 3;

    static double[][] randVectX = new double[numPart][numSteps];
    static double[][] randVectY = new double[numPart][numSteps];
    static double[][] randVectZ = new double[numPart][numSteps];

    static double maxVXYZ;

    static double[] randMeanX = new double[numSteps];
    static double[] randMeanY = new double[numSteps];
    static double[] randMeanZ = new double[numSteps];

    static double[] randSquareMeanX = new double[numSteps];
    static double[] randSquareMeanY = new double[numSteps];
    static double[] randSquareMeanZ = new double[numSteps];

    // Controle da Animacao
    private int currentStep = 0;
    private Timer animationTimer;
    private boolean isPlaying = true;

    // Camera 3D (Angulos em radianos)
    private double rotX = Math.toRadians(20);  // elev=20
    private double rotY = Math.toRadians(-170); // azim=-170
    private double zoom = 1.0;
    private Point lastMousePos;

    public Simulador3() {
        setBackground(new Color(248, 250, 251));
        setPreferredSize(new Dimension(850, 700));

        // Controle do Mouse para Rotacao 3D
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastMousePos != null) {
                    int dx = e.getX() - lastMousePos.x;
                    int dy = e.getY() - lastMousePos.y;

                    rotY += dx * 0.01;
                    rotX += dy * 0.01;

                    // Limita elevacao
                    rotX = Math.max(-Math.PI / 2 + 0.05, Math.min(Math.PI / 2 - 0.05, rotX));

                    lastMousePos = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getWheelRotation() < 0) {
                    zoom *= 1.1;
                } else {
                    zoom /= 1.1;
                }
                repaint();
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);

        // Timer de animacao: 100ms por passo (plt.pause(0.1))
        animationTimer = new Timer(100, e -> {
            if (currentStep < numSteps - 1) {
                currentStep++;
                repaint();
            } else {
                animationTimer.stop();
                isPlaying = false;
            }
        });
        animationTimer.start();
    }

    public static void main(String[] args) {
        // 1. Gera simulacao de Monte Carlo
        executarSimulacao();

        // 2. Abre a interface grafica Swing
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Diffusion in 3D: " + numPart + " particles during " + numSteps + " steps");
            Simulador3 painel = new Simulador3();

            // Barra inferior com botoes de controle
            JPanel controlPanel = new JPanel();
            JButton btnPlayPause = new JButton("Pausar");
            JButton btnRestart = new JButton("Reiniciar");
            JLabel lblStep = new JLabel("Passo: 1 / " + numSteps);

            btnPlayPause.addActionListener(e -> {
                if (painel.isPlaying) {
                    painel.animationTimer.stop();
                    painel.isPlaying = false;
                    btnPlayPause.setText("Continuar");
                } else {
                    painel.animationTimer.start();
                    painel.isPlaying = true;
                    btnPlayPause.setText("Pausar");
                }
            });

            btnRestart.addActionListener(e -> {
                painel.currentStep = 0;
                painel.isPlaying = true;
                btnPlayPause.setText("Pausar");
                painel.animationTimer.restart();
            });

            new Timer(50, e -> {
                lblStep.setText("Passo: " + (painel.currentStep + 1) + " / " + numSteps);
            }).start();

            controlPanel.add(btnPlayPause);
            controlPanel.add(btnRestart);
            controlPanel.add(lblStep);

            frame.setLayout(new BorderLayout());
            frame.add(painel, BorderLayout.CENTER);
            frame.add(controlPanel, BorderLayout.SOUTH);

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static void executarSimulacao() {
        Random random = new Random();

        for (int p = 0; p < numPart; p++) {
            double somaX = 0, somaY = 0, somaZ = 0;
            for (int s = 0; s < numSteps; s++) {
                somaX += random.nextBoolean() ? 1 : -1;
                somaY += random.nextBoolean() ? 1 : -1;
                somaZ += random.nextBoolean() ? 1 : -1;

                randVectX[p][s] = somaX;
                randVectY[p][s] = somaY;
                randVectZ[p][s] = somaZ;
            }
        }

        double maxVal = 0;
        for (int p = 0; p < numPart; p++) {
            for (int s = 0; s < numSteps; s++) {
                maxVal = Math.max(maxVal, Math.abs(randVectX[p][s]));
                maxVal = Math.max(maxVal, Math.abs(randVectY[p][s]));
                maxVal = Math.max(maxVal, Math.abs(randVectZ[p][s]));
            }
        }
        maxVXYZ = 1.2 * maxVal;

        for (int s = 0; s < numSteps; s++) {
            double somaX = 0, somaY = 0, somaZ = 0;
            double somaSqX = 0, somaSqY = 0, somaSqZ = 0;

            for (int p = 0; p < numPart; p++) {
                somaX += randVectX[p][s];
                somaY += randVectY[p][s];
                somaZ += randVectZ[p][s];

                somaSqX += randVectX[p][s] * randVectX[p][s];
                somaSqY += randVectY[p][s] * randVectY[p][s];
                somaSqZ += randVectZ[p][s] * randVectZ[p][s];
            }

            randMeanX[s] = somaX / numPart;
            randMeanY[s] = somaY / numPart;
            randMeanZ[s] = somaZ / numPart;

            randSquareMeanX[s] = Math.sqrt(somaSqX / numPart);
            randSquareMeanY[s] = Math.sqrt(somaSqY / numPart);
            randSquareMeanZ[s] = Math.sqrt(somaSqZ / numPart);
        }
    }

    private static class Renderable3D implements Comparable<Renderable3D> {
        double depth;

        @Override
        public int compareTo(Renderable3D o) {
            return Double.compare(this.depth, o.depth);
        }

        void draw(Graphics2D g2) {}
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        double scale = (Math.min(width, height) / (2.6 * maxVXYZ)) * zoom;

        List<Renderable3D> renderList = new ArrayList<>();
        List<Renderable3D> trails = new ArrayList<>();

        adicionarEixosECaixa(renderList, centerX, centerY, scale);

        // 2. Particulas no passo atual
        for (int p = 0; p < numPart; p++) {
            int[] trailX = new int[currentStep + 1];
            int[] trailY = new int[currentStep + 1];
            for (int s = 0; s <= currentStep; s++) {
                Point3D trailPoint = project(randVectX[p][s], randVectY[p][s], randVectZ[p][s],
                        centerX, centerY, scale);
                trailX[s] = (int) trailPoint.x;
                trailY[s] = (int) trailPoint.y;
            }
            Color particleColor = PARTICLE_COLORS[p % PARTICLE_COLORS.length];
            Renderable3D trail = new Renderable3D() {
                @Override
                void draw(Graphics2D g2d) {
                    g2d.setColor(particleColor);
                    g2d.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawPolyline(trailX, trailY, trailX.length);
                }
            };
            trails.add(trail);

            double x = randVectX[p][currentStep];
            double y = randVectY[p][currentStep];
            double z = randVectZ[p][currentStep];

            Point3D pt = project(x, y, z, centerX, centerY, scale);

            Renderable3D particle = new Renderable3D() {
                { this.depth = pt.depth; }
                @Override
                void draw(Graphics2D g2d) {
                    g2d.setColor(particleColor);
                    int r = 4;
                    g2d.fillOval((int) pt.x - r, (int) pt.y - r, 2 * r, 2 * r);
                }
            };
            renderList.add(particle);
        }

        // 3. Esfera / Elipsoide N-Sigma
        double cx = randMeanX[currentStep];
        double cy = randMeanY[currentStep];
        double cz = randMeanZ[currentStep];

        double rx = nSigma * randSquareMeanX[currentStep];
        double ry = nSigma * randSquareMeanY[currentStep];
        double rz = nSigma * randSquareMeanZ[currentStep];

        adicionarElipsoide(renderList, cx, cy, cz, rx, ry, rz, centerX, centerY, scale);

        Collections.sort(renderList);

        for (Renderable3D item : renderList) {
            item.draw(g2);
        }
        for (Renderable3D trail : trails) {
            trail.draw(g2);
        }
        for (int p = 0; p < numPart; p++) {
            Point3D particle = project(randVectX[p][currentStep], randVectY[p][currentStep],
                    randVectZ[p][currentStep], centerX, centerY, scale);
            g2.setColor(PARTICLE_COLORS[p % PARTICLE_COLORS.length]);
            g2.fillOval((int) particle.x - 4, (int) particle.y - 4, 8, 8);
        }

        // Titulo
        g2.setColor(new Color(30, 30, 30));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        String title = "Difusão em 3D  |  passo " + (currentStep + 1) + "/" + numSteps;
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(35, 48, 58));
        g2.drawString(title, (width - fm.stringWidth(title)) / 2, 30);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g2.setColor(new Color(82, 98, 107));
        String metrics = String.format(java.util.Locale.US, "N = %d    MSD = %.3f    RMSD = %.3f",
            numPart, randSquareMeanX[currentStep] * randSquareMeanX[currentStep]
                + randSquareMeanY[currentStep] * randSquareMeanY[currentStep]
                + randSquareMeanZ[currentStep] * randSquareMeanZ[currentStep],
            Math.sqrt(randSquareMeanX[currentStep] * randSquareMeanX[currentStep]
                + randSquareMeanY[currentStep] * randSquareMeanY[currentStep]
                + randSquareMeanZ[currentStep] * randSquareMeanZ[currentStep]));
        g2.drawString(metrics, 18, height - 18);
        g2.dispose();
    }

    private void adicionarEixosECaixa(List<Renderable3D> list, int cx, int cy, double scale) {
        double b = maxVXYZ;
        double[][] edges = {
            {-b,-b,-b,  b,-b,-b}, { b,-b,-b,  b, b,-b}, { b, b,-b, -b, b,-b}, {-b, b,-b, -b,-b,-b},
            {-b,-b, b,  b,-b, b}, { b,-b, b,  b, b, b}, { b, b, b, -b, b, b}, {-b, b, b, -b,-b, b},
            {-b,-b,-b, -b,-b, b}, { b,-b,-b,  b,-b, b}, { b, b,-b,  b, b, b}, {-b, b,-b, -b, b, b}
        };

        for (double[] e : edges) {
            Point3D p1 = project(e[0], e[1], e[2], cx, cy, scale);
            Point3D p2 = project(e[3], e[4], e[5], cx, cy, scale);
            double avgDepth = (p1.depth + p2.depth) / 2.0;

            Renderable3D edge = new Renderable3D() {
                { this.depth = avgDepth; }
                @Override
                void draw(Graphics2D g2d) {
                    g2d.setColor(new Color(215, 215, 215));
                    g2d.setStroke(new BasicStroke(1.0f));
                    g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
                }
            };
            list.add(edge);
        }

        double[][] axis = {
            {0, 0, 0,  b * 1.05, 0, 0},
            {0, 0, 0,  0, b * 1.05, 0},
            {0, 0, 0,  0, 0, b * 1.05}
        };
        String[] labels = {"dx", "dy", "dz"};
        Color[] colors = {new Color(180, 50, 50), new Color(40, 150, 40), new Color(40, 70, 180)};

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            Point3D p1 = project(axis[i][0], axis[i][1], axis[i][2], cx, cy, scale);
            Point3D p2 = project(axis[i][3], axis[i][4], axis[i][5], cx, cy, scale);

            Renderable3D axItem = new Renderable3D() {
                { this.depth = (p1.depth + p2.depth) / 2.0; }
                @Override
                void draw(Graphics2D g2d) {
                    g2d.setColor(colors[idx]);
                    g2d.setStroke(new BasicStroke(2.0f));
                    g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
                    g2d.setFont(new Font("Times New Roman", Font.BOLD, 14));
                    g2d.drawString(labels[idx], (int) p2.x + 5, (int) p2.y - 5);
                }
            };
            list.add(axItem);
        }
    }

    private void adicionarElipsoide(List<Renderable3D> list, double cx, double cy, double cz,
                                    double rx, double ry, double rz, int scrX, int scrY, double scale) {
        if (rx < 0.001 || ry < 0.001 || rz < 0.001) return;

        int stacks = 18;
        int slices = 24;

        double dPhi = Math.PI / stacks;
        double dTheta = 2 * Math.PI / slices;

        for (int i = 0; i < stacks; i++) {
            double phi1 = i * dPhi;
            double phi2 = (i + 1) * dPhi;

            for (int j = 0; j < slices; j++) {
                double theta1 = j * dTheta;
                double theta2 = (j + 1) * dTheta;

                Point3D p1 = projectSphere(cx, cy, cz, rx, ry, rz, phi1, theta1, scrX, scrY, scale);
                Point3D p2 = projectSphere(cx, cy, cz, rx, ry, rz, phi2, theta1, scrX, scrY, scale);
                Point3D p3 = projectSphere(cx, cy, cz, rx, ry, rz, phi2, theta2, scrX, scrY, scale);
                Point3D p4 = projectSphere(cx, cy, cz, rx, ry, rz, phi1, theta2, scrX, scrY, scale);

                double avgDepth = (p1.depth + p2.depth + p3.depth + p4.depth) / 4.0;

                int[] xPoints = {(int) p1.x, (int) p2.x, (int) p3.x, (int) p4.x};
                int[] yPoints = {(int) p1.y, (int) p2.y, (int) p3.y, (int) p4.y};

                Renderable3D face = new Renderable3D() {
                    { this.depth = avgDepth; }
                    @Override
                    void draw(Graphics2D g2d) {
                        g2d.setColor(new Color(60, 130, 240, 50));
                        g2d.fillPolygon(xPoints, yPoints, 4);

                        g2d.setColor(new Color(40, 90, 200, 70));
                        g2d.setStroke(new BasicStroke(0.7f));
                        g2d.drawPolygon(xPoints, yPoints, 4);
                    }
                };
                list.add(face);
            }
        }
    }

    private Point3D projectSphere(double cx, double cy, double cz, double rx, double ry, double rz,
                                  double phi, double theta, int scrX, int scrY, double scale) {
        double x = cx + rx * Math.sin(phi) * Math.cos(theta);
        double y = cy + ry * Math.sin(phi) * Math.sin(theta);
        double z = cz + rz * Math.cos(phi);
        return project(x, y, z, scrX, scrY, scale);
    }

    private Point3D project(double x, double y, double z, int scrX, int scrY, double scale) {
        double cosY = Math.cos(rotY);
        double sinY = Math.sin(rotY);
        double x1 = x * cosY - y * sinY;
        double y1 = x * sinY + y * cosY;
        double z1 = z;

        double cosX = Math.cos(rotX);
        double sinX = Math.sin(rotX);
        double y2 = y1 * cosX - z1 * sinX;
        double z2 = y1 * sinX + z1 * cosX;
        double x2 = x1;

        double px = scrX + x2 * scale;
        double py = scrY - z2 * scale;
        double depth = y2;

        return new Point3D(px, py, depth);
    }

    private static class Point3D {
        double x, y, depth;
        Point3D(double x, double y, double depth) {
            this.x = x;
            this.y = y;
            this.depth = depth;
        }
    }
}