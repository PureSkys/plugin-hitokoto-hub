package top.puresky.hitokotohub.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@GVK(
    group = "hitokotohub.puresky.top",
    version = "v1alpha1",
    kind = "Category",
    plural = "categories",
    singular = "category"
)
public class Category extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    private Status status = new Status();

    @Data
    @Schema(name = "CategorySpec")
    public static class Spec {
        @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50)
        private String name;

        @Schema(description = "分类描述", maxLength = 200)
        private String description;
    }

    @Data
    @Schema(name = "CategoryStatus")
    public static class Status {
        @Schema(description = "句子数量", defaultValue = "0")
        private long sentenceCount = 0;
    }
}