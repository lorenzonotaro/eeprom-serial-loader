package com.computer8bit.eeprom;

import com.computer8bit.eeprom.data.EEPROMDataByte;
import com.computer8bit.eeprom.data.EEPROMDataSet;
import com.computer8bit.eeprom.serial.PortDescriptor;
import com.computer8bit.eeprom.serial.SerialException;
import com.computer8bit.eeprom.serial.SerialInterface;
import com.computer8bit.eeprom.table.DataTable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

public class Window {

    private static final int BAUD_RATE = 9600;
    private static final int ROW_WIDTH = 16;
    JPanel contentPane;
    public static final int DATA_LENGTH = 4096, MAX_DATA_LENGTH = 4096;
    private EEPROMDataSet dataSet;

    private JComboBox<PortDescriptor> serialPortSelection;
    private JButton refreshSerialPorts;
    private JTable dataTable;
    private JSpinner dataSizeSpinner;
    private JLabel serialPortStatus;
    private JButton writeEEPROMButton;
    private ByteEditor byteEditor;
    private JProgressBar progressBar;
    private JLabel operationStatusLabel;
    private JButton readEEPROMButton;
    private JMenuBar menuBar;

    private SerialInterface serialInterface;
    private boolean serialPortValid = false;

    Window() {
        refreshSerialPorts.addActionListener(this::refreshSerialPorts);
        dataSizeSpinner.addChangeListener(this::dataSizeChanged);
        serialPortSelection.addItemListener(this::serialPortChanged);
        serialInterface = new SerialInterface();
        menuBar = new JMenuBar();
        JMenuItem helpMenu = new JMenuItem("Help");
        menuBar.add(makeFileMenu());
        menuBar.add(helpMenu);
        writeEEPROMButton.addActionListener(this::writeData);
        byteEditor.getContentPane().setEnabled(false);
        readEEPROMButton.addActionListener(this::readDataFromEEPROM);
    }

