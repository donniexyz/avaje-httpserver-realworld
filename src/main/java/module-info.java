import io.avaje.http.api.Path;

@Path("/api")
module avaje.realworld {
  opens com.avaje.jdk.realworld;
  opens com.avaje.jdk.realworld.models.entities;

  // // jwt validation
  requires com.auth0.jwt;
  // // JDBC extensions
  requires dev.mccue.jdbc;
  // // Database connection pooling
  requires io.ebean.core;
  requires io.ebean.querybean;
  // // Logging
  requires io.jstach.rainbowgum.slf4j;
  // // HTTP server
  requires jdk.httpserver;
  // // Postgres Driver
  requires org.postgresql.jdbc;
  // // Turns text into url safe slug
  requires slugify;
  // // Configuration
  requires io.avaje.config;
  // // Dependency Injection
  requires io.avaje.inject;
  // // JAX-Style controller generation
  requires io.avaje.http.api;
  // // Json
  requires io.avaje.jsonb;
  // // Jspecify
  requires org.jspecify;
  // // Validation
  requires io.avaje.validation;
  requires io.avaje.validation.contraints;
  requires io.avaje.validation.http;
  // // Json DI plugin
  requires io.avaje.jsonb.plugin;
  // // jdk.httpserver wrapper
  requires io.avaje.jex;
  requires io.avaje.jex.staticcontent;
  requires static io.avaje.spi;
  requires jakarta.inject;
    requires stormpot;
    requires lmdbjava;

    exports com.avaje.jdk.realworld.models;

  provides io.avaje.inject.spi.InjectExtension with
      com.avaje.jdk.realworld.RealworldModule;
  provides io.avaje.jsonb.spi.JsonbExtension with
      com.avaje.jdk.realworld.jsonb.GeneratedJsonComponent;
  provides io.avaje.validation.spi.ValidationExtension with
      com.avaje.jdk.realworld.models.request.valid.GeneratedValidatorComponent;
  provides io.ebean.config.EntityClassRegister with
      com.avaje.jdk.realworld.models.entities.EbeanEntityRegister;
  provides io.ebean.config.DatabaseConfigProvider with
      com.avaje.jdk.realworld.security.Encryptor;

    requires static lombok;
}
