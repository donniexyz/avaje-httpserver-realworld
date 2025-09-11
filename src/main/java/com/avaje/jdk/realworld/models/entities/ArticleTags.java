package com.avaje.jdk.realworld.models.entities;

import io.ebean.Model;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "realworld.article_tag")
public class ArticleTags extends Model {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false, insertable = false, updatable = false)
    private final ArticleEntity article;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tag_id", nullable = false, insertable = false, updatable = false)
    private final TagEntity tag;
    @EmbeddedId
    private ArticleTagId id;

    public ArticleTags(ArticleEntity article, TagEntity tag) {
        this.article = article;
        this.tag = tag;
    }

    public ArticleEntity article() {
        return article;
    }

    public TagEntity tag() {
        return tag;
    }

    public ArticleTagId id() {
        return id;
    }

    public void id(ArticleTagId id) {
        this.id = id;
    }

    @Embeddable
    public record ArticleTagId(UUID articleId, UUID tagId) {
    }
}
