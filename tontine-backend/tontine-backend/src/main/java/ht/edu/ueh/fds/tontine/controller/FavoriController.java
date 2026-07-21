package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.TransfertDtos.FavoriResponse;
import ht.edu.ueh.fds.tontine.service.FavoriService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/** Bénéficiaires favoris de l'utilisateur connecté. */
@RestController
@RequestMapping("/api/favoris")
@RequiredArgsConstructor
public class FavoriController {

    private final FavoriService favoriService;

    @GetMapping
    public List<FavoriResponse> lister(Principal principal) {
        return favoriService.lister(principal.getName());
    }

    @PostMapping("/{beneficiaireId}")
    public Map<String, String> ajouter(Principal principal, @PathVariable String beneficiaireId) {
        favoriService.ajouter(principal.getName(), beneficiaireId);
        return Map.of("message", "Ajouté aux favoris.");
    }

    @DeleteMapping("/{beneficiaireId}")
    public Map<String, String> supprimer(Principal principal, @PathVariable String beneficiaireId) {
        favoriService.supprimer(principal.getName(), beneficiaireId);
        return Map.of("message", "Retiré des favoris.");
    }
}
