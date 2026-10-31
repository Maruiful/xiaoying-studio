package com.xiaoying.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** assetIds 按上传位顺序排列。 */
public record CreateTaskRequest(
        @NotBlank(message = "模板不能为空") String templateId,

        @NotEmpty(message = "请上传素材") @Size(max = 20, message = "素材数量过多")
        List<@NotNull(message = "素材 ID 不能为空") Long> assetIds) {}
