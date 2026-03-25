package interview.repository;

import interview.model.FaceVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 人脸验证记录数据访问接口
 */
@Repository
public interface FaceVerificationRepository extends JpaRepository<FaceVerification, Long> {
    
    /**
     * 根据身份证号查询验证记录
     * @param idNumber 身份证号
     * @return 验证记录列表
     */
    List<FaceVerification> findByIdNumberOrderByCreateTimeDesc(String idNumber);
    
    /**
     * 查询最近的验证记录
     * @param idNumber 身份证号
     * @return 验证记录列表
     */
    List<FaceVerification> findTop5ByIdNumberOrderByCreateTimeDesc(String idNumber);
} 