package com.solar.ops.device.util;

import com.solar.ops.device.dto.InverterDataDTO;
import io.netty.buffer.ByteBuf;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ModbusDataParser {

    private static final double VOLTAGE_SCALE = 0.1;
    private static final double CURRENT_SCALE = 0.01;
    private static final double POWER_SCALE = 1.0;
    private static final double ENERGY_SCALE = 0.01;
    private static final double TEMPERATURE_SCALE = 0.1;

    public static InverterDataDTO parseModbusData(ByteBuf buffer, String deviceId, String stationId) {
        if (buffer.readableBytes() < 26) {
            return null;
        }

        InverterDataDTO data = new InverterDataDTO();
        data.setDeviceId(deviceId);
        data.setStationId(stationId);
        data.setTimestamp(System.currentTimeMillis());

        int slaveId = buffer.readUnsignedByte();
        int functionCode = buffer.readUnsignedByte();

        if (functionCode != 0x03 && functionCode != 0x04) {
            return null;
        }

        int byteCount = buffer.readUnsignedByte();
        if (buffer.readableBytes() < byteCount) {
            return null;
        }

        data.setVoltage(readScaledValue(buffer, VOLTAGE_SCALE));
        data.setCurrent(readScaledValue(buffer, CURRENT_SCALE));
        data.setPower(readScaledValue(buffer, POWER_SCALE));
        data.setEnergy(readScaledEnergy(buffer));
        data.setTemperature(readScaledValue(buffer, TEMPERATURE_SCALE));
        data.setFaultCode(buffer.readUnsignedShort());

        return data;
    }

    public static InverterDataDTO parseHexData(String hexData, String deviceId, String stationId) {
        if (hexData == null || hexData.length() < 52) {
            return null;
        }

        byte[] bytes = hexToBytes(hexData);
        if (bytes == null || bytes.length < 26) {
            return null;
        }

        InverterDataDTO data = new InverterDataDTO();
        data.setDeviceId(deviceId);
        data.setStationId(stationId);
        data.setTimestamp(System.currentTimeMillis());

        int offset = 0;
        int slaveId = bytes[offset++] & 0xFF;
        int functionCode = bytes[offset++] & 0xFF;

        if (functionCode != 0x03 && functionCode != 0x04) {
            return null;
        }

        int byteCount = bytes[offset++] & 0xFF;

        data.setVoltage(readScaledValue(bytes, offset, VOLTAGE_SCALE));
        offset += 2;
        data.setCurrent(readScaledValue(bytes, offset, CURRENT_SCALE));
        offset += 2;
        data.setPower(readScaledValue(bytes, offset, POWER_SCALE));
        offset += 2;
        data.setEnergy(readScaledEnergy(bytes, offset));
        offset += 4;
        data.setTemperature(readScaledValue(bytes, offset, TEMPERATURE_SCALE));
        offset += 2;
        data.setFaultCode(((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF));

        return data;
    }

    private static double readScaledValue(ByteBuf buffer, double scale) {
        int raw = buffer.readUnsignedShort();
        return round(raw * scale, 2);
    }

    private static double readScaledValue(byte[] bytes, int offset, double scale) {
        int raw = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
        return round(raw * scale, 2);
    }

    private static double readScaledEnergy(ByteBuf buffer) {
        long raw = buffer.readUnsignedInt();
        return round(raw * ENERGY_SCALE, 2);
    }

    private static double readScaledEnergy(byte[] bytes, int offset) {
        long raw = ((long) (bytes[offset] & 0xFF) << 24)
                | ((long) (bytes[offset + 1] & 0xFF) << 16)
                | ((long) (bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
        return round(raw * ENERGY_SCALE, 2);
    }

    private static double round(double value, int places) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return null;
        }
        hex = hex.replace(" ", "").replace("\t", "");
        if (hex.length() % 2 != 0) {
            return null;
        }
        int len = hex.length() / 2;
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            int high = Character.digit(hex.charAt(i * 2), 16);
            int low = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) {
                return null;
            }
            data[i] = (byte) ((high << 4) | low);
        }
        return data;
    }

    public static boolean validateData(InverterDataDTO data) {
        if (data == null) {
            return false;
        }
        if (data.getDeviceId() == null || data.getDeviceId().isEmpty()) {
            return false;
        }
        if (data.getVoltage() != null && (data.getVoltage() < 0 || data.getVoltage() > 1000)) {
            return false;
        }
        if (data.getCurrent() != null && (data.getCurrent() < 0 || data.getCurrent() > 500)) {
            return false;
        }
        if (data.getPower() != null && (data.getPower() < 0 || data.getPower() > 10000)) {
            return false;
        }
        if (data.getTemperature() != null && (data.getTemperature() < -50 || data.getTemperature() > 200)) {
            return false;
        }
        return true;
    }
}
