package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.CertificatFiabilite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificatFiabiliteRepository extends JpaRepository<CertificatFiabilite, String> {

    /** Recherche un certificat par sa reference publique (pour la verification). */
    Optional<CertificatFiabilite> findByReference(String reference);

    boolean existsByReference(String reference);
}
