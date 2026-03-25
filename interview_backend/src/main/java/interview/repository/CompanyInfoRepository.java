package interview.repository;

import interview.model.CompanyInfo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyInfoRepository extends JpaRepository<CompanyInfo, Long>, JpaSpecificationExecutor<CompanyInfo> {
    List<CompanyInfo> findAllByIsDeletedFalse();
    
    Optional<CompanyInfo> findByUserIdAndIsDeletedFalse(Long userId);
} 