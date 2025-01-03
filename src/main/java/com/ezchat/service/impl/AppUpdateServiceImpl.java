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
import com.ezchat.utils.StringUtils;
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
 * @date:2024/12/31
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
    public Integer deleteAppUpdateById(Integer id) throws BusinessException {
        AppUpdate dbInfo = this.appUpdateMapper.selectById(id);
        if (dbInfo.getStatus() != AppUpdateStatusEnum.INIT.getStatus()){
            throw new BusinessException("不能删除已发布的更新信息");
        }
        return this.appUpdateMapper.deleteById(id);
    }

    /**
     * 根据Version查询数据
     */
    public AppUpdate getAppUpdateByVersion(String version) {
        return this.appUpdateMapper.selectByVersion(version);
    }

    /**
     * 根据Version更新数据
     */
    public Integer updateAppUpdateByVersion(AppUpdate bean, String version) {
        return this.appUpdateMapper.updateByVersion(bean, version);
    }

    /**
     * 根据Version删除数据
     */
    public Integer deleteAppUpdateByVersion(String version) {
        return this.appUpdateMapper.deleteByVersion(version);
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
        AppUpdateStatusEnum statusEnum = AppUpdateStatusEnum.getByStatus(appUpdate.getStatus());
        if (null == fileTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (appUpdate.getId() != null){
            AppUpdate dbInfo = appUpdateMapper.selectById(appUpdate.getId());
            if (!AppUpdateStatusEnum.INIT.getStatus().equals(dbInfo.getStatus())){
                throw new BusinessException("不能修改已发布的更新信息");
            }
        }
        AppUpdateQuery query = new AppUpdateQuery();
        query.setOrderBy("id desc");
        query.setSimplePage(new SimplePage(0, 1));
        List<AppUpdate> list = appUpdateMapper.selectList(query);
        if (!list.isEmpty()) {
            AppUpdate lastAppUpdate = list.get(0);
            //版本号比较
            Long dbVersion = Long.parseLong(lastAppUpdate.getVersion().replace(".", ""));
            Long currentVersion = Long.parseLong(appUpdate.getVersion().replace(".", ""));
            //新增条件限制
            if (appUpdate.getId() == null && currentVersion <= dbVersion) {
                throw new BusinessException("当前版本号必须大于历史版本号");
            }
            //更新条件限制
            if (appUpdate.getId() != null && currentVersion <= dbVersion && appUpdate.getId().equals(lastAppUpdate.getId())) {
                throw new BusinessException("当前版本号必须大于历史版本号");
            }
            AppUpdate versionDb = appUpdateMapper.selectByVersion(appUpdate.getVersion());
			//不能更新两条相同版本号记录
            if (appUpdate.getId() != null && versionDb != null && !versionDb.getId().equals(appUpdate.getId())) {
				throw new BusinessException("当前版本号已存在");
            }
        }
        if (appUpdate.getId() == null) {
            appUpdate.setCreateTime(new Date());
            appUpdate.setStatus(AppUpdateStatusEnum.INIT.getStatus());
            appUpdateMapper.insert(appUpdate);
        } else {
            appUpdate.setStatus(null);
            appUpdate.setGrayscaleUid(null);
            appUpdateMapper.updateById(appUpdate, appUpdate.getId());
        }
        if (file != null) {
            File folder = new File(appConfig.getProjectFolder() + Constans.APP_UPDATE_FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            file.transferTo(new File(folder.getAbsolutePath() + "/" + appUpdate.getId() + Constans.APP_EXE_SUFFIX));
        }
    }

    /**
     * 发布app更新
     * @param id
     * @param status
     * @param grayscaleUid
     * @throws BusinessException
     */
    @Override
    public void postAppUpdate(Integer id, Integer status, String grayscaleUid) throws BusinessException {
        AppUpdateStatusEnum statusEnum = AppUpdateStatusEnum.getByStatus(status);
        if (null == statusEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (statusEnum == AppUpdateStatusEnum.GRAYSCALE && StringUtils.isEmpty(grayscaleUid)) {
            throw new BusinessException("灰度发布UID不能为空");
        }
        if (AppUpdateStatusEnum.GRAYSCALE != statusEnum){
            grayscaleUid = "";
        }
        AppUpdate appUpdate = new AppUpdate();
        appUpdate.setStatus(status);
        appUpdate.setGrayscaleUid(grayscaleUid);
        appUpdateMapper.updateById(appUpdate, id);
    }

    /**
     * 获取最新更新
     * @param appVersion
     * @param uid
     * @return
     */
    @Override
    public AppUpdate getLatestUpdate(String appVersion, String uid) {
        return appUpdateMapper.selectLatestUpdate(appVersion, uid);
    }
}
