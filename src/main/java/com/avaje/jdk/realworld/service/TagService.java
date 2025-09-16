package com.avaje.jdk.realworld.service;

import com.avaje.jdk.realworld.interfaces.ITagService;
import com.avaje.jdk.realworld.models.responses.Tags;
import io.ebean.DB;
import jakarta.inject.Singleton;

@Singleton
public final class TagService implements ITagService {

    private static final String SELECT_TAGS = "SELECT name FROM realworld.tag";

    @Override
    public Tags getTagsHandler() {
        return new Tags(DB.sqlQuery(SELECT_TAGS).mapToScalar(String.class).findList());
    }

    @Override
    public Tags getThrowErrorHandler() {
        throw new RuntimeException("Intentionally error");
    }
}
