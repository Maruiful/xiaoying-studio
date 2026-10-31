package com.xiaoying;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class XiaoyingApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaoyingApplication.class, args);
    }
}
