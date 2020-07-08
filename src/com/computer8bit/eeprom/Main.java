package com.computer8bit.eeprom;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
           UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e){ e.printStackTrace();}
        JFrame frame = new JFrame("EEPROM Serial Programmer");
        Window window = new Window();
        frame.setContentPane(window.contentPane);
        frame.setJMenuBar(window.getMenuBar());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
