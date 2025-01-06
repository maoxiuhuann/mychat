package com.ezchat.service;

import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.ChatMessage;
import com.ezchat.entity.query.ChatMessageQuery;

import java.util.List;
/**
 * @Description:聊天消息表Service
 * @author:xiuyuan
 * @date:2025/01/06
 */
public interface ChatMessageService {

	/**
	 * 根据条件查询列表
	 */
	List<ChatMessage> findListByParam(ChatMessageQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(ChatMessageQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<ChatMessage> findListByPage(ChatMessageQuery query);

	/**
	 * 新增
	 */
	Integer add(ChatMessage bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<ChatMessage> listBean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<ChatMessage> listBean);

	/**
	 * 根据MessageId查询数据
	 */
	ChatMessage getChatMessageByMessageId(Long messageId);

	/**
	 * 根据MessageId更新数据
	 */
	Integer updateChatMessageByMessageId(ChatMessage bean, Long messageId);

	/**
	 * 根据MessageId删除数据
	 */
	Integer deleteChatMessageByMessageId(Long messageId);

}
