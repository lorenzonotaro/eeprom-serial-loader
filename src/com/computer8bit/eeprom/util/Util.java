package com.computer8bit.eeprom.util;

public class Util {
    public static byte parseByte(String strVal){
        int value;
        if(strVal.startsWith("0x")){
            value = Integer.parseInt(strVal.substring(2).toUpperCase(), 16);
        }else{
            value = Integer.parseInt(strVal, 10);
        }
        if(value < 0 || value > 255)
            throw new NumberFormatException("not in range");
        return (byte) (value & 0xFF);
    }
}
