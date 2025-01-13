package com.ezchat.webSocket.netty;


import com.ezchat.entity.config.AppConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * websocket启动器
 */
@Component
public class NettyWebSocketStarter implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(NettyWebSocketStarter.class);
    //处理连接
    private static EventLoopGroup bossGroup = new NioEventLoopGroup();
    //处理读写
    private static EventLoopGroup workerGroup = new NioEventLoopGroup();

    @Resource
    private HandlerWebSocket handlerWebSocket;

    @Resource
    private AppConfig appConfig;

    @PreDestroy
    private void close(){
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public void run() {
        try {
            //创建 Netty 的服务启动器 ServerBootstrap，用于配置和启动服务。
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            //将 bossGroup 和 workerGroup 分别绑定到启动器，分别用于接收连接和处理读写。
            serverBootstrap.group(bossGroup, workerGroup);
            //指定通道类型为 NioServerSocketChannel，这是 Netty 的 NIO 服务端实现。
            serverBootstrap.channel(NioServerSocketChannel.class)
                    //添加一个日志处理器 LoggingHandler，以 DEBUG 级别记录服务端的运行信息。
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    //设置子通道的处理器，这些处理器用于对客户端的具体请求进行处理。
                    .childHandler(new ChannelInitializer() {
                        //初始化每一个客户端连接对应的 Channel，定义具体地处理逻辑。
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            //获取 ChannelPipeline，它是一个处理链，用于存储并执行一系列的处理器。
                            ChannelPipeline pipeline = channel.pipeline();
                            //设置处理器
                            //添加一个 HTTP 编解码器 HttpServerCodec，用于解析和编码 HTTP 请求。
                            pipeline.addLast(new HttpServerCodec());
                            //添加一个 HTTP 消息聚合器 HttpObjectAggregator，将多个 HTTP 消息片段聚合为一个完整的 FullHttpRequest 或 FullHttpResponse。
                            //参数 64 * 1024: 表示最大消息体积为 64 KB。
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                            //心跳 long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit
                            //读超时时间，写超时时间，所有超时类型时间，单位-设置心跳规则
                            pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                            //自定义处理器-处理心跳
                            pipeline.addLast(new HandlerHeartBeat());
                            //添加一个 WebSocket 协议处理器 WebSocketServerProtocolHandler，将 HTTP 升级为 WebSocket。参数 "/ws": 指定 WebSocket 的 URI。
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true, 64 * 1024, true, true, 1000L));
                            //自定义处理器-处理websocket消息
                            pipeline.addLast(handlerWebSocket);
                        }
                    });
            Integer wsPort = appConfig.getWsPort();
            String wsPortStr = System.getProperty("ws.port");
            if (!StringUtils.isEmpty(wsPortStr)){
                wsPort = Integer.parseInt(wsPortStr);
            }
            //sync(): 阻塞当前线程直到绑定操作完成。
            ChannelFuture channelFuture = serverBootstrap.bind(wsPort).sync();
            logger.info("Netty服务启动成功，端口:{}",appConfig.getWsPort());
            //阻塞当前线程，等待服务端通道关闭。
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.error("Netty启动失败", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
