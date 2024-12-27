package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.constans.Constans;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.po.UserInfo;
import com.ezchat.entity.query.UserInfoQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.entity.vo.UserInfoVo;
import com.ezchat.exception.BusinessException;
import com.ezchat.service.UserInfoService;
import com.ezchat.utils.CopyUtils;
import com.ezchat.utils.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.IOException;

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
