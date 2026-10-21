package com.superpatient.auth;

/**
 * 微信登录凭证换取 openid。
 * Mock 与真实实现二选一，由 app.wechat.mock-enabled 控制。
 */
public interface WechatAuthClient {

    /**
     * @param jsCode 小程序 wx.login() 返回的一次性 code
     * @return 该微信用户在当前小程序下的 openid
     */
    String resolveOpenid(String jsCode);
}
