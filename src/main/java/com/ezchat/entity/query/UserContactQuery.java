package com.ezchat.entity.query;

import java.util.Date;


/**
 * @Description:联系人查询对象
 * @author:xiuyuan
 * @date:2024/12/16
 */
public class UserContactQuery extends BaseQuery {
	/**
	 * 用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 联系人ID或者群组ID
	 */
	private String contactId;

	private String contactIdFuzzy;

	/**
	 * 联系人类型 0:好友 1:群组
	 */
	private Integer contactType;

	/**
	 * 创建时间
	 */
	private Date createTime;

	private String createTimeStart;

	private String createTimeEnd;

	/**
	 * 状态 0:非好友 1:好友 2:已删除好友 3:被好友删除 4:已拉黑好友 5:被好友拉黑
	 */
	private Integer status;

	/**
	 * 最后更新时间
	 */
	private Date lastUpdateTime;

	/**
	 * 是否查询用户信息
	 */
	private Boolean queryUserInfo;

	/**
	 * 是否查询群组信息
	 */
	private Boolean queryGroupInfo;

	/**
	 * 是否查询联系人用户信息
	 */
	private Boolean queryContactUserInfo;

	/**
	 * 是否过滤掉自己创建的群组
	 */
	private Boolean excludeMyGroup;

	/**
	 * 状态数组-有些状态的联系人需要过滤掉
	 */
	private Integer[] statusArray;

	/**
	 * 标志拉黑时是否是好友
	 */
	private Boolean FormerFriend;

	private String lastUpdateTimeStart;

	private String lastUpdateTimeEnd;



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

	public void setContactType(Integer contactType) {
		this.contactType = contactType;
	}

	public Integer getContactType() {
		return this.contactType;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getCreateTime() {
		return this.createTime;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getStatus() {
		return this.status;
	}

	public void setLastUpdateTime(Date lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public Date getLastUpdateTime() {
		return this.lastUpdateTime;
	}

	public void setUserIdFuzzy(String userIdFuzzy) {
		this.userIdFuzzy = userIdFuzzy;
	}

	public String getUserIdFuzzy() {
		return this.userIdFuzzy;
	}

	public void setContactIdFuzzy(String contactIdFuzzy) {
		this.contactIdFuzzy = contactIdFuzzy;
	}

	public String getContactIdFuzzy() {
		return this.contactIdFuzzy;
	}

	public void setCreateTimeStart(String createTimeStart) {
		this.createTimeStart = createTimeStart;
	}

	public String getCreateTimeStart() {
		return this.createTimeStart;
	}

	public void setCreateTimeEnd(String createTimeEnd) {
		this.createTimeEnd = createTimeEnd;
	}

	public String getCreateTimeEnd() {
		return this.createTimeEnd;
	}

	public void setLastUpdateTimeStart(String lastUpdateTimeStart) {
		this.lastUpdateTimeStart = lastUpdateTimeStart;
	}

	public String getLastUpdateTimeStart() {
		return this.lastUpdateTimeStart;
	}

	public void setLastUpdateTimeEnd(String lastUpdateTimeEnd) {
		this.lastUpdateTimeEnd = lastUpdateTimeEnd;
	}

	public String getLastUpdateTimeEnd() {
		return this.lastUpdateTimeEnd;
	}

	public Boolean getQueryUserInfo() {
		return queryUserInfo;
	}

	public void setQueryUserInfo(Boolean queryUserInfo) {
		this.queryUserInfo = queryUserInfo;
	}

	public Boolean getQueryGroupInfo() {
		return queryGroupInfo;
	}

	public void setQueryGroupInfo(Boolean queryGroupInfo) {
		this.queryGroupInfo = queryGroupInfo;
	}

	public Boolean getQueryContactUserInfo() {
		return queryContactUserInfo;
	}

	public void setQueryContactUserInfo(Boolean queryContactUserInfo) {
		this.queryContactUserInfo = queryContactUserInfo;
	}

	public Boolean getExcludeMyGroup() {
		return excludeMyGroup;
	}

	public void setExcludeMyGroup(Boolean excludeMyGroup) {
		this.excludeMyGroup = excludeMyGroup;
	}

	public Integer[] getStatusArray() {
		return statusArray;
	}

	public void setStatusArray(Integer[] statusArray) {
		this.statusArray = statusArray;
	}

	public Boolean getFormerFriend() {
		return FormerFriend;
	}

	public void setFormerFriend(Boolean formerFriend) {
		FormerFriend = formerFriend;
	}
}