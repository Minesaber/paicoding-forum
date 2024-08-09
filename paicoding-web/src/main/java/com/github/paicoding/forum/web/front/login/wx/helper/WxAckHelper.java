package com.github.paicoding.forum.web.front.login.wx.helper;

import com.github.paicoding.forum.api.model.vo.user.wx.BaseWxMsgResVo;
import com.github.paicoding.forum.api.model.vo.user.wx.WxImgTxtItemVo;
import com.github.paicoding.forum.api.model.vo.user.wx.WxImgTxtMsgResVo;
import com.github.paicoding.forum.api.model.vo.user.wx.WxTxtMsgResVo;
import com.github.paicoding.forum.core.util.CodeGenerateUtil;
import com.github.paicoding.forum.service.chatai.service.ChatgptService;
import com.github.paicoding.forum.service.user.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 处理微信公众号的回调信息
 */
@Slf4j
@Component
public class WxAckHelper {
    @Autowired
    private LoginService sessionService;
    @Autowired
    private WxLoginHelper qrLoginHelper;

    @Autowired
    private ChatgptService chatgptService;

    /**
     * 返回自动响应的文本
     */
    public BaseWxMsgResVo buildResponseBody(String eventType, String content, String fromUser) {
        // 执行文本消息或图文消息的初始化
        String textRes = null;
        List<WxImgTxtItemVo> imgTxtList = null;
        // 订阅
        if ("subscribe".equalsIgnoreCase(eventType)) {
            textRes = """
                    欢迎欢迎，热烈欢迎[笑脸]
                    """;
        }
        // fixme：公众号AI
        else if (chatgptService.inChat(fromUser, content)) {
            try {
                textRes = chatgptService.chat(fromUser, content);
            } catch (Exception e) {
                log.error("派聪明 访问异常! content: {}", content, e);
                textRes = "派聪明 出了点小状况，请稍后再试!";
            }
        }
        // fixme：返回图文消息
        else if ("图文消息".equalsIgnoreCase(content)) {
            WxImgTxtItemVo imgTxt = new WxImgTxtItemVo();
            imgTxt.setTitle("示例");
            imgTxt.setDescription("示例：返回图文消息");
            imgTxt.setPicUrl("https://s3.uuu.ovh/imgs/2023/06/11/9c2d829b8e2e4147.jpg");
            imgTxt.setUrl("https://www.example.com");
            imgTxtList = Arrays.asList(imgTxt);
        }
        // 微信公众号登录
        else if (CodeGenerateUtil.isVerifyCode(content)) {
            sessionService.autoRegisterWxUserInfo(fromUser);
            if (qrLoginHelper.login(content)) {
                textRes = "登录成功";
            } else {
                textRes = "验证码过期了，刷新登录页面重试一下吧";
            }
        }
        // 其他类型消息的处理
        else {
            textRes = """
                    听不懂你在说什么[破涕为笑]
                    """;
        }
        // 返回文本消息或图文消息
        if (textRes != null) {
            WxTxtMsgResVo vo = new WxTxtMsgResVo();
            vo.setContent(textRes);
            return vo;
        } else {
            WxImgTxtMsgResVo vo = new WxImgTxtMsgResVo();
            vo.setArticles(imgTxtList);
            vo.setArticleCount(imgTxtList.size());
            return vo;
        }
    }
}
