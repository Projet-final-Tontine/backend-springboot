package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.PaiementSimule;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accès aux ordres de paiement de la passerelle. */
public interface PaiementSimuleRepository extends JpaRepository<PaiementSimule, String> {
}
