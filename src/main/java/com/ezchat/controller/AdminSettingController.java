package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.constans.Constans;
import com.ezchat.entity.config.AppConfig;
import com.ezchat.entity.dto.SysSettingDTO;
import com.ezchat.entity.po.GroupInfo;
import com.ezchat.entity.query.GroupInfoQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.enums.ResponseCodeEnum;
import com.ezchat.exception.BusinessException;
import com.ezchat.redis.RedisComponent;
import com.ezchat.service.GroupInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import java.io.File;
import java.io.IOException;

@RequestMapping("/admin")
@RestController("AdminSettingController")
public class AdminSettingController extends ABaseController{

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AppConfig appConfig;

    /**
     * 获取系统设置信息
     */
    @RequestMapping("/getSysSetting")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVo getSysSetting() {
        SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
        return getSuccessResponseVo(sysSettingDTO);
    }

    /**
     * 修改保存系统设置信息
     * @param sysSettingDTO
     * @param robotFile
     * @param robotCover
     * @return
     * @throws IOException
     */
    @RequestMapping("/saveSysSetting")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVo saveSysSetting(SysSettingDTO sysSettingDTO,
                                     MultipartFile robotFile,
                                     MultipartFile robotCover) throws IOException {
        if (robotFile!= null){
            String beanFolder = appConfig.getProjectFolder() + Constans.FILE_FOLDER_FILE;
            File targetFileFolder = new File(beanFolder + Constans.FILE_FOLDER_AVATAR_NAME);
            if (!targetFileFolder.exists()){
                targetFileFolder.mkdirs();
            }
            String filePath = targetFileFolder.getPath() + "/" + Constans.ROBOT_UID + Constans.IMAGE_SUFFIX;
            robotFile.transferTo(new File(filePath));
            robotCover.transferTo(new File(filePath + Constans.COVER_IMAGE_SUFFIX));
        }
        redisComponent.saveSysSetting(sysSettingDTO);
        return getSuccessResponseVo(null);
    }
}
