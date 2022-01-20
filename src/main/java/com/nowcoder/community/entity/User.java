package com.nowcoder.community.entity;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@ToString
@Data
public class User {
    private int id;
    private String username;
    private String password;
    private String salt;
    private String email;
    private int type;
    private int status;
    private String activationCode;
    private String headerUrl;
    private Date createDate;

}
