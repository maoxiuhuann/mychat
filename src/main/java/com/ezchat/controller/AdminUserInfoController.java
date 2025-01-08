package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.entity.query.UserInfoQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.exception.BusinessException;
import com.ezchat.service.UserInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@RequestMapping("/admin")
@RestController("AdminUserInfoController")
public class AdminUserInfoController extends ABaseController{

    @Resource
    private UserInfoService userInfoService;

    /**
     * 获取用户信息
     * @param userInfoQuery
     * @return
     */
    @RequestMapping("/loadUser")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVo loadUser(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("create_time desc");
        PaginationResultVO resultVO = userInfoService.findListByPage(userInfoQuery);
        return getSuccessResponseVo(resultVO);
    }

    /**
     * 管理员更新用户状态
     * @param status
     * @param userId
     */
    @RequestMapping("/updateUserStatus")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVo updateUserStatus(@NotNull Integer status,@NotEmpty String userId) throws BusinessException {
        userInfoService.updateUserStatus(status,userId);
        return getSuccessResponseVo(null);
    }

    /**
     * 管理员强制用户下线
     * @param userId
     * @return
     * @throws BusinessException
     */
    @RequestMapping("/forceOffLine")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVo forceOffLine(@NotEmpty String userId) throws BusinessException {
        userInfoService.forceOffLine(userId);
        return getSuccessResponseVo(null);
    }


}
