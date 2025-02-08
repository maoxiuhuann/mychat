package com.ezchat.service.impl;

import com.ezchat.constans.Constans;
import com.ezchat.entity.config.AppConfig;
import com.ezchat.entity.dto.MessageSendDTO;
import com.ezchat.entity.dto.SysSettingDTO;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.po.*;
import com.ezchat.entity.query.*;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.enums.*;
import com.ezchat.exception.BusinessException;
import com.ezchat.mappers.*;
import com.ezchat.redis.RedisComponent;
import com.ezchat.service.GroupInfoService;
import com.ezchat.utils.CopyUtils;
import com.ezchat.utils.StringTools;
import com.ezchat.webSocket.ChannelContextUtils;
import com.ezchat.webSocket.MessageHandler;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import org.omg.CORBA.PRIVATE_MEMBER;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @Description:Service
 * @author:xiuyuan
 * @date:2024/12/16
 */
@Service("groupInfoService")
public class GroupInfoServiceImpl implements GroupInfoService {

    @Resource
    private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;

    @Autowired
    private RedisComponent redisComponent;

    @Autowired
    private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

    @Autowired
    private AppConfig appConfig;

    @Resource
    private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;

    @Resource
    private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;

    @Resource
    private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;

    @Resource
    private ChannelContextUtils channelContextUtils;
    @Autowired
    private MessageHandler messageHandler;
    @Autowired
    private ChatSessionUserServiceImpl chatSessionUserService;
    @Autowired
    private UserContactServiceImpl userContactService;
    @Autowired
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Autowired
    @Lazy
    private GroupInfoService groupInfoService;

