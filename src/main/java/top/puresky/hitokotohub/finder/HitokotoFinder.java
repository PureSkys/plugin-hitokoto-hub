package top.puresky.hitokotohub.finder;

import reactor.core.publisher.Flux;

public interface HitokotoFinder {

    Flux<SentenceVo> randomSentences(int size ,String categoryName);

    Flux<CategoryVo> listCategories();

    @lombok.Data
    @lombok.Builder
    class SentenceVo {
        private String name;
        private String author;
        private String content;
        private String source;
        private String categoryName;
        private long likeCount;
        private long viewCount;
    }

    @lombok.Data
    @lombok.Builder
    class CategoryVo {
        private String name;
        private String displayName;
        private String description;
        private long sentenceCount;
    }
}