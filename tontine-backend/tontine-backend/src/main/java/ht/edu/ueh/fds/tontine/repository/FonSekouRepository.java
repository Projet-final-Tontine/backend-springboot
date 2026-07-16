package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.FonSekou;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FonSekouRepository extends JpaRepository<FonSekou, String> {
    Optional<FonSekou> findBySolId(String solId);
}
