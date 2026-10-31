package com.xiaoying.pipeline;

import com.xiaoying.asset.FileStorage;
import com.xiaoying.task.TaskStage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 第三步：结果整理。按模板选一张预置结果图（classpath:demo/results/），复制到存储目录 results/{taskId}.svg，
 * 返回可预览的结果地址。
 */
@Component
public class OrganizeResultStep implements TaskStep {

    static final String FALLBACK_RESULT = "postcard";

    private final FileStorage fileStorage;

    public OrganizeResultStep(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    public static String resultPath(long taskId) {
        return "results/" + taskId + ".svg";
    }

    public static String resultUrl(long taskId) {
        return "/api/files/results/" + taskId;
    }

    @Override
    public TaskStage stage() {
        return TaskStage.ORGANIZE_RESULT;
    }

    @Override
    public String execute(StepContext context) throws InterruptedException {
        Simulator.simulate(context, stage());

        Resource template = presetFor(context.task().getTemplateId());
        try (InputStream in = template.getInputStream()) {
            fileStorage.save(resultPath(context.taskId()), in);
        } catch (IOException e) {
            throw new UncheckedIOException("保存结果图失败", e);
        }
        return resultUrl(context.taskId());
    }

    /** 模板 demo-postcard 对应 demo/results/postcard.svg；找不到时用默认结果图。 */
    private static Resource presetFor(String templateId) {
        String name = templateId.startsWith("demo-") ? templateId.substring("demo-".length()) : templateId;
        Resource preset = new ClassPathResource("demo/results/" + name + ".svg");
        return preset.exists() ? preset : new ClassPathResource("demo/results/" + FALLBACK_RESULT + ".svg");
    }
}
