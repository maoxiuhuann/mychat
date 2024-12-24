package com.ezchat.service;

import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.UserContactApply;
import com.ezchat.entity.query.UserContactApplyQuery;
import com.ezchat.exception.BusinessException;

import java.util.List;

/**
 * @Description:联系人申请Service
 * @author:xiuyuan
 * @date:2024/12/16
 */
public interface UserContactApplyService {

    /**
     * 根据条件查询列表
     */
    List<UserContactApply> findListByParam(UserContactApplyQuery query);

    /**
     * 根据条件查询数量
     */
    Integer findCountByParam(UserContactApplyQuery query);

    /**
     * 分页查询
     */
    PaginationResultVO<UserContactApply> findListByPage(UserContactApplyQuery query);

    /**
     * 新增
     */
    Integer add(UserContactApply bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<UserContactApply> listBean);

    /**
     * 批量新增或更新
     */
    Integer addOrUpdateBatch(List<UserContactApply> listBean);

    /**
     * 根据ApplyId查询数据
     */
    UserContactApply getUserContactApplyByApplyId(Integer applyId);

    /**
     * 根据ApplyId更新数据
     */
    Integer updateUserContactApplyByApplyId(UserContactApply bean, Integer applyId);

    /**
     * 根据ApplyId删除数据
     */
    Integer deleteUserContactApplyByApplyId(Integer applyId);

    /**
     * 根据ApplyUserIdAndReceiveUserIdAndContactId查询数据
     */
    UserContactApply getUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(String applyUserId, String receiveUserId, String contactId);

    /**
     * 根据ApplyUserIdAndReceiveUserIdAndContactId更新数据
     */
    Integer updateUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(UserContactApply bean, String applyUserId, String receiveUserId, String contactId);

    /**
     * 根据ApplyUserIdAndReceiveUserIdAndContactId删除数据
     */
    Integer deleteUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(String applyUserId, String receiveUserId, String contactId);

	/**
	 * 处理申请
	 * @param userId
	 * @param applyId
	 * @param status
	 */
    void dealWithApply(String userId, Integer applyId, Integer status) throws BusinessException;



}
