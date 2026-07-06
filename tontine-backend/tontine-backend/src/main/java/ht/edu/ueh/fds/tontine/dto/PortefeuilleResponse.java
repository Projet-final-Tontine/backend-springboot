package ht.edu.ueh.fds.tontine.dto;

import java.math.BigDecimal;
import java.util.List;

/** Vue du portefeuille : solde courant + historique des mouvements. */
public record PortefeuilleResponse(
        String id,
        BigDecimal solde,
        List<TransactionPortefeuilleResponse> transactions
) {
}
