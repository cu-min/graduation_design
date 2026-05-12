package com.graduationdesign.newsrecommendation.config;

import com.graduationdesign.newsrecommendation.security.CurrentUserArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    private final UploadStorage uploadStorage;

    public WebMvcConfig(CurrentUserArgumentResolver currentUserArgumentResolver, UploadStorage uploadStorage) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
        this.uploadStorage = uploadStorage;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("/uploads/**")
            .addResourceLocations(uploadStorage.getUploadRoot().toUri().toString());
    }
}
