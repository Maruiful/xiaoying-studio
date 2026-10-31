package com.xiaoying.pipeline;

import com.xiaoying.asset.AssetEntity;
import com.xiaoying.asset.AssetService;
import com.xiaoying.task.TaskStage;
import com.xiaoying.template.TemplateEntity;
import com.xiaoying.template.TemplateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 第一步：素材检查。在后台重新确认素材可用：数量符合模板要求、都属于任务的用户、文件仍在磁盘上、类型被模板接受。
 * 提交接口已经校验过一次，这里防止提交到执行之间素材发生变化。
 */
@Component
@RequiredArgsConstructor
public class CheckAssetsStep implements TaskStep {

    private final AssetService assetService;
    private final TemplateService templateService;

    @Override
    public TaskStage stage() {
        return TaskStage.CHECK_ASSETS;
    }

    @Override
    public String execute(StepContext context) throws InterruptedException {
        TemplateEntity template = templateService.requireTemplate(context.task().getTemplateId());
        List<AssetEntity> assets = assetService.findOwned(context.task().getUserId(), context.assetIds());

        if (context.assetIds().size() != template.getRequiredImageCount()
                || assets.size() != context.assetIds().size()) {
            throw new StepFailedException("素材数量与模板要求不符");
        }
        for (AssetEntity asset : assets) {
            if (!assetService.fileExists(asset)) {
                throw new StepFailedException("素材文件缺失：" + asset.getOriginalName());
            }
            if (!template.getAcceptedMimeTypes().contains(asset.getMimeType())) {
                throw new StepFailedException("素材格式不被模板接受：" + asset.getOriginalName());
            }
        }

        Simulator.simulate(context, stage());
        return "已检查 " + assets.size() + " 张素材";
    }
}
