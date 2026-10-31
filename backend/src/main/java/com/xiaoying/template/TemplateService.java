package com.xiaoying.template;

import com.xiaoying.common.BizException;
import com.xiaoying.common.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final CategoryMapper categoryMapper;
    private final TemplateMapper templateMapper;

    public List<CategoryVO> listCategories() {
        return categoryMapper.findAll().stream().map(TemplateConverter::toVO).toList();
    }

    /** category 为空时返回全部模板；不存在的分类返回空列表，由前端展示空状态。 */
    public List<TemplateCardVO> listTemplates(String categoryId) {
        String filter = StringUtils.hasText(categoryId) ? categoryId : null;
        return templateMapper.findAll(filter).stream()
                .map(TemplateConverter::toCardVO)
                .toList();
    }

    public TemplateDetailVO getTemplate(String id) {
        TemplateEntity template = requireTemplate(id);
        CategoryEntity category = categoryMapper.findById(template.getCategoryId());
        // category_id 有外键约束，分类必然存在
        return TemplateConverter.toDetailVO(template, category.getName());
    }

    /** 供上传、任务等模块校验模板是否存在。 */
    public TemplateEntity requireTemplate(String id) {
        TemplateEntity template = templateMapper.findById(id);
        if (template == null) {
            throw new BizException(ErrorCode.TEMPLATE_NOT_FOUND);
        }
        return template;
    }
}
