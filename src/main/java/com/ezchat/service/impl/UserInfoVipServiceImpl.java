package com.ezchat.service.impl;

import com.ezchat.entity.query.SimplePage;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.UserInfoVip;
import com.ezchat.entity.query.UserInfoVipQuery;
import com.ezchat.enums.PageSize;
import com.ezchat.mappers.UserInfoVipMapper;
import com.ezchat.service.UserInfoVipService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description:靓号表Service
 * @author:xiuyuan
 * @date:2024/12/11
 */
@Service("userInfoVipService")
public class UserInfoVipServiceImpl implements UserInfoVipService {

	@Resource
	private UserInfoVipMapper<UserInfoVip, UserInfoVipQuery> userInfoVipMapper;

	/**
	 * 根据条件查询列表
	 */
	public List<UserInfoVip> findListByParam(UserInfoVipQuery query) {
		return this.userInfoVipMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(UserInfoVipQuery query) {
		return this.userInfoVipMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<UserInfoVip> findListByPage(UserInfoVipQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<UserInfoVip> list = this.findListByParam(query);
		PaginationResultVO<UserInfoVip> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(UserInfoVip bean) {
		return this.userInfoVipMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<UserInfoVip> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoVipMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或更新
	 */
	public Integer addOrUpdateBatch(List<UserInfoVip> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoVipMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据IdAndUserId查询数据
	 */
	public UserInfoVip getUserInfoVipByIdAndUserId(Integer id, String userId) {
		return this.userInfoVipMapper.selectByIdAndUserId(id, userId);
	}

	/**
	 * 根据IdAndUserId更新数据
	 */
	public Integer updateUserInfoVipByIdAndUserId(UserInfoVip bean, Integer id, String userId) {
		return this.userInfoVipMapper.updateByIdAndUserId(bean,id, userId);
	}

	/**
	 * 根据IdAndUserId删除数据
	 */
	public Integer deleteUserInfoVipByIdAndUserId(Integer id, String userId) {
		return this.userInfoVipMapper.deleteByIdAndUserId(id, userId);
	}

	/**
	 * 根据UserId查询数据
	 */
	public UserInfoVip getUserInfoVipByUserId(String userId) {
		return this.userInfoVipMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId更新数据
	 */
	public Integer updateUserInfoVipByUserId(UserInfoVip bean, String userId) {
		return this.userInfoVipMapper.updateByUserId(bean,userId);
	}

	/**
	 * 根据UserId删除数据
	 */
	public Integer deleteUserInfoVipByUserId(String userId) {
		return this.userInfoVipMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email查询数据
	 */
	public UserInfoVip getUserInfoVipByEmail(String email) {
		return this.userInfoVipMapper.selectByEmail(email);
	}

	/**
	 * 根据Email更新数据
	 */
	public Integer updateUserInfoVipByEmail(UserInfoVip bean, String email) {
		return this.userInfoVipMapper.updateByEmail(bean,email);
	}

	/**
	 * 根据Email删除数据
	 */
	public Integer deleteUserInfoVipByEmail(String email) {
		return this.userInfoVipMapper.deleteByEmail(email);
	}

}
