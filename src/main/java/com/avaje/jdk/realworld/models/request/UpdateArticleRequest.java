package com.avaje.jdk.realworld.models.request;

import io.avaje.jsonb.Json;
import io.avaje.validation.constraints.Valid;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

@Json
@Valid
public record UpdateArticleRequest(@NonNull UpdateArticleBody article) {

    public boolean anyUpdates() {
        return article.version.isPresent() && (article.title.isPresent() || article.description.isPresent() || article.body.isPresent());
    }

    public record UpdateArticleBody(
            Optional<String> title, Optional<String> description, Optional<String> body, Optional<String> version) {

        public boolean anyUpdates() {
            return version().isPresent() && (title.isPresent() || description.isPresent() || body.isPresent());
        }
    }
}
