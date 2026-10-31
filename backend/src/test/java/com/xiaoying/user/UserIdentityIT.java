package com.xiaoying.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.xiaoying.TestcontainersConfiguration;
import com.xiaoying.common.ErrorVO;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class UserIdentityIT {

    private static final Pattern UID_COOKIE = Pattern.compile("^uid=([^;]+);");

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("首次访问 /api/me 创建匿名用户并签发 HttpOnly cookie")
    void firstVisitIssuesCookie() {
        ResponseEntity<UserVO> res = rest.getForEntity("/api/me", UserVO.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        String setCookie = res.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("HttpOnly").contains("SameSite=Lax").contains("Path=/");
        assertThat(uidFrom(res)).isEqualTo(res.getBody().id());
        assertThat(userExists(res.getBody().id())).isTrue();
    }

    @Test
    @DisplayName("带上 cookie 再访问时识别为同一用户，不重新签发")
    void sameCookieSameUser() {
        String uid = uidFrom(rest.getForEntity("/api/me", UserVO.class));

        ResponseEntity<UserVO> again = get("/api/me", uid, UserVO.class);

        assertThat(again.getBody().id()).isEqualTo(uid);
        assertThat(again.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    @DisplayName("cookie 格式非法或用户不存在时创建新用户")
    void invalidCookieCreatesNewUser() {
        ResponseEntity<UserVO> garbage = get("/api/me", "not-a-uuid", UserVO.class);
        ResponseEntity<UserVO> unknown = get("/api/me", "00000000-0000-0000-0000-000000000000", UserVO.class);

        assertThat(garbage.getBody().id()).isNotEqualTo("not-a-uuid");
        assertThat(uidFrom(garbage)).isEqualTo(garbage.getBody().id());
        assertThat(unknown.getBody().id()).isNotEqualTo("00000000-0000-0000-0000-000000000000");
        assertThat(uidFrom(unknown)).isEqualTo(unknown.getBody().id());
    }

    @Test
    @DisplayName("新建匿名用户返回 201，cookie 切换到新用户")
    void createUserSwitchesCookie() {
        String oldUid = uidFrom(rest.getForEntity("/api/me", UserVO.class));

        ResponseEntity<UserVO> res = post("/api/users", oldUid, null, UserVO.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().id()).isNotEqualTo(oldUid);
        assertThat(uidFrom(res)).isEqualTo(res.getBody().id());
    }

    @Test
    @DisplayName("切换到已有用户后 cookie 指向该用户")
    void switchToExistingUser() {
        String a = uidFrom(rest.getForEntity("/api/me", UserVO.class));
        String b = uidFrom(rest.getForEntity("/api/me", UserVO.class));

        ResponseEntity<UserVO> res = post("/api/session/switch", b, Map.of("userId", a), UserVO.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().id()).isEqualTo(a);
        assertThat(uidFrom(res)).isEqualTo(a);
        assertThat(get("/api/me", a, UserVO.class).getBody().id()).isEqualTo(a);
    }

    @Test
    @DisplayName("切换到不存在的用户返回 404，格式错误返回 400，cookie 都不变")
    void switchRejectsUnknownOrMalformed() {
        ResponseEntity<ErrorVO> unknown = post(
                "/api/session/switch", null, Map.of("userId", "00000000-0000-0000-0000-000000000000"), ErrorVO.class);
        ResponseEntity<ErrorVO> malformed = post("/api/session/switch", null, Map.of("userId", "abc"), ErrorVO.class);

        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknown.getBody().code()).isEqualTo("USER_NOT_FOUND");
        assertThat(unknown.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(malformed.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(malformed.getBody().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(malformed.getBody().details()).containsKey("userId");
        assertThat(malformed.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    @DisplayName("不需要用户的接口不会创建用户，也不签发 cookie")
    void publicEndpointsDoNotCreateUsers() {
        int before = userCount();

        List<String> paths = List.of("/api/health", "/api/categories", "/api/templates", "/api/templates/nope");
        for (String path : paths) {
            assertThat(rest.getForEntity(path, String.class).getHeaders().get(HttpHeaders.SET_COOKIE))
                    .as(path)
                    .isNull();
        }

        assertThat(userCount()).isEqualTo(before);
    }

    private <T> ResponseEntity<T> get(String path, String uid, Class<T> type) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(cookieHeaders(uid)), type);
    }

    private <T> ResponseEntity<T> post(String path, String uid, Object body, Class<T> type) {
        HttpHeaders headers = cookieHeaders(uid);
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), type);
    }

    private static HttpHeaders cookieHeaders(String uid) {
        HttpHeaders headers = new HttpHeaders();
        if (uid != null) {
            headers.add(HttpHeaders.COOKIE, "uid=" + uid);
        }
        return headers;
    }

    private static String uidFrom(ResponseEntity<?> res) {
        String setCookie = res.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("Set-Cookie").isNotNull();
        Matcher m = UID_COOKIE.matcher(setCookie);
        assertThat(m.find()).as(setCookie).isTrue();
        return m.group(1);
    }

    private boolean userExists(String id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE id = ?", Integer.class, id) == 1;
    }

    private int userCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM app_user", Integer.class);
    }
}
