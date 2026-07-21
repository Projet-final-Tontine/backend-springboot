package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.FavoriBeneficiaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Accès aux bénéficiaires favoris. */
public interface FavoriBeneficiaireRepository extends JpaRepository<FavoriBeneficiaire, String> {

    List<FavoriBeneficiaire> findByProprietaireIdOrderByDateCreationDesc(String proprietaireId);

    Optional<FavoriBeneficiaire> findByProprietaireIdAndBeneficiaireId(String proprietaireId, String beneficiaireId);

    boolean existsByProprietaireIdAndBeneficiaireId(String proprietaireId, String beneficiaireId);
}
