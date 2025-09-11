package com.avaje.jdk.realworld.models.entities;

import io.ebean.Model;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "favorites")
public class FavoriteEntity extends Model {

    @Id
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private ArticleEntity article;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;

    public ArticleEntity article() {
        return article;
    }

    public FavoriteEntity article(ArticleEntity article) {
        this.article = article;
        return this;
    }

    public UserEntity user() {
        return user;
    }

    public FavoriteEntity user(UserEntity user) {
        this.user = user;
        return this;
    }
}
