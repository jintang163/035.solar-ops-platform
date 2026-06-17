package com.solar.ops.admin.service;

import com.alibaba.fastjson.JSON;
import com.solar.ops.admin.entity.GridDispatchCommand;
import com.solar.ops.admin.entity.GridDispatchUploadRecord;
import com.solar.ops.admin.vo.GridDispatchProtocolConfigVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service("iec104Adapter")
public class Iec104ProtocolAdapter implements DispatchProtocolAdapter {

    private static final Logger logger = LoggerFactory.getLogger(Iec104ProtocolAdapter.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean connected = false;
    private GridDispatchProtocolConfigVO currentConfig;

    @Override
    public String getProtocolType() {
        return "IEC104";
    }

    @Override
    public boolean connect(GridDispatchProtocolConfigVO config) {
        if (config == null) {
            logger.warn("[IEC104] 配置为空，使用模拟模式连接");
            this.connected = true;
            return true;
        }
        this.currentConfig = config;

        String targetIp = config.getReverseIsolationEnabled() != null && config.getReverseIsolationEnabled() == 1
                ? config.getIsolationIp() : config.getMasterIp();
        Integer targetPort = config.getReverseIsolationEnabled() != null && config.getReverseIsolationEnabled() == 1
                ? config.getIsolationPort() : config.getMasterPort();

        if (targetIp == null || targetPort == null) {
            logger.warn("[IEC104] IP或端口为空，使用模拟模式连接");
            this.connected = true;
            return true;
        }

        try {
            socket = new Socket();
            int timeout = (config.getConnectTimeout() != null ? config.getConnectTimeout() : 10) * 1000;
            socket.connect(new InetSocketAddress(targetIp, targetPort), timeout);
            socket.setSoTimeout(timeout);
            socket.setKeepAlive(true);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            sendIec104StartDt();
            byte[] resp = new byte[6];
            int len = inputStream.read(resp);
            if (len > 0) {
                this.connected = true;
                logger.info("[IEC104] 连接成功: {}:{}, ASDU地址:{}", targetIp, targetPort, config.getCommonAddress());
                return true;
            }
        } catch (Exception e) {
            logger.warn("[IEC104] TCP连接失败: {}:{}，降级为模拟模式 - {}", targetIp, targetPort, e.getMessage());
            closeQuietly();
        }

        this.connected = true;
        return true;
    }

    @Override
    public void disconnect() {
        if (connected) {
            try {
                if (outputStream != null) {
                    sendIec104StopDt();
                }
            } catch (Exception ignored) {
            }
        }
        closeQuietly();
        this.connected = false;
        logger.info("[IEC104] 连接已断开");
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
            record.setFailReason("IEC104连接未建立");
            return false;
        }

        try {
            String timeStr = LocalDateTime.now().format(FORMATTER);
            long seq = ThreadLocalRandom.current().nextLong(1000000, 9999999L);
            String hexMessage = buildIec104DataFrame(record, seq, timeStr);
            record.setRawMessage(hexMessage);

            if (socket != null && !socket.isClosed() && outputStream != null) {
                outputStream.write(hexStringToBytes(hexMessage));
                outputStream.flush();
                byte[] resp = new byte[100];
                int len = inputStream.read(resp);
                if (len <= 0) {
                    record.setFailReason("IEC104无响应");
                    record.setResponseCode("TIMEOUT");
                    return false;
                }
                record.setResponseCode("OK_" + bytesToHexString(resp, len));
            } else {
                record.setResponseCode("MOCK_OK_" + seq);
            }

            record.setUploadStatus(1);
            record.setResponseCode(record.getResponseCode() != null ? record.getResponseCode() : "MOCK_OK");
            logger.debug("[IEC104] 数据上传成功: 电站={}, 功率={}kW", record.getStationName(), record.getTotalActivePower());
            return true;
        } catch (Exception e) {
            logger.error("[IEC104] 数据上传失败", e);
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
                byte[] data = new byte[1024];
                int len = inputStream.read(data);
                if (len > 0) {
                    result = parseIec104CommandFrame(data, len);
                }
            }
        } catch (Exception e) {
            logger.error("[IEC104] 读取指令失败", e);
        }

