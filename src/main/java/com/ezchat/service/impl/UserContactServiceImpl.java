package com.ezchat.service.impl;

import com.ezchat.constans.Constans;
import com.ezchat.entity.dto.MessageSendDTO;
import com.ezchat.entity.dto.SysSettingDTO;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.dto.UserContactSearchResultDTO;
import com.ezchat.entity.po.*;
import com.ezchat.entity.query.*;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.enums.*;
import com.ezchat.exception.BusinessException;
import com.ezchat.mappers.*;
import com.ezchat.redis.RedisComponent;
import com.ezchat.service.UserContactService;
import com.ezchat.utils.CopyUtils;
import com.ezchat.utils.StringTools;
import com.ezchat.webSocket.ChannelContextUtils;
import com.ezchat.webSocket.MessageHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Description:联系人Service
 * @author:xiuyuan
 * @date:2024/12/16
 */
@Service("userContactService")
public class UserContactServiceImpl implements UserContactService {

    @Resource
    private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;

    @Resource
    private MessageHandler messageHandler;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;

    @Resource
    private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;

    @Resource
    private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;



    /**
     * 根据条件查询列表
     */
    public List<UserContact> findListByParam(UserContactQuery query) {
        return this.userContactMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    public Integer findCountByParam(UserContactQuery query) {
        return this.userContactMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    public PaginationResultVO<UserContact> findListByPage(UserContactQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<UserContact> list = this.findListByParam(query);
        PaginationResultVO<UserContact> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    public Integer add(UserContact bean) {
        return this.userContactMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    public Integer addBatch(List<UserContact> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userContactMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或更新
     */
    public Integer addOrUpdateBatch(List<UserContact> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userContactMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据UserIdAndContactId查询数据
     */
    public UserContact getUserContactByUserIdAndContactId(String userId, String contactId) {
        return this.userContactMapper.selectByUserIdAndContactId(userId, contactId);
    }

    /**
     * 根据UserIdAndContactId更新数据
     */
    public Integer updateUserContactByUserIdAndContactId(UserContact bean, String userId, String contactId) {
        return this.userContactMapper.updateByUserIdAndContactId(bean, userId, contactId);
    }

    /**
     * 根据UserIdAndContactId删除数据
     */
    public Integer deleteUserContactByUserIdAndContactId(String userId, String contactId) {
        return this.userContactMapper.deleteByUserIdAndContactId(userId, contactId);
    }

    /**
     * 搜索联系人
     *
     * @param userId
     * @param contactId
     * @return
     */
    @Override
    public UserContactSearchResultDTO searchContact(String userId, String contactId) {
        //查询想要添加的联系人信息
        UserContactTypeEnum typeEnum = UserContactTypeEnum.getByPrefix(contactId);
        if (null == typeEnum) {
            return null;
        }
        UserContactSearchResultDTO resultDTO = new UserContactSearchResultDTO();
        switch (typeEnum) {
            case USER:
                UserInfo userInfo = this.userInfoMapper.selectByUserId(contactId);
                if (null == userInfo) {
                    return null;
                }
                resultDTO = CopyUtils.copy(userInfo, UserContactSearchResultDTO.class);
                break;
            case GROUP:
                GroupInfo groupInfo = this.groupInfoMapper.selectByGroupId(contactId);
                if (null == groupInfo) {
                    return null;
                }
                resultDTO.setNickName(groupInfo.getGroupName());
                break;
        }
        resultDTO.setContactType(typeEnum.toString());
        resultDTO.setContactId(contactId);

        if (userId.equals(contactId)) {
            resultDTO.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            return resultDTO;
        }
        //查询是否是好友
        UserContact userContact = this.getUserContactByUserIdAndContactId(userId, contactId);
        resultDTO.setStatus(userContact == null ? null : userContact.getStatus());
        return resultDTO;
    }

    /**
     * 添加联系人
     *
     * @param applyUserID
     * @param receivedUserID
     * @param contactId
     * @param contactType
     * @param applyInfo
     */
    @Override
    public void addContact(String applyUserID, String receivedUserID, String contactId, Integer contactType, String applyInfo) throws BusinessException {
        //查询群聊人数是否超过限制
        if (UserContactTypeEnum.GROUP.getType().equals(contactType)) {
            UserContactQuery userContactQuery = new UserContactQuery();
            userContactQuery.setContactId(contactId);
            userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            Integer count = userContactMapper.selectCount(userContactQuery);
            SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
            if (count >= sysSettingDTO.getMaxGroupCount()) {
                throw new BusinessException("群聊人数已满，无法添加");
            }
        }
        Date currentDate = new Date();
        //如果同意好友，双方添加好友关系
        List<UserContact> contactList = new ArrayList<>();
        //申请人添加对方
        UserContact userContact = new UserContact();
        userContact.setUserId(applyUserID);
        userContact.setContactId(contactId);
        userContact.setContactType(contactType);
        userContact.setCreateTime(currentDate);
        userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        userContact.setLastUpdateTime(currentDate);
        contactList.add(userContact);
        //接收人添加申请人,群组时不用添加好友关系
        if (UserContactTypeEnum.USER.getType().equals(contactType)) {
            userContact = new UserContact();
            userContact.setUserId(receivedUserID);
            userContact.setContactId(applyUserID);
            userContact.setContactType(contactType);
            userContact.setCreateTime(currentDate);
            userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            userContact.setLastUpdateTime(currentDate);
            contactList.add(userContact);
        }
        //批量插入
        this.userContactMapper.insertOrUpdateBatch(contactList);
        //申请人添加对方为好友，对方有可能是群组或者用户，添加缓存
        redisComponent.addUserContact(applyUserID, contactId);
        // 如果是被申请人是用户，接收人也添加申请人为好友，添加缓存
        if (UserContactTypeEnum.USER.getType().equals(contactType)){
            redisComponent.addUserContact(receivedUserID,applyUserID);
        }
        // 创建会话
        String sessionId = null;
        //如果是两个用户之间的会话
        if (UserContactTypeEnum.USER.getType().equals(contactType)){
            sessionId = StringTools.getChatSessionId4User(new String[]{applyUserID,receivedUserID});
        }else {
            //如果是群组之间的会话
            sessionId = StringTools.getChatSessionId4Group(contactId);
        }

        List<ChatSessionUser> chatSessionUserList = new ArrayList<>();

        if (UserContactTypeEnum.USER.getType().equals(contactType)){
            //创建会话
            //对chatSession表的操作
            ChatSession chatSession = new ChatSession();
            chatSession.setSessionId(sessionId);
            chatSession.setLastMessage(applyInfo);
            chatSession.setLastReceiveTime(currentDate.getTime());
            chatSessionMapper.insertOrUpdate(chatSession);

            //对chatSessionUser表的操作
            //申请方session
            ChatSessionUser applySessionUser = new ChatSessionUser();
            applySessionUser.setUserId(applyUserID);
            applySessionUser.setContactId(receivedUserID);
            applySessionUser.setSessionId(sessionId);
            UserInfo contactUser = this.userInfoMapper.selectByUserId(receivedUserID);
            applySessionUser.setContactName(contactUser.getNickName());
            chatSessionUserList.add(applySessionUser);
            //接收方session
            ChatSessionUser receivedSessionUser = new ChatSessionUser();
            receivedSessionUser.setUserId(receivedUserID);
            receivedSessionUser.setContactId(applyUserID);
            receivedSessionUser.setSessionId(sessionId);
            UserInfo applyUser = this.userInfoMapper.selectByUserId(applyUserID);
            receivedSessionUser.setContactName(applyUser.getNickName());
            chatSessionUserList.add(receivedSessionUser);
            chatSessionUserMapper.insertOrUpdateBatch(chatSessionUserList);

            //对chatMessage表的操作
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(sessionId);
            chatMessage.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
            chatMessage.setMessageContent(applyInfo);
            chatMessage.setSendUserId(applyUserID);
            chatMessage.setSendUserNickName(applyUser.getNickName());
            chatMessage.setSendTime(currentDate.getTime());
            chatMessage.setContactId(receivedUserID);
            chatMessage.setContactType(UserContactTypeEnum.USER.getType());
            chatMessageMapper.insert(chatMessage);

            //发送消息
            MessageSendDTO messageSendDTO = CopyUtils.copy(chatMessage, MessageSendDTO.class);
            //发送给接收人
            messageHandler.sendMessage(messageSendDTO);
            //发送给申请人，这时候发送人就是接收人，联系人就是申请人
            messageSendDTO.setMessageType(MessageTypeEnum.ADD_FRIEND_SELF.getType());
            messageSendDTO.setContactId(applyUserID);
            messageSendDTO.setExtendData(contactUser);
            messageHandler.sendMessage(messageSendDTO);
        }else {

        }
    }


    /**
     * 删除或者拉黑好友
     *
     * @param userId
     * @param contactId
     * @param statusEnum
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserContact(String userId, String contactId, UserContactStatusEnum statusEnum) {
        //移除好友
        UserContact userContact = new UserContact();
        userContact.setStatus(statusEnum.getStatus());
        userContactMapper.updateByUserIdAndContactId(userContact, userId, contactId);

        //将好友的联系人也移除自己
        UserContact friendContact = new UserContact();
        if (UserContactStatusEnum.DEL == statusEnum) {
            //被删除
            friendContact.setStatus(UserContactStatusEnum.DEL_BE.getStatus());
        } else if (UserContactStatusEnum.BLACKLIST == statusEnum) {
            //被拉黑
            friendContact.setStatus(UserContactStatusEnum.BLACKLIST_BE.getStatus());
        }
        userContactMapper.updateByUserIdAndContactId(friendContact, contactId, userId);
        //todo 从我的好友列表缓存中删除好友
        //todo 从好友列表缓存中删除我
    }

    /**
     * 注册时添加机器人好友
     *
     * @param userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addContact4Robot(String userId) {
        Date currentDate = new Date();
        SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
        String robotContactId = sysSettingDTO.getRobotUid();
        String robotNickName = sysSettingDTO.getRobotNickName();
        String sendMessage = sysSettingDTO.getRobotWelcome();
        sendMessage = StringTools.cleanHtmlTag(sendMessage);
        //添加机器人好友
        UserContact userContact = new UserContact();
        userContact.setUserId(userId);
        userContact.setContactId(robotContactId);
        userContact.setContactType(UserContactTypeEnum.USER.getType());
        userContact.setCreateTime(currentDate);
        userContact.setLastUpdateTime(currentDate);
        userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        userContactMapper.insert(userContact);
        //增加会话信息
        String sessionId = StringTools.getChatSessionId4User(new String[]{userId, robotContactId});
        ChatSession chatSession = new ChatSession();
        chatSession.setSessionId(sessionId);
        chatSession.setLastMessage(sendMessage);
        chatSession.setLastReceiveTime(currentDate.getTime());
        chatSessionMapper.insert(chatSession);
        //增加会话用户信息
        ChatSessionUser chatSessionUser = new ChatSessionUser();
        chatSessionUser.setUserId(userId);
        chatSessionUser.setContactId(robotContactId);
        chatSessionUser.setContactName(robotNickName);
        chatSessionUser.setSessionId(sessionId);
        chatSessionUserMapper.insert(chatSessionUser);
        //增加聊天消息
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
        chatMessage.setMessageContent(sendMessage);
        chatMessage.setSendUserId(robotContactId);
        chatMessage.setSendUserNickName(robotNickName);
        chatMessage.setSendTime(currentDate.getTime());
        chatMessage.setContactId(userId);
        chatMessage.setContactType(UserContactTypeEnum.USER.getType());
        chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
        chatMessageMapper.insert(chatMessage);

    }
}
