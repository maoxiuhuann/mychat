package com.ezchat.service;

import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.AppUpdate;
import com.ezchat.entity.query.AppUpdateQuery;
import com.ezchat.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
/**
 * @Description:app发布Service
 * @author:xiuyuan
 * @date:2024/12/30
 */
public interface AppUpdateService {

	/**
	 * 根据条件查询列表
	 */
	List<AppUpdate> findListByParam(AppUpdateQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(AppUpdateQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<AppUpdate> findListByPage(AppUpdateQuery query);

	/**
	 * 新增
	 */
	Integer add(AppUpdate bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<AppUpdate> listBean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<AppUpdate> listBean);

	/**
	 * 根据Id查询数据
	 */
	AppUpdate getAppUpdateById(Integer id);

	/**
	 * 根据Id更新数据
	 */
	Integer updateAppUpdateById(AppUpdate bean, Integer id);

	/**
	 * 根据Id删除数据
	 */
	Integer deleteAppUpdateById(Integer id);

	/**
	 * 保存app更新信息
	 * @param appUpdate
	 * @param file
	 */
	void saveAppUpdate(AppUpdate appUpdate, MultipartFile file) throws BusinessException, IOException;
}