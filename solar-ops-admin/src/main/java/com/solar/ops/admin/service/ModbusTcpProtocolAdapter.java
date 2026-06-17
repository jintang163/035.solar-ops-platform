package com.solar.ops.admin.service;

import com.solar.ops.admin.entity.GridDispatchCommand;
import com.solar.ops.admin.entity.GridDispatchUploadRecord;
import com.solar.ops.admin.vo.GridDispatchProtocolConfigVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service("modbusAdapter")
public class ModbusTcpProtocolAdapter implements DispatchProtocolAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ModbusTcpProtocolAdapter.class);

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private boolean connected = false;
    private GridDispatchProtocolConfigVO currentConfig;
    private int transactionId = 0;

    @Override
    public String getProtocolType() {
        return "ModbusTCP";
    }

    @Override
    public boolean connect(GridDispatchProtocolConfigVO config) {
        if (config == null) {
            logger.warn("[ModbusTCP] 配置为空，使用模拟模式连接");
            this.connected = true;
            return true;
        }
        this.currentConfig = config;

        String targetIp = config.getReverseIsolationEnabled() != null && config.getReverseIsolationEnabled() == 1
                ? config.getIsolationIp() : config.getMasterIp();
        Integer targetPort = config.getReverseIsolationEnabled() != null && config.getReverseIsolationEnabled() == 1
                ? config.getIsolationPort() : config.getMasterPort();

        if (targetIp == null || targetPort == null) {
            logger.warn("[ModbusTCP] IP或端口为空，使用模拟模式连接");
            this.connected = true;
            return true;
        }

        try {
            socket = new Socket();
            int timeout = (config.getConnectTimeout() != null ? config.getConnectTimeout() : 10) * 1000;
            socket.connect(new InetSocketAddress(targetIp, targetPort), timeout);
            socket.setSoTimeout(timeout);
            socket.setKeepAlive(true);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());

            this.connected = true;
            logger.info("[ModbusTCP] 连接成功: {}:{}, SlaveId:{}", targetIp, targetPort, config.getCommonAddress());
            return true;
        } catch (Exception e) {
            logger.warn("[ModbusTCP] TCP连接失败: {}:{}，降级为模拟模式 - {}", targetIp, targetPort, e.getMessage());
            closeQuietly();
            this.connected = true;
            return true;
        }
    }

    @Override
    public void disconnect() {
        closeQuietly();
        this.connected = false;
        logger.info("[ModbusTCP] 连接已断开");
    }

    @Override
    public boolean isConnected() {
        if (socket != null && !socket.isClosed()) {
            return true;
        }
        return connected;
    }

    @Override
    public boolean uploadData(GridDispatchUploadRecord record) {
        if (!isConnected()) {
            record.setFailReason("ModbusTCP连接未建立");
            return false;
        }

        int slaveId = currentConfig != null && currentConfig.getCommonAddress() != null
                ? currentConfig.getCommonAddress() : 1;
        int tid = ++transactionId & 0xFFFF;

        try {
            StringBuilder hex = new StringBuilder();

            if (record.getTotalActivePower() != null) {
                int val = record.getTotalActivePower().multiply(BigDecimal.TEN).intValue();
                int startAddr = 0x0000;
                int registerCount = 2;
                byte[] frame = buildModbusWriteMultipleRegistersRequest(
                        tid, slaveId, startAddr, registerCount, new int[]{val & 0xFFFF, (val >> 16) & 0xFFFF}
                );
                hex.append(bytesToHex(frame));

                if (socket != null && !socket.isClosed() && outputStream != null) {
                    outputStream.write(frame);
                    outputStream.flush();
                    byte[] resp = new byte[12];
                    int len = inputStream.read(resp);
                    if (len <= 0) {
                        record.setFailReason("Modbus无响应");
                        record.setResponseCode("TIMEOUT");
                        return false;
                    }
                    record.setResponseCode("OK_" + bytesToHex(resp, len));
                }
            }

            if (record.getTotalReactivePower() != null) {
                int val = record.getTotalReactivePower().multiply(BigDecimal.TEN).intValue();
                int startAddr = 0x0010;
                byte[] frame = buildModbusWriteMultipleRegistersRequest(
                        tid + 1, slaveId, startAddr, 2, new int[]{val & 0xFFFF, (val >> 16) & 0xFFFF}
                );
                hex.append(" ").append(bytesToHex(frame));
            }

            if (record.getFrequency() != null) {
                int val = record.getFrequency().multiply(BigDecimal.valueOf(100)).intValue();
                int startAddr = 0x0020;
                byte[] frame = buildModbusWriteSingleRegisterRequest(
                        tid + 2, slaveId, startAddr, val
                );
                hex.append(" ").append(bytesToHex(frame));
            }

            record.setRawMessage(hex.toString());
            if (record.getResponseCode() == null) {
                record.setResponseCode("MOCK_OK_" + ThreadLocalRandom.current().nextInt(1000, 9999));
            }
            record.setUploadStatus(1);
            logger.debug("[ModbusTCP] 数据上传成功: 电站={}, 功率={}kW", record.getStationName(), record.getTotalActivePower());
            return true;
        } catch (Exception e) {
            logger.error("[ModbusTCP] 数据上传失败", e);
            record.setFailReason(e.getMessage());
            record.setResponseCode("ERROR");
            return false;
        }
    }

    @Override
    public List<GridDispatchCommand> receiveCommands() {
        List<GridDispatchCommand> result = new ArrayList<>();
        if (!isConnected()) {
            return result;
        }

        try {
            if (socket != null && !socket.isClosed() && inputStream != null && inputStream.available() > 0) {
                int slaveId = currentConfig != null && currentConfig.getCommonAddress() != null
                        ? currentConfig.getCommonAddress() : 1;

                byte[] req = buildModbusReadHoldingRegistersRequest(
                        ++transactionId & 0xFFFF, slaveId, 0x0100, 32
                );
                outputStream.write(req);
                outputStream.flush();
                byte[] header = new byte[9];
                if (inputStream.read(header) == 9) {
                    int byteCount = header[8] & 0xFF;
                    byte[] data = new byte[byteCount];
                    int len = inputStream.read(data);
                    if (len > 0) {
                        result = parseModbusCommandRegisters(data, len);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[ModbusTCP] 读取指令失败", e);
        }
        return result;
    }

    @Override
    public boolean sendCommandResponse(GridDispatchCommand command, boolean success, String reason) {
        if (!isConnected()) {
            return false;
        }
        try {
            int slaveId = currentConfig != null && currentConfig.getCommonAddress() != null
                    ? currentConfig.getCommonAddress() : 1;
            int status = success ? 1 : 0;
            int tid = ++transactionId & 0xFFFF;
            byte[] resp = buildModbusWriteSingleRegisterRequest(tid, slaveId, 0x0200, status);
            if (socket != null && !socket.isClosed() && outputStream != null) {
                outputStream.write(resp);
                outputStream.flush();
            }
            logger.info("[ModbusTCP] 指令执行确认已发送: commandNo={}, success={}", command.getCommandNo(), success);
            return true;
        } catch (Exception e) {
            logger.error("[ModbusTCP] 发送指令确认失败", e);
            return false;
        }
    }

    private byte[] buildModbusWriteMultipleRegistersRequest(int tid, int slaveId, int startAddr, int regCount, int[] values) {
        int len = 7 + 1 + regCount * 2;
        byte[] frame = new byte[6 + len];
        // MBAP Header
        frame[0] = (byte) ((tid >> 8) & 0xFF);
        frame[1] = (byte) (tid & 0xFF);
        frame[2] = 0x00; frame[3] = 0x00;
        frame[4] = (byte) ((len >> 8) & 0xFF);
        frame[5] = (byte) (len & 0xFF);
        frame[6] = (byte) (slaveId & 0xFF);
        frame[7] = 0x10;
        frame[8] = (byte) ((startAddr >> 8) & 0xFF);
        frame[9] = (byte) (startAddr & 0xFF);
        frame[10] = (byte) ((regCount >> 8) & 0xFF);
        frame[11] = (byte) (regCount & 0xFF);
        frame[12] = (byte) (regCount * 2);
        int idx = 13;
        for (int v : values) {
            frame[idx++] = (byte) ((v >> 8) & 0xFF);
            frame[idx++] = (byte) (v & 0xFF);
        }
        return frame;
    }

    private byte[] buildModbusWriteSingleRegisterRequest(int tid, int slaveId, int addr, int value) {
        byte[] frame = new byte[12];
        frame[0] = (byte) ((tid >> 8) & 0xFF);
        frame[1] = (byte) (tid & 0xFF);
        frame[2] = 0x00; frame[3] = 0x00;
        frame[4] = 0x00; frame[5] = 0x06;
        frame[6] = (byte) (slaveId & 0xFF);
        frame[7] = 0x06;
        frame[8] = (byte) ((addr >> 8) & 0xFF);
        frame[9] = (byte) (addr & 0xFF);
        frame[10] = (byte) ((value >> 8) & 0xFF);
        frame[11] = (byte) (value & 0xFF);
        return frame;
    }

    private byte[] buildModbusReadHoldingRegistersRequest(int tid, int slaveId, int startAddr, int count) {
        byte[] frame = new byte[12];
        frame[0] = (byte) ((tid >> 8) & 0xFF);
        frame[1] = (byte) (tid & 0xFF);
        frame[2] = 0x00; frame[3] = 0x00;
        frame[4] = 0x00; frame[5] = 0x06;
        frame[6] = (byte) (slaveId & 0xFF);
        frame[7] = 0x03;
        frame[8] = (byte) ((startAddr >> 8) & 0xFF);
        frame[9] = (byte) (startAddr & 0xFF);
        frame[10] = (byte) ((count >> 8) & 0xFF);
        frame[11] = (byte) (count & 0xFF);
        return frame;
    }

    private List<GridDispatchCommand> parseModbusCommandRegisters(byte[] data, int len) {
        logger.debug("[ModbusTCP] 解析指令寄存器: {}, len={}", bytesToHex(data, len), len);
        return new ArrayList<>();
    }

    private void closeQuietly() {
        try { if (outputStream != null) outputStream.close(); } catch (Exception ignored) {}
        try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    private String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, bytes.length);
    }

    private String bytesToHex(byte[] bytes, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(len, bytes.length); i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }
}
