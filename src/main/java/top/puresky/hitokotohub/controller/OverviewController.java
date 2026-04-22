package top.puresky.hitokotohub.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.ApiVersion;
import io.swagger.v3.oas.annotations.tags.Tag;

@ApiVersion("console.api.hitokotohub.puresky.top/v1alpha1")
@RequestMapping("/overview")
@RestController
@RequiredArgsConstructor
@Tag(name = "OverviewV1alpha1")
public class OverviewController {
    private final ReactiveExtensionClient client;

    @GetMapping("/")
    @Operation(summary = "获取概览信息")
    public Mono<String> getOverview() {
        return Mono.just("Hello, World!");
    }

    @Data
    public static class OverviewResponse {
        private long sentenceCount;
        private long categoryCount;
        private long totalSentences;
        private long likeCount;
        private long viewCount;
    }
}
