package com.devbuild.renko.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        exposeDirectory("uploads", registry);
    }

    private void exposeDirectory(String dirName, ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(dirName);
        String uploadPath = uploadDir.toFile().getAbsolutePath();
        
        if (dirName.startsWith("../")) dirName = dirName.replace("../", "");
        
        String location = "file:///" + uploadPath.replace("\\", "/") + "/";
        registry.addResourceHandler("/" + dirName + "/**").addResourceLocations(location);
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.web.servlet.LocaleResolver localeResolver() {
        org.springframework.web.servlet.i18n.SessionLocaleResolver slr = new org.springframework.web.servlet.i18n.SessionLocaleResolver();
        slr.setDefaultLocale(java.util.Locale.FRENCH);
        return slr;
    }

    @Override
    public void addInterceptors(@org.springframework.lang.NonNull org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        org.springframework.web.servlet.i18n.LocaleChangeInterceptor lci = new org.springframework.web.servlet.i18n.LocaleChangeInterceptor();
        lci.setParamName("lang");
        registry.addInterceptor(lci);
    }
}
