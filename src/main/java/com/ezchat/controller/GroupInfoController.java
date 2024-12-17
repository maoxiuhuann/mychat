package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.po.GroupInfo;
import com.ezchat.entity.query.GroupInfoQuery;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.exception.BusinessException;
import com.ezchat.service.GroupInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

/**
 * @Description:Controller
 * @author:xiuyuan
 * @date:2024/12/16
 */
@RestController
@RequestMapping("/group")
@Validated
public class GroupInfoController extends ABaseController {

    @Resource
    private GroupInfoService groupInfoService;

    /**
     * 保存群组信息-新增或修改
     *
     * @param request
     * @param groupId
     * @param groupName
     * @param groupNotice
     * @param joinType
     * @param avatarFile
     * @param avatarCover
     * @return
     */
    @RequestMapping("/saveGroup")
    @GlobalInterceptor
    public ResponseVo saveGroup(HttpServletRequest request,
                                String groupId,
                                @NotEmpty String groupName,
                                String groupNotice,
                                @NotNull Integer joinType,
                                MultipartFile avatarFile,//头像文件-原图和缩略图
                                MultipartFile avatarCover) throws IOException, BusinessException {

        //从header中获取token-已经使用AOP保证token不为空即用户已经登陆
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setGroupId(groupId);
        groupInfo.setGroupName(groupName);
        groupInfo.setGroupNotice(groupNotice);
        groupInfo.setGroupOwnerId(tokenUserInfoDTO.getUserId());
        groupInfo.setJoinType(joinType);
        this.groupInfoService.saveGroup(groupInfo, avatarFile, avatarCover);
        return getSuccessResponseVo(null);
    }


    /**
     * 加载我创建的群组列表
     *
     * @param request
     * @return
     */
    @RequestMapping("/loadMyGroup")
    @GlobalInterceptor
    public ResponseVo loadMyGroup(HttpServletRequest request) {
        //从header中获取token-已经使用AOP保证token不为空即用户已经登陆
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        GroupInfoQuery groupInfoQuery = new GroupInfoQuery();
        groupInfoQuery.setGroupOwnerId(tokenUserInfoDTO.getUserId());
        groupInfoQuery.setOrderBy("create_time desc");
        List<GroupInfo> groupInfoList = this.groupInfoService.findListByParam(groupInfoQuery);
        return getSuccessResponseVo(groupInfoList);
    }
}
