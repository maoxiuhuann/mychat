package com.ezchat.service.impl;

import com.ezchat.constans.Constans;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.dto.UserContactSearchResultDTO;
import com.ezchat.entity.po.GroupInfo;
import com.ezchat.entity.po.UserContactApply;
import com.ezchat.entity.po.UserInfo;
import com.ezchat.entity.query.*;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.UserContact;
import com.ezchat.enums.*;
import com.ezchat.exception.BusinessException;
import com.ezchat.mappers.GroupInfoMapper;
import com.ezchat.mappers.UserContactApplyMapper;
import com.ezchat.mappers.UserContactMapper;
import com.ezchat.mappers.UserInfoMapper;
import com.ezchat.service.UserContactService;
import com.ezchat.utils.CopyUtils;
import com.ezchat.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description:联系人Service
 * @author:xiuyuan
 * @date:2024/12/16
 */
@Service("userContactService")
public class UserContactServiceImpl implements UserContactService {

    @Resource
    private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;

    @Resource
    private UserContactApplyMapper<UserContactApply, UserContactApplyQuery> userContactApplyMapper;

    /**
     * 根据条件查询列表
     */
    public List<UserContact> findListByParam(UserContactQuery query) {
        return this.userContactMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    public Integer findCountByParam(UserContactQuery query) {
        return this.userContactMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    public PaginationResultVO<UserContact> findListByPage(UserContactQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<UserContact> list = this.findListByParam(query);
        PaginationResultVO<UserContact> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    public Integer add(UserContact bean) {
        return this.userContactMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    public Integer addBatch(List<UserContact> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userContactMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或更新
     */
    public Integer addOrUpdateBatch(List<UserContact> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userContactMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据UserIdAndContactId查询数据
     */
    public UserContact getUserContactByUserIdAndContactId(String userId, String contactId) {
        return this.userContactMapper.selectByUserIdAndContactId(userId, contactId);
    }

    /**
     * 根据UserIdAndContactId更新数据
     */
    public Integer updateUserContactByUserIdAndContactId(UserContact bean, String userId, String contactId) {
        return this.userContactMapper.updateByUserIdAndContactId(bean, userId, contactId);
    }

    /**
     * 根据UserIdAndContactId删除数据
     */
    public Integer deleteUserContactByUserIdAndContactId(String userId, String contactId) {
        return this.userContactMapper.deleteByUserIdAndContactId(userId, contactId);
    }

    /**
     * 搜索联系人
     *
     * @param userId
     * @param contactId
     * @return
     */
    @Override
    public UserContactSearchResultDTO searchContact(String userId, String contactId) {
        //查询想要添加的联系人信息
        UserContactTypeEnum typeEnum = UserContactTypeEnum.getByPrefix(contactId);
        if (null == typeEnum) {
            return null;
        }
        UserContactSearchResultDTO resultDTO = new UserContactSearchResultDTO();
        switch (typeEnum) {
            case USER:
                UserInfo userInfo = this.userInfoMapper.selectByUserId(contactId);
                if (null == userInfo) {
                    return null;
                }
                resultDTO = CopyUtils.copy(userInfo, UserContactSearchResultDTO.class);
                break;
            case GROUP:
                GroupInfo groupInfo = this.groupInfoMapper.selectByGroupId(contactId);
                if (null == groupInfo) {
                    return null;
                }
                resultDTO.setNickName(groupInfo.getGroupName());
                break;
        }
        resultDTO.setContactType(typeEnum.toString());
        resultDTO.setContactId(contactId);

        if (userId.equals(contactId)) {
            resultDTO.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            return resultDTO;
        }
        //查询是否是好友
        UserContact userContact = this.getUserContactByUserIdAndContactId(userId, contactId);
        resultDTO.setStatus(userContact == null ? null : userContact.getStatus());
        return resultDTO;
    }

    /**
     * 申请添联系人
     *
     * @param tokenUserInfoDTO
     * @param contactId
     * @param applyInfo
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer applyAdd(TokenUserInfoDTO tokenUserInfoDTO, String contactId, String applyInfo) throws BusinessException {
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
        if (null == contactTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //发送申请的用户
        String applyUserId = tokenUserInfoDTO.getUserId();
        //默认申请信息
        applyInfo = StringUtils.isEmpty(applyInfo) ? String.format(Constans.DEFAULT_APPLY_REASON, tokenUserInfoDTO.getNickName()) : applyInfo;
        Long currentTime = System.currentTimeMillis();
        Integer joinType = null;
        //收到申请的用户
        String receiveUserId = contactId;
        //查询是否已经是好友,如果已经拉黑则无法添加
        UserContact userContact = this.getUserContactByUserIdAndContactId(applyUserId, receiveUserId);
        if (null != userContact && UserContactStatusEnum.BLACKLIST_BE.getStatus().equals(userContact.getStatus())) {
            throw new BusinessException("对方将你拉黑，无法添加");
        }
        if (UserContactTypeEnum.GROUP == contactTypeEnum) {
            GroupInfo groupInfo = this.groupInfoMapper.selectByGroupId(contactId);
            if (null == groupInfo || GroupStatusEnum.DISSOLUTION.getStatus().equals(groupInfo.getStatus())) {
                throw new BusinessException("该群不存在或已解散，无法添加");
            }

            receiveUserId = groupInfo.getGroupOwnerId();
            joinType = groupInfo.getJoinType();
        } else {
            UserInfo userInfo = this.userInfoMapper.selectByUserId(contactId);
            if (null == userInfo) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            joinType = userInfo.getJoinType();
        }
        //直接加入不用添加申请记录
        if (JoinTypeEnum.JOIN.getType().equals(joinType)) {
            //TODO 添加联系人
            return joinType;
        }
        //查询是否已经有申请记录
        UserContactApply dbApply = this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
        if (null == dbApply) {
            UserContactApply apply = new UserContactApply();
            apply.setApplyUserId(applyUserId);
            apply.setContactType(contactTypeEnum.getType());
            apply.setReceiveUserId(receiveUserId);
            apply.setLastApplyTime(currentTime);
            apply.setContactId(contactId);
            apply.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
            apply.setApplyInfo(applyInfo);
            this.userContactApplyMapper.insert(apply);
        } else {
            //如果已经有申请记录，则更新申请信息
            UserContactApply apply = new UserContactApply();
            apply.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
            apply.setLastApplyTime(currentTime);
            apply.setApplyInfo(applyInfo);
            this.userContactApplyMapper.updateByApplyId(apply, dbApply.getApplyId());
        }
        if (dbApply == null || !UserContactApplyStatusEnum.INIT.getStatus().TYPE.equals(dbApply.getStatus())){
            //TODO 发送申请信息给接收方
        }
        return joinType;
    }
}
