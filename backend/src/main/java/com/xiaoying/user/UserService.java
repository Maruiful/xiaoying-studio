package com.xiaoying.user;

import com.xiaoying.common.BizException;
import com.xiaoying.common.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final Clock clock;

    public Optional<UserEntity> find(String id) {
        return Optional.ofNullable(userMapper.findById(id));
    }

    public UserEntity require(String id) {
        return find(id).orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
    }

    /** 创建匿名用户。不记录日志：userId 就是 cookie 的值，属于凭证。 */
    public UserEntity create() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString());
        // 列是 DATETIME(3)，先截到毫秒，避免 MySQL 四舍五入后与返回值不一致
        user.setCreatedAt(Instant.now(clock).truncatedTo(ChronoUnit.MILLIS));
        userMapper.insert(user);
        return user;
    }
}
