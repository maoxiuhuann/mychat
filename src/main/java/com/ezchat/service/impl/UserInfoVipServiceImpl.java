package com.ezchat.service.impl;

import com.ezchat.entity.po.UserInfo;
import com.ezchat.entity.query.SimplePage;
import com.ezchat.entity.query.UserInfoQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.UserInfoVip;
import com.ezchat.entity.query.UserInfoVipQuery;
import com.ezchat.enums.PageSize;
import com.ezchat.enums.ResponseCodeEnum;
import com.ezchat.enums.VipAccountStatusEnum;
import com.ezchat.exception.BusinessException;
import com.ezchat.mappers.UserInfoMapper;
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

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

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

	/**
	 * 根据Id删除数据
	 * @param id
	 * @return
	 */
	public Integer deleteUserInfoVipById(Integer id) {
		return this.userInfoVipMapper.deleteById(id);
	}

	/**
	 * 保存靓号
	 * @param userInfoVip
	 */
    @Override
    public void savaAccount(UserInfoVip userInfoVip) throws BusinessException {
		//禁止修改已使用的靓号信息
		if (userInfoVip.getId() != null){
			UserInfoVip dbInfo = userInfoVipMapper.selectById(userInfoVip.getId());
			if (VipAccountStatusEnum.USED.getStatus().equals(dbInfo.getStatus())){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
		}
		UserInfoVip dbInfo;

		//根据邮箱进行靓号操作
		dbInfo = userInfoVipMapper.selectByEmail(userInfoVip.getEmail());
		// 新增时判断邮箱是否存在-前端传来的userinfo包括id，前端传来的id为null，说明是新增，数据库中没有该邮箱的记录，则新增记录
		if (userInfoVip.getId() == null && dbInfo != null){
			throw new BusinessException("靓号邮箱已存在");
		}
		// 更新时判断邮箱是否存在-有这样一种情况：有两个靓号邮箱1和2，在数据库中的id分别为1、2，前端想要讲靓号1的邮箱改成2，此时就会进入这个if判断
		if (userInfoVip.getId() != null && dbInfo != null && dbInfo.getId() != null && !userInfoVip.getId().equals(dbInfo.getId())){
			throw new BusinessException("靓号邮箱已存在");
		}

		//根据邮箱进行靓号操作
		dbInfo = userInfoVipMapper.selectByUserId(userInfoVip.getUserId());
		// 新增时判断用户ID是否存在
		if (userInfoVip.getId() == null && dbInfo != null){
			throw new BusinessException("靓号已存在");
		}
		// 更新时判断用户ID是否存在
		if (userInfoVip.getId() != null && dbInfo != null && dbInfo.getId() != null && !userInfoVip.getId().equals(dbInfo.getId())){
			throw new BusinessException("靓号已存在");
		}

		//判断邮箱是否已经被注册
		UserInfo userInfo = userInfoMapper.selectByEmail(userInfoVip.getEmail());
		if (userInfo != null){
			throw new BusinessException("靓号邮箱已被注册");
		}
		userInfo = userInfoMapper.selectByUserId(userInfoVip.getUserId());
		//判断靓号是否已经被注册
		if (userInfo != null){
			throw new BusinessException("靓号已被注册");
		}

		if (userInfoVip.getId() != null){
			userInfoVipMapper.updateById(userInfoVip, userInfoVip.getId());
		}else {
			userInfoVip.setStatus(VipAccountStatusEnum.NO_USE.getStatus());
			userInfoVipMapper.insert(userInfoVip);
		}
    }
}
