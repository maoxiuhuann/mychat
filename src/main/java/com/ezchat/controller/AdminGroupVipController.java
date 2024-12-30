package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.entity.po.GroupInfo;
import com.ezchat.entity.po.UserInfoVip;
import com.ezchat.entity.query.GroupInfoQuery;
import com.ezchat.entity.query.UserInfoVipQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.enums.ResponseCodeEnum;
import com.ezchat.exception.BusinessException;
import com.ezchat.service.GroupInfoService;
import com.ezchat.service.UserInfoVipService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@RequestMapping("/admin")
@RestController("AdminGroupController")
public class AdminGroupVipController extends ABaseController{

    @Resource
    private GroupInfoService groupInfoService;

    /**
     * 分页查询群组列表
     * @param groupInfoQuery
     * @return
     */
    @RequestMapping("/loadGroup")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVo loadBeautyAccountList(GroupInfoQuery groupInfoQuery) {
        /*SELECT g.*, u.nick_name,( SELECT COUNT( 1 ) FROM user_contact c WHERE c.contact_id = g.group_id ) memberCount
         FROM group_info g
        INNER JOIN user_info u ON u.user_id = g.group_owner_id*/
        /*SELECT g.*, (SELECT u.nick_name FROM user_info u WHERE u.user_id = g.group_owner_id) groupOwnerNickName,( SELECT COUNT( 1 ) FROM user_contact c WHERE c.contact_id = g.group_id ) memberCount
        FROM group_info g*/
        groupInfoQuery.setOrderBy("create_time desc");
        groupInfoQuery.setQueryMemberCount(true);
        groupInfoQuery.setQueryGroupOwnerName(true);
        PaginationResultVO resultVO = groupInfoService.findListByPage(groupInfoQuery);
        return getSuccessResponseVo(resultVO);
    }

    /**
     * 解散群组
     * @param groupId
     * @return
     * @throws BusinessException
     */
    @RequestMapping("/dissolutionGroup")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVo dissolutionGroup(@NotEmpty String groupId) throws BusinessException {
        GroupInfo groupInfo = groupInfoService.getGroupInfoByGroupId(groupId);
        if (null == groupId){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        groupInfoService.dissolutionGroup(groupInfo.getGroupOwnerId(),groupId);
        return getSuccessResponseVo(null);
    }
}
