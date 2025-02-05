package com.ezchat.service;

import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.ChatSessionUser;
import com.ezchat.entity.query.ChatSessionUserQuery;

import java.util.List;
/**
 * @Description:会话用户Service
 * @author:xiuyuan
 * @date:2025/01/06
 */
public interface ChatSessionUserService {

	/**
	 * 根据条件查询列表
	 */
	List<ChatSessionUser> findListByParam(ChatSessionUserQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(ChatSessionUserQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<ChatSessionUser> findListByPage(ChatSessionUserQuery query);

	/**
	 * 新增
	 */
	Integer add(ChatSessionUser bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<ChatSessionUser> listBean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<ChatSessionUser> listBean);

	/**
	 * 根据UserIdAndContactId查询数据
	 */
	ChatSessionUser getChatSessionUserByUserIdAndContactId(String userId, String contactId);

	/**
	 * 根据UserIdAndContactId更新数据
	 */
	Integer updateChatSessionUserByUserIdAndContactId(ChatSessionUser bean, String userId, String contactId);

	/**
	 * 根据UserIdAndContactId删除数据
	 */
	Integer deleteChatSessionUserByUserIdAndContactId(String userId, String contactId);

	/**
	 * 更新冗余信息
	 * @param contactName
	 * @param contactId
	 */
	public void updateRedundanceInfo(String contactName,String contactId);
}
