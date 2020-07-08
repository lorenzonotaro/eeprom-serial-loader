package com.computer8bit.eeprom.table;

import com.computer8bit.eeprom.event.EEPROMDataChangeEvent;
import com.computer8bit.eeprom.data.EEPROMDataSet;
import com.computer8bit.eeprom.util.Util;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;

class DataTableModel extends AbstractTableModel {

    private EEPROMDataSet data;
    private int rowWidth;

    DataTableModel(EEPROMDataSet data, int rowWidth){
        super();
        this.data = data;
        data.addChangeListener(this::dataChanged);
        this.rowWidth = rowWidth;
    }

    private void dataChanged(EEPROMDataChangeEvent changeEvent) {
        if(changeEvent.getType().equals(EEPROMDataChangeEvent.Type.RESIZE)){
            super.fireTableDataChanged();
        }else fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        if(data == null) return 0;
        int count =  data.getLength() / rowWidth;
        if ((count == 0 && data.getLength() != 0) || data.getLength() % count != 0) ++count;
        return count;
    }

    @Override
    public int getColumnCount() {
        return 1 + rowWidth;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 0 && rowIndex * rowWidth + columnIndex - 1 < data.getLength();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        boolean isFirstCol = columnIndex == 0;
        int val;
        if(isFirstCol){
            val = rowIndex * rowWidth;
        }else if(rowIndex * rowWidth + columnIndex - 1 < data.getLength()){
            val = data.getByteValueAt(rowIndex * rowWidth + columnIndex - 1) & 0xFF;
        }else{
            return "";
        }
        return String.format(isFirstCol ? "0x%04x" : "0x%02x", val);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if(rowIndex * rowWidth + columnIndex - 1 >= data.getLength()){
            JOptionPane.showMessageDialog(null,  "Unable to edit this value. Try increasing the data size.", "Error.", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String strVal = aValue.toString();
        byte value;
        try{
            value = Util.parseByte(strVal);
            data.getByteAt(rowIndex * rowWidth + columnIndex - 1).setValue(value);
        }catch(NumberFormatException e){
            JOptionPane.showMessageDialog(null,  "Invalid input value '" + strVal + "'", "Error.", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void fireTableDataChanged() {
        fireTableChanged(new TableModelEvent(this, //tableModel
                0, //firstRow
                getRowCount() - 1, //lastRow
                TableModelEvent.ALL_COLUMNS, //column
                TableModelEvent.UPDATE)); //changeType
    }
}


