package br.com.dentibot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DentibotApplication {

    public static void main(String[] args) {
        SpringApplication.run(DentibotApplication.class, args);
    }
}
