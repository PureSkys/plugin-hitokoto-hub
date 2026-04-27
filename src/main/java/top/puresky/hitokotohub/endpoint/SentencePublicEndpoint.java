package top.puresky.hitokotohub.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.data.domain.Sort;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.Queries;
import top.puresky.hitokotohub.extension.Category;
import top.puresky.hitokotohub.extension.Sentence;

@Component
@RequiredArgsConstructor
@Slf4j
public class SentencePublicEndpoint implements CustomEndpoint {

    private static final String TAG = "SentencePublicV1alpha1";
    private static final String GROUP_VERSION = "public.api.hitokotohub.puresky.top/v1alpha1";
    private static final long LIKE_COOLDOWN = Duration.ofHours(12).toMillis();

    private final ReactiveExtensionClient client;
    private final Map<String, Long> likeCache = new ConcurrentHashMap<>();

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return route()
            .GET("sentence/random", this::getRandomSentences, builder -> builder
                .operationId("getRandomSentences")
                .summary("随机获取句子")
                .tag(TAG)
                .parameter(parameterBuilder()
                    .in(ParameterIn.QUERY)
                    .name("categoryName")
                    .description("分类名称，不传则返回所有类型")
                    .implementation(String.class)
                    .required(false))
                .parameter(parameterBuilder()
                    .in(ParameterIn.QUERY)
                    .name("limit")
                    .description("返回数量，默认8条，最多20条")
                    .implementation(Integer.class)
                    .required(false))
                .response(responseBuilder()
                    .implementation(RandomSentenceResponse.class)))
            .GET("sentence/like", this::toggleLike, builder -> builder
                .operationId("toggleLike")
                .summary("点赞/取消点赞句子")
                .tag(TAG)
                .parameter(parameterBuilder()
                    .in(ParameterIn.QUERY)
                    .name("name")
                    .description("句子名称")
                    .implementation(String.class)
                    .required(true))
                .parameter(parameterBuilder()
                    .in(ParameterIn.QUERY)
                    .name("action")
                    .description("操作类型，like 或 unlike")
                    .implementation(String.class)
                    .required(false))
                .response(responseBuilder()
                    .implementation(LikeResponse.class)))
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(GROUP_VERSION);
    }

    private Mono<ServerResponse> getRandomSentences(ServerRequest request) {
        String categoryName = request.queryParam("categoryName").orElse(null);
        int limit = request.queryParam("limit").map(Integer::parseInt).orElse(8);
        int actualLimit = Math.min(limit, 20);

        ListOptions options;
        if (categoryName != null && !categoryName.isBlank()) {
            options = ListOptions.builder().fieldQuery(
                Queries.and(Queries.equal("spec.categoryName", categoryName),
                    Queries.equal("status.isPublished", true),
                    Queries.isNull("metadata.deletionTimestamp"))).build();
        } else {
            options = ListOptions.builder().fieldQuery(
                Queries.and(Queries.equal("status.isPublished", true),
                    Queries.isNull("metadata.deletionTimestamp"))).build();
        }

        Mono<String> displayNameMono;
        if (categoryName != null && !categoryName.isBlank()) {
            displayNameMono = client.get(Category.class, categoryName)
                .map(category -> category.getSpec().getName()).defaultIfEmpty(categoryName);
        } else {
            displayNameMono = Mono.just("全部");
        }

        return client.countBy(Sentence.class, options).filter(total -> total > 0).flatMap(total -> {
            int totalPages = (int) Math.ceil((double) total / actualLimit);
            int randomPage = RandomUtils.insecure().randomInt(1, totalPages + 1);

            return client.listBy(Sentence.class, options,
                    PageRequestImpl.of(randomPage, actualLimit, Sort.unsorted()))
                .map(ListResult::getItems);
        }).defaultIfEmpty(Collections.emptyList()).zipWith(displayNameMono).map(tuple -> {
            List<Sentence> sentences = tuple.getT1();
            String displayName = tuple.getT2();

            Collections.shuffle(sentences);

            List<SentenceItem> items = sentences.stream().map(this::toSentenceItem).toList();

            RandomSentenceResponse response = new RandomSentenceResponse();
            response.setCategoryName(displayName);
            response.setRequested(actualLimit);
            response.setReturned(items.size());
            response.setSentences(items);
            return response;
        }).flatMap(response -> ServerResponse.ok().bodyValue(response));
    }

    private Mono<ServerResponse> toggleLike(ServerRequest request) {
        String name = request.queryParam("name").orElse("");
        String action = request.queryParam("action").orElse("like");
        String ip = getClientIp(request.exchange().getRequest());
        String likeKey = ip + ":like:" + name;
        String unlikeKey = ip + ":unlike:" + name;
        boolean isUnlike = "unlike".equals(action);

        String checkKey = isUnlike ? unlikeKey : likeKey;
        Long lastTime = likeCache.get(checkKey);
        long now = System.currentTimeMillis();

        if (lastTime != null && (now - lastTime) < LIKE_COOLDOWN) {
            long remainingSeconds = 12 * 60 * 60 - ((now - lastTime) / 1000);
            return client.get(Sentence.class, name)
                .map(sentence -> buildLikeResponse(sentence, false,
                    "请在 " + formatRemainingTime(remainingSeconds) + " 后再"
                        + (isUnlike ? "取消点赞" : "点赞"),
                    "rate_limited"))
                .defaultIfEmpty(buildErrorResponse())
                .flatMap(response -> ServerResponse.ok().bodyValue(response));
        }

        String oppositeKey = isUnlike ? likeKey : unlikeKey;

        return client.get(Sentence.class, name)
            .flatMap(sentence -> {
                if (sentence.getStatus() == null) {
                    sentence.setStatus(new Sentence.Status());
                }

                long currentLikes = sentence.getStatus().getLikeCount();

                if (isUnlike) {
                    sentence.getStatus().setLikeCount(Math.max(0, currentLikes - 1));
                } else {
                    sentence.getStatus().setLikeCount(currentLikes + 1);
                }

                return client.update(sentence).map(updated -> {
                    likeCache.put(checkKey, System.currentTimeMillis());
                    likeCache.remove(oppositeKey);
                    return buildLikeResponse(updated, true,
                        isUnlike ? "取消点赞成功" : "点赞成功", "ok");
                });
            })
            .defaultIfEmpty(buildErrorResponse())
            .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }

    private LikeResponse buildLikeResponse(Sentence sentence, boolean success, String message,
        String code) {
        LikeResponse response = new LikeResponse();
        response.setSuccess(success);
        response.setMessage(message);
        response.setCode(code);
        response.setSentence(toSentenceItem(sentence));
        return response;
    }

    private LikeResponse buildErrorResponse() {
        LikeResponse response = new LikeResponse();
        response.setSuccess(false);
        response.setMessage("句子不存在");
        response.setCode("not_found");
        response.setSentence(null);
        return response;
    }

    private SentenceItem toSentenceItem(Sentence s) {
        SentenceItem item = new SentenceItem();
        item.setAuthor(s.getSpec().getAuthor());
        item.setContent(s.getSpec().getContent());
        item.setSource(s.getSpec().getSource());
        item.setCreatedBy(s.getSpec().getCreatedBy());
        item.setLikeCount(s.getStatus() != null ? s.getStatus().getLikeCount() : 0);
        item.setViewCount(s.getStatus() != null ? s.getStatus().getViewCount() : 0);
        return item;
    }

    private String formatRemainingTime(long seconds) {
        if (seconds < 60) {
            return seconds + " 秒";
        }
        if (seconds < 3600) {
            return (seconds / 60) + " 分钟";
        }
        return (seconds / 3600) + " 小时";
    }

    private String getClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
            ? request.getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }

    @Data
    @Schema(name = "RandomSentenceResponse")
    public static class RandomSentenceResponse {
        @Schema(description = "请求的分类名称")
        private String categoryName;
        @Schema(description = "请求数量")
        private long requested;
        @Schema(description = "实际返回数量")
        private long returned;
        @Schema(description = "句子列表")
        private List<SentenceItem> sentences;
    }

    @Data
    @Schema(name = "SentenceItem")
    public static class SentenceItem {
        @Schema(description = "作者")
        private String author;
        @Schema(description = "内容")
        private String content;
        @Schema(description = "来源")
        private String source;
        @Schema(description = "创建者")
        private String createdBy;
        @Schema(description = "点赞数")
        private long likeCount;
        @Schema(description = "浏览数")
        private long viewCount;
    }

    @Data
    @Schema(name = "LikeResponse")
    public static class LikeResponse {
        @Schema(description = "是否成功")
        private boolean success;
        @Schema(description = "状态码：ok / rate_limited / not_found")
        private String code;
        @Schema(description = "提示信息")
        private String message;
        @Schema(description = "句子完整信息")
        private SentenceItem sentence;
    }
}
