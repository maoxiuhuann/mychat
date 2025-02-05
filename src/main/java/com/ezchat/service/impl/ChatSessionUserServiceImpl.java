package com.ezchat.service.impl;

import com.ezchat.entity.dto.MessageSendDTO;
import com.ezchat.entity.po.UserContact;
import com.ezchat.entity.query.SimplePage;
import com.ezchat.entity.query.UserContactQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.ChatSessionUser;
import com.ezchat.entity.query.ChatSessionUserQuery;
import com.ezchat.enums.MessageTypeEnum;
import com.ezchat.enums.PageSize;
import com.ezchat.enums.UserContactStatusEnum;
import com.ezchat.enums.UserContactTypeEnum;
import com.ezchat.mappers.ChatSessionUserMapper;
import com.ezchat.mappers.UserContactMapper;
import com.ezchat.service.ChatSessionUserService;
import com.ezchat.webSocket.MessageHandler;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description:会话用户Service
 * @author:xiuyuan
 * @date:2025/01/06
 */
@Service("chatSessionUserService")
public class ChatSessionUserServiceImpl implements ChatSessionUserService {

	@Resource
	private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;

	@Resource
	private MessageHandler messageHandler;
    @Autowired
    private UserContactMapper userContactMapper;

	/**
	 * 根据条件查询列表
	 */
	public List<ChatSessionUser> findListByParam(ChatSessionUserQuery query) {
		return this.chatSessionUserMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(ChatSessionUserQuery query) {
		return this.chatSessionUserMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<ChatSessionUser> findListByPage(ChatSessionUserQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<ChatSessionUser> list = this.findListByParam(query);
		PaginationResultVO<ChatSessionUser> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(ChatSessionUser bean) {
		return this.chatSessionUserMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<ChatSessionUser> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatSessionUserMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或更新
	 */
	public Integer addOrUpdateBatch(List<ChatSessionUser> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatSessionUserMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据UserIdAndContactId查询数据
	 */
	public ChatSessionUser getChatSessionUserByUserIdAndContactId(String userId, String contactId) {
		return this.chatSessionUserMapper.selectByUserIdAndContactId(userId, contactId);
	}

	/**
	 * 根据UserIdAndContactId更新数据
	 */
	public Integer updateChatSessionUserByUserIdAndContactId(ChatSessionUser bean, String userId, String contactId) {
		return this.chatSessionUserMapper.updateByUserIdAndContactId(bean,userId, contactId);
	}

	/**
	 * 根据UserIdAndContactId删除数据
	 */
	@Override
	public Integer deleteChatSessionUserByUserIdAndContactId(String userId, String contactId) {
		return this.chatSessionUserMapper.deleteByUserIdAndContactId(userId, contactId);
	}

	/**
	 * 更新冗余信息、修改昵称发送ws消息-实时更新昵称
	 * @param contactName
	 * @param contactId
	 */
	public void updateRedundanceInfo(String contactName,String contactId){
		ChatSessionUser updateInfo = new ChatSessionUser();
		updateInfo.setContactName(contactId);
		ChatSessionUserQuery chatSessionUserQuery = new ChatSessionUserQuery();
		chatSessionUserQuery.setContactId(contactId);
		chatSessionUserMapper.updateByQuery(updateInfo,chatSessionUserQuery);
		UserContactTypeEnum userContactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
		if (userContactTypeEnum == userContactTypeEnum.GROUP){
			MessageSendDTO messageSendDTO = new MessageSendDTO();
			messageSendDTO.setContactType(UserContactTypeEnum.getByPrefix(contactId).getType());
			messageSendDTO.setContactId(contactId);
			messageSendDTO.setExtendData(contactName);
			messageSendDTO.setMessageType(MessageTypeEnum.CONTEXT_NAME_UPDATE.getType());
			messageHandler.sendMessage(messageSendDTO);
		}else {
			UserContactQuery userContactQuery = new UserContactQuery();
			userContactQuery.setContactType(UserContactTypeEnum.USER.getType());
			userContactQuery.setContactId(contactId);
			userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			List<UserContact> userContactList = userContactMapper.selectList(userContactQuery);
			for (UserContact userContact : userContactList){
				MessageSendDTO messageSendDTO = new MessageSendDTO();
				messageSendDTO.setContactType(userContactTypeEnum.getType());
				messageSendDTO.setContactId(userContact.getUserId());
				messageSendDTO.setExtendData(contactName);
				messageSendDTO.setMessageType(MessageTypeEnum.CONTEXT_NAME_UPDATE.getType());
				messageSendDTO.setSendUserId(contactId);
				messageSendDTO.setSendUserNickName(contactName);
				messageHandler.sendMessage(messageSendDTO);
			}
		}
	}
}
