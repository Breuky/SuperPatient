package com.superpatient.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.superpatient.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

/**
 * 真实实现：调用微信 jscode2session 换取 openid。
 * 未配置 appid/secret 时启动即失败，避免"以为接了真登录实际还是 Mock"。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.wechat.mock-enabled", havingValue = "false", matchIfMissing = true)
public class RealWechatAuthClient implements WechatAuthClient {

    private final RestClient restClient = RestClient.create("https://api.weixin.qq.com");
    private final String appid;
    private final String secret;

    public RealWechatAuthClient(@Value("${app.wechat.appid:}") String appid,
                                @Value("${app.wechat.secret:}") String secret) {
        if (appid.isBlank() || secret.isBlank()) {
            throw new IllegalStateException(
                    "已关闭 Mock 登录，但未配置 app.wechat.appid / app.wechat.secret；"
                            + "本地开发请在 application-local.yml 中设置 app.wechat.mock-enabled: true");
        }
        this.appid = appid;
        this.secret = secret;
    }

    @Override
    public String resolveOpenid(String jsCode) {
        if (jsCode == null || jsCode.isBlank()) {
            throw new BusinessException("登录凭证不能为空");
        }

        JsonNode body;
        try {
            body = restClient.get()
                    .uri(URI.create("https://api.weixin.qq.com/sns/jscode2session"
                            + "?appid=" + appid
                            + "&secret=" + secret
                            + "&js_code=" + jsCode
                            + "&grant_type=authorization_code"))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.error("调用微信 jscode2session 失败", e);
            throw new BusinessException("微信登录服务不可用，请稍后重试");
        }

        if (body == null || body.path("openid").isMissingNode() || body.path("openid").isNull()) {
            String errmsg = body == null ? "empty response" : body.path("errmsg").asText();
            log.warn("微信登录失败: {}", errmsg);
            throw new BusinessException("微信登录失败: " + errmsg);
        }
        // session_key 不下发、不入库
        return body.path("openid").asText();
    }
}
