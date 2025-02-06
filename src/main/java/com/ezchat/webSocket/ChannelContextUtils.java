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
import com.ezchat.mappers.ChatSessionUserMapper;
import com.ezchat.mappers.UserContactApplyMapper;
import com.ezchat.mappers.UserInfoMapper;
import com.ezchat.redis.RedisComponent;

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
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ws通道工具类
 */
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
    private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;

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

        // 创建或获取 AttributeKey，绑定用户 ID--在 Netty 框架中，AttributeKey<T> 是一个 通道（Channel）级别的属性存储机制，用于在 Channel 上存储和获取与之相关的 自定义数据。

        //AttributeKey<T> 主要用于给 Channel 绑定用户 ID、会话信息等，在整个连接生命周期中可以随时访问。
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
         * SELECT
         * 	u.*,
         * 	c.last_message lastMessage,
         * 	c.last_receive_time lastReceiveTime,
         * CASE
         *
         * 		WHEN substring( contact_id, 1, 1 )= 'G' THEN
         * 		( SELECT count( 1 ) FROM user_contact uc WHERE uc.contact_id = u.contact_id ) ELSE 0
         * 	END memberCount
         * FROM
         * 	chat_session_user u
         * 	INNER JOIN chat_session c ON c.session_id = u.session_id
         * WHERE
         * 	user_id = ?
         * ORDER BY
         * 	last_receive_time DESC
         */
        ChatSessionUserQuery sessionUserQuery = new ChatSessionUserQuery();
        sessionUserQuery.setUserId(userId);
        sessionUserQuery.setOrderBy("last_receive_time desc");
        List<ChatSessionUser> chatSessionUserList = chatSessionUserMapper.selectList(sessionUserQuery);

        WsInitData wsInitData = new WsInitData();
        wsInitData.setChatSessionList(chatSessionUserList);

        /**
         * 2. 查询用户每个会话三天内聊天消息：即查询chatMessage表中contactId是userId和userID加入群组groupId的聊天记录
         * SELECT
         * 	message_id,
         * 	session_id,
         * 	message_type,
         * 	message_content,
         * 	send_user_id,
         * 	send_user_nick_name,
         * 	send_time,
         * 	contact_id,
         * 	contact_type,
         * 	file_size,
         * 	file_name,
         * 	file_type,
         * STATUS
         * FROM
         * 	chat_message
         * WHERE
         * 	send_time >= ?
         * 	AND contact_id IN (?,?,?,?)
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
         * SELECT
         * 	count( 1 )
         * FROM
         * 	user_contact_apply a
         * WHERE
         * 	receive_user_id = ?
         * 	AND a.STATUS = ?
         * 	AND last_apply_time >= ?
         */
        UserContactApplyQuery applyQuery = new UserContactApplyQuery();
        applyQuery.setReceiveUserId(userId);
        applyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
        //只查询离线之后收到的好友申请
        applyQuery.setLastApplyTimeStamp(lastLogOffTime);
        Integer applyCount = userContactApplyMapper.selectCount(applyQuery);

        wsInitData.setApplyCount(applyCount);

        //发送消息-wsinit消息其实就是用户成功登录后，接收到的  “上次离线后才接收到的新消息”   ，要将这样的消息推送给用户
        MessageSendDTO messageSendDTO = new MessageSendDTO();
        messageSendDTO.setMessageType(MessageTypeEnum.INIT.getType());
        messageSendDTO.setContactId(userId);
        messageSendDTO.setExtendData(wsInitData);
        sendMsg(messageSendDTO, userId);
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

    public void addUser2Group(String userId, String groupId) {
        Channel channel = USER_CONTEXT_MAP.get(userId);
        add2Group(groupId, channel);
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

    public void sendMessage(MessageSendDTO messageSendDTO) {
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(messageSendDTO.getContactId());
        switch (contactTypeEnum) {
            case USER:
                send2User(messageSendDTO);
                break;
            case GROUP:
                send2Group(messageSendDTO);
                break;
        }
    }

    //发送给用户
    private void send2User(MessageSendDTO messageSendDTO) {
        String contactId = messageSendDTO.getContactId();
        if (StringTools.isEmpty(contactId)) {
            return;
        }
        sendMsg(messageSendDTO, contactId);
        //强制下线
        if (MessageTypeEnum.FORCE_OFF_LINE.getType().equals(messageSendDTO.getMessageType())){
            //关闭通道
            closeContext(contactId);
        }
    }

    //发送给群组
    private void send2Group(MessageSendDTO messageSendDTO) {
        String contactId = messageSendDTO.getContactId();
        if (StringTools.isEmpty(contactId)) {
            return;
        }
        ChannelGroup channelGroup = GROUP_CONCURRENT_MAP.get(contactId);
        if (channelGroup == null){
            return;
        }
        channelGroup.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObj2Json(messageSendDTO)));
    }

    // 发送消息
    public void sendMsg(MessageSendDTO messageSendDTO, String receiverId) {
        Channel userChannel = USER_CONTEXT_MAP.get(receiverId);
        if (userChannel == null) {
            return;
        }
        // 申请人发送的好友消息直接渲染到自己的消息列表
        if (MessageTypeEnum.ADD_FRIEND_SELF.getType().equals(messageSendDTO.getMessageType())){
            UserInfo userInfo = (UserInfo) messageSendDTO.getExtendData();
            messageSendDTO.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
            messageSendDTO.setContactId(userInfo.getUserId());
            messageSendDTO.setContactName(userInfo.getNickName());
            messageSendDTO.setExtendData(null);
        }else {
            // A -> B 的信息，B 的 客户端会收到 A 的信息，对客户端来说，B 收到的消息 contactId 应该是 A 的 ID，contactName 应该是 A 的昵称,好友申请的时候不处理
            messageSendDTO.setContactId(messageSendDTO.getSendUserId());
            messageSendDTO.setContactName(messageSendDTO.getSendUserNickName());
        }
        userChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObj2Json(messageSendDTO)));
    }

    /**
     * 关闭通道
     * @param userId
     */
    public void closeContext(String userId){
        if (StringTools.isEmpty(userId)){
            return;
        }
        redisComponent.cleanUserTokenByUserId(userId);
        Channel channel = USER_CONTEXT_MAP.get(userId);
        if (channel == null){
            return;
        }
        channel.close();
    }


}
