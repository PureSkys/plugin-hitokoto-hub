package top.puresky.hitokotohub.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.Queries;
import top.puresky.hitokotohub.extension.Category;
import top.puresky.hitokotohub.extension.Sentence;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverviewConsoleEndpoint implements CustomEndpoint {

    private static final String TAG = "OverviewV1alpha1";
    private static final String GROUP_VERSION = "console.api.hitokotohub.puresky.top/v1alpha1";

    private final ReactiveExtensionClient client;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return route()
            .GET("overview", this::getOverview, builder -> builder
                .operationId("getOverview")
                .summary("获取概览信息")
                .tag(TAG)
                .response(responseBuilder()
                    .implementation(OverviewResponse.class)))
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(GROUP_VERSION);
    }

    private Mono<ServerResponse> getOverview(ServerRequest request) {
        Mono<Long> sentenceCount = client.countBy(Sentence.class, null);
        Mono<Long> categoryCount = client.countBy(Category.class, null);
        Mono<Long> publishedSentenceCount = client.countBy(Sentence.class,
            ListOptions.builder()
                .fieldQuery(Queries.equal("status.isPublished", true))
                .build()
        );
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

                    return client.countBy(Sentence.class,
                            ListOptions.builder()
                                .fieldQuery(Queries.and(
                                    Queries.equal("spec.categoryName", categoryName),
                                    Queries.equal("status.isPublished", true)
                                )).build()
                        )
                        .doOnNext(count -> {
                            dist.setPublishedCount(count);
                            dist.setNotPublishedCount(totalCount - count);
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
                response.setNotPublishedSentenceCount(tuple.getT1() - tuple.getT3());
                response.setCategoryDistribution(tuple.getT4());
                return response;
            })
            .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }

    @Data
    @Schema(name = "OverviewResponse")
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

        @Data
        @Schema(name = "CategoryDistribution")
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
