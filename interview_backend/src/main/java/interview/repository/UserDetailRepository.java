package interview.repository;

import interview.model.UserDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserDetailRepository extends JpaRepository<UserDetail, Long> {
    Optional<UserDetail> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
} 