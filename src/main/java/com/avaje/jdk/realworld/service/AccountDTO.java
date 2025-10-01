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
        final int emailOffset = builder.createString(email);
        final int usernameOffset = builder.createString(username);
        final int passwordOffset = builder.createString(password);
        final int bioOffset = builder.createString(bio);
        final int imageOffset = builder.createString(image);
        final int idOffset = builder.createString(id);

        com.avaje.jdk.realworld.models.flat.Account.startAccount(builder);
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
