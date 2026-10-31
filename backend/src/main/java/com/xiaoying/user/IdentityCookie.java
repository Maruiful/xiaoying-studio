package com.xiaoying.user;

import com.xiaoying.config.IdentityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** 读写身份 cookie。cookie 只保存 userId，HttpOnly，前端脚本读不到。 */
@Component
@RequiredArgsConstructor
public class IdentityCookie {

    private final IdentityProperties props;

    /** cookie 不存在或不是合法的 UUID 时返回 empty。 */
    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> props.cookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .filter(UserIds::isValid)
                .findFirst();
    }

    public void write(HttpServletResponse response, String userId) {
        ResponseCookie cookie = ResponseCookie.from(props.cookieName(), userId)
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(props.cookieMaxAge())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
