package com.xiaoying.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SwitchSessionRequest(
        @NotBlank(message = "用户 ID 不能为空") @Pattern(regexp = UserIds.PATTERN, message = "用户 ID 格式不正确")
        String userId) {}
