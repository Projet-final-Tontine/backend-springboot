package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.Sol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolRepository extends JpaRepository<Sol, String> {

    /** Adhesion : retrouver un Sol grace au code partage par la Manman sol. */
    Optional<Sol> findByCodeInvitation(String codeInvitation);

    boolean existsByCodeInvitation(String codeInvitation);

    /** Tous les Sols diriges par une Manman sol donnee. */
    List<Sol> findByMamanSolId(String mamanSolId);

    /** Pour le tableau de bord admin : Sols par statut (OUVERT, EN_COURS, TERMINE). */
    List<Sol> findByStatut(String statut);

    long countByStatut(String statut);
}
