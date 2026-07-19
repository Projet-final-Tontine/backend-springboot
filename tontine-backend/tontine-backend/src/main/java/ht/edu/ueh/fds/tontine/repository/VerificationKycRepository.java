package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.VerificationKyc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Accès au dossier KYC d'un utilisateur. */
public interface VerificationKycRepository extends JpaRepository<VerificationKyc, String> {

    Optional<VerificationKyc> findByUtilisateurId(String utilisateurId);
}
