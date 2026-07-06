package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.exception.BusinessException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Map;

/** Televersement de fichiers (photo de profil). */
@RestController
@RequestMapping("/api/fichiers")
public class FichierController {

    /** Recoit une image et renvoie son URL publique (/uploads/...). */
    @PostMapping("/photo")
    public Map<String, String> televerserPhoto(Principal principal,
                                               @RequestParam("fichier") MultipartFile fichier)
            throws IOException {
        if (fichier.isEmpty()) {
            throw new BusinessException("Aucun fichier recu.");
        }
        String nomOriginal = fichier.getOriginalFilename();
        String extension = (nomOriginal != null && nomOriginal.contains("."))
                ? nomOriginal.substring(nomOriginal.lastIndexOf('.'))
                : ".jpg";
        String nom = principal.getName() + "_" + System.currentTimeMillis() + extension;

        Path dossier = Paths.get("uploads");
        Files.createDirectories(dossier);
        fichier.transferTo(dossier.resolve(nom).toAbsolutePath());

        return Map.of("url", "/uploads/" + nom);
    }
}
