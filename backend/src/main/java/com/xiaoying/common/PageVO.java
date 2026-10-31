package com.xiaoying.common;

import java.util.List;

/** 分页响应。page 从 1 开始。 */
public record PageVO<T>(List<T> items, long total, int page, int size) {}
