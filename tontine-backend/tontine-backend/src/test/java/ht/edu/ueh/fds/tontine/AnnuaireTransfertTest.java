package ht.edu.ueh.fds.tontine;

import ht.edu.ueh.fds.tontine.dto.AnnuaireDtos.RechercheUtilisateurResponse;
import ht.edu.ueh.fds.tontine.dto.TransfertDtos.TransfertRequest;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import ht.edu.ueh.fds.tontine.service.AnnuaireService;
import ht.edu.ueh.fds.tontine.service.PortefeuilleService;
import ht.edu.ueh.fds.tontine.service.TransfertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prouve le username unique (à la Wise) et le transfert entre utilisateurs :
 * format, disponibilité, unicité insensible à la casse, recherche par username
 * ou e-mail, interdiction de s'envoyer à soi-même, et transfert débit/crédit.
 */
@SpringBootTest
class AnnuaireTransfertTest {

    @Autowired AnnuaireService annuaireService;
    @Autowired TransfertService transfertService;
    @Autowired PortefeuilleService portefeuilleService;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired ht.edu.ueh.fds.tontine.service.FavoriService favoriService;
    @Autowired ht.edu.ueh.fds.tontine.repository.VerificationKycRepository kycRepository;

    @Test
    void username_format_disponibilite_et_unicite() {
        String u = "user" + (System.nanoTime() % 100000);
        creer(u);

        // Disponibilité insensible à la casse.
        assertThat(annuaireService.verifierDisponibilite(u).disponible()).isFalse();
        assertThat(annuaireService.verifierDisponibilite(u.toUpperCase()).disponible()).isFalse();
        assertThat(annuaireService.verifierDisponibilite("libre" + (System.nanoTime() % 100000))
                .disponible()).isTrue();

        // Formats invalides refusés.
        for (String mauvais : new String[]{"ab", "123jean", "jean pierre", "jean-pierre", "jean@"}) {
            assertThat(annuaireService.verifierDisponibilite(mauvais).disponible())
                    .as("« %s » doit être invalide", mauvais).isFalse();
        }
        // Formats valides acceptés.
        for (String bon : new String[]{"jean", "jean123", "jean_pierre", "jp.dev"}) {
            AnnuaireService.validerFormat(bon); // ne lève pas
        }
    }

