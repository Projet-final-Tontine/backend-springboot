package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.UtilisateurResponse;
import ht.edu.ueh.fds.tontine.repository.SolRepository;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Lecture seule pour le tableau de bord admin (page web) :
 * liste des utilisateurs et indicateurs cles. L'acces est deja
 * restreint au role ADMIN par la configuration de securite.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UtilisateurRepository utilisateurRepository;
    private final SolRepository solRepository;

    /** Liste de tous les utilisateurs (supervision). */
    @GetMapping("/utilisateurs")
    public List<UtilisateurResponse> listerUtilisateurs() {
        return utilisateurRepository.findAll().stream()
                .map(UtilisateurResponse::from).toList();
    }

    /** Indicateurs cles (KPIs) du tableau de bord. */
    @GetMapping("/stats")
    public Map<String, Object> statistiques() {
        var utilisateurs = utilisateurRepository.findAll();
        long actifs = utilisateurs.stream().filter(u -> "ACTIF".equals(u.getStatut())).count();
        long bloques = utilisateurs.stream().filter(u -> "BLOQUE".equals(u.getStatut())).count();
        long enAttente = utilisateurs.stream().filter(u -> "EN_ATTENTE".equals(u.getStatut())).count();
        return Map.of(
                "totalUtilisateurs", utilisateurs.size(),
                "comptesActifs", actifs,
                "comptesBloques", bloques,
                "comptesEnAttente", enAttente,
                "totalSols", solRepository.count(),
                "solsEnCours", solRepository.countByStatut("EN_COURS"));
    }
}
