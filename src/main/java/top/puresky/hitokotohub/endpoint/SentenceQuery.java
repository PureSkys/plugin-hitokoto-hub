package top.puresky.hitokotohub.endpoint;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static run.halo.app.extension.index.query.Queries.contains;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.router.QueryParamBuildUtil.sortParameter;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.annotation.Nullable;
import java.util.Optional;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.router.IListRequest;
import run.halo.app.extension.router.SortableRequest;

public class SentenceQuery extends SortableRequest {

    public SentenceQuery(ServerRequest request) {
        super(request.exchange());
    }

    @Nullable
    public String getKeyword() {
        return queryParams.getFirst("keyword");
    }

    @Nullable
    public String getCategoryName() {
        return queryParams.getFirst("categoryName");
    }

    @Override
    public ListOptions toListOptions() {
        var builder = ListOptions.builder(super.toListOptions());

        Optional.ofNullable(getKeyword()).filter(StringUtils::hasText)
            .ifPresent(keyword -> builder.andQuery(contains("spec.content", keyword)));

        Optional.ofNullable(getCategoryName()).filter(StringUtils::hasText)
            .ifPresent(categoryName -> builder.andQuery(equal("spec.categoryName", categoryName)));

        return builder.build();
    }

    public static void buildParameters(Builder builder) {
        IListRequest.buildParameters(builder);
        builder.parameter(sortParameter()).parameter(
            parameterBuilder().in(ParameterIn.QUERY).name("keyword")
                .description("Sentences filtered by content keyword.").implementation(String.class)
                .required(false)).parameter(
            parameterBuilder().in(ParameterIn.QUERY).name("categoryName")
                .description("Sentences filtered by category metadata name.")
                .implementation(String.class).required(false));
    }
}
