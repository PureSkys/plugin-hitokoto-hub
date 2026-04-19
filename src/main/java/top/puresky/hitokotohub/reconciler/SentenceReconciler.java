package top.puresky.hitokotohub.reconciler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionOperator;
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

    private void initStatus(Sentence sentence) {
        // 第一次创建 → 自动初始化
        if (sentence.getStatus() == null) {
            sentence.setStatus(new Sentence.Status());
        }
        sentence.getStatus().setLikeCount(0);
        sentence.getStatus().setViewCount(0);
        client.update(sentence);
    }

    @Override
    public Result reconcile(Request request) {
        client.fetch(Sentence.class, request.name()).ifPresent(sentence -> {
            if (ExtensionOperator.isDeleted(sentence)) {
                return;
            }
            if (sentence.getStatus() == null) {
                initStatus(sentence);
            }
            // 获取当前句子的分类ID
            String categoryMetadataName = sentence.getSpec().getCategoryName();
            // 构建句子查询选项
            ListOptions listSentenceOptions = ListOptions.builder()
                .fieldQuery(Queries.equal("spec.categoryName", categoryMetadataName))
                .build();
            // 更新相关分类下句子数量状态
            client.fetch(Category.class, categoryMetadataName).ifPresentOrElse(category -> {
                // 获取当前句子所属分类的数量
                long sentenceCount =
                    client.countBy(Sentence.class, listSentenceOptions);
                if (category.getStatus() == null) {
                    category.setStatus(new Category.Status());
                }
                category.getStatus().setSentenceCount(sentenceCount);
                client.update(category);
            }, () -> client.delete(sentence));
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