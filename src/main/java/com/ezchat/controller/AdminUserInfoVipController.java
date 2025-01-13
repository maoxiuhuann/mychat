package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.entity.po.UserInfoVip;
import com.ezchat.entity.query.UserInfoQuery;
import com.ezchat.entity.query.UserInfoVipQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.exception.BusinessException;
import com.ezchat.service.UserInfoService;
import com.ezchat.service.UserInfoVipService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 管理员靓号管理
 */
@RequestMapping("/admin")
@RestController("AdminUserInfoVipController")
public class AdminUserInfoVipController extends ABaseController{

    @Resource
    private UserInfoVipService userInfoVipService;


    /**
     * Load Beauty Account List
     * @param userInfoVipQuery
     * @return
     */
    @RequestMapping("/loadBeautyAccountList")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVo loadBeautyAccountList(UserInfoVipQuery userInfoVipQuery) {
        userInfoVipQuery.setOrderBy("id desc");
        PaginationResultVO resultVO = userInfoVipService.findListByPage(userInfoVipQuery);
        return getSuccessResponseVo(resultVO);
    }

    /**
     * 保存靓号
     * @param userInfoVip
     * @return
     */
    @RequestMapping("/saveBeautAccount")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVo saveBeautAccount(UserInfoVip userInfoVip) throws BusinessException {
        userInfoVipService.savaAccount(userInfoVip);
        return getSuccessResponseVo(null);
    }

    /**
     *  删除靓号
     * @param id
     * @return
     * @throws BusinessException
     */
    @RequestMapping("/delBeautAccount")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVo delBeautAccount(@NotNull Integer id) throws BusinessException {
        userInfoVipService.deleteUserInfoVipById(id);
        return getSuccessResponseVo(null);
    }
}
