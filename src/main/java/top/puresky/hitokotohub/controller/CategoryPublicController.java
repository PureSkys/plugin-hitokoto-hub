package top.puresky.hitokotohub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.Queries;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.plugin.ApiVersion;
import top.puresky.hitokotohub.extension.Category;

@RestController
@RequiredArgsConstructor
@ApiVersion("api.hitokotohub.puresky.top/v1alpha1")
@RequestMapping("/category")
@Tag(name = "CategoryPublicV1alpha1")
public class CategoryPublicController {

    private final ReactiveExtensionClient client;

    @GetMapping("/list")
    @Operation(summary = "获取所有分类")
    public Mono<List<CategoryItem>> listCategories() {
        var listOptions = new ListOptions();
        listOptions.setFieldSelector(
            FieldSelector.of(Queries.isNull("metadata.deletionTimestamp")));

        return client.listAll(Category.class, listOptions, Sort.unsorted()).collectList()
            .map(categories -> categories.stream().map(category -> {
                CategoryItem item = new CategoryItem();
                item.setName(category.getMetadata().getName());
                item.setDisplayName(category.getSpec().getName());
                item.setDescription(category.getSpec().getDescription());
                item.setSentenceCount(
                    category.getStatus() != null ? category.getStatus().getSentenceCount() : 0);
                return item;
            }).toList());
    }

    @Data
    @Schema(name = "CategoryItem")
    public static class CategoryItem {
        @Schema(description = "分类标识")
        private String name;
        @Schema(description = "分类显示名称")
        private String displayName;
        @Schema(description = "分类描述")
        private String description;
        @Schema(description = "句子数量")
        private long sentenceCount;
    }
}