package interview.service;

import interview.model.UserDetail;

public interface UserDetailService {
    UserDetail getUserDetailByUserId(Long userId);
    UserDetail saveUserDetail(UserDetail userDetail);
    UserDetail updateUserDetail(Long userId, UserDetail userDetail);
} 