package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.constans.Constans;
import com.ezchat.entity.config.AppConfig;
import com.ezchat.entity.po.AppUpdate;
import com.ezchat.entity.query.AppUpdateQuery;
import com.ezchat.entity.vo.AppUpdateVo;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.enums.AppUpdateFileTypeEnum;
import com.ezchat.exception.BusinessException;
import com.ezchat.service.AppUpdateService;
import com.ezchat.utils.CopyUtils;
import com.ezchat.utils.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;


@RequestMapping("/admin")
@RestController("UpdateController")
public class UpdateController extends ABaseController {

    @Resource
    private AppUpdateService appUpdateService;

    @Resource
    private AppConfig appConfig;

    /**
     * 检查更新
     * @param appVersion
     * @param uid
     * @return
     */
    @RequestMapping("/checkVersion")
    @GlobalInterceptor
    public ResponseVo checkVersion(String appVersion, String uid) {
        if (StringUtils.isEmpty(appVersion)) {
            return getSuccessResponseVo(null);
        }
        AppUpdate appUpdate = appUpdateService.getLatestUpdate(appVersion, uid);
        if (appUpdate == null){
            return getSuccessResponseVo(null);
        }
        AppUpdateVo appUpdateVo = CopyUtils.copy(appUpdate, AppUpdateVo.class);
        if (AppUpdateFileTypeEnum.LOCAL.getType().equals(appUpdate.getFileType())){
            File file = new File(appConfig.getProjectFolder() + Constans.APP_UPDATE_FOLDER + appUpdate.getId() + Constans.APP_EXE_SUFFIX);
            appUpdateVo.setSize(file.length());
        }else {
            appUpdateVo.setSize(0L);
        }
        appUpdateVo.setUpdateList(Arrays.asList(appUpdate.getUpdateDescArray()));
        String fileName = Constans.APP_NAME + appUpdate.getVersion() + Constans.APP_EXE_SUFFIX;
        appUpdateVo.setFileName(fileName);
        return getSuccessResponseVo(appUpdateVo);
    }
}
