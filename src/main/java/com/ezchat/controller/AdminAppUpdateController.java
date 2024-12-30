package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.entity.po.AppUpdate;
import com.ezchat.entity.query.AppUpdateQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.exception.BusinessException;
import com.ezchat.service.AppUpdateService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import java.io.IOException;

@RequestMapping("/admin")
@RestController("AdminAppUpdateController")
public class AdminAppUpdateController extends ABaseController{

    @Resource
    private AppUpdateService appUpdateService;

    /**
     * 获取更新列表
     */
    @RequestMapping("/loadUpdateList")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVo loadUpdateList(AppUpdateQuery appUpdateQuery) {
        appUpdateQuery.setOrderBy("id desc");
        PaginationResultVO resultVO = appUpdateService.findListByPage(appUpdateQuery);
        return getSuccessResponseVo(resultVO);
    }

    /**
     * 保存app更新信息
     * @param id
     * @param version
     * @param updateDesc
     * @param outerLink
     * @param file
     * @return
     */
    @RequestMapping("/saveUpdate")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVo saveUpdate(Integer id,
                                 @NotEmpty String version,
                                 @NotEmpty String updateDesc,
                                 @NotEmpty  Integer fileType,
                                 String outerLink,
                                 MultipartFile file) throws BusinessException, IOException {
        AppUpdate appUpdate = new AppUpdate();
        appUpdate.setId(id);
        appUpdate.setVersion(version);
        appUpdate.setUpdateDesc(updateDesc);
        appUpdate.setFileType(fileType);
        appUpdate.setOuterLink(outerLink);
        appUpdateService.saveAppUpdate(appUpdate, file);
        return getSuccessResponseVo(null);
    }
}
