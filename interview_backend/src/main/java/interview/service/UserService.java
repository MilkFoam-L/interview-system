package interview.service;

import interview.model.User;

public interface UserService {
    User register(User user);
    User login(String username, String password);
    boolean existsByUsername(String username);
    User findById(Long id);
    void changePassword(Long userId, String oldPassword, String newPassword);
    User updateUser(User user);
} 