package ht.edu.ueh.fds.tontine;

import ht.edu.ueh.fds.tontine.dto.PaiementRequests.InitierPaiementResponse;
import ht.edu.ueh.fds.tontine.dto.VerificationRegistreResponse;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import ht.edu.ueh.fds.tontine.service.PasserelleService;
import ht.edu.ueh.fds.tontine.service.PortefeuilleService;
import ht.edu.ueh.fds.tontine.service.RegistreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prouve le parcours complet de la passerelle de paiement : un dépôt puis un
 * retrait passent par initier -> confirmer, mettent à jour le portefeuille et
 * scellent chaque opération dans le Registre Inviolable. La confirmation est
 * idempotente (pas de double crédit).
 */
@SpringBootTest
class PasserellePaiementTest {

    @Autowired PasserelleService passerelle;
    @Autowired PortefeuilleService portefeuille;
    @Autowired RegistreService registre;
    @Autowired UtilisateurRepository utilisateurRepository;

    private String userId;

    @BeforeEach
    void creerUtilisateur() {
        Utilisateur u = utilisateurRepository.save(Utilisateur.builder()
                .nom("Test").prenom("Passerelle").sexe("M")
                .telephone("509" + System.nanoTime() % 100000000L)
                .email("passerelle" + System.nanoTime() + "@sol.ht")
                .adresse("Port-au-Prince")
                .cinNif("CIN-" + System.nanoTime())
                .dateNaissance(LocalDate.of(1995, 1, 1))
                .motDePasseHache("x")
                .role("MEMBRE")
                .build());
        userId = u.getId();
    }

    @Test
    void depotPuisRetrait_metAJourLeSoldeEtScelleLeRegistre() {
        long blocsAvant = registre.verifier().nombreBlocs();

        // 1. Dépôt MonCash de 500 HTG.
        InitierPaiementResponse dep = passerelle.initier(userId, "DEPOT",
                "MONCASH", new BigDecimal("500"));
        assertThat(dep.statut()).isEqualTo("EN_ATTENTE");
        assertThat(dep.redirectUrl()).contains("/pay/" + dep.orderId());

        passerelle.confirmer(dep.orderId());
        assertThat(portefeuille.solde(userId)).isEqualByComparingTo("500.00");

        // 2. Idempotence : reconfirmer ne recrédite pas.
        passerelle.confirmer(dep.orderId());
        assertThat(portefeuille.solde(userId)).isEqualByComparingTo("500.00");

        // 3. Retrait NatCash de 200 HTG.
        InitierPaiementResponse ret = passerelle.initier(userId, "RETRAIT",
                "NATCASH", new BigDecimal("200"));
        passerelle.confirmer(ret.orderId());
        assertThat(portefeuille.solde(userId)).isEqualByComparingTo("300.00");

        // 4. Deux opérations scellées, registre intègre.
        VerificationRegistreResponse v = registre.verifier();
        assertThat(v.intacte()).isTrue();
        assertThat(v.nombreBlocs()).isEqualTo(blocsAvant + 2);
    }
}
