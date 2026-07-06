package ht.edu.ueh.fds.tontine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Sert les fichiers televerses (photos de profil) en HTTP. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Chemin ABSOLU du dossier des televersements : un chemin relatif
        // ("file:uploads/") n'est pas resolu de facon fiable sous Spring Boot 3,
        // ce qui provoque « No static resource ». On calcule donc l'URI absolue.
        Path dossierUploads = Paths.get("uploads").toAbsolutePath().normalize();
        String emplacement = dossierUploads.toUri().toString();
        if (!emplacement.endsWith("/")) {
            emplacement = emplacement + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(emplacement);
    }
}
