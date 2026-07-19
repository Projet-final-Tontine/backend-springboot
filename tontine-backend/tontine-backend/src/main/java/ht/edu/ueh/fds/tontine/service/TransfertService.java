package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.dto.AnnuaireDtos.RechercheUtilisateurResponse;
import ht.edu.ueh.fds.tontine.dto.PortefeuilleResponse;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Transfert d'argent entre utilisateurs (par username ou e-mail). Débite le
 * portefeuille de l'expéditeur et crédite celui du bénéficiaire ; chaque
 * mouvement est scellé dans le Registre Inviolable.
 */
@Service
@RequiredArgsConstructor
public class TransfertService {

    private final AnnuaireService annuaireService;
    private final PortefeuilleService portefeuilleService;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * Effectue le transfert. Interdit : l'envoi à soi-même, un bénéficiaire
     * suspendu/inactif (gérés par la recherche), et un solde insuffisant.
     */
    @Transactional
    public PortefeuilleResponse transferer(String expediteurId, String beneficiaireRequete,
                                           BigDecimal montant, String note) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant du transfert doit être positif.");
        }
        // Résout le bénéficiaire (exclut déjà soi-même et les comptes indisponibles).
        RechercheUtilisateurResponse benef = annuaireService.rechercher(beneficiaireRequete, expediteurId);

        String monUsername = utilisateurRepository.findById(expediteurId)
                .map(u -> u.getUsername()).orElse("un membre");
        String suffixe = (note == null || note.isBlank()) ? "" : " — " + note.trim();

        // Débit expéditeur (vérifie le solde) puis crédit bénéficiaire ; les deux
        // scellent un bloc au registre. La transaction assure l'atomicité.
        portefeuilleService.debiter(expediteurId, montant, "TRANSFERT",
                "Transfert à @" + benef.username() + suffixe);
        portefeuilleService.crediter(benef.id(), montant, "TRANSFERT",
                "Reçu de @" + monUsername + suffixe);

        return portefeuilleService.consulter(expediteurId);
    }
}
