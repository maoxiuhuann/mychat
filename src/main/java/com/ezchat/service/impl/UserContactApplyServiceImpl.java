package com.ezchat.service.impl;

import com.ezchat.entity.dto.SysSettingDTO;
import com.ezchat.entity.po.UserContact;
import com.ezchat.entity.query.SimplePage;
import com.ezchat.entity.query.UserContactQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.UserContactApply;
import com.ezchat.entity.query.UserContactApplyQuery;
import com.ezchat.enums.*;
import com.ezchat.exception.BusinessException;
import com.ezchat.mappers.UserContactApplyMapper;
import com.ezchat.mappers.UserContactMapper;
import com.ezchat.redis.RedisComponent;
import com.ezchat.service.UserContactApplyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
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

    @Resource
    private RedisComponent redisComponent;

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
        return this.userContactApplyMapper.updateByApplyId(bean, applyId);
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
        return this.userContactApplyMapper.updateByApplyUserIdAndReceiveUserIdAndContactId(bean, applyUserId, receiveUserId, contactId);
    }

    /**
     * 根据ApplyUserIdAndReceiveUserIdAndContactId删除数据
     */
    public Integer deleteUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(String applyUserId, String receiveUserId, String contactId) {
        return this.userContactApplyMapper.deleteByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
    }

    /**
     * 处理申请
     *
     * @param userId
     * @param applyId
     * @param status
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dealWithApply(String userId, Integer applyId, Integer status) throws BusinessException {
        UserContactApplyStatusEnum statusEnum = UserContactApplyStatusEnum.getByStatus(status);
        // 不处理默认状态的申请
        if (statusEnum == null || UserContactApplyStatusEnum.INIT.equals(statusEnum)) {
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
        Integer count = this.userContactApplyMapper.updateByQuery(updateInfo, query);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 处理同意申请，双方双向添加好友关系
        if (UserContactApplyStatusEnum.PASS.getStatus().equals(status)) {
            this.addContact(applyInfo.getApplyUserId(), applyInfo.getReceiveUserId(), applyInfo.getContactId(), applyInfo.getContactType(), applyInfo.getApplyInfo());
        }

        if (UserContactApplyStatusEnum.BLACKLIST.getStatus().equals(status)) {
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

}