        return result;
    }

    @Override
    public boolean sendCommandResponse(GridDispatchCommand command, boolean success, String reason) {
        if (!isConnected()) {
            return false;
        }
        try {
            String respHex = buildIec104CommandConfirmFrame(command, success, reason);
            if (socket != null && !socket.isClosed() && outputStream != null) {
                outputStream.write(hexStringToBytes(respHex));
                outputStream.flush();
            }
            logger.info("[IEC104] 指令执行确认已发送: commandNo={}, success={}", command.getCommandNo(), success);
            return true;
        } catch (Exception e) {
            logger.error("[IEC104] 发送指令确认失败", e);
            return false;
        }
    }

    @Override
    public boolean sendInverterAdjustCommand(GridDispatchCommand command) {
        if (!isConnected()) {
            logger.warn("[IEC104] 连接未建立，无法发送逆变器调节指令");
            return false;
        }

        try {
            StringBuilder hexBuilder = new StringBuilder();
            int seq = ThreadLocalRandom.current().nextInt(1000, 9999);

            Integer commandType = command.getCommandType();
            if (commandType == null) {
                commandType = 1;
            }

            int infoAddr = getInfoAddrByCommandType(commandType);
            BigDecimal targetValue = getTargetValueByCommandType(command);

            if (targetValue == null) {
                logger.warn("[IEC104] 目标值为空，无法发送调节指令，commandType={}", commandType);
                return false;
            }

            hexBuilder.append("6816");
            hexBuilder.append(String.format("%04X", seq * 2));
            hexBuilder.append("0000");
            hexBuilder.append("2D");
            hexBuilder.append("01");
            hexBuilder.append("06010100000000");

            int floatBits = Float.floatToIntBits(targetValue.floatValue());
            hexBuilder.append(String.format("%08X", floatBits));

            hexBuilder.append(String.format("%06X", infoAddr & 0xFFFFFF));

            String hexMessage = hexBuilder.toString();

            if (socket != null && !socket.isClosed() && outputStream != null) {
                outputStream.write(hexStringToBytes(hexMessage));
                outputStream.flush();

                byte[] resp = new byte[100];
                int len = inputStream.read(resp);
                if (len <= 0) {
                    logger.warn("[IEC104] 发送逆变器调节指令无响应");
                    return false;
                }
                logger.info("[IEC104] 逆变器调节指令发送成功, 响应: {}", bytesToHexString(resp, len));
            } else {
                logger.info("[IEC104] 模拟模式下发送逆变器调节指令: type={}, target={}", commandType, targetValue);
            }

            command.setRawMessage(hexMessage);
            logger.info("[IEC104] 逆变器调节指令已下发: commandNo={}, type={}, targetValue={}",
                    command.getCommandNo(), commandType, targetValue);
            return true;

        } catch (Exception e) {
            logger.error("[IEC104] 发送逆变器调节指令失败", e);
            return false;
        }
    }

    private int getInfoAddrByCommandType(int commandType) {
        switch (commandType) {
            case 1:
                return 0x0100;
            case 2:
                return 0x0110;
            case 3:
                return 0x0120;
            case 4:
                return 0x0130;
            case 5:
                return 0x0140;
            default:
                return 0x0100;
        }
    }

    private BigDecimal getTargetValueByCommandType(GridDispatchCommand command) {
        Integer type = command.getCommandType();
        if (type == null) return null;
        switch (type) {
            case 1:
                return command.getTargetActivePower();
            case 2:
                return command.getTargetReactivePower();
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

    private void sendIec104StartDt() throws Exception {
        byte[] startDt = {0x68, 0x04, 0x07, 0x00, 0x00, 0x00};
        outputStream.write(startDt);
        outputStream.flush();
    }

    private void sendIec104StopDt() throws Exception {
        byte[] stopDt = {0x68, 0x04, 0x13, 0x00, 0x00, 0x00};
        outputStream.write(stopDt);
        outputStream.flush();
    }

    private String buildIec104DataFrame(GridDispatchUploadRecord record, long seq, String time) {
        StringBuilder sb = new StringBuilder();
        sb.append("681A");
        sb.append(String.format("%04X", seq * 2));
        sb.append("0000");
        sb.append("09");
        sb.append("01");
        sb.append("06010100000000");
        if (record.getTotalActivePower() != null) {
            int val = record.getTotalActivePower().multiply(BigDecimal.TEN).intValue();
            sb.append(String.format("%08X", val));
        } else {
            sb.append("00000000");
        }
        if (record.getFrequency() != null) {
            int f = record.getFrequency().multiply(BigDecimal.valueOf(100)).intValue();
            sb.append(String.format("%04X", f));
        } else {
            sb.append("1388");
        }
        sb.append(time.substring(8));
        logger.debug("[IEC104] 模拟数据帧: {}", sb);
        return sb.toString();
    }

    private List<GridDispatchCommand> parseIec104CommandFrame(byte[] data, int len) {
        List<GridDispatchCommand> commands = new ArrayList<>();
        String hexFrame = bytesToHexString(data, len);
        logger.debug("[IEC104] 解析指令帧: {}", hexFrame);

        if (len < 6 || (data[0] & 0xFF) != 0x68) {
            logger.warn("[IEC104] 无效的IEC104帧格式");
            return commands;
        }

        int apduLen = data[1] & 0xFF;
        if (apduLen < 4 || len < (apduLen + 2)) {
            logger.warn("[IEC104] 帧长度不匹配，期望长度={}, 实际长度={}", apduLen + 2, len);
            return commands;
        }

        int typeId = data[6] & 0xFF;
        int vsq = data[7] & 0xFF;
        int cot = data[8] & 0xFF;
        int asduAddr = ((data[10] & 0xFF) << 8) | (data[9] & 0xFF);
        int infoObjCount = vsq & 0x7F;

        logger.debug("[IEC104] 帧解析: typeId={}, cot={}, asduAddr={}, infoObjCount={}", typeId, cot, asduAddr, infoObjCount);

        if (typeId == 0x2D || typeId == 0x0D || typeId == 0x06) {
            int pos = 11;
            for (int i = 0; i < infoObjCount && pos + 7 <= len; i++) {
                try {
                    int infoAddr = ((data[pos + 2] & 0xFF) << 16)
                            | ((data[pos + 1] & 0xFF) << 8)
                            | (data[pos] & 0xFF);
                    pos += 3;

                    GridDispatchCommand command = new GridDispatchCommand();
                    command.setProtocolCommandId(String.valueOf(infoAddr));
                    command.setAsduAddress(asduAddr);

                    if (typeId == 0x2D) {
                        int rawValue = ((data[pos + 3] & 0xFF) << 24)
                                | ((data[pos + 2] & 0xFF) << 16)
                                | ((data[pos + 1] & 0xFF) << 8)
                                | (data[pos] & 0xFF);
                        float floatValue = Float.intBitsToFloat(rawValue);
                        BigDecimal value = BigDecimal.valueOf(floatValue).setScale(2, RoundingMode.HALF_UP);

                        int commandType = mapInfoAddrToCommandType(infoAddr);
                        command.setCommandType(commandType);

                        if (commandType == 1) {
                            command.setTargetActivePower(value);
                        } else if (commandType == 2) {
                            command.setTargetReactivePower(value);
                        } else if (commandType == 3) {
                            command.setTargetVoltage(value);
                        } else if (commandType == 4) {
                            command.setTargetFrequency(value);
                        }
                        command.setRawMessage(hexFrame);
                        commands.add(command);
                        pos += 5;
                    } else if (typeId == 0x0D) {
                        int rawValue = ((data[pos + 1] & 0xFF) << 8) | (data[pos] & 0xFF);
                        command.setAdjustRatio(rawValue);
                        command.setCommandType(1);
                        command.setRawMessage(hexFrame);
                        commands.add(command);
                        pos += 3;
                    } else if (typeId == 0x06) {
                        int scs = data[pos] & 0x01;
                        command.setStartStop(scs == 1);
                        command.setCommandType(5);
                        command.setRawMessage(hexFrame);
                        commands.add(command);
                        pos += 2;
                    }
                } catch (Exception e) {
                    logger.error("[IEC104] 解析第{}个信息对象失败", i + 1, e);
                    break;
                }
            }
        } else if (typeId == 0x64) {
            GridDispatchCommand command = new GridDispatchCommand();
            command.setCommandType(1);
            command.setTargetActivePower(BigDecimal.ZERO);
            command.setRawMessage(hexFrame);
            commands.add(command);
            logger.info("[IEC104] 收到初始化结束帧，创建默认调节指令");
        }

        if (!commands.isEmpty()) {
            logger.info("[IEC104] 成功解析{}条调度指令", commands.size());
        }

        return commands;
    }

    private int mapInfoAddrToCommandType(int infoAddr) {
        switch (infoAddr) {
            case 0x0001:
            case 0x0100:
                return 1;
            case 0x0002:
            case 0x0110:
                return 2;
            case 0x0003:
            case 0x0120:
                return 3;
            case 0x0004:
            case 0x0130:
                return 4;
            case 0x0005:
                return 5;
            default:
                return 1;
        }
    }

    private String buildIec104CommandConfirmFrame(GridDispatchCommand command, boolean success, String reason) {
        String cmdNo = command.getCommandNo() != null ? command.getCommandNo() : "DIS";
        String result = success ? "680A0100" : "680A0200";
        String json = JSON.toJSONString(command);
        logger.debug("[IEC104] 指令确认帧: {} -> {} success={} reason={}", cmdNo, result, success, reason);
        logger.debug("[IEC104] 原始指令: {}", json);
        return result;
    }

    private void closeQuietly() {
        try { if (outputStream != null) outputStream.close(); } catch (Exception ignored) {}
        try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    private byte[] hexStringToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private String bytesToHexString(byte[] bytes, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(len, bytes.length); i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }
}
