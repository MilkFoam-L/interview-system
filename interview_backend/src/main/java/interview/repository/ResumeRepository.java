package interview.repository;

import interview.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    // 可以添加自定义查询方法
    List<Resume> findByUserIdOrderByUpdateTimeDesc(Long userId);
    List<Resume> findByUserId(Long userId);
} 