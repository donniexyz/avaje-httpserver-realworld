package com.avaje.jdk.realworld.web;

import com.avaje.jdk.realworld.exception.AppError;
import com.avaje.jdk.realworld.models.request.CommentRequest;
import com.avaje.jdk.realworld.models.request.CreateArticleRequest;
import com.avaje.jdk.realworld.models.request.CreateArticleRequest.CreateContent;
import com.avaje.jdk.realworld.models.request.UpdateArticleRequest;
import com.avaje.jdk.realworld.models.request.UpdateArticleRequest.UpdateArticleBody;
import com.avaje.jdk.realworld.security.AppRole;
import com.avaje.jdk.realworld.security.Roles;
import com.github.slugify.Slugify;
import dev.mccue.jdbc.SQLFragment;
import io.avaje.http.api.*;
import io.avaje.jex.http.Context;
import io.avaje.jex.http.HttpResponseException;
import io.ebean.DB;

import java.util.*;

@Roles(AppRole.JWT)
@Controller("/articles")
public class ArticleController {

    private static final String ARTICLE = "article";

    private static final String ARTICLE_SQL =
            """
                    SELECT
                        jsonb_build_object(
                            'article', jsonb_build_object(
                                'slug', realworld.article.slug,
                                'title', realworld.article.title,
                                'description', realworld.article.description,
                                'body', realworld.article.body,
                                'tagList', array(
                                    SELECT realworld.tag.name
                                    FROM realworld.article_tag
                                    LEFT JOIN realworld.tag ON realworld.tag.id = realworld.article_tag.tag_id
                                    WHERE realworld.article_tag.article_id = realworld.article.id
                                    ORDER BY realworld.tag.name
                                ),
                                'createdAt', realworld.article.created_at,
                                'updatedAt', realworld.article.updated_at,
                                'favorited', exists(
                                    SELECT id
                                    FROM realworld.favorite
                                    WHERE article_id = realworld.article.id AND user_id = ?
                                ),
                               'favoritesCount', (
                                    SELECT count(id)
                                    FROM realworld.favorite
                                    WHERE article_id = realworld.article.id
                                ),
                                'author', (
                                    SELECT jsonb_build_object(
                                        'username', realworld.user.username,
                                        'bio', realworld.user.bio,
                                        'image', realworld.user.image,
                                        'following', exists(
                                            SELECT id
                                            FROM realworld.follow
                                            WHERE from_user_id = ? AND to_user_id = realworld.user.id
                                        )
                                    )
                                    FROM realworld.user
                                    WHERE realworld.user.id = realworld.article.user_id
                            )
                        )) AS article
                    FROM realworld.article
                    WHERE id = ?
                    """;

    private static final String NO_MATCHING_ARTICLE = "No matching article";

    private static final String USER_ID = "userId";

