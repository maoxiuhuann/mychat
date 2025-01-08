package com.ezchat.entity.config;

import com.ezchat.utils.StringTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("appconfig")
public class AppConfig {

    /**
     * websocket 端口
     */
    @Value("${ws.port:}")
    private Integer wsPort;

    /**
     * 文件目录
     */
    @Value("${project.folder:}")
    private String projectFolder;

    /**
     * 管理员邮箱
     */
    @Value("${admin.emails:}")
    private String adminEmails;

    public Integer getWsPort() {
        return wsPort;
    }

    public String getProjectFolder() {
        if (StringTools.isEmpty(projectFolder) && !projectFolder.endsWith("/")) {
            projectFolder = projectFolder + "/";
        }
        return projectFolder;
    }


    public String getAdminEmails() {
        return adminEmails;
    }
}
