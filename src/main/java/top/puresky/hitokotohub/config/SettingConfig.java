package top.puresky.hitokotohub.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import reactor.core.publisher.Mono;

public interface SettingConfig {
    Mono<BasicConfig> getBasicConfig();

    @Data
    class BasicConfig {
        public static final String GROUP = "basic";
        @Schema(description = "最大随机条数")
        private String maxRandomLimit;
        @Schema(description = "默认随机条数")
        private String randomLimit;
        @Schema(description = "默认分类")
        private String defaultCategory;
        @Schema(description = "默认返回格式")
        private String encode;
        @Schema(description = "点赞冷却时间（小时）")
        private String likeCooldown;
        @Schema(description = "启用浏览量统计")
        private String enableViewCount;
    }
}
