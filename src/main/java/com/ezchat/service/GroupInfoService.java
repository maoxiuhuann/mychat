package com.ezchat.service;

import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.GroupInfo;
import com.ezchat.entity.query.GroupInfoQuery;
import com.ezchat.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @Description:Service
 * @author:xiuyuan
 * @date:2024/12/16
 */
public interface GroupInfoService {

    /**
     * 根据条件查询列表
     */
    List<GroupInfo> findListByParam(GroupInfoQuery query);

    /**
     * 根据条件查询数量
     */
    Integer findCountByParam(GroupInfoQuery query);

    /**
     * 分页查询
     */
    PaginationResultVO<GroupInfo> findListByPage(GroupInfoQuery query);

    /**
     * 新增
     */
    Integer add(GroupInfo bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<GroupInfo> listBean);

    /**
     * 批量新增或更新
     */
    Integer addOrUpdateBatch(List<GroupInfo> listBean);

    /**
     * 根据GroupId查询数据
     */
    GroupInfo getGroupInfoByGroupId(String groupId);

    /**
     * 根据GroupId更新数据
     */
    Integer updateGroupInfoByGroupId(GroupInfo bean, String groupId);

    /**
     * 根据GroupId删除数据
     */
    Integer deleteGroupInfoByGroupId(String groupId);

    /**
     * 保存群组信息-新增或修改
     * @param groupInfo
     * @param avatarFile
     * @param avatarCover
     */
    void saveGroup(GroupInfo groupInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException, BusinessException;

}
