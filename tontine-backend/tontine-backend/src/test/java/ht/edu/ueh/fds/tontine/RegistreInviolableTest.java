package ht.edu.ueh.fds.tontine;

import ht.edu.ueh.fds.tontine.dto.VerificationRegistreResponse;
import ht.edu.ueh.fds.tontine.entity.BlocRegistre;
import ht.edu.ueh.fds.tontine.repository.BlocRegistreRepository;
import ht.edu.ueh.fds.tontine.service.RegistreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prouve la promesse du Registre Inviolable : la chaîne est vérifiée intègre
 * tant qu'on n'y touche pas, et toute falsification d'une écriture passée est
 * détectée (c'est le scénario de démonstration devant le jury).
 */
@SpringBootTest
class RegistreInviolableTest {

    @Autowired
    RegistreService registreService;

    @Autowired
    BlocRegistreRepository registreRepository;

    @Test
    void chaineIntacte_puisDetecteUneFalsification() {
        // 1. On scelle trois mouvements d'argent dans le registre.
        registreService.sceller("DEPOT", "CREDIT", new BigDecimal("500.00"),
                "user-1", "Dépôt Mon Cash", "tx-1");
        registreService.sceller("COTISATION", "DEBIT", new BigDecimal("250.00"),
                "user-1", "Cotisation Sol A", "tx-2");
        registreService.sceller("GAIN_MAIN", "CREDIT", new BigDecimal("1500.00"),
                "user-2", "Réception de la main", "tx-3");

        // 2. Sans altération, la chaîne est intègre.
        VerificationRegistreResponse avant = registreService.verifier();
        assertThat(avant.intacte()).isTrue();
        assertThat(avant.nombreBlocs()).isEqualTo(3);
        assertThat(avant.positionRupture()).isNull();
        assertThat(avant.empreinteGlobale()).isNotBlank();

        // 3. Un fraudeur modifie en base le montant d'une écriture passée
        //    (bloc #1) sans pouvoir recalculer un hash valide (pas de secret).
        BlocRegistre bloc = registreRepository.findAllByOrderByPositionAsc().get(1);
        bloc.setMontant(new BigDecimal("999999.00"));
        registreRepository.save(bloc);

        // 4. Le registre attrape immédiatement la falsification au bon bloc.
        VerificationRegistreResponse apres = registreService.verifier();
        assertThat(apres.intacte()).isFalse();
        assertThat(apres.positionRupture()).isEqualTo(1L);
        assertThat(apres.empreinteGlobale()).isNull();
    }
}
