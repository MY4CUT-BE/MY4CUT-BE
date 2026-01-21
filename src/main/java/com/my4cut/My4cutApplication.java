package com.my4cut;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class My4cutApplication {

    public static void main(String[] args) {
        SpringApplication.run(My4cutApplication.class, args);
    }

}
