package com.springbatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.GenerationType;
import lombok.Getter;

@Getter
@Entity
public class Member {

    @Id @Column(name = "user_no")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;
    private String email;
    private String password;
    private String name;
    private int age;

}