    /**
     * 根据条件查询列表
     */
    public List<GroupInfo> findListByParam(GroupInfoQuery query) {
        return this.groupInfoMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    public Integer findCountByParam(GroupInfoQuery query) {
        return this.groupInfoMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    public PaginationResultVO<GroupInfo> findListByPage(GroupInfoQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<GroupInfo> list = this.findListByParam(query);
        PaginationResultVO<GroupInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    public Integer add(GroupInfo bean) {
        return this.groupInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    public Integer addBatch(List<GroupInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.groupInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或更新
     */
    public Integer addOrUpdateBatch(List<GroupInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.groupInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据GroupId查询数据
     */
    public GroupInfo getGroupInfoByGroupId(String groupId) {
        return this.groupInfoMapper.selectByGroupId(groupId);
    }

    /**
     * 根据GroupId更新数据
     */
    public Integer updateGroupInfoByGroupId(GroupInfo bean, String groupId) {
        return this.groupInfoMapper.updateByGroupId(bean, groupId);
    }

    /**
     * 根据GroupId删除数据
     */
    public Integer deleteGroupInfoByGroupId(String groupId) {
        return this.groupInfoMapper.deleteByGroupId(groupId);
    }

    /**
     * 保存群组信息-新增或修改
     *
     * @param groupInfo
     * @param avatarFile
     * @param avatarCover
     */
    @Override
    @Transactional
    public void saveGroup(GroupInfo groupInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException, BusinessException {

        Date currentDate = new Date();

        //新增
        if (StringTools.isEmpty(groupInfo.getGroupId())) {
            GroupInfoQuery query = new GroupInfoQuery();
            query.setGroupOwnerId(groupInfo.getGroupOwnerId());
            //查询群主已经创建的群数量
            Integer count = this.groupInfoMapper.selectCount(query);
            SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
            if (count >= sysSettingDTO.getMaxGroupCount()) {
                throw new BusinessException("最多只能创建" + sysSettingDTO.getMaxGroupCount() + "个群聊，群组数量已达上限，无法创建新的群组！");
            }

            if (null == avatarFile) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }

            groupInfo.setCreateTime(currentDate);
            groupInfo.setGroupId(StringTools.getGroupId());
            this.groupInfoMapper.insert(groupInfo);

            //将群组添加为联系人
            UserContact userContact = new UserContact();
            userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            userContact.setContactType(UserContactTypeEnum.GROUP.getType());
            userContact.setContactId(groupInfo.getGroupId());
            userContact.setUserId(groupInfo.getGroupOwnerId());
            userContact.setCreateTime(currentDate);
            userContact.setLastUpdateTime(currentDate);
            this.userContactMapper.insert(userContact);

            // 创建会话
            String sessionId = StringTools.getChatSessionId4Group(groupInfo.getGroupId());
            ChatSession chatSession = new ChatSession();
            chatSession.setSessionId(sessionId);
            chatSession.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
            chatSession.setLastReceiveTime(currentDate.getTime());
            chatSessionMapper.insertOrUpdate(chatSession);

            //创建群主会话
            ChatSessionUser chatSessionUser = new ChatSessionUser();
            chatSessionUser.setSessionId(sessionId);
            chatSessionUser.setUserId(groupInfo.getGroupOwnerId());
            chatSessionUser.setContactId(groupInfo.getGroupId());
            chatSessionUser.setContactName(groupInfo.getGroupName());
            chatSessionUserMapper.insert(chatSessionUser);

            //创建消息
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(sessionId);
            chatMessage.setMessageType(MessageTypeEnum.GROUP_CREATE.getType());
            chatMessage.setMessageContent(MessageTypeEnum.GROUP_CREATE.getInitMessage());
            chatMessage.setSendTime(currentDate.getTime());
            chatMessage.setContactId(groupInfo.getGroupId());
            chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
            chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
            chatMessageMapper.insert(chatMessage);
            //将群组添加到联系人缓存中
            redisComponent.addUserContact(groupInfo.getGroupOwnerId(), groupInfo.getGroupId());
            //将群主加入群组通道
            channelContextUtils.addUser2Group(groupInfo.getGroupOwnerId(), groupInfo.getGroupId());

            // 发送ws欢迎消息
            chatSessionUser.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
            chatSessionUser.setLastReceiveTime(currentDate.getTime());
            chatSessionUser.setMemberCount(1);

            MessageSendDTO messageSendDTO = CopyUtils.copy(chatMessage, MessageSendDTO.class);
            messageSendDTO.setExtendData(chatSessionUser);
            messageSendDTO.setLastMessage(chatSessionUser.getLastMessage());
            messageHandler.sendMessage(messageSendDTO);
        } else {
            //修改
            GroupInfo dbInfo = this.groupInfoMapper.selectByGroupId(groupInfo.getGroupId());
            //判断是否为群主-非群主无法修改群信息
            if (!dbInfo.getGroupOwnerId().equals(groupInfo.getGroupOwnerId())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            this.groupInfoMapper.updateByGroupId(groupInfo, groupInfo.getGroupId());
            //更新冗余信息、修改昵称发送ws消息-实时更新昵称
            String contactNameUpdate = null;
            if (!dbInfo.getGroupName().equals(groupInfo.getGroupName())){
                contactNameUpdate = groupInfo.getGroupName();
            }
            if (contactNameUpdate == null){
                return;
            }
            chatSessionUserService.updateRedundanceInfo(contactNameUpdate, groupInfo.getGroupId());
        }
        if (null == avatarCover){
            return;
        }
        String baseFolder = appConfig.getProjectFolder() + Constans.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + Constans.FILE_FOLDER_AVATAR_NAME);
        if (!targetFileFolder.exists()){
            targetFileFolder.mkdirs();
        }
        //可以根据groupId生成唯一的文件名，所以不存数据库
        String filePath = targetFileFolder.getPath() + "/" + groupInfo.getGroupId() + Constans.IMAGE_SUFFIX;

        avatarFile.transferTo(new File(filePath));
        avatarCover.transferTo(new File(filePath + Constans.COVER_IMAGE_SUFFIX));
    }

    /**
     * 解散群组
     * @param groupOwnerId
     * @param groupId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolutionGroup(String groupOwnerId, String groupId) throws BusinessException {
        GroupInfo dbInfo = groupInfoMapper.selectByGroupId(groupId);
        //判断是否为群主-非群主无法解散群组
        if (null == dbInfo || !dbInfo.getGroupOwnerId().equals(groupOwnerId)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //删除群组
        GroupInfo updateInfo = new GroupInfo();
        updateInfo.setStatus(GroupStatusEnum.DISSOLUTION.status);
        groupInfoMapper.updateByGroupId(updateInfo, groupId);
        //删除群组联系人
        UserContactQuery userContactQuery = new UserContactQuery();
        //条件
        userContactQuery.setContactId(groupId);
        userContactQuery.setContactType(UserContactTypeEnum.GROUP.getType());
        //更新信息
        UserContact updateUserContact = new UserContact();
        updateUserContact.setStatus(UserContactStatusEnum.DEL.getStatus());

        // 移除相关群成员的联系人缓存
        userContactMapper.updateByParam(updateUserContact, userContactQuery);
        List<UserContact> userContactList = userContactMapper.selectList(userContactQuery);
        for (UserContact userContact : userContactList) {
            redisComponent.removeUserContact(userContact.getUserId(),userContact.getContactId());
        }
        String sessionId = StringTools.getChatSessionId4Group(groupId);
        Date currentDate = new Date();
        //更新会话
        String messageContent = MessageTypeEnum.DISSOLUTION_GROUP.getInitMessage();
        ChatSession chatSession = new ChatSession();
        chatSession.setLastMessage(messageContent);
        chatSession.setLastReceiveTime(currentDate.getTime());
        chatSessionMapper.updateBySessionId(chatSession, sessionId);
        //更新群消息
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setSendTime(currentDate.getTime());
        chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
        chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
        chatMessage.setMessageType(MessageTypeEnum.DISSOLUTION_GROUP.getType());
        chatMessage.setContactId(groupId);
        chatMessage.setMessageContent(messageContent);
        chatMessageMapper.insert(chatMessage);
        // 发送ws解散通知消息
        MessageSendDTO messageSendDTO = CopyUtils.copy(chatMessage, MessageSendDTO.class);
        messageHandler.sendMessage(messageSendDTO);
    }

    /**
     * 操作群成员
     * @param tokenUserInfoDTO
     * @param groupId
     * @param contactIds
     * @param opType
     */
    @Override
    public void addOrUpdateGroupMember(TokenUserInfoDTO tokenUserInfoDTO, String groupId, String contactIds, Integer opType) throws BusinessException {
        GroupInfo groupInfo = groupInfoMapper.selectByGroupId(groupId);
        //判断是否为群主-非群主无法操作群成员
        if (null == groupInfo || !groupInfo.getGroupOwnerId().equals(tokenUserInfoDTO.getUserId())){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String[] contactIdList = contactIds.split(",");
        for (String contactId : contactIdList){
            if (GroupControlOpTypeEnum.REMOVE_USER.getType().equals(opType)){
                //删除群成员
                //this.leaveGroup(contactId,groupId,MessageTypeEnum);这样调用事务失效
                groupInfoService.leaveGroup(contactId,groupId,MessageTypeEnum.REMOVE_GROUP);

            }else {
                //添加群成员
                userContactService.addContact(contactId,null,groupId,UserContactTypeEnum.GROUP.getType(),null);
            }
        }
    }

    /**
     * 离开群聊
     * @param userId
     * @param groupId
     * @param messageTypeEnum
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(String userId, String groupId, MessageTypeEnum messageTypeEnum) throws BusinessException {
        GroupInfo groupInfo = groupInfoMapper.selectByGroupId(groupId);
        if (null == groupInfo){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (userId.equals(groupInfo.getGroupOwnerId())){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        Integer count = userContactMapper.deleteByUserIdAndContactId(userId,groupId);
        if (count == 0){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserInfo userInfo = userInfoMapper.selectByUserId(userId);

        String sessionId = StringTools.getChatSessionId4Group(groupId);
        Date currentDate = new Date();
        String messageContent = String.format(messageTypeEnum.getInitMessage(), userInfo.getNickName());

        ChatSession chatSession = new ChatSession();
        chatSession.setLastMessage(messageContent);
        chatSession.setLastReceiveTime(currentDate.getTime());
        chatSessionMapper.updateBySessionId(chatSession, sessionId);

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setSendTime(currentDate.getTime());
        chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
        chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
        chatMessage.setMessageType(messageTypeEnum.getType());
        chatMessage.setContactId(groupId);
        chatMessage.setMessageContent(messageContent);
        chatMessageMapper.insert(chatMessage);

        UserContactQuery userContactQuery = new UserContactQuery();
        userContactQuery.setContactId(groupId);
        userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        Integer memberCount = userContactMapper.selectCount(userContactQuery);
        MessageSendDTO messageSendDTO = CopyUtils.copy(chatMessage, MessageSendDTO.class);
        messageSendDTO.setExtendData(userId);
        messageSendDTO.setMemberCount(memberCount);
        messageHandler.sendMessage(messageSendDTO);
    }
}
