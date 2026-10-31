package com.xiaoying.user;

import java.util.regex.Pattern;

/** 用户 ID 是小写 UUID。读取 cookie 和校验请求参数时，先检查格式，再查数据库。 */
public final class UserIds {

    public static final String PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    private static final Pattern COMPILED = Pattern.compile(PATTERN);

    private UserIds() {}

    public static boolean isValid(String value) {
        return value != null && COMPILED.matcher(value).matches();
    }
}
