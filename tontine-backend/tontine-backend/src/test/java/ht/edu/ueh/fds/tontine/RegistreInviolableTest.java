package ht.edu.ueh.fds.tontine;

import ht.edu.ueh.fds.tontine.dto.BlocRegistreResponse;
import ht.edu.ueh.fds.tontine.dto.VerificationRegistreResponse;
import ht.edu.ueh.fds.tontine.entity.BlocRegistre;
import ht.edu.ueh.fds.tontine.repository.BlocRegistreRepository;
import ht.edu.ueh.fds.tontine.service.RegistreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prouve les deux promesses du Registre Inviolable :
 * <ul>
 *   <li>toute falsification d'une écriture passée est détectée (scénario démo) ;</li>
 *   <li>chaque utilisateur ne voit que ses propres écritures.</li>
 * </ul>
 * Les tests sont robustes aux blocs déjà présents (base partagée entre tests).
 */
@SpringBootTest
class RegistreInviolableTest {

    @Autowired
    RegistreService registreService;

    @Autowired
    BlocRegistreRepository registreRepository;

    @Test
    @Transactional  // annule la falsification en fin de test (base partagée entre tests)
    void chaineIntacte_puisDetecteUneFalsification() {
        long avant = registreRepository.count();

        registreService.sceller("DEPOT", "CREDIT", new BigDecimal("500.00"),
                "user-A", "Dépôt Mon Cash", "tx-1");
        registreService.sceller("COTISATION", "DEBIT", new BigDecimal("250.00"),
                "user-A", "Cotisation Sol A", "tx-2");
        registreService.sceller("GAIN_MAIN", "CREDIT", new BigDecimal("1500.00"),
                "user-B", "Réception de la main", "tx-3");

        VerificationRegistreResponse ok = registreService.verifier();
        assertThat(ok.intacte()).isTrue();
        assertThat(ok.empreinteGlobale()).isNotBlank();

        // Falsifie le 2ᵉ bloc que ce test a scellé (index global = avant + 1).
        List<BlocRegistre> chaine = registreRepository.findAllByOrderByPositionAsc();
        BlocRegistre cible = chaine.get((int) avant + 1);
        long positionCible = cible.getPosition();
        cible.setMontant(new BigDecimal("999999.00"));
        registreRepository.save(cible);

        VerificationRegistreResponse apres = registreService.verifier();
        assertThat(apres.intacte()).isFalse();
        assertThat(apres.positionRupture()).isEqualTo(positionCible);
    }

    @Test
    void chaqueUtilisateurNeVoitQueSesEcritures() {
        String userX = "user-X-" + System.nanoTime();
        String userY = "user-Y-" + System.nanoTime();

        registreService.sceller("DEPOT", "CREDIT", new BigDecimal("100.00"), userX, "Dépôt X1", "x1");
        registreService.sceller("DEPOT", "CREDIT", new BigDecimal("200.00"), userX, "Dépôt X2", "x2");
        registreService.sceller("DEPOT", "CREDIT", new BigDecimal("300.00"), userY, "Dépôt Y1", "y1");

        List<BlocRegistreResponse> blocsX = registreService.lister(userX);
        assertThat(blocsX).hasSize(2);
        assertThat(blocsX).allMatch(b -> b.description() != null && b.description().startsWith("Dépôt X"));

        // La vérification porte sur toute la chaîne, mais compte les écritures de X.
        VerificationRegistreResponse vX = registreService.verifier(userX);
        assertThat(vX.intacte()).isTrue();
        assertThat(vX.nombreBlocs()).isEqualTo(2);

        VerificationRegistreResponse vY = registreService.verifier(userY);
        assertThat(vY.nombreBlocs()).isEqualTo(1);
    }
}
