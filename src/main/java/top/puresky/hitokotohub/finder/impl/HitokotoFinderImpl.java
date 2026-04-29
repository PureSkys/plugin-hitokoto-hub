package top.puresky.hitokotohub.finder.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.Queries;
import run.halo.app.theme.finders.Finder;
import top.puresky.hitokotohub.extension.Category;
import top.puresky.hitokotohub.extension.Sentence;
import top.puresky.hitokotohub.finder.HitokotoFinder;

@Finder("hitokotoFinder")
@Component
@RequiredArgsConstructor
public class HitokotoFinderImpl implements HitokotoFinder {

    private final ReactiveExtensionClient client;

    @Override
    public Flux<SentenceVo> randomSentences(int size, String categoryName) {
        int actualSize = Math.min(size, 20);
        var query = Queries.equal("status.isPublished", true);
        if (categoryName != null && !categoryName.isBlank()) {
            query = Queries.and(query, Queries.equal("spec.categoryName", categoryName));
        }
        var options = ListOptions.builder().fieldQuery(query).build();
        return client.countBy(Sentence.class, options)
            .filter(total -> total > 0)
            .flatMapMany(total -> {
                int totalPages = (int) Math.ceil((double) total / actualSize);
                int randomPage = RandomUtils.insecure().randomInt(1, totalPages + 1);
                return client.listBy(Sentence.class, options,
                        PageRequestImpl.of(randomPage, actualSize, Sort.unsorted()))
                    .flatMapMany(result -> Flux.fromIterable(result.getItems()));
            })
            .map(this::toSentenceVo);
    }

    @Override
    public Flux<CategoryVo> listCategories() {
        return client.listAll(Category.class, new ListOptions(), Sort.unsorted())
            .filter(c -> c.getStatus() != null && c.getStatus().getSentenceCount() > 0)
            .map(this::toCategoryVo);
    }

    private SentenceVo toSentenceVo(Sentence s) {
        var spec = s.getSpec();
        var status = s.getStatus();
        return SentenceVo.builder().name(s.getMetadata().getName()).author(spec.getAuthor())
            .content(spec.getContent()).source(spec.getSource())
            .categoryName(spec.getCategoryName())
            .likeCount(status != null ? status.getLikeCount() : 0)
            .viewCount(status != null ? status.getViewCount() : 0).build();
    }

    private CategoryVo toCategoryVo(Category c) {
        var spec = c.getSpec();
        var status = c.getStatus();
        return CategoryVo.builder().name(c.getMetadata().getName()).displayName(spec.getName())
            .description(spec.getDescription())
            .sentenceCount(status != null ? status.getSentenceCount() : 0).build();
    }
}