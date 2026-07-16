package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.ContributionSekou;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContributionSekouRepository extends JpaRepository<ContributionSekou, String> {
}
