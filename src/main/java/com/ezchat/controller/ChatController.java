package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.constans.Constans;
import com.ezchat.entity.config.AppConfig;
import com.ezchat.entity.dto.MessageSendDTO;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.po.ChatMessage;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.enums.ResponseCodeEnum;
import com.ezchat.exception.BusinessException;
import com.ezchat.service.ChatMessageService;
import com.ezchat.service.ChatSessionUserService;
import com.ezchat.utils.StringTools;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

@RestController
@RequestMapping("/chat")
public class ChatController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private ChatSessionUserService chatSessionUserService;

    @Resource
    private AppConfig appConfig;


    /**
     * 发送消息，入库
     *
     * @param request
     * @param contactId
     * @param messageContent
     * @param messageType
     * @param fileSize
     * @param fileName
     * @param fileType
     * @return
     * @throws BusinessException
     */
    @RequestMapping("/sendMessage")
    @GlobalInterceptor
    public ResponseVo sendMessage(HttpServletRequest request,
                                  @NotEmpty String contactId,
                                  @NotEmpty @Max(500) String messageContent,
                                  @NotNull Integer messageType,
                                  Long fileSize,
                                  String fileName,
                                  Integer fileType) throws BusinessException {

        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContactId(contactId);
        chatMessage.setMessageContent(messageContent);
        chatMessage.setFileName(fileName);
        chatMessage.setFileSize(fileSize);
        chatMessage.setFileType(fileType);
        chatMessage.setMessageType(messageType);
        MessageSendDTO messageSendDTO = chatMessageService.saveAndSendMessage(chatMessage, tokenUserInfoDTO);
        return getSuccessResponseVo(messageSendDTO);
    }

    /**
     * 文件上传
     *
     * @param request
     * @param messageId
     * @param file
     * @param cover
     * @return
     * @throws BusinessException
     */
    @RequestMapping("/uploadFile")
    @GlobalInterceptor
    public ResponseVo uploadFile(HttpServletRequest request, @NotNull Long messageId,
                                 @NotNull MultipartFile file,
                                 @NotNull MultipartFile cover) throws BusinessException {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        chatMessageService.saveMessageFile(tokenUserInfoDTO.getUserId(), messageId, file, cover);
        return getSuccessResponseVo(null);
    }

    /**
     * 下载文件
     *
     * @param request
     * @param response
     * @param fileId--文件id相当于消息id
     * @param showCover
     * @return
     * @throws BusinessException
     */
    @RequestMapping("/downloadFile")
    @GlobalInterceptor
    public void downloadFile(HttpServletRequest request, HttpServletResponse response, @NotEmpty String fileId, @NotNull Boolean showCover) throws BusinessException {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        OutputStream out = null;
        FileInputStream in = null;
        try {
            File file = null;
            if (!StringTools.isNumber(fileId)) {
                //下载头像文件
                String avatarFolderName = Constans.FILE_FOLDER_FILE + Constans.FILE_FOLDER_AVATAR_NAME;
                String avatarPath = appConfig.getProjectFolder() + avatarFolderName + fileId + Constans.IMAGE_SUFFIX;
                if (showCover) {
                    avatarPath = avatarPath + Constans.COVER_IMAGE_SUFFIX;
                }
                file = new File(avatarPath);
                if (!file.exists()) {
                    throw new BusinessException(ResponseCodeEnum.CODE_602);
                }
            } else {
                file = chatMessageService.downloadFile(tokenUserInfoDTO, Long.parseLong(fileId), showCover);
            }

            response.setContentType("application/x-msdownload;charset=utf-8");
            response.setHeader("Content-Disposition", "attachment;");
            response.setContentLengthLong(file.length());
            in = new FileInputStream(file);
            //缓冲区
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            logger.error("下载文件失败", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    logger.error("关闭输出流失败", e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    logger.error("关闭输入流失败", e);
                }
            }
        }
    }
}