    @Get("/")
    public String listArticlesHandler(
            Context ctx,
            String tag,
            String favorited,
            String author,
            @Default("20") int limit,
            @Default("0") int offset) {
        var userId = ctx.attribute(USER_ID);

        var query = new ArrayList<SQLFragment>();
        query.add(
                SQLFragment.of(
                        """
                                WITH
                                    articles AS (
                                        SELECT jsonb_build_object(
                                            'slug', realworld.article.slug,
                                            'title', realworld.article.title,
                                            'description', realworld.article.description,
                                            'tagList', array(
                                                SELECT realworld.tag.name
                                                FROM realworld.article_tag
                                                LEFT JOIN realworld.tag ON realworld.tag.id = realworld.article_tag.tag_id
                                                WHERE realworld.article_tag.article_id = realworld.article.id
                                                ORDER BY realworld.tag.name
                                            ),
                                            'createdAt', realworld.article.created_at,
                                            'updatedAt', realworld.article.updated_at,
                                            'favorited', exists(
                                                SELECT id
                                                FROM realworld.favorite
                                                WHERE article_id = realworld.article.id AND user_id = ?
                                            ),
                                            'favoritesCount', (
                                                SELECT count(id)
                                                FROM realworld.favorite
                                                WHERE article_id = realworld.article.id
                                            ),
                                            'author', (
                                                SELECT jsonb_build_object(
                                                    'username', realworld.user.username,
                                                    'bio', realworld.user.bio,
                                                    'image', realworld.user.image,
                                                    'following', exists(
                                                        SELECT id
                                                        FROM realworld.follow
                                                        WHERE from_user_id = ? AND to_user_id = realworld.user.id
                                                    )
                                                )
                                                FROM realworld.user
                                                WHERE realworld.user.id = realworld.article.user_id
                                            )
                                        )
                                        FROM realworld.article
                                
                                """,
                        List.of(userId, userId)));

        if (tag != null)
            query.add(
                    SQLFragment.of(
                            """
                                      WHERE EXISTS(
                                                    SELECT id
                                                    FROM realworld.article_tag
                                                    WHERE realworld.article_tag.article_id = realworld.article.id
                                                        AND ? = (
                                                            SELECT name
                                                            FROM realworld.tag
                                                            WHERE realworld.tag.id = realworld.article_tag.tag_id
                                                        )
                                                )
                                    """,
                            List.of(tag)));

        if (favorited != null)
            query.add(
                    SQLFragment.of(
                            """
                                                WHERE exists(
                                                    SELECT id
                                                    FROM realworld.favorite
                                                    WHERE article_id = realworld.article.id AND user_id = (
                                                        SELECT id
                                                        FROM realworld.user
                                                        WHERE username = ?
                                                    )
                                                )
                                    """,
                            List.of(favorited)));

        if (author != null)
            query.add(
                    SQLFragment.of(
                            """
                                                WHERE realworld.article.user_id IN (
                                                    SELECT id
                                                    FROM realworld.user
                                                    WHERE realworld.user.username = ?
                                                )
                                    """,
                            List.of(author)));
        query.add(
                SQLFragment.of(
                        """
                                        ORDER BY realworld.article.created_at DESC
                                """));
        query.add(SQLFragment.of(" LIMIT ? ", List.of(limit)));
        query.add(SQLFragment.of(" OFFSET ? ", List.of(offset)));
        query.add(
                SQLFragment.of(
                        """
                                    )
                                SELECT jsonb_build_object(
                                    'articles', array(
                                        SELECT * FROM articles
                                    ),
                                    'articlesCount', (
                                        SELECT count(*) FROM articles
                                    )
                                ) AS articles
                                """));
        var joined = SQLFragment.join("", query);

        var sqlQuery = DB.sqlQuery(joined.sql());
        joined.parameters().forEach(sqlQuery::setParameter);

        return sqlQuery.findOneOrEmpty().orElseThrow().get("articles").toString();
    }

