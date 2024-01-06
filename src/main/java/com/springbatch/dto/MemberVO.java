package com.springbatch.dto;

import lombok.Data;

@Data
public class MemberVO {
    private String userNo;
    private String email;
    private String name;
    private String password;
    private int age;
}
