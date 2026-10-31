package com.xiaoying.user;

import java.time.Instant;
import lombok.Data;

/** 对应 app_user 表。匿名用户只有 id 和创建时间。 */
@Data
public class UserEntity {
    private String id;
    private Instant createdAt;
}
