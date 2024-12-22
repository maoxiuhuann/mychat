package com.ezchat.service.impl;

import com.ezchat.entity.po.UserContact;
import com.ezchat.entity.query.SimplePage;
import com.ezchat.entity.query.UserContactQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.UserContactApply;
import com.ezchat.entity.query.UserContactApplyQuery;
import com.ezchat.enums.PageSize;
import com.ezchat.enums.ResponseCodeEnum;
import com.ezchat.enums.UserContactApplyStatusEnum;
import com.ezchat.enums.UserContactStatusEnum;
import com.ezchat.exception.BusinessException;
import com.ezchat.mappers.UserContactApplyMapper;
import com.ezchat.mappers.UserContactMapper;
import com.ezchat.service.UserContactApplyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @Description:联系人申请Service
 * @author:xiuyuan
 * @date:2024/12/16
 */
@Service("userContactApplyService")
public class UserContactApplyServiceImpl implements UserContactApplyService {

	@Resource
	private UserContactApplyMapper<UserContactApply, UserContactApplyQuery> userContactApplyMapper;

	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

	/**
	 * 根据条件查询列表
	 */
	public List<UserContactApply> findListByParam(UserContactApplyQuery query) {
		return this.userContactApplyMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(UserContactApplyQuery query) {
		return this.userContactApplyMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<UserContactApply> findListByPage(UserContactApplyQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<UserContactApply> list = this.findListByParam(query);
		PaginationResultVO<UserContactApply> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(UserContactApply bean) {
		return this.userContactApplyMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<UserContactApply> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactApplyMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或更新
	 */
	public Integer addOrUpdateBatch(List<UserContactApply> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactApplyMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据ApplyId查询数据
	 */
	public UserContactApply getUserContactApplyByApplyId(Integer applyId) {
		return this.userContactApplyMapper.selectByApplyId(applyId);
	}

	/**
	 * 根据ApplyId更新数据
	 */
	public Integer updateUserContactApplyByApplyId(UserContactApply bean, Integer applyId) {
		return this.userContactApplyMapper.updateByApplyId(bean,applyId);
	}

	/**
	 * 根据ApplyId删除数据
	 */
	public Integer deleteUserContactApplyByApplyId(Integer applyId) {
		return this.userContactApplyMapper.deleteByApplyId(applyId);
	}

	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId查询数据
	 */
	public UserContactApply getUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(String applyUserId, String receiveUserId, String contactId) {
		return this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
	}

	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId更新数据
	 */
	public Integer updateUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(UserContactApply bean, String applyUserId, String receiveUserId, String contactId) {
		return this.userContactApplyMapper.updateByApplyUserIdAndReceiveUserIdAndContactId(bean,applyUserId, receiveUserId, contactId);
	}

	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId删除数据
	 */
	public Integer deleteUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(String applyUserId, String receiveUserId, String contactId) {
		return this.userContactApplyMapper.deleteByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
	}

	/**
	 * 处理申请
	 * @param userId
	 * @param applyId
	 * @param status
	 */
    @Override
	@Transactional(rollbackFor = Exception.class)
    public void dealWithApply(String userId, Integer applyId, Integer status) throws BusinessException {
		UserContactApplyStatusEnum statusEnum = UserContactApplyStatusEnum.getByStatus(status);
		// 不处理默认状态的申请
		if (statusEnum == null || UserContactApplyStatusEnum.INIT.equals(statusEnum)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		// 根据applyId查询申请信息
		UserContactApply applyInfo = this.userContactApplyMapper.selectByApplyId(applyId);
		// 后端校验申请信息是否存在以及是否为当前用户在操作
		if (applyInfo == null || !userId.equals(applyInfo.getReceiveUserId())) {
			throw new BusinessException(ResponseCodeEnum.CODE_601);
		}
		// 更新申请状态：  	update user_contact_apply set `status` = ?,last_off_time = ? where apply_id = ?
		UserContactApply updateInfo = new UserContactApply();
		updateInfo.setStatus(status);
		updateInfo.setLastApplyTime(System.currentTimeMillis());
        //限制只能在原状态下修改状态  	update user_contact_apply set `status` = ?,last_off_time = ? where apply_id = ? and `status` = 0
		//区别就在拼接的where条件中增加了status=0，只能操作未处理的申请
		UserContactApplyQuery query = new UserContactApplyQuery();
		query.setApplyId(applyId);
		query.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
		//TODO 在ezjava中新增这样的方法
		Integer count = this.userContactApplyMapper.updateByParam(updateInfo, query);
		if (count == 0){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		if (UserContactApplyStatusEnum.PASS.getStatus().equals(status)){
			//TODO 添加联系人
		}

		if (UserContactApplyStatusEnum.BLACKLIST.getStatus().equals(status)){
			Date currentDate = new Date();
			UserContact userContact = new UserContact();
			userContact.setUserId(applyInfo.getApplyUserId());
			userContact.setContactId(applyInfo.getContactId());
			userContact.setContactType(applyInfo.getContactType());
			userContact.setCreateTime(currentDate);
			userContact.setStatus(UserContactStatusEnum.BLACKLIST_BE.getStatus());
			userContact.setLastUpdateTime(currentDate);
			userContactMapper.insertOrUpdate(userContact);
		}
    }

}
