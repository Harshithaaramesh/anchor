package com.harshitha.anchor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * With both spring-boot-starter-webmvc and spring-boot-starter-webflux on the classpath,
 * Spring Boot's autoconfiguration favors Spring MVC (the servlet stack) for the
 * DispatcherServlet/DispatcherHandler, so this project runs as an MVC app. Reactor types
 * (Flux/Mono) are still usable directly as @Controller return values though - Spring MVC
 * bridges them via ReactiveAdapterRegistry, which is what makes the SSE streaming in
 * StreamController work without pulling in the reactive server runtime.
 */
@Configuration
public class WebFluxConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*")
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}
