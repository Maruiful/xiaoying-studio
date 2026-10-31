package com.xiaoying.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 匿名身份 cookie 的配置，对应 app.identity.*。 */
@ConfigurationProperties("app.identity")
public record IdentityProperties(
        @DefaultValue("uid") String cookieName,
        @DefaultValue("365d") Duration cookieMaxAge,
        /* 本地和 Compose 都是 http，默认不加 Secure；部署到 https 时设为 true */
        @DefaultValue("false") boolean cookieSecure) {}
