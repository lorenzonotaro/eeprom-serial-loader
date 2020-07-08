package com.computer8bit.eeprom;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        try {
           UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e){ e.printStackTrace();}
        JFrame frame = new JFrame("EEPROM Serial Programmer");
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        Window window = new Window();
        frame.setContentPane(window.contentPane);
        frame.setJMenuBar(window.getMenuBar());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
