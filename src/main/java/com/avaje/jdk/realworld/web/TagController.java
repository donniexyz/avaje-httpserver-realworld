package com.avaje.jdk.realworld.web;

import com.avaje.jdk.realworld.models.responses.Tags;
import com.avaje.jdk.realworld.security.AppRole;
import com.avaje.jdk.realworld.security.Roles;
import com.avaje.jdk.realworld.service.TagService;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Get;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public final class TagController {

    private static final String SELECT_TAGS = "SELECT name FROM realworld.tag";

    private final TagService tagService;

    //  @Inject
    //  public TagController(TagService tagService) {
    //    this.tagService = tagService;
    //  }

    @Get("/tags")
    @Roles(AppRole.ANYONE)
    public Tags getTagsHandler() {
        return tagService.getTagsHandler();
    }

    @Get("/throw-error")
    @Roles(AppRole.ANYONE)
    public Tags getThrowErrorHandler() {
        return tagService.getThrowErrorHandler();
    }
}
