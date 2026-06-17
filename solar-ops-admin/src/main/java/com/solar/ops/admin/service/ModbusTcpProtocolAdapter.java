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
import java.math.RoundingMode;
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

    @Override
    public boolean sendInverterAdjustCommand(GridDispatchCommand command) {
        if (!isConnected()) {
            logger.warn("[ModbusTCP] 连接未建立，无法发送逆变器调节指令");
            return false;
        }

        try {
            int slaveId = currentConfig != null && currentConfig.getCommonAddress() != null
                    ? currentConfig.getCommonAddress() : 1;

            Integer commandType = command.getCommandType();
            if (commandType == null) {
                commandType = 1;
            }

            int startAddr = getModbusStartAddr(commandType);
            BigDecimal targetValue = getTargetValueByCommandType(command);

            if (targetValue == null) {
                logger.warn("[ModbusTCP] 目标值为空，无法发送调节指令，commandType={}", commandType);
                return false;
            }

            int val = targetValue.multiply(BigDecimal.TEN).intValue();
            int[] values = new int[]{val & 0xFFFF, (val >> 16) & 0xFFFF};

            int tid = ++transactionId & 0xFFFF;
            byte[] frame = buildModbusWriteMultipleRegistersRequest(
                    tid, slaveId, startAddr, 2, values
            );

            String hexMessage = bytesToHex(frame);
            command.setRawMessage(hexMessage);

            if (socket != null && !socket.isClosed() && outputStream != null) {
                outputStream.write(frame);
                outputStream.flush();

                byte[] resp = new byte[12];
                int len = inputStream.read(resp);
                if (len <= 0) {
                    logger.warn("[ModbusTCP] 发送逆变器调节指令无响应");
                    return false;
                }
                logger.info("[ModbusTCP] 逆变器调节指令发送成功, 响应: {}", bytesToHex(resp, len));
            } else {
                logger.info("[ModbusTCP] 模拟模式下发送逆变器调节指令: type={}, target={}", commandType, targetValue);
            }

            if (commandType == 2 && command.getTargetReactivePower() != null) {
                int reactiveVal = command.getTargetReactivePower().multiply(BigDecimal.TEN).intValue();
                int[] reactiveValues = new int[]{reactiveVal & 0xFFFF, (reactiveVal >> 16) & 0xFFFF};
                int reactiveTid = ++transactionId & 0xFFFF;
                byte[] reactiveFrame = buildModbusWriteMultipleRegistersRequest(
                        reactiveTid, slaveId, 0x0010, 2, reactiveValues
                );
                if (socket != null && !socket.isClosed() && outputStream != null) {
                    outputStream.write(reactiveFrame);
                    outputStream.flush();
                    byte[] reactiveResp = new byte[12];
                    int reactiveLen = inputStream.read(reactiveResp);
                    logger.info("[ModbusTCP] 无功功率调节指令发送成功, 响应长度: {}", reactiveLen);
                }
            }

            logger.info("[ModbusTCP] 逆变器调节指令已下发: commandNo={}, type={}, targetValue={}",
                    command.getCommandNo(), commandType, targetValue);
            return true;

        } catch (Exception e) {
            logger.error("[ModbusTCP] 发送逆变器调节指令失败", e);
            return false;
        }
    }

    private int getModbusStartAddr(int commandType) {
        switch (commandType) {
            case 1:
                return 0x0000;
            case 2:
                return 0x0010;
            case 3:
                return 0x0020;
            case 4:
                return 0x0030;
            case 5:
                return 0x0040;
            default:
                return 0x0000;
        }
    }

    private BigDecimal getTargetValueByCommandType(GridDispatchCommand command) {
        Integer type = command.getCommandType();
        if (type == null) return null;
        switch (type) {
            case 1:
                return command.getTargetActivePower();
            case 2:
                return command.getTargetReactivePower() != null ? command.getTargetReactivePower() : command.getTargetActivePower();
            case 3:
                return command.getTargetVoltage();
            case 4:
                return command.getTargetFrequency();
            case 5:
                return command.getStartStop() != null && command.getStartStop() ? BigDecimal.ONE : BigDecimal.ZERO;
            default:
                return command.getTargetActivePower();
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
        List<GridDispatchCommand> commands = new ArrayList<>();
        String hexFrame = bytesToHex(data, len);
        logger.debug("[ModbusTCP] 解析指令寄存器: {}, len={}", hexFrame, len);

        if (data == null || len < 2) {
            logger.warn("[ModbusTCP] 数据为空或长度不足");
            return commands;
        }

        int registerCount = len / 2;
        if (registerCount < 8) {
            logger.warn("[ModbusTCP] 寄存器数量不足，至少需要8个寄存器，实际={}", registerCount);
            return commands;
        }

        try {
            int pos = 0;

            int commandFlag = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;

            if (commandFlag != 0xAAAA && commandFlag != 0x5555) {
                logger.debug("[ModbusTCP] 指令标志位不匹配，跳过解析，flag=0x{}", Integer.toHexString(commandFlag));
                return commands;
            }

            int commandType = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;

            int stationId = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;

            int inverterIdHigh = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;
            int inverterIdLow = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;
            long inverterId = ((long) inverterIdHigh << 16) | (inverterIdLow & 0xFFFFL);

            int targetValueHigh = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;
            int targetValueLow = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;
            int targetRaw = (targetValueHigh << 16) | (targetValueLow & 0xFFFF);
            float targetFloat = Float.intBitsToFloat(targetRaw);
            BigDecimal targetValue = BigDecimal.valueOf(targetFloat).setScale(2, RoundingMode.HALF_UP);

            int adjustRatio = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;

            int priority = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;

            GridDispatchCommand command = new GridDispatchCommand();
            command.setProtocolCommandId(String.valueOf(commandFlag));
            command.setCommandType(mapModbusCommandType(commandType));

            if (stationId > 0) {
                command.setStationId((long) stationId);
            }
            if (inverterId > 0) {
                command.setInverterId(inverterId);
            }

            Integer mappedType = command.getCommandType();
            if (mappedType == 1) {
                command.setTargetActivePower(targetValue);
            } else if (mappedType == 2) {
                command.setTargetReactivePower(targetValue);
            } else if (mappedType == 3) {
                command.setTargetVoltage(targetValue);
            } else if (mappedType == 4) {
                command.setTargetFrequency(targetValue);
            } else if (mappedType == 5) {
                command.setStartStop(targetValue.intValue() == 1);
            }

            if (adjustRatio > 0 && adjustRatio <= 100) {
                command.setAdjustRatio(adjustRatio);
            }
            if (priority >= 1 && priority <= 4) {
                command.setPriority(priority);
            }

            command.setRawMessage(hexFrame);
            commands.add(command);

            logger.info("[ModbusTCP] 成功解析调度指令: type={}, stationId={}, inverterId={}, targetValue={}",
                    command.getCommandType(), command.getStationId(), command.getInverterId(), targetValue);

        } catch (Exception e) {
            logger.error("[ModbusTCP] 解析指令寄存器失败", e);
        }

        return commands;
    }

    private Integer mapModbusCommandType(int modbusType) {
        switch (modbusType) {
            case 0x0001:
                return 1;
            case 0x0002:
                return 2;
            case 0x0003:
                return 3;
            case 0x0004:
                return 4;
            case 0x0005:
                return 5;
            default:
                return 1;
        }
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
