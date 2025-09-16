package com.avaje.jdk.realworld.interfaces;

import com.avaje.jdk.realworld.models.responses.Tags;

public interface ITagService {
    Tags getTagsHandler();

    Tags getThrowErrorHandler();
}