    @Test
    void recherche_et_transfert_entre_utilisateurs() {
        Utilisateur a = creer("expediteur" + (System.nanoTime() % 100000));
        Utilisateur b = creer("benef" + (System.nanoTime() % 100000));

        // Recherche par username (avec @) puis par e-mail.
        RechercheUtilisateurResponse parUser = annuaireService.rechercher("@" + b.getUsername(), a.getId());
        assertThat(parUser.id()).isEqualTo(b.getId());
        assertThat(annuaireService.rechercher(b.getEmail(), a.getId()).id()).isEqualTo(b.getId());

        // On ne peut pas se rechercher / s'envoyer à soi-même.
        assertThatThrownBy(() -> annuaireService.rechercher(a.getUsername(), a.getId()))
                .isInstanceOf(BusinessException.class);

        // Règle stricte : l'expéditeur doit avoir une identité vérifiée (KYC).
        approuverKyc(a.getId());

        // Transfert : A dépose 1000, envoie 400 à B -> reçu avec référence unique.
        portefeuilleService.deposerParMoyen(a.getId(), new BigDecimal("1000"), "TEST", "ref");
        var recu = transfertService.transferer(a.getId(),
                new TransfertRequest("@" + b.getUsername(), new BigDecimal("400"), "HTG", "merci", "Empreinte digitale"));
        assertThat(recu.reference()).startsWith("SOL-");
        assertThat(recu.transactionId()).startsWith("TX-");
        assertThat(recu.soldeRestant()).isEqualByComparingTo("600");
        assertThat(portefeuilleService.solde(a.getId())).isEqualByComparingTo("600");
        assertThat(portefeuilleService.solde(b.getId())).isEqualByComparingTo("400");

        // L'historique de A contient ce transfert (sens ENVOYE).
        var histo = transfertService.historique(a.getId(), "ENVOYES", null);
        assertThat(histo).hasSize(1);
        assertThat(histo.get(0).sens()).isEqualTo("ENVOYE");
        assertThat(histo.get(0).montant()).isEqualByComparingTo("400");

        // Solde insuffisant refusé.
        assertThatThrownBy(() -> transfertService.transferer(a.getId(),
                new TransfertRequest("@" + b.getUsername(), new BigDecimal("999999"), "HTG", null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void transfert_refuse_si_identite_non_verifiee() {
        Utilisateur a = creer("nonkyc" + (System.nanoTime() % 100000));
        Utilisateur b = creer("cible" + (System.nanoTime() % 100000));
        // A a de l'argent mais PAS de KYC approuvé.
        portefeuilleService.deposerParMoyen(a.getId(), new BigDecimal("1000"), "TEST", "ref");

        assertThatThrownBy(() -> transfertService.transferer(a.getId(),
                new TransfertRequest("@" + b.getUsername(), new BigDecimal("100"), "HTG", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dentité"); // « Vérification d'identité requise… »

        // Une fois l'identité vérifiée, le même transfert passe.
        approuverKyc(a.getId());
        var recu = transfertService.transferer(a.getId(),
                new TransfertRequest("@" + b.getUsername(), new BigDecimal("100"), "HTG", null, "Empreinte digitale"));
        assertThat(recu.reference()).startsWith("SOL-");
    }

    @Test
    void recu_detail_verification_et_favoris() {
        Utilisateur a = creer("exp" + (System.nanoTime() % 100000));
        Utilisateur b = creer("ben" + (System.nanoTime() % 100000));
        approuverKyc(a.getId());
        portefeuilleService.deposerParMoyen(a.getId(), new BigDecimal("500"), "TEST", "ref");

        var recu = transfertService.transferer(a.getId(),
                new TransfertRequest("@" + b.getUsername(), new BigDecimal("200"), "HTG", "cadeau", "Mot de passe"));

        // Détail accessible à l'expéditeur ; vérification publique par référence.
        var detail = transfertService.detail(a.getId(), recu.id());
        assertThat(detail.sens()).isEqualTo("ENVOYE");
        assertThat(detail.message()).isEqualTo("cadeau");
        assertThat(transfertService.verifier(recu.reference())).isNotNull();
        assertThat(transfertService.verifier("SOL-INEXISTANT")).isNull();

        // Favoris : ajouter, lister, supprimer.
        favoriService.ajouter(a.getId(), b.getId());
        assertThat(favoriService.lister(a.getId())).hasSize(1);
        assertThat(favoriService.lister(a.getId()).get(0).username()).isEqualTo(b.getUsername());
        favoriService.supprimer(a.getId(), b.getId());
        assertThat(favoriService.lister(a.getId())).isEmpty();
    }

    /** Crée un dossier KYC approuvé (identité vérifiée) pour l'utilisateur. */
    private void approuverKyc(String userId) {
        kycRepository.save(ht.edu.ueh.fds.tontine.entity.VerificationKyc.builder()
                .id(java.util.UUID.randomUUID().toString())
                .utilisateurId(userId)
                .typeDocument("CARTE_IDENTITE")
                .rectoUrl("recto.jpg")
                .versoUrl("verso.jpg")
                .statut("APPROUVE")
                .dateSoumission(java.time.LocalDateTime.now())
                .dateDecision(java.time.LocalDateTime.now())
                .build());
    }

    private Utilisateur creer(String username) {
        long n = System.nanoTime();
        return utilisateurRepository.save(Utilisateur.builder()
                .nom("Test").prenom("User").sexe("M")
                .telephone("509" + n % 100000000L)
                .email("u" + n + "@sol.ht").username(username).adresse("PAP")
                .cinNif("CIN-" + n).dateNaissance(LocalDate.of(1990, 1, 1))
                .motDePasseHache("x").role("MEMBRE").statut("ACTIF").build());
    }
}
