package com.xiaoying.user;

import java.time.Instant;

public record UserVO(String id, Instant createdAt) {

    static UserVO from(UserEntity e) {
        return new UserVO(e.getId(), e.getCreatedAt());
    }
}
