package com.my4cut.domain.auth;

public class User {

    private Long id;
    private String email;
    private String password; // 암호화된 비밀번호

    public User(Long id, String email, String password) {
        this.id = id;
        this.email = email;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
