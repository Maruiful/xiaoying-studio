package com.xiaoying.user;

/**
 * 当前请求的用户。在 Controller 方法参数中声明即可，由 CurrentUserArgumentResolver 解析：
 * cookie 有效时取对应用户，否则创建匿名用户并写入 cookie。
 */
public record CurrentUser(String id) {}
