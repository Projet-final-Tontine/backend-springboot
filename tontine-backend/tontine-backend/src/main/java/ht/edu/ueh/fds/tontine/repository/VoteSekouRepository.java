package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.VoteSekou;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteSekouRepository extends JpaRepository<VoteSekou, String> {

    Optional<VoteSekou> findByDemandeIdAndVotantId(String demandeId, String votantId);

    long countByDemandeIdAndPour(String demandeId, boolean pour);
}
