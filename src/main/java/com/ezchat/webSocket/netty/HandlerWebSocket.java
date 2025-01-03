package com.ezchat.webSocket.netty;

import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.redis.RedisComponent;
import com.ezchat.utils.StringUtils;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 自定义 WebSocket 消息处理器，继承 SimpleChannelInboundHandler，用于处理 WebSocket 文本帧。
 */
@Component
@ChannelHandler.Sharable
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(HandlerWebSocket.class);

    @Resource
    private RedisComponent redisComponent;

    /**
     * 通道激活时调用，一般用来处理连接建立后的初始化操作。
     * @param ctx 上下文对象，表示当前通道的上下文信息。
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("有新的连接加入");
    }

    /**
     * 通道关闭后调用：一般用来做一些资源释放操作
     * @param ctx 上下文对象，表示当前通道的上下文信息。
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("连接断开");
    }

    /**
     * 读取 WebSocket 消息。当收到文本消息（TextWebSocketFrame）时会触发该方法。
     * @param ctx 上下文对象，表示当前通道的上下文信息。
     * @param textWebSocketFrame 接收到的 WebSocket 文本帧。
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame textWebSocketFrame) throws Exception {
        // 获取当前通道对象。
        Channel channel = ctx.channel();
        logger.info("收到消息：" + textWebSocketFrame.text());
    }

    /**
     * 用户事件触发时调用，继承的默认实现，可用于扩展特殊事件处理。
     * @param ctx 上下文对象。
     * @param evt 事件对象。
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete){
            WebSocketServerProtocolHandler.HandshakeComplete complete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String uri = complete.requestUri();
            String token = getToken(uri);
            if (token == null){
                ctx.channel().close();
                return;
            }
            TokenUserInfoDTO tokenUserInfoDTO = redisComponent.getTokenUserInfoDTO(token);
            if (tokenUserInfoDTO == null){
                ctx.channel().close();
                return;
            }
        }
    }

    /**
     * 获取uri中的token参数值。
     * @param uri
     * @return
     */
    private String getToken(String uri){
        if (StringUtils.isEmpty(uri) || uri.indexOf("?") == -1){
            return null;
        }
        String[] queryParams = uri.split("\\?");
        if (queryParams.length != 2){
            return null;
        }
        String[] params = queryParams[1].split("=");
        if (params.length!= 2){
            return null;
        }
        return params[1];
    }
}
