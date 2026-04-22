package top.puresky.hitokotohub.reconciler;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionOperator;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.index.query.Queries;
import top.puresky.hitokotohub.extension.Category;
import top.puresky.hitokotohub.extension.Sentence;

@Component
@RequiredArgsConstructor
public class CategoryReconciler implements Reconciler<Reconciler.Request> {

    private final ExtensionClient client;
    private static final String FINALIZER_NAME = "hitokotohub.puresky.top/category-protector";


    @Override
    public Result reconcile(Request request) {
        client.fetch(Category.class, request.name()).ifPresent(category -> {

            if (ExtensionOperator.isDeleted(category)) {
                // 添加删除分类终结器
                ExtensionUtil.addFinalizers(category.getMetadata(), Set.of(FINALIZER_NAME));
                client.update(category);

                String categoryMetadataName = category.getMetadata().getName();
                ListOptions listSentenceOptions = ListOptions.builder()
                    .fieldQuery(Queries.equal("spec.categoryName", categoryMetadataName)).build();
                client.listAll(Sentence.class, listSentenceOptions, Sort.unsorted())
                    .forEach(client::delete);
                // 移除删除分类终结器
                ExtensionUtil.removeFinalizers(category.getMetadata(), Set.of(FINALIZER_NAME));
                client.update(category);
            }
        });
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder.extension(new Category()).build();
    }
}