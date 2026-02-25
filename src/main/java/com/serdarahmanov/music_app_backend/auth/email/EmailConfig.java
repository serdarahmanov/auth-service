package com.serdarahmanov.music_app_backend.auth.email;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
public class EmailConfig {

    @Bean(name = "emailTemplateResolver")
    public ITemplateResolver thymeleafTemplateResolver() {

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("mail-templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCheckExistence(true); // IMPORTANT!
        templateResolver.setOrder(1);             // IMPORTANT!
        templateResolver.setCacheable(false);     // optional, helps during development

        return templateResolver;
    }

    @Bean(name = "emailTemplateEngine")
    public SpringTemplateEngine thymeleafTemplateEngine( @Qualifier("emailTemplateResolver") ITemplateResolver emailTemplateResolver) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(emailTemplateResolver);
        return templateEngine;
    }
}