    private void readDataFromEEPROM(ActionEvent actionEvent) {
        if (!serialPortValid) {
            JOptionPane.showMessageDialog(null, "Select a valid device first.", "Invalid device", JOptionPane.ERROR_MESSAGE);
            return;
        }
        doAsThread(() -> {
            try {
                byte[] data = serialInterface.readData(progressBar::setValue, operationStatusLabel::setText);
                setDataSet(data);
                operationStatusLabel.setText("Done.");
            } catch (SerialException ex) {
                JOptionPane.showMessageDialog(null, "Unable to write to EEPROM: " + ex.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private JMenu makeFileMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenuItem open = new JMenuItem("Open...");
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        open.addActionListener(this::openFile);
        JMenuItem save = new JMenuItem("Save...");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        save.addActionListener(this::saveFile);
        fileMenu.add(open);
        fileMenu.add(save);

        fileMenu.addSeparator();

        JMenu exportSubmenu = new JMenu("Export");
        exportSubmenu.setMnemonic(KeyEvent.VK_E);
        JMenuItem exportAsHexDump = new JMenuItem("As hex dump...");
        exportAsHexDump.addActionListener(this::exportHexDump);
        exportSubmenu.add(exportAsHexDump);
        JMenuItem exportAsRawData = new JMenuItem("As raw data...");
        exportAsRawData.addActionListener(this::exportRawData);
        exportSubmenu.add(exportAsRawData);
        fileMenu.add(exportSubmenu);

        fileMenu.addSeparator();

        JMenu importSubmenu = new JMenu("Import");
        importSubmenu.setMnemonic(KeyEvent.VK_I);
        JMenuItem importAsHexDump = new JMenuItem("From hex dump...");
        importAsHexDump.addActionListener(this::importHexDump);
        importSubmenu.add(importAsHexDump);
        JMenuItem importAsRawData = new JMenuItem("From raw data...");
        importAsRawData.addActionListener(this::importRawData);
        importSubmenu.add(importAsRawData);
        fileMenu.add(importSubmenu);

        return fileMenu;
    }

    private void importRawData(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int response = fc.showDialog(null, "Import");
        if (response == JFileChooser.APPROVE_OPTION) {
            try {
                byte[] bytes = Files.readAllBytes(fc.getSelectedFile().toPath());
                EEPROMDataByte[] newData = new EEPROMDataByte[bytes.length];
                for (int i = 0; i < bytes.length; i++) {
                    byte b = bytes[i];
                    newData[i] = new EEPROMDataByte(b, null, i);
                }
                setDataSet(newData);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Unable to export the file. (" + e.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importHexDump(ActionEvent actionEvent) {
        throw new Error("not implemented");
    }

    private void exportRawData(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int response = fc.showDialog(null, "Export");
        if (response == JFileChooser.APPROVE_OPTION) {
            String filename = fc.getSelectedFile().getAbsolutePath();
            try (FileWriter fw = new FileWriter(filename)) {
                for (EEPROMDataByte dataByte : dataSet.getData()) {
                    fw.write(dataByte.getValue());
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Unable to export the file. (" + e.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportHexDump(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int response = fc.showDialog(null, "Export");
        if (response == JFileChooser.APPROVE_OPTION) {
            String filename = fc.getSelectedFile().getAbsolutePath();
            try (FileWriter fw = new FileWriter(filename)) {
                EEPROMDataByte[] data = dataSet.getData();
                for (int i = 0; i < data.length; i++) {
                    EEPROMDataByte dataByte = data[i];
                    fw.write(String.format("0x%02x ", dataByte.getValue()));
                    if((i + 1) % ROW_WIDTH == 0)
                        fw.write('\n');
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Unable to export the file. (" + e.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void writeData(ActionEvent e) {
        if (!serialPortValid) {
            JOptionPane.showMessageDialog(null, "Select a valid device first.", "Invalid device", JOptionPane.ERROR_MESSAGE);
            return;
        }
        doAsThread(() -> {
            try {
                serialInterface.writeData(dataSet.getDataAsBytes(), progressBar::setValue, operationStatusLabel::setText);
                operationStatusLabel.setText("Done.");
            } catch (SerialException ex) {
                JOptionPane.showMessageDialog(null, "Unable to write to EEPROM: " + ex.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

    }

    private void saveFile(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileNameExtensionFilter("EEPROM data files", "eeprom"));
        fc.setAcceptAllFileFilterUsed(true);
        int response = fc.showDialog(null, "Save");
        if (response == JFileChooser.APPROVE_OPTION) {
            String filename = fc.getSelectedFile().getAbsolutePath();
            if(!filename.endsWith(".eeprom")) filename += ".eeprom";
            try (FileWriter fw = new FileWriter(filename)) {
                Gson gs = new GsonBuilder().create();
                String json = gs.toJson(dataSet.getData(), EEPROMDataByte[].class);
                fw.write(json);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Unable to save the file. (" + e.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openFile(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileNameExtensionFilter("EEPROM data files", "eeprom"));
        fc.setAcceptAllFileFilterUsed(true);
        int response = fc.showDialog(contentPane, "Open");
        if (response == JFileChooser.APPROVE_OPTION) {
            try {
                String str = new String(Files.readAllBytes(fc.getSelectedFile().toPath()));
                Gson gs = new GsonBuilder().create();
                EEPROMDataByte[] newData = gs.fromJson(str, EEPROMDataByte[].class);
                if (newData.length > MAX_DATA_LENGTH) {
                    JOptionPane.showMessageDialog(contentPane, "The input file (" + newData.length + " bytes) exceeds the maximum length: only the first " + MAX_DATA_LENGTH + " bytes will be imported.", "Input cropped", JOptionPane.INFORMATION_MESSAGE);
                    newData = Arrays.copyOf(newData, MAX_DATA_LENGTH);
                }
                setDataSet(newData);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(contentPane, "Unable to save the file. (" + e.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void setDataSet(EEPROMDataByte[] newData) {
        dataSet.setData(newData);
        dataSizeSpinner.setValue(newData.length);
    }


    private void setDataSet(byte[] data) {
        dataSet.setData(data);
        dataSizeSpinner.setValue(data.length);
    }


    private void serialPortChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.DESELECTED)
            return;
        PortDescriptor item = (PortDescriptor) serialPortSelection.getSelectedItem();
        if (item == null || item.getPort() == null)
            return;
        doAsThread(() -> {
            try {
                serialPortSelection.setEnabled(false);
                serialPortStatus.setForeground(Color.BLACK);
                serialPortStatus.setText("Retrieving info...");
                serialInterface.setActivePort(item.getPort());
                String version = serialInterface.getLoaderVersion();
                serialPortStatus.setForeground(Color.GREEN.darker());
                serialPortStatus.setText("OK! Chip " + version);
                this.serialPortValid = true;
            } catch (SerialException exception) {
                serialPortStatus.setForeground(Color.RED);
                serialPortStatus.setText("Invalid device (" + exception.getMessage() + ")!");
                try {
                    serialInterface.setActivePort(null);
                } catch (SerialException ex) {
                    ex.printStackTrace();
                }
                this.serialPortValid = false;
            } finally {
                serialPortSelection.setEnabled(true);
            }
        });

    }

    private void doAsThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    private void dataSizeChanged(ChangeEvent e) {
        dataSet.resize(((Integer) dataSizeSpinner.getModel().getValue()));
    }

    private void refreshSerialPorts(ActionEvent e) {
        serialPortStatus.setText("Select a serial port");
        serialPortStatus.setForeground(Color.BLACK);
        serialPortValid = false;
        serialPortSelection.removeAllItems();
        Arrays.asList(PortDescriptor.getDescriptors()).forEach(serialPortSelection::addItem);
        serialPortSelection.setSelectedIndex(0);
    }

    private void createUIComponents() {
        dataSet = new EEPROMDataSet(DATA_LENGTH);
        byteEditor = new ByteEditor(dataSet);
        dataSizeSpinner = new JSpinner(new SpinnerNumberModel(DATA_LENGTH, 1, MAX_DATA_LENGTH, 10));
        serialPortSelection = new JComboBox<>(PortDescriptor.getDescriptors());
        setupTable();
    }

    private void setupTable() {
        dataTable = new DataTable(this, dataSet, ROW_WIDTH);
    }
    JMenuBar getMenuBar() {
        return menuBar;
    }

    public void updateByteEditor(EventObject e) {

        int row = dataTable.getSelectedRow();
        int col = dataTable.getSelectedColumn();
        int address = row * ROW_WIDTH + col - 1;
        if (row >= 0 && col >= 1 && address >= 0 && address < dataSet.getData().length) {
            byteEditor.setDataByte(dataSet.getByteAt(address));
        } else {
            byteEditor.setDataByte(null);
        }
    }

    public JPanel getContentPane() {
        return contentPane;
    }
}