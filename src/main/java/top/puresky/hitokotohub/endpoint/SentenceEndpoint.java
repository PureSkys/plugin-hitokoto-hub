package top.puresky.hitokotohub.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ReactiveExtensionClient;
import top.puresky.hitokotohub.extension.Sentence;

@Component
public class SentenceEndpoint implements CustomEndpoint {
    private final ReactiveExtensionClient client;

    public SentenceEndpoint(ReactiveExtensionClient client) {
        this.client = client;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = "SentenceV1alpha1";
        return SpringdocRouteBuilder.route()
            .POST("/sentence/batch", this::batchCreateSentence,
                builder -> builder.operationId("batchCreateSentence")
                    .description("Batch create sentences")
                    .tag(tag)
                    .requestBody(requestBodyBuilder().implementation(Sentence[].class))
                    .response(responseBuilder().implementation(BatchResult.class))
            )
            .GET("/sentence/get", this::getSentence,
                builder -> builder.operationId("getSentence")
                    .description("Get a sentence")
                    .tag(tag)
            )
            .build();
    }

    private Mono<ServerResponse> getSentence(ServerRequest request) {
        return ServerResponse.ok().bodyValue("getSentence");
    }

    private Mono<ServerResponse> batchCreateSentence(ServerRequest request) {
        // 统计总数、成功数、失败数
        AtomicInteger total = new AtomicInteger(0);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        return request.bodyToFlux(Sentence.class)
            .doOnNext(this::initMetadata)
            .flatMap(sentence -> client.create(sentence)
                .doOnSuccess(s -> success.incrementAndGet()) // 成功+1
                .onErrorResume(e -> {
                    failed.incrementAndGet(); // 失败+1
                    return Mono.empty(); // 出错不中断整个批量
                })).then(Mono.fromCallable(() -> {
                BatchResult result = new BatchResult();
                result.setTotal(total.get());
                result.setSuccess(success.get());
                result.setFailed(failed.get());
                return result;
            }))
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private void initMetadata(Sentence sentence) {
        // 统一使用sentence-作为generateName前缀
        sentence.getMetadata().setGenerateName("sentence-");
        // 初始化状态
        if (sentence.getStatus() == null) {
            Sentence.Status status = new Sentence.Status();
            status.setViewCount(0);
            status.setLikeCount(0);
            sentence.setStatus(status);
        }
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.hitokotohub.puresky.top/v1alpha1");
    }

    // ========== 返回结果 ==========
    @Data
    public static class BatchResult {
        private int total;
        private int success;
        private int failed;

    }
}
