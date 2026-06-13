package com.solar.ops.device.netty;

import com.solar.ops.device.dto.InverterDataDTO;
import com.solar.ops.device.service.DeviceDataService;
import com.solar.ops.device.util.ModbusDataParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TcpServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger log = LoggerFactory.getLogger(TcpServerHandler.class);

    private static final Map<String, ChannelHandlerContext> channels = new ConcurrentHashMap<>();

    private final DeviceDataService deviceDataService;

    private String deviceId;

    public TcpServerHandler(DeviceDataService deviceDataService) {
        this.deviceDataService = deviceDataService;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String remoteAddr = getRemoteAddress(ctx);
        log.info("设备连接建立: {}", remoteAddr);
        channels.put(remoteAddr, ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String remoteAddr = getRemoteAddress(ctx);
        log.info("设备连接断开: {}", remoteAddr);
        channels.remove(remoteAddr);
        if (deviceId != null) {
            deviceDataService.handleDeviceOffline(deviceId);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        String remoteAddr = getRemoteAddress(ctx);
        log.debug("收到设备数据: {}, 长度: {}字节", remoteAddr, msg.readableBytes());

        try {
            if (deviceId == null) {
                deviceId = extractDeviceId(msg);
                if (deviceId != null) {
                    deviceDataService.handleDeviceOnline(deviceId, remoteAddr);
                }
            }

            InverterDataDTO data = ModbusDataParser.parseModbusData(msg, deviceId, null);
            if (data != null && ModbusDataParser.validateData(data)) {
                deviceDataService.processDeviceData(data);
            } else {
                log.warn("设备数据解析失败或校验不通过: {}", remoteAddr);
            }
        } catch (Exception e) {
            log.error("处理设备数据异常: {}", remoteAddr, e);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.warn("设备读超时: {}", getRemoteAddress(ctx));
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("TCP连接异常: {}", getRemoteAddress(ctx), cause);
        ctx.close();
    }

    private String getRemoteAddress(ChannelHandlerContext ctx) {
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    private String extractDeviceId(ByteBuf msg) {
        if (msg.readableBytes() > 0) {
            int slaveId = msg.getUnsignedByte(0);
            return "INV-" + String.format("%04d", slaveId);
        }
        return null;
    }

    public static Map<String, ChannelHandlerContext> getChannels() {
        return channels;
    }

    public static int getOnlineCount() {
        return channels.size();
    }
}
