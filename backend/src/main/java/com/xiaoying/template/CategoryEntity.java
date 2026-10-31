package com.xiaoying.template;

import java.time.Instant;
import lombok.Data;

/** 对应 category 表。 */
@Data
public class CategoryEntity {
    private String id;
    private String name;
    private int sortOrder;
    private Instant createdAt;
}
