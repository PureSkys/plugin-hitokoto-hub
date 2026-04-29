package top.puresky.hitokotohub;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.theme.TemplateNameResolver;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class HitokotoTemplateRouter {

    private final TemplateNameResolver templateNameResolver;

    @Bean
    RouterFunction<ServerResponse> hitokotoRouterFunction() {
        return route(GET("/hitokoto"), this::renderHitokotoPage);
    }

    Mono<ServerResponse> renderHitokotoPage(ServerRequest request) {
        var model = new HashMap<String, Object>();
        model.put("sentences", List.of());
        return templateNameResolver.resolveTemplateNameOrDefault(request.exchange(), "hitokoto")
            .flatMap(templateName -> ServerResponse.ok().render(templateName, model));
    }
}