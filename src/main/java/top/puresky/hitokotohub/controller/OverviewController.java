package top.puresky.hitokotohub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.Queries;
import run.halo.app.plugin.ApiVersion;
import top.puresky.hitokotohub.extension.Category;
import top.puresky.hitokotohub.extension.Sentence;

@ApiVersion("console.api.hitokotohub.puresky.top/v1alpha1")
@RequestMapping("/overview")
@RestController
@RequiredArgsConstructor
@Tag(name = "OverviewV1alpha1")
@Slf4j
public class OverviewController {
    private final ReactiveExtensionClient client;

    @GetMapping("")
    @Operation(summary = "获取概览信息")
    public Mono<OverviewResponse> getOverview() {
        // 统计总数
        Mono<Long> sentenceCount = client.countBy(Sentence.class, null);
        Mono<Long> categoryCount = client.countBy(Category.class, null);
        Mono<Long> publishedSentenceCount = client.countBy(Sentence.class,
            ListOptions.builder()
                .fieldQuery(Queries.equal("status.isPublished", true))
                .build()
        );
        // 分类分布统计
        Mono<List<OverviewResponse.CategoryDistribution>> categoryDistribution =
            client.listAll(Category.class, null, Sort.unsorted())
                .flatMap(category -> {
                    OverviewResponse.CategoryDistribution dist =
                        new OverviewResponse.CategoryDistribution();
                    String categoryName = category.getMetadata().getName();
                    String displayName = category.getSpec().getName();
                    long totalCount = category.getStatus().getSentenceCount();

                    dist.setCategoryName(categoryName);
                    dist.setDisplayName(displayName);
                    dist.setCount(totalCount);

                    // 查询分类下已发布数量
                    return client.countBy(Sentence.class,
                            ListOptions.builder()
                                .fieldQuery(Queries.and(
                                    Queries.equal("spec.categoryName", categoryName),
                                    Queries.equal("status.isPublished", true)
                                )).build()
                        )
                        .doOnNext(s->{
                            dist.setPublishedCount(s);
                            dist.setNotPublishedCount(totalCount-s);
                        })
                        .thenReturn(dist);
                })
                .collectList();

        return Mono.zip(
                sentenceCount,
                categoryCount,
                publishedSentenceCount,
                categoryDistribution
            )
            .map(tuple -> {
                OverviewResponse response = new OverviewResponse();
                response.setSentenceCount(tuple.getT1());
                response.setCategoryCount(tuple.getT2());
                response.setPublishedSentenceCount(tuple.getT3());
                response.setNotPublishedSentenceCount(tuple.getT1()-tuple.getT3());
                response.setCategoryDistribution(tuple.getT4());
                return response;
            });
    }

    @Data
    public static class OverviewResponse {

        @Schema(description = "句子总数")
        private long sentenceCount;

        @Schema(description = "分类总数")
        private long categoryCount;

        @Schema(description = "已发布句子数")
        private long publishedSentenceCount;

        @Schema(description = "未发布句子数")
        private long notPublishedSentenceCount;


        @Schema(description = "各分类句子数量分布")
        private List<CategoryDistribution> categoryDistribution;

        // 分类分布项
        @Data
        public static class CategoryDistribution {
            @Schema(description = "分类 metadata name")
            private String categoryName;

            @Schema(description = "分类显示名称")
            private String displayName;

            @Schema(description = "句子数量")
            private long count;

            @Schema(description = "公开的句子数量")
            private long publishedCount;

            @Schema(description = "未公开的句子数量")
            private long notPublishedCount;
        }
    }
}
