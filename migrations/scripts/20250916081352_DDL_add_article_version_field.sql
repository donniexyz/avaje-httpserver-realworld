-- // First migration.
ALTER TABLE realworld.article ADD "version" int4 NOT NULL DEFAULT 0;

ALTER TABLE realworld.article ALTER COLUMN "version" DROP DEFAULT;


-- //@UNDO
-- ALTER TABLE realworld.article DROP COLUMN "version";
