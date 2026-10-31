package com.xiaoying.template;

import static org.assertj.core.api.Assertions.assertThat;

import com.xiaoying.TestcontainersConfiguration;
import com.xiaoying.common.ErrorVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/** 基于 V2__seed.sql 的种子数据验证模板接口。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TemplateApiIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    @DisplayName("分类按 sort_order 返回")
    void listsCategoriesInOrder() {
        CategoryVO[] categories = rest.getForObject("/api/categories", CategoryVO[].class);

        assertThat(categories).extracting(CategoryVO::id).containsExactly("creative", "festival", "portrait");
    }

    @Test
    @DisplayName("不带分类返回全部模板，带分类只返回该分类")
    void filtersTemplatesByCategory() {
        TemplateCardVO[] all = rest.getForObject("/api/templates", TemplateCardVO[].class);
        TemplateCardVO[] festival = rest.getForObject("/api/templates?category=festival", TemplateCardVO[].class);

        assertThat(all).hasSize(8);
        assertThat(festival)
                .extracting(TemplateCardVO::id)
                .containsExactly("demo-spring-festival", "demo-birthday", "demo-christmas");
    }

    @Test
    @DisplayName("不存在的分类返回空列表")
    void unknownCategoryReturnsEmpty() {
        TemplateCardVO[] none = rest.getForObject("/api/templates?category=nope", TemplateCardVO[].class);

        assertThat(none).isEmpty();
    }

    @Test
    @DisplayName("模板详情包含素材要求")
    void returnsTemplateDetail() {
        TemplateDetailVO detail = rest.getForObject("/api/templates/demo-postcard", TemplateDetailVO.class);

        assertThat(detail.title()).isEqualTo("旅行明信片");
        assertThat(detail.categoryName()).isEqualTo("创意图片");
        assertThat(detail.requiredImageCount()).isEqualTo(2);
        assertThat(detail.acceptedMimeTypes()).containsExactly("image/jpeg", "image/png");
        assertThat(detail.maxFileSizeBytes()).isEqualTo(5_242_880L);
    }

    @Test
    @DisplayName("模板不存在返回 404 TEMPLATE_NOT_FOUND")
    void missingTemplateReturns404() {
        var response = rest.getForEntity("/api/templates/nope", ErrorVO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("TEMPLATE_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("模板不存在");
    }

    @Test
    @DisplayName("未知路径和不支持的方法返回统一错误格式")
    void frameworkErrorsUseErrorVO() {
        var unknown = rest.getForEntity("/api/nothing-here", ErrorVO.class);
        var wrongMethod = rest.postForEntity("/api/categories", null, ErrorVO.class);

        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknown.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(wrongMethod.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(wrongMethod.getBody().code()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    @DisplayName("每个模板的封面都能访问")
    void everyCoverIsServed() {
        TemplateCardVO[] all = rest.getForObject("/api/templates", TemplateCardVO[].class);

        for (TemplateCardVO card : all) {
            var cover = rest.getForEntity(card.coverUrl(), byte[].class);
            assertThat(cover.getStatusCode()).as(card.coverUrl()).isEqualTo(HttpStatus.OK);
            assertThat(cover.getHeaders().getContentType()).isEqualTo(MediaType.valueOf("image/svg+xml"));
        }
    }
}
