package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.Portefeuille;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortefeuilleRepository extends JpaRepository<Portefeuille, String> {

    /** Le portefeuille d'un utilisateur (un seul par personne). */
    Optional<Portefeuille> findByUtilisateurId(String utilisateurId);
}