    @Get("/feed")
    public String feedArticlesHandler(Context ctx, @Default("20") int limit, @Default("0") int offset) {

        var userId = ctx.attribute(USER_ID);
        var query = new ArrayList<SQLFragment>();
        query.add(
                SQLFragment.of(
                        """
                                WITH
                                    articles AS (
                                        SELECT jsonb_build_object(
                                            'slug', realworld.article.slug,
                                            'title', realworld.article.title,
                                            'description', realworld.article.description,
                                            'tagList', array(
                                                SELECT realworld.tag.name
                                                FROM realworld.article_tag
                                                LEFT JOIN realworld.tag ON realworld.tag.id = realworld.article_tag.tag_id
                                                WHERE realworld.article_tag.article_id = realworld.article.id
                                                ORDER BY realworld.tag.name
                                            ),
                                            'createdAt', realworld.article.created_at,
                                            'updatedAt', realworld.article.updated_at,
                                            'favorited', exists(
                                                SELECT id
                                                FROM realworld.favorite
                                                WHERE article_id = realworld.article.id AND user_id = ?
                                            ),
                                            'favoritesCount', (
                                                SELECT count(id)
                                                FROM realworld.favorite
                                                WHERE article_id = realworld.article.id
                                            ),
                                            'author', (
                                                SELECT jsonb_build_object(
                                                    'username', realworld.user.username,
                                                    'bio', realworld.user.bio,
                                                    'image', realworld.user.image,
                                                    'following', exists(
                                                        SELECT id
                                                        FROM realworld.follow
                                                        WHERE from_user_id = ? AND to_user_id = realworld.user.id
                                                    )
                                                )
                                                FROM realworld.user
                                                WHERE realworld.user.id = realworld.article.user_id
                                            )
                                        )
                                        FROM realworld.article
                                         WHERE user_id IN (
                                            SELECT from_user_id
                                            FROM realworld.follow
                                            WHERE from_user_id = ? AND to_user_id = (
                                                SELECT id
                                                FROM realworld.user
                                                WHERE realworld.user.id = realworld.article.user_id
                                            )
                                        )
                                        ORDER BY realworld.article.created_at DESC
                                """,
                        List.of(userId, userId, userId)));

        query.add(SQLFragment.of(" LIMIT ? ", List.of(limit)));
        query.add(SQLFragment.of(" OFFSET ? ", List.of(offset)));

        query.add(
                SQLFragment.of(
                        """
                                    )
                                SELECT jsonb_build_object(
                                    'articles', array(
                                        SELECT * FROM articles
                                    ),
                                    'articlesCount', (
                                        SELECT count(*) FROM articles
                                    )
                                ) AS articles
                                """));

        var sql = SQLFragment.join("", query);
        var sqlQuery = DB.sqlQuery(sql.sql());
        sql.parameters().forEach(sqlQuery::setParameter);

        return sqlQuery.findOneOrEmpty().orElseThrow().get("articles").toString();
    }

    @Get("/{slug}")
    public String getArticleHandler(Context ctx, String slug) {
        var userId = ctx.attribute(USER_ID);
        return DB.sqlQuery(
                        """
                                SELECT
                                    jsonb_build_object(
                                        'article', jsonb_build_object(
                                            'slug', realworld.article.slug,
                                            'title', realworld.article.title,
                                            'description', realworld.article.description,
                                            'body', realworld.article.body,
                                            'tagList', array(
                                                SELECT realworld.tag.name
                                                FROM realworld.article_tag
                                                LEFT JOIN realworld.tag ON realworld.tag.id = realworld.article_tag.tag_id
                                                WHERE realworld.article_tag.article_id = realworld.article.id
                                                ORDER BY realworld.tag.name
                                            ),
                                            'createdAt', realworld.article.created_at,
                                            'updatedAt', realworld.article.updated_at,
                                            'favorited', exists(
                                                SELECT id
                                                FROM realworld.favorite
                                                WHERE article_id = realworld.article.id AND user_id = ?
                                            ),
                                            'favoritesCount', (
                                                SELECT count(id)
                                                FROM realworld.favorite
                                                WHERE article_id = realworld.article.id
                                            ),
                                            'author', (
                                                SELECT jsonb_build_object(
                                                    'username', realworld.user.username,
                                                    'bio', realworld.user.bio,
                                                    'image', realworld.user.image,
                                                    'following', exists(
                                                        SELECT id
                                                        FROM realworld.follow
                                                        WHERE from_user_id = ? AND to_user_id = realworld.user.id
                                                    )
                                                )
                                                FROM realworld.user
                                                WHERE realworld.user.id = realworld.article.user_id
                                        )
                                    )) AS article
                                FROM realworld.article
                                 WHERE slug = ?
                                """)
                .setParameters(userId, userId, slug)
                .findOneOrEmpty()
                .orElseThrow(() -> new HttpResponseException(404, new AppError(NO_MATCHING_ARTICLE)))
                .get(ARTICLE)
                .toString();
    }

    private static final Random random = new Random();

