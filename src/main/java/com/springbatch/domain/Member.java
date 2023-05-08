package com.springbatch.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Entity @Setter
public class Member {
    @Column(name = "user_no")
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userNo;

    private String email;
    private String name;
    private String password;

    private int age;

}
