package com.github.paicoding.forum.web.front.login.wx.callback;

import com.github.paicoding.forum.api.model.vo.user.wx.BaseWxMsgResVo;
import com.github.paicoding.forum.api.model.vo.user.wx.WxTxtMsgReqVo;
import com.github.paicoding.forum.api.model.vo.user.wx.WxTxtMsgResVo;
import com.github.paicoding.forum.service.user.service.LoginService;
import com.github.paicoding.forum.web.front.login.wx.helper.WxLoginHelper;
import com.github.paicoding.forum.web.front.login.wx.helper.WxAckHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 处理微信公众号的回调请求
 */
@RequestMapping(path = "wx")
@RestController
public class WxCallbackRestController {
    @Autowired
    private LoginService sessionService;
    @Autowired
    private WxLoginHelper qrLoginHelper;
    @Autowired
    private WxAckHelper wxHelper;

    /**
     * 公众号接入：token校验
     */
    @GetMapping(path = "callback")
    public String check(HttpServletRequest request) {
        String echoStr = request.getParameter("echostr");
        if (StringUtils.isNoneEmpty(echoStr)) {
            return echoStr;
        }
        return "";
    }

    /**
     * fixme：需要做防刷校验
     * 公众号回调：各类事件的处理
     */
    @PostMapping(path = "callback",
            consumes = {"application/xml", "text/xml"},
            produces = "application/xml;charset=utf-8")
    public BaseWxMsgResVo callBack(@RequestBody WxTxtMsgReqVo msg) {
        /*
        fixme：带参数二维码需要企业微信认证
        1、扫描带参数的二维码事件
            1）如果用户还未关注公众号，则用户可以关注公众号，关注后微信会将带场景值关注事件推送给开发者。
            2）如果用户已经关注公众号，则微信会将带场景值扫描事件推送给开发者。
         */
        if ("scan".equalsIgnoreCase(msg.getEvent())) {
            // 扫描带参数的二维码事件：获取二维码携带的场景值
            String key = msg.getEventKey();
            if (StringUtils.isNotBlank(key) || key.startsWith("qrscene_")) {
                String code = key.substring("qrscene_".length());
                // 更新服务端的注册、登录状态
                sessionService.autoRegisterWxUserInfo(msg.getFromUserName());
                // 寻找半长连接，推送状态更新
                qrLoginHelper.login(code);
                WxTxtMsgResVo res = new WxTxtMsgResVo();
                res.setContent("登录成功");
                fillResVo(res, msg);
                return res;
            }
        }
        /*
        2、其他事件的处理
         */
        BaseWxMsgResVo res = wxHelper.buildResponseBody(msg.getEvent(), msg.getContent(), msg.getFromUserName());
        fillResVo(res, msg);
        return res;
    }

    private void fillResVo(BaseWxMsgResVo res, WxTxtMsgReqVo msg) {
        res.setFromUserName(msg.getToUserName());
        res.setToUserName(msg.getFromUserName());
        res.setCreateTime(System.currentTimeMillis() / 1000);
    }
}
