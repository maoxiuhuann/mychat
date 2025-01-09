package com.ezchat.entity.po;

import com.ezchat.enums.UserContactTypeEnum;

import java.io.Serializable;


/**
 * @Description:会话用户
 * @author:xiuyuan
 * @date:2025/01/06
 */
public class ChatSessionUser implements Serializable {
	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 联系人ID
	 */
	private String contactId;

	/**
	 * 会话ID
	 */
	private String sessionId;

	/**
	 * 联系人名称
	 */
	private String contactName;

	/**
	 * 会话历史消息
	 */
	private String lastMessage;

	/**
	 * 最后接收时间
	 */
	private String lastReceiveTime;

	/**
	 * 群成员数量
	 */
	private Integer memberCount;

	/**
	 * 联系人类型
	 */
	private Integer contactType;


	public String getLastMessage() {
		return lastMessage;
	}

	public void setLastMessage(String lastMessage) {
		this.lastMessage = lastMessage;
	}

	public String getLastReceiveTime() {
		return lastReceiveTime;
	}

	public void setLastReceiveTime(String lastReceiveTime) {
		this.lastReceiveTime = lastReceiveTime;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return this.userId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public String getContactId() {
		return this.contactId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getSessionId() {
		return this.sessionId;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public String getContactName() {
		return this.contactName;
	}

	public Integer getMemberCount() {
		return memberCount;
	}

	public void setMemberCount(Integer memberCount) {
		this.memberCount = memberCount;
	}

	public Integer getContactType() {
		return UserContactTypeEnum.getByPrefix(contactId).getType();
	}

	public void setContactType(Integer contactType) {
		this.contactType = contactType;
	}

	@Override
	public String toString() {
		return "用户ID:" + (userId == null ? "空" : userId) + ",联系人ID:" + (contactId == null ? "空" : contactId) + ",会话ID:" + (sessionId == null ? "空" : sessionId) + ",联系人名称:" + (contactName == null ? "空" : contactName);
	}
}