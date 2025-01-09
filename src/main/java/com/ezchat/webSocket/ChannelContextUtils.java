package com.ezchat.webSocket;

import com.ezchat.constans.Constans;
import com.ezchat.entity.dto.MessageSendDTO;
import com.ezchat.entity.dto.WsInitData;
import com.ezchat.entity.po.ChatMessage;
import com.ezchat.entity.po.ChatSessionUser;
import com.ezchat.entity.po.UserContactApply;
import com.ezchat.entity.po.UserInfo;
import com.ezchat.entity.query.ChatMessageQuery;
import com.ezchat.entity.query.ChatSessionUserQuery;
import com.ezchat.entity.query.UserContactApplyQuery;
import com.ezchat.entity.query.UserInfoQuery;
import com.ezchat.enums.MessageTypeEnum;
import com.ezchat.enums.UserContactApplyStatusEnum;
import com.ezchat.enums.UserContactTypeEnum;
import com.ezchat.mappers.ChatMessageMapper;
import com.ezchat.mappers.UserContactApplyMapper;
import com.ezchat.mappers.UserInfoMapper;
import com.ezchat.redis.RedisComponent;
import com.ezchat.service.ChatSessionUserService;
import com.ezchat.utils.JsonUtils;
import com.ezchat.utils.StringTools;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ChannelContextUtils {

    // 用户与 Channel 绑定的存储
    private static final ConcurrentHashMap<String, Channel> USER_CONTEXT_MAP = new ConcurrentHashMap<>();

    // 群组与 ChannelGroup 的存储
    private static final ConcurrentHashMap<String, ChannelGroup> GROUP_CONCURRENT_MAP = new ConcurrentHashMap<>();

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private ChatSessionUserService chatSessionUserService;

    @Autowired
    private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;

    @Resource
    private UserContactApplyMapper<UserContactApply, UserContactApplyQuery> userContactApplyMapper;

    /**
     * 将用户的上下文信息绑定到 Channel，并加入到指定的群组。
     *
     * @param userId  用户的唯一标识符
     * @param channel 用户对应的 Netty Channel
     *                <p>
     *                1. 通过 AttributeKey 将 userId 绑定到当前 Channel。
     *                2. 将 userId 和 Channel 存储到 USER_CONTEXT_MAP，用于快速查找。
     *                3. 更新用户心跳信息到 Redis，表示用户在线状态。
     *                4. 将 Channel 加入到默认群组（groupId = "1000"）中，便于群聊管理。
     */
    public void addContext(String userId, Channel channel) {
        String channelId = channel.id().toString();

        // 创建或获取 AttributeKey，绑定用户 ID
        AttributeKey attributeKey = null;
        if (!AttributeKey.exists(channelId)) {
            attributeKey = AttributeKey.newInstance(channelId);
        } else {
            attributeKey = AttributeKey.valueOf(channelId);
        }
        channel.attr(attributeKey).set(userId);

        // 将用户channel加入联系人列表中的群组
        List<String> contactIdList = redisComponent.getUserContactList(userId);
        for (String groupId : contactIdList) {
            if (groupId.startsWith(UserContactTypeEnum.GROUP.getPrefix())) {
                add2Group(groupId, channel);
            }
        }

        // 将用户 ID 和对应的 Channel 存储到 Map
        USER_CONTEXT_MAP.put(userId, channel);
        // 绑定用户ID和channel后立刻更新用户心跳
        redisComponent.saveUserHeartBeat(userId);

        //更新用户最后连接时间
        UserInfo updateInfo = new UserInfo();
        updateInfo.setLastLoginTime(new Date());
        userInfoMapper.updateByUserId(updateInfo, userId);

        //历史消息推送-最近三天
        UserInfo userInfo = userInfoMapper.selectByUserId(userId);
        Long dblastLogOffTime = userInfo.getLastOffTime();
        Long lastLogOffTime = dblastLogOffTime;
        //if判断现在时间减去最后下线时间大于3天，说明用户已经离线3天以上，更新lastLogOffTime为3天前的时间
        if (dblastLogOffTime != null && System.currentTimeMillis() - Constans.MILLIS_SECONDS_MESSAGE_EXPIRE > dblastLogOffTime) {
            lastLogOffTime = System.currentTimeMillis() - Constans.MILLIS_SECONDS_MESSAGE_EXPIRE;
        }
        /**
         * 1.查询会话信息，查询用户所有的会话信息，保证换设备也能够同步会话
         */
        ChatSessionUserQuery sessionUserQuery = new ChatSessionUserQuery();
        sessionUserQuery.setUserId(userId);
        sessionUserQuery.setOrderBy("last_receive_time desc");
        List<ChatSessionUser> chatSessionUserList = chatSessionUserService.findListByParam(sessionUserQuery);

        WsInitData wsInitData = new WsInitData();
        wsInitData.setChatSessionList(chatSessionUserList);

        /**
         * 2. 查询用户每个会话三天内聊天消息：即查询chatMessage表中contactId是userId和userID加入群组groupId的聊天记录
         */
        List<String> groupIdList = contactIdList.stream().filter(item -> item.startsWith(UserContactTypeEnum.GROUP.getPrefix())).collect(Collectors.toList());
        groupIdList.add(userId);
        ChatMessageQuery chatMessageQuery = new ChatMessageQuery();
        chatMessageQuery.setContactIdList(groupIdList);
        chatMessageQuery.setLastReceiveTime(lastLogOffTime);
        List<ChatMessage> chatMessageList = chatMessageMapper.selectList(chatMessageQuery);

        wsInitData.setChatMessageList(chatMessageList);
        /**
         * 3.查询好友申请数量
         */
        UserContactApplyQuery applyQuery = new UserContactApplyQuery();
        applyQuery.setReceiveUserId(userId);
        applyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
        //只查询离线之后收到的好友申请
        applyQuery.setLastApplyTimeStamp(lastLogOffTime);
        Integer applyCount = userContactApplyMapper.selectCount(applyQuery);

        wsInitData.setApplyCount(applyCount);

        //发送消息-wsinit消息其实就是用户成功登录后，接收到的  上次离线后才接收到的新消息，要将这样的消息推送给用户
        MessageSendDTO messageSendDTO = new MessageSendDTO();
        messageSendDTO.setMessageType(MessageTypeEnum.INIT.getType());
        messageSendDTO.setContactId(userId);
        messageSendDTO.setExtendData(wsInitData);
        sendMsg(messageSendDTO, userId);
    }

    public static void sendMsg(MessageSendDTO messageSendDTO, String receiverId) {
        if (receiverId == null) {
            return;
        }
        Channel sendChannel = USER_CONTEXT_MAP.get(receiverId);
        if (sendChannel == null) {
            return;
        }
        // 相对于客户端而言，联系人就是发送人，所以这里将发送人信息复制到接收人信息中
        messageSendDTO.setContactId(messageSendDTO.getSendUserId());
        messageSendDTO.setContactName(messageSendDTO.getSendUserNickName());
        sendChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObj2Json(messageSendDTO)));
    }

    /**
     * 将指定的 Channel 加入到指定的 ChannelGroup 中。
     *
     * @param groupId 群组的唯一标识符
     * @param channel 要加入群组的 Channel
     *                <p>
     *                1. 如果 groupId 对应的 ChannelGroup 不存在，则创建一个新的 ChannelGroup 并存入 GROUP_CONCURRENT_MAP。
     *                2. 检查传入的 Channel 是否为 null，如果为 null 则直接返回。
     *                3. 将 Channel 添加到对应的 ChannelGroup 中。
     */
    private void add2Group(String groupId, Channel channel) {
        ChannelGroup group = GROUP_CONCURRENT_MAP.get(groupId);
        if (group == null) {
            group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            GROUP_CONCURRENT_MAP.put(groupId, group);
        }
        if (channel == null) {
            return;
        }
        group.add(channel);
    }

    /**
     * 用户离线时，从 ChannelGroup 中移除用户对应的 Channel，更新最后下线时间
     *
     * @param channel
     */
    public void removeContext(Channel channel) {

        Attribute<String> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        String userId = attribute.get();
        if (!StringTools.isEmpty(userId)) {
            USER_CONTEXT_MAP.remove(userId);
        }
        redisComponent.removeUserHeartBeat(userId);
        //更新用户最后下线时间
        UserInfo userInfo = new UserInfo();
        userInfo.setLastOffTime(System.currentTimeMillis());
        userInfoMapper.updateByUserId(userInfo, userId);
    }
}
