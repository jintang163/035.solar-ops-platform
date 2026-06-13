package com.solar.ops.device.netty;

import com.solar.ops.device.config.NettyProperties;
import com.solar.ops.device.service.DeviceDataService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Component
public class NettyServer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    @Autowired
    private NettyProperties nettyProperties;

    @Autowired
    private DeviceDataService deviceDataService;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    private volatile boolean running = false;

    public void start() {
        if (running) {
            log.warn("Netty服务已在运行");
            return;
        }

        bossGroup = new NioEventLoopGroup(nettyProperties.getBossThreads());
        workerGroup = new NioEventLoopGroup(nettyProperties.getWorkerThreads());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, nettyProperties.getSoBacklog())
                    .childOption(ChannelOption.SO_KEEPALIVE, nettyProperties.isKeepAlive())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new IdleStateHandler(nettyProperties.getIdleTimeout(), 0, 0, TimeUnit.SECONDS),
                                    new ProtocolDecoder(),
                                    new TcpServerHandler(deviceDataService)
                            );
                        }
                    });

            channelFuture = bootstrap.bind(nettyProperties.getPort()).sync();
            running = true;
            log.info("Netty TCP服务启动成功，端口: {}", nettyProperties.getPort());

            channelFuture.channel().closeFuture().addListener(future -> {
                running = false;
                log.info("Netty TCP服务已停止");
            });

        } catch (Exception e) {
            log.error("Netty TCP服务启动失败", e);
            stop();
        }
    }

    public void stop() {
        running = false;
        if (channelFuture != null) {
            channelFuture.channel().close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        log.info("Netty TCP服务已关闭");
    }

    @PreDestroy
    public void destroy() {
        stop();
    }

    @Override
    public void run(String... args) throws Exception {
        start();
    }

    public boolean isRunning() {
        return running;
    }
}
