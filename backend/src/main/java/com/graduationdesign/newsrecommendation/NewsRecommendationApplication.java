package com.graduationdesign.newsrecommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NewsRecommendationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsRecommendationApplication.class, args);
    }
}
