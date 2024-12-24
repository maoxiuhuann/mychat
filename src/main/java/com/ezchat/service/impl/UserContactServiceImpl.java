package com.ezchat.service.impl;

import com.ezchat.constans.Constans;
import com.ezchat.entity.dto.SysSettingDTO;
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
import com.ezchat.redis.RedisComponent;
import com.ezchat.service.UserContactApplyService;
import com.ezchat.service.UserContactService;
import com.ezchat.utils.CopyUtils;
import com.ezchat.utils.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
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

    @Resource
    private RedisComponent redisComponent;

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
     * 申请添加联系人
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
        if (null != userContact && ArrayUtils.contains(new Integer[]{
                                UserContactStatusEnum.BLACKLIST_BE.getStatus(),
                                UserContactStatusEnum.BLACKLIST_BE_FIRST.getStatus()}, userContact.getStatus())
        ) {
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
            this.addContact(applyUserId, receiveUserId, contactId, contactTypeEnum.getType(), applyInfo);
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
        if (dbApply == null || !UserContactApplyStatusEnum.INIT.getStatus().TYPE.equals(dbApply.getStatus())) {
            //TODO 发送申请信息给接收方
        }
        return joinType;
    }

    /**
     * 添加联系人
     *
     * @param applyUserID
     * @param receiveUserID
     * @param contactId
     * @param contactType
     * @param applyInfo
     */
    @Override
    public void addContact(String applyUserID, String receiveUserID, String contactId, Integer contactType, String applyInfo) throws BusinessException {
        //查询群聊人数是否超过限制
        if (UserContactTypeEnum.GROUP.getType().equals(contactType)){
            UserContactQuery userContactQuery = new UserContactQuery();
            userContactQuery.setContactId(contactId);
            userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            Integer count = userContactMapper.selectCount(userContactQuery);
            SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
            if (count >= sysSettingDTO.getMaxGroupCount()){
                throw new BusinessException("群聊人数已满，无法添加");
            }
        }
        Date currentDate = new Date();
        //如果同意好友，双方添加好友关系
        List<UserContact> contactList = new ArrayList<>();
        //申请人添加对方
        UserContact userContact = new UserContact();
        userContact.setUserId(applyUserID);
        userContact.setContactId(contactId);
        userContact.setContactType(contactType);
        userContact.setCreateTime(currentDate);
        userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        userContact.setLastUpdateTime(currentDate);
        contactList.add(userContact);
        //接收人添加申请人,群组时不用添加好友关系
        if (UserContactTypeEnum.USER.getType().equals(contactType)){
            userContact = new UserContact();
            userContact.setUserId(receiveUserID);
            userContact.setContactId(applyUserID);
            userContact.setContactType(contactType);
            userContact.setCreateTime(currentDate);
            userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            userContact.setLastUpdateTime(currentDate);
            contactList.add(userContact);
        }
        //批量插入
        this.userContactMapper.insertOrUpdateBatch(contactList);
        //TODO 如果是好友，接收人也添加申请人为好友，添加缓存

        //TODO 创建会话
    }


    /**
     * 删除或者拉黑好友
     * @param userId
     * @param contactId
     * @param statusEnum
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserContact(String userId, String contactId, UserContactStatusEnum statusEnum) {
        //移除好友
        UserContact userContact = new UserContact();
        userContact.setStatus(statusEnum.getStatus());
        userContactMapper.updateByUserIdAndContactId(userContact, userId, contactId);

        //将好友的联系人也移除自己
        UserContact friendContact = new UserContact();
        if (UserContactStatusEnum.DEL == statusEnum){
            //被删除
            friendContact.setStatus(UserContactStatusEnum.DEL_BE.getStatus());
        }else if (UserContactStatusEnum.BLACKLIST == statusEnum){
            //被拉黑
            friendContact.setStatus(UserContactStatusEnum.BLACKLIST_BE.getStatus());
        }
        userContactMapper.updateByUserIdAndContactId(friendContact, contactId, userId);
        //todo 从我的好友列表缓存中删除好友
        //todo 从好友列表缓存中删除我
    }
}
