package com.example;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/** Passeio aleatorio unidimensional para ilustrar a difusao. */
public class Simulador1 extends JPanel {
	private static final Color[] PARTICLE_COLORS = {
		new Color(31, 119, 180), new Color(255, 127, 14), new Color(44, 160, 44),
		new Color(214, 39, 40), new Color(148, 103, 189), new Color(140, 86, 75),
		new Color(227, 119, 194), new Color(127, 127, 127), new Color(188, 189, 34),
		new Color(23, 190, 207)
	};

	private int[][] trajectories = new int[0][0];
	private int particleCount;
	private int stepCount;
	private int maxDisplacement;

	public Simulador1(int particleCount, int stepCount) {
		setPreferredSize(new Dimension(900, 650));
		setBackground(Color.WHITE);
		simular(particleCount, stepCount);
	}

	public void simular(int particleCount, int stepCount) {
		this.particleCount = particleCount;
		this.stepCount = stepCount;
		trajectories = new int[particleCount][stepCount + 1];
		maxDisplacement = 0;
		Random random = new Random();

		for (int particle = 0; particle < particleCount; particle++) {
			for (int step = 1; step <= stepCount; step++) {
				int direction = random.nextBoolean() ? 1 : -1;
				trajectories[particle][step] = trajectories[particle][step - 1] + direction;
				maxDisplacement = Math.max(maxDisplacement,
						Math.abs(trajectories[particle][step]));
			}
		}
		repaint();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			Simulador1 plot = new Simulador1(100, 100);
			JFrame frame = new JFrame("Difusao em 1D");
			JSpinner particles = new JSpinner(new SpinnerNumberModel(100, 1, 1000, 10));
			JSpinner steps = new JSpinner(new SpinnerNumberModel(100, 1, 1000, 10));
			JButton simulate = new JButton("Simular");
			JPanel controls = new JPanel();
			controls.add(new JLabel("Particulas (N):"));
			controls.add(particles);
			controls.add(new JLabel("Passos (S):"));
			controls.add(steps);
			controls.add(simulate);
			simulate.addActionListener(event -> plot.simular(
					(int) particles.getValue(), (int) steps.getValue()));

			frame.add(plot, BorderLayout.CENTER);
			frame.add(controls, BorderLayout.SOUTH);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int left = 85;
		int right = getWidth() - 35;
		int top = 65;
		int bottom = getHeight() - 75;
		int plotWidth = right - left;
		int plotHeight = bottom - top;
		int range = Math.max(1, (int) Math.ceil(maxDisplacement * 1.15));
		double xScale = plotWidth / (2.0 * range);

		desenharEixos(g, left, right, top, bottom, range);
		g.setStroke(new BasicStroke(1.0f));
		for (int particle = 0; particle < particleCount; particle++) {
			g.setColor(PARTICLE_COLORS[particle % PARTICLE_COLORS.length]);
			for (int step = 0; step <= stepCount; step++) {
				int x = left + (int) Math.round((trajectories[particle][step] + range) * xScale);
				int y = bottom - (int) Math.round(step * plotHeight / (double) stepCount);
				if (step > 0) {
					int previousX = left + (int) Math.round(
							(trajectories[particle][step - 1] + range) * xScale);
					int previousY = bottom - (int) Math.round(
							(step - 1) * plotHeight / (double) stepCount);
					g.drawLine(previousX, previousY, x, y);
				}
				g.fillOval(x - 2, y - 2, 4, 4);
			}
		}

		String title = "Difusao em 1D: " + particleCount + " particulas, " + stepCount + " passos";
		g.setColor(new Color(35, 35, 35));
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
		FontMetrics metrics = g.getFontMetrics();
		g.drawString(title, (getWidth() - metrics.stringWidth(title)) / 2, 30);
		g.dispose();
	}

	private void desenharEixos(Graphics2D g, int left, int right, int top, int bottom, int range) {
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		int tickStep = Math.max(1, (int) Math.ceil(range / 5.0));
		double xScale = (right - left) / (2.0 * range);

		for (int displacement = -range; displacement <= range; displacement += tickStep) {
			int x = left + (int) Math.round((displacement + range) * xScale);
			g.setColor(new Color(230, 233, 237));
			g.drawLine(x, top, x, bottom);
			g.setColor(Color.DARK_GRAY);
			String label = Integer.toString(displacement);
			g.drawString(label, x - g.getFontMetrics().stringWidth(label) / 2, bottom + 18);
		}

		int stepTick = Math.max(1, (int) Math.ceil(stepCount / 5.0));
		for (int step = 0; step <= stepCount; step += stepTick) {
			int y = bottom - (int) Math.round(step * (bottom - top) / (double) stepCount);
			g.setColor(new Color(230, 233, 237));
			g.drawLine(left, y, right, y);
			g.setColor(Color.DARK_GRAY);
			String label = Integer.toString(step);
			g.drawString(label, left - 12 - g.getFontMetrics().stringWidth(label), y + 4);
		}

		g.setColor(new Color(70, 70, 70));
		g.setStroke(new BasicStroke(1.4f));
		g.drawLine(left, top, left, bottom);
		g.drawLine(left, bottom, right, bottom);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
		String xLabel = "Deslocamento (x/delta)";
		g.drawString(xLabel, left + (right - left - g.getFontMetrics().stringWidth(xLabel)) / 2,
				getHeight() - 25);
		Graphics2D rotated = (Graphics2D) g.create();
		rotated.rotate(-Math.PI / 2);
		String yLabel = "Numero de passos (t/tau)";
		rotated.drawString(yLabel, -(top + (bottom - top + rotated.getFontMetrics().stringWidth(yLabel)) / 2),
				25);
		rotated.dispose();
	}

}
