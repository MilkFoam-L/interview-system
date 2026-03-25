package interview.repository;

import interview.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    
    // 区分大小写的用户名查询 - 使用原生SQL
    @Query(value = "SELECT * FROM users u WHERE BINARY u.username = :username", nativeQuery = true)
    Optional<User> findByUsernameCaseSensitive(@Param("username") String username);
    
    // 区分大小写的用户名存在性检查 - 使用原生SQL
    @Query(value = "SELECT COUNT(*) > 0 FROM users u WHERE BINARY u.username = :username", nativeQuery = true)
    boolean existsByUsernameCaseSensitive(@Param("username") String username);
} 