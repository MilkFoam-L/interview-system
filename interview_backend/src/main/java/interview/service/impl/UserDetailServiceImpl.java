package interview.service.impl;

import interview.model.UserDetail;
import interview.repository.UserDetailRepository;
import interview.service.UserDetailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class UserDetailServiceImpl implements UserDetailService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserDetailServiceImpl.class);
    
    @Autowired
    private UserDetailRepository userDetailRepository;
    
    @Override
    public UserDetail getUserDetailByUserId(Long userId) {
        logger.info("获取用户详情, userId: {}", userId);
        return userDetailRepository.findByUserId(userId).orElse(null);
    }
    
    @Override
    @Transactional
    public UserDetail saveUserDetail(UserDetail userDetail) {
        logger.info("保存用户详情, userId: {}", userDetail.getUserId());
        // 注意：由于使用了@CreationTimestamp和@UpdateTimestamp注解
        // 创建时间和更新时间会由Hibernate自动处理
        return userDetailRepository.save(userDetail);
    }
    
    @Override
    @Transactional
    public UserDetail updateUserDetail(Long userId, UserDetail userDetail) {
        logger.info("更新用户详情, userId: {}", userId);
        
        Optional<UserDetail> existingDetail = userDetailRepository.findByUserId(userId);
        
        if (existingDetail.isPresent()) {
            // 更新现有记录
            UserDetail detail = existingDetail.get();
            
            // 只更新非空字段
            if (userDetail.getJobTitle() != null) {
                detail.setJobTitle(userDetail.getJobTitle());
            }
            
            if (userDetail.getJobIntention() != null) {
                detail.setJobIntention(userDetail.getJobIntention());
            }
            
            if (userDetail.getExperience() != null) {
                detail.setExperience(userDetail.getExperience());
            }
            
            if (userDetail.getEducation() != null) {
                detail.setEducation(userDetail.getEducation());
            }
            
            if (userDetail.getLocation() != null) {
                detail.setLocation(userDetail.getLocation());
            }
            
            if (userDetail.getSkills() != null) {
                detail.setSkills(userDetail.getSkills());
            }
            
            // 注意：不需要手动设置更新时间，@UpdateTimestamp会自动处理
            return userDetailRepository.save(detail);
        } else {
            // 创建新记录
            userDetail.setUserId(userId);
            // 注意：不需要手动设置创建时间，@CreationTimestamp会自动处理
            return userDetailRepository.save(userDetail);
        }
    }
} 