    String articleSlug(String title) {
        var sb = new StringBuilder(Slugify.builder().build().slugify(title));
        sb.append("-");
        for (int i = 0; i < 8; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    @Post("/")
    public String createArticleHandler(Context ctx, CreateArticleRequest req) {
        var userId = ctx.attribute(USER_ID);

        if (!(req
                instanceof
                CreateArticleRequest(
                        CreateContent(
                                String title,
                                String description,
                                String body,
                                Optional<Integer> version,
                                Optional<List<String>> tagListOp
                        )
                ))) {
            throw new IllegalArgumentException("body shouldn't be null");
        }

        if (version.isPresent() && version.get() != 0) throw new IllegalArgumentException("version must be empty or 0");

        try (var txn = DB.beginTransaction()) {

            txn.setBatchMode(true);

            var articleId = UUID.randomUUID();

            DB.sqlUpdate(
                            """
                                    INSERT INTO realworld.article(id, user_id, title, slug, description, body, version)
                                    VALUES (?, ?, ?, ?, ?, ?, ?)
                                    """)
                    .setParameters(articleId, userId, title, articleSlug(title), description, body, 0)
                    .execute();

            var tagIds = new ArrayList<UUID>();

            var tagList = tagListOp.orElse(List.of());
            for (var tag : tagList) {

                UUID id =
                        DB.sqlQuery(
                                        """
                                                INSERT INTO realworld.tag(name)
                                                VALUES (?)
                                                ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
                                                RETURNING id
                                                """)
                                .setParameter(tag)
                                .mapToScalar(UUID.class)
                                .findOne();
                tagIds.add(id);
            }

            var articleTageQuery =
                    DB.sqlUpdate(
                            """
                                    INSERT INTO realworld.article_tag(article_id, tag_id)
                                    VALUES (?, ?)
                                    """);
            for (var tagId : tagIds) {
                articleTageQuery.setParameter(1, articleId);
                articleTageQuery.setParameter(2, tagId);
                articleTageQuery.addBatch();
            }

            txn.commitAndContinue();

            return DB.sqlQuery(
                            """
                                    SELECT
                                        jsonb_build_object(
                                            'article', jsonb_build_object(
                                                'slug', realworld.article.slug,
                                                'title', realworld.article.title,
                                                'description', realworld.article.description,
                                                'body', realworld.article.body,
                                                'version', realworld.article.version,
                                                'tagList', array(
                                                    SELECT realworld.tag.name
                                                    FROM realworld.article_tag
                                                    LEFT JOIN realworld.tag ON realworld.tag.id = realworld.article_tag.tag_id
                                                    WHERE realworld.article_tag.article_id = realworld.article.id
                                                    ORDER BY realworld.tag.name
                                                ),
                                                'createdAt', realworld.article.created_at,
                                                'updatedAt', realworld.article.updated_at,
                                                'favorited', exists(
                                                    SELECT id
                                                    FROM realworld.favorite
                                                    WHERE article_id = realworld.article.id AND user_id = ?
                                                ),
                                                'favoritesCount', (
                                                    SELECT count(id)
                                                    FROM realworld.favorite
                                                    WHERE article_id = realworld.article.id
                                                ),
                                                'author', (
                                                    SELECT jsonb_build_object(
                                                        'username', realworld.user.username,
                                                        'bio', realworld.user.bio,
                                                        'image', realworld.user.image,
                                                        'following', exists(
                                                            SELECT id
                                                            FROM realworld.follow
                                                            WHERE from_user_id = ? AND to_user_id = realworld.user.id
                                                        )
                                                    )
                                                    FROM realworld.user
                                                    WHERE realworld.user.id = article.user_id
                                            )
                                        )) AS article
                                    FROM realworld.article
                                     WHERE id = ?
                                    """)
                    .setParameters(userId, userId, articleId)
                    .findOneOrEmpty()
                    .orElseThrow()
                    .get(ARTICLE)
                    .toString();
        }
    }

    @Put("/{slug}")
    public String updateArticleHandler(Context ctx, String slug, UpdateArticleRequest req) {
        var userId = ctx.attribute(USER_ID);

        UUID articleId = null;
        if (req.anyUpdates()
                && req
                instanceof
                UpdateArticleRequest(
                        UpdateArticleBody(
                                Optional<String> title,
                                Optional<String> description,
                                Optional<String> body,
                                Optional<String> version
                        )
                )) {
            var sets = new ArrayList<SQLFragment>();

            title.ifPresent(
                    t -> sets.add(SQLFragment.of("title = ?, slug = ?", List.of(title, articleSlug(t)))));

            description.ifPresent(d -> sets.add(SQLFragment.of("description = ?", List.of(d))));

            body.ifPresent(b -> sets.add(SQLFragment.of("body = ?", List.of(b))));
            version.ifPresent(b -> sets.add(SQLFragment.of("version = ?", List.of(b))));

            var sql =
                    SQLFragment.of(
                                    """
                                             UPDATE realworld.article
                                             SET
                                             \s\s\s\s
                                            """)
                            .concat(SQLFragment.join(", ", sets))
                            .concat(
                                    SQLFragment.of(
                                            """
                                                    
                                                    WHERE slug = ?
                                                    RETURNING id
                                                    """,
                                            List.of(slug)));

            var sqlQuery = DB.sqlQuery(sql.sql());
            sql.parameters().forEach(sqlQuery::setParameter);
            articleId = sqlQuery.mapToScalar(UUID.class).findOne();
        }

        if (articleId == null) {
            throw new HttpResponseException(404, new AppError(NO_MATCHING_ARTICLE));
        }
        return getArticle(userId, articleId);
    }

    private String getArticle(Object userId, UUID articleId) {

        return DB.sqlQuery(ARTICLE_SQL)
                .setParameters(userId, userId, articleId)
                .findOneOrEmpty()
                .orElseThrow()
                .get(ARTICLE)
                .toString();
    }

    @Delete("/{slug}")
    public void deleteArticleHandler(Context ctx, String slug) {
        var userId = ctx.attribute(USER_ID);

        var result =
                DB.sqlUpdate(
                                """
                                        DELETE FROM realworld.article
                                        WHERE user_id = ? AND slug = ?
                                        """)
                        .setParameters(userId, slug)
                        .execute();
        if (result == 0) {
            throw new HttpResponseException(500, new AppError("could not delete article"));
        }
    }

    @Post("/{slug}/comments")
    public String addCommentsToArticleHandler(Context ctx, CommentRequest request, String slug) {
        var body = request.comment().body();
        var userId = ctx.attribute(USER_ID);

        UUID articleId =
                DB.sqlQuery(
                                """
                                        SELECT id
                                        FROM realworld.article
                                        WHERE slug = ?
                                        """)
                        .setParameter(slug)
                        .mapToScalar(UUID.class)
                        .findOneOrEmpty()
                        .orElseThrow(() -> new HttpResponseException(404, new AppError(NO_MATCHING_ARTICLE)));

        var commentId = UUID.randomUUID();
        DB.sqlUpdate(
                        """
                                INSERT INTO realworld.comment(id, article_id, user_id, body)
                                VALUES (?, ?, ?, ?)
                                """)
                .setParameters(commentId, articleId, userId, body)
                .execute();

        return DB.sqlQuery(
                        """
                                SELECT
                                    jsonb_build_object(
                                        'comment', (
                                            SELECT jsonb_build_object(
                                                'id', realworld.comment.id,
                                                'createdAt', realworld.comment.created_at,
                                                'updatedAt', realworld.comment.updated_at,
                                                'body', realworld.comment.body,
                                                'author', (
                                                    SELECT jsonb_build_object(
                                                        'username', realworld.user.username,
                                                        'bio', realworld.user.bio,
                                                        'image', realworld.user.image,
                                                        'following', exists(
                                                             SELECT id
                                                             FROM realworld.follow
                                                             WHERE from_user_id = ? AND to_user_id = realworld.user.id
                                                         )
                                                    )
                                                    FROM realworld.user
                                                    WHERE realworld.user.id = realworld.comment.user_id
                                                )
                                            )
                                            FROM realworld.comment
                                            WHERE realworld.comment.id = ?
                                        )
                                    ) AS comment
                                """)
                .setParameters(userId, commentId)
                .findOneOrEmpty()
                .orElseThrow()
                .get("comment")
                .toString();
    }

    @Get("/{slug}/comments")
    public String getCommentsFromArticleHandler(Context ctx, String slug) {
        var userId = ctx.attribute(USER_ID);

        return DB.sqlQuery(
                        """
                                SELECT
                                    jsonb_build_object(
                                        'comments', array(
                                            SELECT jsonb_build_object(
                                                'id', realworld.comment.id,
                                                'createdAt', realworld.comment.created_at,
                                                'updatedAt', realworld.comment.updated_at,
                                                'body', realworld.comment.body,
                                                'author', (
                                                    SELECT jsonb_build_object(
                                                        'username', realworld.user.username,
                                                        'bio', realworld.user.bio,
                                                        'image', realworld.user.image,
                                                        'following', exists(
                                                             SELECT id
                                                             FROM realworld.follow
                                                             WHERE from_user_id = ? AND to_user_id = realworld.user.id
                                                         )
                                                    )
                                                    FROM realworld.user
                                                    WHERE realworld.user.id = realworld.comment.user_id
                                                )
                                            )
                                            FROM realworld.comment
                                            WHERE realworld.comment.article_id = realworld.article.id
                                        )
                                    ) AS comments
                                FROM realworld.article WHERE realworld.article.slug = ?
                                """)
                .setParameters(userId, slug)
                .findOneOrEmpty()
                .orElseThrow(() -> new HttpResponseException(404, new AppError(NO_MATCHING_ARTICLE)))
                .get("comments")
                .toString();
    }

    @Delete("/{slug}/comments/{commentId}")
    public void deleteCommentHandler(Context ctx, String slug, UUID commentId) {
        var userId = ctx.attribute(USER_ID);
        var result =
                DB.sqlUpdate(
                                """
                                        DELETE FROM realworld.comment
                                        WHERE
                                            realworld.comment.id = ? AND
                                            realworld.comment.user_id = ? AND
                                            realworld.comment.article_id IN (
                                                SELECT id
                                                FROM realworld.article
                                                WHERE realworld.article.slug = ?
                                            )
                                        """)
                        .setParameters(commentId, userId, slug)
                        .execute();

        if (result == 0) {

            throw new HttpResponseException(500, new AppError("could not delete article"));
        }
    }

    @Post("/{slug}/favorite")
    public String favoriteArticleHandler(Context ctx, String slug) {
        var userId = ctx.attribute(USER_ID);

        var articleId = findArticle(slug);

        DB.sqlUpdate(
                        """
                                INSERT INTO realworld.favorite(article_id, user_id)
                                VALUES (?, ?)
                                ON CONFLICT
                                  DO NOTHING
                                """)
                .setParameters(articleId, userId)
                .execute();
        return getArticle(userId, articleId);
    }

    private UUID findArticle(String slug) {
        return DB.sqlQuery(
                        """
                                SELECT id
                                FROM realworld.article
                                WHERE slug = ?
                                """)
                .setParameter(1, slug)
                .mapToScalar(UUID.class)
                .findOneOrEmpty()
                .orElseThrow(() -> new HttpResponseException(404, new AppError(NO_MATCHING_ARTICLE)));
    }

    @Delete("/{slug}/favorite")
    public String unfavoriteArticleHandler(Context ctx, String slug) {
        var userId = ctx.attribute(USER_ID);
        var articleId = findArticle(slug);
        DB.sqlUpdate(
                        """
                                DELETE FROM realworld.favorite
                                WHERE article_id = ? AND user_id = ?
                                """)
                .setParameters(articleId, userId)
                .execute();

        return getArticle(userId, articleId);
    }
}
