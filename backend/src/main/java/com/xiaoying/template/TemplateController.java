package com.xiaoying.template;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping("/categories")
    public List<CategoryVO> listCategories() {
        return templateService.listCategories();
    }

    @GetMapping("/templates")
    public List<TemplateCardVO> listTemplates(@RequestParam(required = false) String category) {
        return templateService.listTemplates(category);
    }

    @GetMapping("/templates/{id}")
    public TemplateDetailVO getTemplate(@PathVariable String id) {
        return templateService.getTemplate(id);
    }
}
