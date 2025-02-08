package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.entity.config.AppConfig;
import com.ezchat.entity.dto.MessageSendDTO;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.po.ChatMessage;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.exception.BusinessException;
import com.ezchat.service.ChatMessageService;
import com.ezchat.service.ChatSessionUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

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

    @RequestMapping("/uploadFile")
    @GlobalInterceptor
    public ResponseVo sendMessage(HttpServletRequest request, @NotNull Long messageId,
                                  @NotNull MultipartFile file,
                                  @NotNull MultipartFile cover) throws BusinessException {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        chatMessageService.saveMessageFile(tokenUserInfoDTO.getUserId(), messageId, file, cover);
        return getSuccessResponseVo(null);
    }
}
