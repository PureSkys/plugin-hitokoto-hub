package top.puresky.hitokotohub.reconciler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.index.query.Queries;
import top.puresky.hitokotohub.extension.Category;
import top.puresky.hitokotohub.extension.Sentence;

@Component
@RequiredArgsConstructor
public class SentenceReconciler implements Reconciler<Reconciler.Request> {

    private final ExtensionClient client;

    @Override
    public Result reconcile(Request request) {
        client.fetch(Sentence.class, request.name()).ifPresent(sentence -> {
            // 获取当前句子的分类ID
            String categoryName = sentence.getSpec().getCategoryName();
            // 检查分类是否存在
            boolean categoryExist = client.fetch(Category.class, categoryName).isPresent();
            if (!categoryExist) {
                client.delete(sentence);
                return;
            }
            // 构建句子查询选项
            ListOptions listSentenceOptions = ListOptions.builder()
                .fieldQuery(Queries.equal("spec.categoryName", categoryName))
                .build();
            // 获取当前句子所属分类的数量
            long sentenceCount =
                client.countBy(Sentence.class, listSentenceOptions);
            // 更新相关分类下句子数量状态
            client.fetch(Category.class, categoryName).ifPresent(category -> {
                if (category.getStatus() == null) {
                    category.setStatus(new Category.Status());
                }
                category.getStatus().setSentenceCount(sentenceCount);
                client.update(category);
            });
        });
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder
            .extension(new Sentence())
            .build();
    }
}