package com.computer8bit.eeprom;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class Main {

    public static final String WINDOW_TITLE = "EEPROM Serial Loader";
    private static boolean launchGUI = true;
    private static String file = null;

    public static void main(String[] args) {
        parseArgs(args);

        if(launchGUI)
            launchGUI();
    }

    private static void launchGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e){ e.printStackTrace();}
        JFrame frame = new JFrame(WINDOW_TITLE);
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        Window window = new Window(frame);
        frame.setContentPane(window.contentPane);
        frame.setJMenuBar(window.getMenuBar());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        if(file != null)
            window.openFile(Path.of(file));
    }

    private static void parseArgs(String[] args) {
        if(args.length > 0){
            file = args[0];
        }
    }
}
