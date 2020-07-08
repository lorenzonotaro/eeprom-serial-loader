package com.computer8bit.eeprom.serial;

import com.computer8bit.eeprom.Window;
import com.fazecast.jSerialComm.SerialPort;

import java.io.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class SerialInterface {
    private static final String PROTOCOL_SIGNATURE = "EEPROMLD";
    private static final int PAYLOAD_SIZE = 64, ROM_SIZE = 4096;
    private static final int DEFAULT_TIMEOUT = 200;
    private static final int BAUD_RATE = 115200;
    private SerialPort activePort;

    public SerialPort getActivePort() {
        return activePort;
    }

    private void requireActivePort() {
        Objects.requireNonNull(activePort, "No active serial port");
    }

    public synchronized void setActivePort(SerialPort activePort) throws SerialException {
        if (this.activePort != null) {
            this.activePort.closePort();
        }
        this.activePort = activePort;
        if (activePort == null)
            return;

        activePort.setComPortParameters(BAUD_RATE, 8, 1, SerialPort.NO_PARITY);

        if (!activePort.openPort())
            throw new SerialException("unable to open port");

        tryDelay(1500);
    }

    public synchronized String getLoaderVersion() throws SerialException {
        requireActivePort();
        byte[] buffer = new byte[32];

        activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, DEFAULT_TIMEOUT, 0);

        buffer[0] = 'v';

        if (write(buffer, 1) <= 0) {
            throw new SerialException("error while writing to device");
        }

        if (read(buffer) <= 0)
            throw new SerialException("device not responding");

        String val = new String(buffer).trim();

        if (!val.startsWith(PROTOCOL_SIGNATURE))
            throw new SerialException("invalid signature");

        return val.substring(PROTOCOL_SIGNATURE.length());
    }

    public synchronized void writeData(byte[] data, Consumer<Integer> progressFunction, Consumer<String> statusFunction) throws SerialException {
        byte[] tmpBuffer = new byte[1];
        requireActivePort();

        flushInputBuffer();

        activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

        try {
            if (data.length != Window.MAX_DATA_LENGTH)
                data = Arrays.copyOf(data, ROM_SIZE);

            for (int i = 0; i < 16; i++) {
                System.out.printf("%x ", data[i]);
            }
            System.out.println();

            tmpBuffer[0] = 'w';
            if (write(tmpBuffer) <= 0) {
                throw new SerialException("unable to send write request to device");
            }

            int PAYLOAD_COUNT = ROM_SIZE / PAYLOAD_SIZE;
            for (int i = 0; i < PAYLOAD_COUNT; i++) {
                statusFunction.accept("Sending block " + (i + 1) + "/" + PAYLOAD_COUNT);
                if (read(tmpBuffer, 1) != 1)
                    throw new SerialException("unable to read section confirmation character");
                if (tmpBuffer[0] != 'n')
                    throw new SerialException("device sent wrong section confirmation character (" + (char) tmpBuffer[0] + ")");
                if (write(data, PAYLOAD_SIZE, i * PAYLOAD_SIZE) < PAYLOAD_SIZE)
                    throw new SerialException("unable to write to device (block " + (i + 1) + ")");
                progressFunction.accept(Math.round((float) (i + 1) / PAYLOAD_COUNT * 100));
                while (activePort.bytesAwaitingWrite() > 0) tryDelay(1);
            }

            if (read(tmpBuffer, 1) <= 0)
                throw new SerialException("device not responding");

            if (tmpBuffer[0] != 'k')
                throw new SerialException("wrong confirmation character");

            statusFunction.accept("");
            progressFunction.accept(0);
            byte[] readData = readData(progressFunction, statusFunction);

            statusFunction.accept("Checking data...");
            if (!Arrays.equals(readData, data))
                throw new SerialException("data check failed");
        } finally {
            activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, DEFAULT_TIMEOUT, 0);
        }
    }

    public byte[] readData(Consumer<Integer> progressFunction, Consumer<String> statusFunction) throws SerialException {

        requireActivePort();

        flushInputBuffer();

        activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

        byte[] tmpBuffer = new byte[1];
        int PAYLOAD_COUNT = ROM_SIZE / PAYLOAD_SIZE;

        progressFunction.accept(0);
        byte[] readData = new byte[ROM_SIZE];
        try {
            tmpBuffer[0] = 'r';
            if (write(tmpBuffer, 1) <= 0)
                throw new SerialException("unable to send read request to device");

            for (int i = 0; i < PAYLOAD_COUNT; i++) {
                statusFunction.accept("Reading block " + (i + 1) + "/" + PAYLOAD_COUNT);
                tmpBuffer[0] = 'n';
                if (write(tmpBuffer, 1) <= 0)
                    throw new SerialException("unable to write section confirmation character during read");
                if (read(readData, PAYLOAD_SIZE, i * PAYLOAD_SIZE) != PAYLOAD_SIZE)
                    throw new SerialException("unable to read from device (block " + (i + 1) + ")");
                progressFunction.accept(Math.round((float) (i + 1) / PAYLOAD_COUNT * 100));
            }

        } finally {
            activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, DEFAULT_TIMEOUT, 0);
        }
        return readData;
    }

    private void flushInputBuffer() {
        int available = activePort.bytesAvailable();
        if (available > 0) {
            byte[] tmp = new byte[available];
            read(tmp, available);
        }
    }

    private int write(byte[] buffer) {
        return activePort.writeBytes(buffer, buffer.length);
    }

    private int write(byte[] buffer, int length, int offset) {
        return activePort.writeBytes(buffer, length, offset);
    }

    private int write(byte[] buffer, int length) {
        return write(buffer, length, 0);
    }

    private int read(byte[] buffer) {
        return read(buffer, buffer.length, 0);
    }

    private int read(byte[] buffer, int length) {
        return read(buffer, length, 0);
    }


    private int read(byte[] buffer, int length, int offset) {
        for (int i = 0; i < length; i++) {
            if (activePort.readBytes(buffer, 1, offset + i) <= 0)
                return i;
        }
        return length;
    }

    private void tryDelay(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            System.out.println("Warning! Caught InterruptedException while delaying!");
        }
    }

    private byte[] toBytes(int i, byte[] buffer) {
        buffer[0] = (byte) ((i >> 24) & 0xFF);
        buffer[1] = (byte) ((i >> 16) & 0xFF);
        buffer[2] = (byte) ((i >> 8) & 0xFF);
        buffer[3] = (byte) (i & 0xFF);
        return buffer;
    }
}
