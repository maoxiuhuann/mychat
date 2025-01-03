package com.ezchat.webSocket.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

/**
 * 自定义心跳检测处理器，继承 ChannelDuplexHandler，用于处理连接的心跳事件。
 */
public class HandlerHeartBeat extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(HandlerHeartBeat.class);

    /**
     * 当用户事件触发时调用，用于处理 IdleStateEvent（空闲事件）。
     * @param ctx 上下文对象。
     * @param evt 事件对象，可能是 IdleStateEvent。
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断事件类型是否为 IdleStateEvent（空闲事件）。
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            // 判断是否是读超时事件（READER_IDLE）。
            if (event.state() == IdleState.READER_IDLE){
                logger.info("心跳超时，关闭连接");
                ctx.close();
                // 判断是否是写超时事件（WRITER_IDLE）。
            }else if (event.state() == IdleState.WRITER_IDLE){
                // 向客户端发送心跳包。
                ctx.writeAndFlush("heartbeat");
            }
        }
    }
}