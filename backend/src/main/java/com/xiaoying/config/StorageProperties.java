package com.xiaoying.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 本地文件存储，对应 app.storage.*。Compose 中指向数据卷 /data。 */
@ConfigurationProperties("app.storage")
public record StorageProperties(@DefaultValue("./data") Path dir) {}
