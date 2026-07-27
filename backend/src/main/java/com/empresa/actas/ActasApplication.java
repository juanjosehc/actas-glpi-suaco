package com.empresa.actas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.empresa.actas", "com.actasglpi"})
public class ActasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActasApplication.class, args);
    }
}
