package com.xiaoying.user;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final IdentityCookie identityCookie;

    /** 返回当前用户；没有有效 cookie 时自动创建匿名用户。 */
    @GetMapping("/me")
    public UserVO me(CurrentUser user) {
        return UserVO.from(userService.require(user.id()));
    }

    /** 新建匿名用户并切换过去。 */
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserVO createUser(HttpServletResponse response) {
        UserEntity user = userService.create();
        identityCookie.write(response, user.getId());
        return UserVO.from(user);
    }

    /** 切换到已有用户。用户不存在时返回 404 USER_NOT_FOUND。 */
    @PostMapping("/session/switch")
    public UserVO switchUser(@Valid @RequestBody SwitchSessionRequest request, HttpServletResponse response) {
        UserEntity user = userService.require(request.userId());
        identityCookie.write(response, user.getId());
        return UserVO.from(user);
    }
}
