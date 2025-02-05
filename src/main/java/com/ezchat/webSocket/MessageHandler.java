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


    //该注解用于标记一个方法，该方法将在当前Bean的依赖注入完成后被自动调用。在这里，它确保在MessageHandler类被实例化后，监听功能会开始工作。
    @PostConstruct
    public void listenMessage() {
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        // 添加一个监听器，监听MessageSendDTO类型的消息
        rTopic.addListener(MessageSendDTO.class,(MessageSendDTO,sendDTO)->{
            logger.info("收到广播消息：{}", JsonUtils.convertObj2Json(sendDTO));
            channelContextUtils.sendMessage(sendDTO);
        });
    }

    public void sendMessage(MessageSendDTO sendDTO){
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        rTopic.publish(sendDTO);
    }
}
