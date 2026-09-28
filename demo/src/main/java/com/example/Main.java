package com.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main extends JFrame {

    public Main() {
        super("Plataforma de Simulação de Difusão");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(1100, 750));
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(242, 246, 247));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        tabs.setBackground(new Color(242, 246, 247));
        tabs.setForeground(new Color(42, 59, 67));
        tabs.addTab("Difusão 1D", new Simulador1());
        tabs.addTab("Difusão 2D", new Simulador2());
        tabs.addTab("Difusão 3D", new Simulador3());
        tabs.addTab("Laboratório", new PainelMelhorias());

        JPanel content = new JPanel(new BorderLayout());
        content.add(tabs, BorderLayout.CENTER);
        setContentPane(content);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("TabbedPane.selected", new Color(222, 239, 236));
            UIManager.put("TabbedPane.contentAreaColor", new Color(248, 250, 251));
            Main frame = new Main();
            frame.setVisible(true);
        });
    }
}


