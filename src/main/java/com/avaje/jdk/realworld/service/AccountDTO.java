package com.avaje.jdk.realworld.service;

import com.google.flatbuffers.FlatBufferBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.beans.Transient;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {

  private String email;
  private String username;
  private String password;
  private String bio;
  private String image;
  private String id;

  @Transient
  public int fill(FlatBufferBuilder builder) {
        // --- Phase 1: Create offsets for non-null strings ---
        // Using 0 as a sentinel for a null/omitted field.
        int idOffset = 0;
        if (id != null) idOffset = builder.createString(id);

        int emailOffset = 0;
        if (email != null) emailOffset = builder.createString(email);

        int usernameOffset = 0;
        if (username != null) usernameOffset = builder.createString(username);

        int passwordOffset = 0;
        if (password != null) passwordOffset = builder.createString(password);

        int bioOffset = 0;
        if (bio != null) bioOffset = builder.createString(bio);

        int imageOffset = 0;
        if (image != null) imageOffset = builder.createString(image);

        com.avaje.jdk.realworld.models.flat.Account.startAccount(builder);

        // --- Phase 2: Add non-null fields to the table ---
        // The generated `add...` methods do nothing if the offset is 0.
        com.avaje.jdk.realworld.models.flat.Account.addId(builder, idOffset);
        com.avaje.jdk.realworld.models.flat.Account.addEmail(builder, emailOffset);
        com.avaje.jdk.realworld.models.flat.Account.addUsername(builder, usernameOffset);
        com.avaje.jdk.realworld.models.flat.Account.addPassword(builder, passwordOffset);
        com.avaje.jdk.realworld.models.flat.Account.addBio(builder, bioOffset);
        com.avaje.jdk.realworld.models.flat.Account.addImage(builder, imageOffset);

        final int accountOffset = com.avaje.jdk.realworld.models.flat.Account.endAccount(builder);

        com.avaje.jdk.realworld.models.flat.Account.finishAccountBuffer(builder, accountOffset);

        return accountOffset;
    }
}
