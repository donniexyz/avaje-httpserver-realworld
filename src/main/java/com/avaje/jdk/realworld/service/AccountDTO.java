package com.avaje.jdk.realworld.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

}
