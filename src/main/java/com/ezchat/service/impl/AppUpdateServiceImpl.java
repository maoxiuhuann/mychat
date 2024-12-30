package com.ezchat.service.impl;

import com.ezchat.constans.Constans;
import com.ezchat.entity.config.AppConfig;
import com.ezchat.entity.query.SimplePage;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.AppUpdate;
import com.ezchat.entity.query.AppUpdateQuery;
import com.ezchat.enums.AppUpdateFileTypeEnum;
import com.ezchat.enums.AppUpdateStatusEnum;
import com.ezchat.enums.PageSize;
import com.ezchat.enums.ResponseCodeEnum;
import com.ezchat.exception.BusinessException;
import com.ezchat.mappers.AppUpdateMapper;
import com.ezchat.service.AppUpdateService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @Description:app发布Service
 * @author:xiuyuan
 * @date:2024/12/30
 */
@Service("appUpdateService")
public class AppUpdateServiceImpl implements AppUpdateService {

    @Resource
    private AppUpdateMapper<AppUpdate, AppUpdateQuery> appUpdateMapper;

    @Resource
    private AppConfig appConfig;

    /**
     * 根据条件查询列表
     */
    public List<AppUpdate> findListByParam(AppUpdateQuery query) {
        return this.appUpdateMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    public Integer findCountByParam(AppUpdateQuery query) {
        return this.appUpdateMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    public PaginationResultVO<AppUpdate> findListByPage(AppUpdateQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<AppUpdate> list = this.findListByParam(query);
        PaginationResultVO<AppUpdate> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    public Integer add(AppUpdate bean) {
        return this.appUpdateMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    public Integer addBatch(List<AppUpdate> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.appUpdateMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或更新
     */
    public Integer addOrUpdateBatch(List<AppUpdate> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.appUpdateMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据Id查询数据
     */
    public AppUpdate getAppUpdateById(Integer id) {
        return this.appUpdateMapper.selectById(id);
    }

    /**
     * 根据Id更新数据
     */
    public Integer updateAppUpdateById(AppUpdate bean, Integer id) {
        return this.appUpdateMapper.updateById(bean, id);
    }

    /**
     * 根据Id删除数据
     */
    public Integer deleteAppUpdateById(Integer id) {
        return this.appUpdateMapper.deleteById(id);
    }

    /**
     * 保存app更新信息
     *
     * @param appUpdate
     * @param file
     */
    @Override
    public void saveAppUpdate(AppUpdate appUpdate, MultipartFile file) throws BusinessException, IOException {
        AppUpdateFileTypeEnum fileTypeEnum = AppUpdateFileTypeEnum.getByType(appUpdate.getFileType());
        if (null == fileTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        AppUpdateQuery query = new AppUpdateQuery();
        query.setOrderBy("version desc");
        query.setSimplePage(new SimplePage(0, 1));
        List<AppUpdate> list = appUpdateMapper.selectList(query);
        if (!list.isEmpty()) {
            AppUpdate lastAppUpdate = list.get(0);
            Long dbVersion = Long.parseLong(lastAppUpdate.getVersion().replace(".", ""));
            Long currentVersion = Long.parseLong(appUpdate.getVersion().replace(".", ""));
			//新增条件限制
            if (appUpdate.getId() == null && currentVersion <= dbVersion){
				throw new BusinessException("当前版本号必须大于历史版本号");
			}
			//更新条件限制
			if (appUpdate.getId() != null && currentVersion <= dbVersion && !appUpdate.getVersion().equals(lastAppUpdate.getVersion())){
				throw new BusinessException("当前版本号必须大于历史版本号");
			}
        }
        if (appUpdate.getId() == null){
            appUpdate.setCreateTime(new Date());
            appUpdate.setStatus(AppUpdateStatusEnum.INIT.getStatus());
            appUpdateMapper.insert(appUpdate);
        }else {
            appUpdate.setStatus(null);
            appUpdate.setGrayscaleUid(null);
            appUpdateMapper.updateById(appUpdate, appUpdate.getId());
        }
        if (file != null){
            File folder = new File(appConfig.getProjectFolder() + Constans.APP_UPDATE_FOLDER);
            if (!folder.exists()){
                folder.mkdirs();
            }
            file.transferTo(new File(folder.getAbsolutePath() + "/" + appUpdate.getId() + Constans.APP_EXE_SUFFIX));
        }
    }
}
