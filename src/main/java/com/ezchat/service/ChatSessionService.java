package com.ezchat.service;

import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.ChatSession;
import com.ezchat.entity.query.ChatSessionQuery;

import java.util.List;
/**
 * @Description:会话信息Service
 * @author:xiuyuan
 * @date:2025/01/06
 */
public interface ChatSessionService {

	/**
	 * 根据条件查询列表
	 */
	List<ChatSession> findListByParam(ChatSessionQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(ChatSessionQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<ChatSession> findListByPage(ChatSessionQuery query);

	/**
	 * 新增
	 */
	Integer add(ChatSession bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<ChatSession> listBean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<ChatSession> listBean);

	/**
	 * 根据SessionId查询数据
	 */
	ChatSession getChatSessionBySessionId(String sessionId);

	/**
	 * 根据SessionId更新数据
	 */
	Integer updateChatSessionBySessionId(ChatSession bean, String sessionId);

	/**
	 * 根据SessionId删除数据
	 */
	Integer deleteChatSessionBySessionId(String sessionId);

}
