package ht.edu.ueh.fds.tontine;

import ht.edu.ueh.fds.tontine.dto.TableauDeBordResponse;
import ht.edu.ueh.fds.tontine.entity.*;
import ht.edu.ueh.fds.tontine.repository.*;
import ht.edu.ueh.fds.tontine.service.TableauDeBordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prouve le calcul du tableau de bord : total cotisé, projection de la
 * prochaine « main » (montant du pot = cotisation × membres actifs), prochaine
 * échéance et courbe d'épargne.
 */
@SpringBootTest
class TableauDeBordTest {

    @Autowired TableauDeBordService tableauDeBord;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired SolRepository solRepository;
    @Autowired MembreSolRepository membreSolRepository;
    @Autowired CotisationRepository cotisationRepository;
    @Autowired TourRepository tourRepository;

    @Test
    void construit_lesIndicateursEtLesProjections() {
        Utilisateur u = creerUtilisateur();
        Utilisateur u2 = creerUtilisateur();

        Sol sol = solRepository.save(Sol.builder()
                .nom("Sol Test").codeInvitation("CODE-" + System.nanoTime() % 1000000)
                .nombreMaxMembres(5).montantCotisation(new BigDecimal("1000"))
                .frequence("MENSUEL").dateDebut(LocalDate.now()).mamanSol(u)
                .build());

        MembreSol m = membreSolRepository.save(MembreSol.builder()
                .utilisateur(u).sol(sol).statutMembre("ACTIF").ordrePassage(1).build());
        membreSolRepository.save(MembreSol.builder()
                .utilisateur(u2).sol(sol).statutMembre("ACTIF").ordrePassage(2).build());

        // Une cotisation validée (1000) et une en attente (échéance future).
        cotisationRepository.save(Cotisation.builder()
                .membreSol(m).sol(sol).montantAttendu(new BigDecimal("1000"))
                .montantPaye(new BigDecimal("1000")).dateEcheance(LocalDate.now().minusDays(5))
                .datePaiementEffectif(LocalDateTime.now().minusDays(5)).statut("VALIDE").build());
        cotisationRepository.save(Cotisation.builder()
                .membreSol(m).sol(sol).montantAttendu(new BigDecimal("1000"))
                .montantPaye(BigDecimal.ZERO).dateEcheance(LocalDate.now().plusDays(10))
                .statut("EN_ATTENTE").build());

        // Un tour à venir où u est bénéficiaire (main pas encore versée).
        tourRepository.save(Tour.builder()
                .sol(sol).beneficiaire(u).numeroTour(1)
                .datePrevue(LocalDate.now().plusDays(20)).statut("OUVERT").build());

        TableauDeBordResponse tb = tableauDeBord.construire(u.getId());

        assertThat(tb.totalCotise()).isEqualByComparingTo("1000");
        assertThat(tb.nbSolsActifs()).isEqualTo(1);
        // Pot = 1000 × 2 membres actifs = 2000.
        assertThat(tb.totalARecevoir()).isEqualByComparingTo("2000");
        assertThat(tb.prochaineMain()).isNotNull();
        assertThat(tb.prochaineMain().montant()).isEqualByComparingTo("2000");
        assertThat(tb.prochaineMain().solNom()).isEqualTo("Sol Test");
        assertThat(tb.prochaineEcheance()).isNotNull();
        assertThat(tb.prochaineEcheance().montant()).isEqualByComparingTo("1000");
        assertThat(tb.epargne()).hasSize(1);
        assertThat(tb.epargne().get(0).cumul()).isEqualByComparingTo("1000");
    }

    private Utilisateur creerUtilisateur() {
        long n = System.nanoTime();
        return utilisateurRepository.save(Utilisateur.builder()
                .nom("Test").prenom("Bord").sexe("M")
                .telephone("509" + n % 100000000L)
                .email("bord" + n + "@sol.ht").adresse("PAP")
                .cinNif("CIN-" + n).dateNaissance(LocalDate.of(1995, 1, 1))
                .motDePasseHache("x").role("MEMBRE").build());
    }
}
