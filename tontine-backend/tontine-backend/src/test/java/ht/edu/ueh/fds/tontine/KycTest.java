package ht.edu.ueh.fds.tontine;

import ht.edu.ueh.fds.tontine.dto.KycRequests.KycEtatResponse;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import ht.edu.ueh.fds.tontine.service.KycService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prouve le parcours KYC : état initial NON_SOUMIS, confirmation d'identité
 * (dont la date de naissance), soumission approuvée automatiquement (mode démo),
 * et refus d'une carte d'identité sans verso.
 */
@SpringBootTest
class KycTest {

    @Autowired KycService kycService;
    @Autowired UtilisateurRepository utilisateurRepository;

    @Test
    void parcoursKyc_confirmeIdentite_puisApprouve() {
        String userId = creerUtilisateur();

        // 1. Rien soumis au départ.
        assertThat(kycService.etat(userId).statut()).isEqualTo("NON_SOUMIS");

        // 2. Confirmation d'identité (dont la date de naissance).
        KycEtatResponse maj = kycService.majIdentite(userId, "Jean", "Marc",
                "1990-05-15", "Delmas 33");
        assertThat(maj.dateNaissance()).isEqualTo("1990-05-15");
        assertThat(maj.adresse()).isEqualTo("Delmas 33");

        // 3. Soumission d'une carte d'identité (recto + verso) -> approuvée (démo).
        KycEtatResponse ap = kycService.soumettre(userId, "CARTE_IDENTITE",
                "/uploads/recto.jpg", "/uploads/verso.jpg");
        assertThat(ap.statut()).isEqualTo("APPROUVE");
        assertThat(ap.typeDocument()).isEqualTo("CARTE_IDENTITE");

        // 4. Une carte d'identité sans verso est refusée.
        assertThatThrownBy(() -> kycService.soumettre(userId, "CARTE_IDENTITE",
                "/uploads/recto.jpg", null))
                .isInstanceOf(BusinessException.class);
    }

    private String creerUtilisateur() {
        long n = System.nanoTime();
        return utilisateurRepository.save(Utilisateur.builder()
                .nom("Test").prenom("Kyc").sexe("M")
                .telephone("509" + n % 100000000L)
                .email("kyc" + n + "@sol.ht").adresse("PAP")
                .cinNif("CIN-" + n).dateNaissance(LocalDate.of(1990, 1, 1))
                .motDePasseHache("x").role("MEMBRE").build()).getId();
    }
}
