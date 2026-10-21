package com.superpatient.auth;

import com.superpatient.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 开发用实现：直接把 code 当作 openid，不做任何校验。
 * 仅供本地/演示环境使用，上线必须关闭（app.wechat.mock-enabled=false）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.wechat.mock-enabled", havingValue = "true")
public class MockWechatAuthClient implements WechatAuthClient {

    @PostConstruct
    void warnOnStartup() {
        log.warn("微信登录处于 Mock 模式：任意 code 都会被当作 openid，"
                + "任何人可冒用他人身份登录。仅限本地开发，切勿部署到公网。");
    }

    @Override
    public String resolveOpenid(String jsCode) {
        if (jsCode == null || jsCode.isBlank()) {
            throw new BusinessException("登录凭证不能为空");
        }
        return jsCode;
    }
}
