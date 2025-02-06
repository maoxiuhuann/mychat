package com.ezchat.service.impl;

import com.ezchat.constans.Constans;
import com.ezchat.entity.dto.MessageSendDTO;
import com.ezchat.entity.dto.SysSettingDTO;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.po.ChatSession;
import com.ezchat.entity.po.ChatSessionUser;
import com.ezchat.entity.po.UserContact;
import com.ezchat.entity.query.ChatSessionQuery;
import com.ezchat.entity.query.ChatSessionUserQuery;
import com.ezchat.entity.query.SimplePage;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.ChatMessage;
import com.ezchat.entity.query.ChatMessageQuery;
import com.ezchat.enums.*;
import com.ezchat.exception.BusinessException;
import com.ezchat.mappers.ChatMessageMapper;
import com.ezchat.mappers.ChatSessionMapper;
import com.ezchat.mappers.ChatSessionUserMapper;
import com.ezchat.redis.RedisComponent;
import com.ezchat.service.ChatMessageService;
import com.ezchat.utils.CopyUtils;
import com.ezchat.utils.StringTools;
import com.ezchat.webSocket.MessageHandler;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import jdk.nashorn.internal.parser.Token;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description:聊天消息表Service
 * @author:xiuyuan
 * @date:2025/01/06
 */
@Service("chatMessageService")
public class ChatMessageServiceImpl implements ChatMessageService {

    @Resource
    private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;

    @Autowired
    private RedisComponent redisComponent;

    @Autowired
    private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;
    @Autowired
    private MessageHandler messageHandler;


    /**
     * 根据条件查询列表
     */
    public List<ChatMessage> findListByParam(ChatMessageQuery query) {
        return this.chatMessageMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    public Integer findCountByParam(ChatMessageQuery query) {
        return this.chatMessageMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    public PaginationResultVO<ChatMessage> findListByPage(ChatMessageQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<ChatMessage> list = this.findListByParam(query);
        PaginationResultVO<ChatMessage> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    public Integer add(ChatMessage bean) {
        return this.chatMessageMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    public Integer addBatch(List<ChatMessage> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.chatMessageMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或更新
     */
    public Integer addOrUpdateBatch(List<ChatMessage> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.chatMessageMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据MessageId查询数据
     */
    public ChatMessage getChatMessageByMessageId(Long messageId) {
        return this.chatMessageMapper.selectByMessageId(messageId);
    }

    /**
     * 根据MessageId更新数据
     */
    public Integer updateChatMessageByMessageId(ChatMessage bean, Long messageId) {
        return this.chatMessageMapper.updateByMessageId(bean, messageId);
    }

    /**
     * 根据MessageId删除数据
     */
    public Integer deleteChatMessageByMessageId(Long messageId) {
        return this.chatMessageMapper.deleteByMessageId(messageId);
    }

    @Override
    public MessageSendDTO saveAndSendMessage(ChatMessage chatMessage, TokenUserInfoDTO tokenUserInfoDTO) throws BusinessException {
        //不是机器人回复，判断好友状态
        if (Constans.ROBOT_UID.equals(tokenUserInfoDTO.getUserId())) {
            List<String> contactIdList = redisComponent.getUserContactList(tokenUserInfoDTO.getUserId());
            if (!contactIdList.contains(chatMessage.getContactId())) {
                UserContactTypeEnum userContactTypeEnum = UserContactTypeEnum.getByPrefix(chatMessage.getContactId());
                if (userContactTypeEnum.USER.equals(userContactTypeEnum)) {
                    throw new BusinessException(ResponseCodeEnum.CODE_902);
                } else {
                    throw new BusinessException(ResponseCodeEnum.CODE_903);
                }
            }
        }

        String sessionId = null;
        String sendUserId = tokenUserInfoDTO.getUserId();
        String contactId = chatMessage.getContactId();
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(chatMessage.getContactId());
        //判断群聊还是私聊，生成sessionId
        if (UserContactTypeEnum.USER.equals(contactTypeEnum)) {
            sessionId = StringTools.getChatSessionId4User(new String[]{sendUserId, contactId});
        } else {
            sessionId = StringTools.getChatSessionId4Group(contactId);
        }
        chatMessage.setSessionId(sessionId);
        //设置发送时间
        Long currentTime = System.currentTimeMillis();
        chatMessage.setSendTime(currentTime);
        //判断消息类型
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(chatMessage.getMessageType());
        //消息类型非法情况-1.不存在2.不属于聊天消息类型
        if (null == messageTypeEnum || !ArrayUtils.contains(new Integer[]{MessageTypeEnum.CHAT.getType(), MessageTypeEnum.MEDIA_CHAT.getType()}, chatMessage.getMessageType())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //设置消息发送状态-1.MEDIS_CHAT消息发送中2.SENDED消息已发送
        Integer status = MessageTypeEnum.MEDIA_CHAT == messageTypeEnum ? MessageStatusEnum.SENDING.getStatus() : MessageStatusEnum.SENDED.getStatus();
        chatMessage.setStatus(status);
        //清除html标签,防止xss攻击
        String messageContent = StringTools.cleanHtmlTag(chatMessage.getMessageContent());
        chatMessage.setMessageContent(messageContent);
        //记录消息表
        chatMessage.setSendUserId(sendUserId);
        chatMessage.setSendUserNickName(tokenUserInfoDTO.getNickName());
        chatMessage.setContactType(contactTypeEnum.getType());
        chatMessageMapper.insert(chatMessage);

        //更新聊天会话表
        ChatSession chatSession = new ChatSession();
        chatSession.setLastMessage(messageContent);
        if (UserContactTypeEnum.GROUP.equals(contactTypeEnum)){
            chatSession.setLastMessage(tokenUserInfoDTO.getNickName() + ":" + messageContent);
        }
        chatSession.setLastReceiveTime(currentTime);
        chatSessionMapper.updateBySessionId(chatSession, sessionId);

        //发送消息
        MessageSendDTO messageSendDTO = CopyUtils.copy(chatMessage, MessageSendDTO.class);
        if (Constans.ROBOT_UID.equals(contactId)){
            SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
            TokenUserInfoDTO robot = new TokenUserInfoDTO();
            robot.setUserId(sysSettingDTO.getRobotUid());
            robot.setNickName(sysSettingDTO.getRobotNickName());
            ChatMessage robotMessage = new ChatMessage();
            robotMessage.setContactId(sendUserId);
            //这里可以对接AI，实现聊天机器人回复
            robotMessage.setMessageContent("我只是一个机器人喵");
            robotMessage.setMessageType(MessageTypeEnum.CHAT.getType());
            //用户给机器人发消息，第一次设置的contactId是机器人的uid,再次调用这个方法时，contactId是用户的uid，进入else发送给用户
            saveAndSendMessage(robotMessage, robot);
        }else {
            messageHandler.sendMessage(messageSendDTO);
        }
        return messageSendDTO;
    }

}
