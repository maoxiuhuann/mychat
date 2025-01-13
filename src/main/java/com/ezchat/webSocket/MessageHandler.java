package com.ezchat.webSocket;

import com.ezchat.entity.dto.MessageSendDTO;
import com.ezchat.utils.JsonUtils;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 消息处理器
 */
@Component("messageHandler")
public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private static final String MESSAGE_TOPIC = "message.topic";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ChannelContextUtils channelContextUtils;

    @PostConstruct
    public void listenMessage() {
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        rTopic.addListener(MessageSendDTO.class,(MessageSendDTO,sendDTO)->{
            logger.info("收到广播消息：{}", JsonUtils.convertObj2Json(sendDTO));
            //channelContextUtils.sendMsg(sendDTO);
        });
    }

    public void sendMessage(MessageSendDTO sendDTO){
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        rTopic.publish(sendDTO);
    }
}
