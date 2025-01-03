package com.ezchat.entity.vo;

import java.util.List;

public class AppUpdateVo {
    /**
     * 数据库id
     */
    private Integer id;

    /**
     * 版本号
     */
    private String version;

    /**
     *更新描述
     */
    private List<String> updateList;

    /**
     * 文件大小
     */
    private Long size;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 下载地址
     */
    private String outerLink;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }



    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getOuterLink() {
        return outerLink;
    }

    public void setOuterLink(String outerLink) {
        this.outerLink = outerLink;
    }

    public List<String> getUpdateList() {
        return updateList;
    }

    public void setUpdateList(List<String> updateList) {
        this.updateList = updateList;
    }
}
