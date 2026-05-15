package top.puresky.hitokotohub.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@Data
@EqualsAndHashCode(callSuper = true)
@GVK(
    group = "hitokotohub.puresky.top",
    version = "v1alpha1",
    kind = "CategoryViewRecord",
    plural = "categoryviewrecords",
    singular = "categoryviewrecord"
)
public class CategoryViewRecord extends AbstractExtension {

    public enum EventType {
        VIEW,
        LIKE,
        UNLIKE
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    @Data
    public static class Spec {

        @Schema(description = "分类名称（唯一标识）")
        private String categoryName;

        @Schema(description = "事件类型：VIEW / LIKE / UNLIKE")
        private EventType eventType;
    }
}