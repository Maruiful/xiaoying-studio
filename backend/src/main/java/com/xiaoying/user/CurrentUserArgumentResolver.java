package com.xiaoying.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 为声明了 CurrentUser 参数的接口解析当前用户。
 *
 * <p>只在需要用户的接口上按需创建匿名用户，模板、健康检查、静态文件等接口不会创建用户。
 * 如果改用对所有请求生效的过滤器，Compose 健康检查和 Server Component 不带 cookie 的请求都会产生无用的用户记录。
 */
@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final IdentityCookie identityCookie;
    private final UserService userService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == CurrentUser.class;
    }

    @Override
    public CurrentUser resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        var existing = identityCookie.read(request).flatMap(userService::find);
        if (existing.isPresent()) {
            return new CurrentUser(existing.get().getId());
        }
        UserEntity created = userService.create();
        identityCookie.write(webRequest.getNativeResponse(HttpServletResponse.class), created.getId());
        return new CurrentUser(created.getId());
    }
}
