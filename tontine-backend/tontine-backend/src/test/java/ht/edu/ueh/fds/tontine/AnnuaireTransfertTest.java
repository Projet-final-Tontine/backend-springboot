package ht.edu.ueh.fds.tontine;

import ht.edu.ueh.fds.tontine.dto.AnnuaireDtos.RechercheUtilisateurResponse;
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

        // Transfert : A dépose 1000, envoie 400 à B.
        portefeuilleService.deposerParMoyen(a.getId(), new BigDecimal("1000"), "TEST", "ref");
        transfertService.transferer(a.getId(), "@" + b.getUsername(), new BigDecimal("400"), "merci");
        assertThat(portefeuilleService.solde(a.getId())).isEqualByComparingTo("600");
        assertThat(portefeuilleService.solde(b.getId())).isEqualByComparingTo("400");

        // Solde insuffisant refusé.
        assertThatThrownBy(() -> transfertService.transferer(a.getId(), "@" + b.getUsername(),
                new BigDecimal("999999"), null)).isInstanceOf(BusinessException.class);
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
