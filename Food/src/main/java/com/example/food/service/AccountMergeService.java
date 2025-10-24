package com.example.food.service;

import com.example.food.model.AuthProvider;
import com.example.food.model.User;
import com.example.food.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountMergeService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Xử lý trường hợp email đã tồn tại với auth provider khác
     * @param email Email của user
     * @param newAuthProvider Auth provider mới
     * @param googleName Tên từ Google (nếu có)
     * @return User đã được merge hoặc tạo mới
     */
    @Transactional
    public User handleAccountMerge(String email, AuthProvider newAuthProvider, String googleName) {
        User existingUser = userRepository.findByEmail(email).orElse(null);

        if (existingUser == null) {
            // Tạo user mới
            return createNewUser(email, newAuthProvider, googleName);
        }

        // Kiểm tra auth provider hiện tại
        AuthProvider currentAuthProvider = existingUser.getAuthProvider();

        if (currentAuthProvider == newAuthProvider) {
            // Cùng auth provider, chỉ cần return user hiện tại
            return existingUser;
        }

        // Khác auth provider - cần merge
        return mergeAccounts(existingUser, newAuthProvider, googleName);
    }

    /**
     * Tạo user mới
     */
    private User createNewUser(String email, AuthProvider authProvider, String googleName) {
        User user = new User();
        user.setEmail(email);
        user.setAuthProvider(authProvider);
        user.setRoleId(1);

        if (authProvider == AuthProvider.GOOGLE) {
            user.setFullName(googleName != null ? googleName : "Google User");
            user.setPassword("GOOGLE_USER_" + System.currentTimeMillis());
        } else {
            user.setFullName("User");
            user.setPassword("EMAIL_USER_" + System.currentTimeMillis());
        }

        return userRepository.save(user);
    }

    /**
     * Merge tài khoản khi có email trùng lặp
     */
    private User mergeAccounts(User existingUser, AuthProvider newAuthProvider, String googleName) {
        // Cập nhật thông tin từ Google nếu cần
        if (newAuthProvider == AuthProvider.GOOGLE && googleName != null) {
            if (existingUser.getFullName() == null || existingUser.getFullName().equals("User") || existingUser.getFullName().equals("Google User")) {
                existingUser.setFullName(googleName);
            }
        }

        // Nếu user đã có password (EMAIL auth provider), giữ nguyên để hỗ trợ cả hai phương thức
        if (existingUser.getAuthProvider() == AuthProvider.EMAIL) {
            // User đã có password, chỉ cập nhật thông tin từ Google
            return userRepository.save(existingUser);
        }

        // Nếu user chỉ có Google auth provider, cập nhật thông tin
        existingUser.setAuthProvider(newAuthProvider);

        return userRepository.save(existingUser);
    }

    /**
     * Kiểm tra xem user có thể đăng nhập bằng phương thức nào
     */
    public boolean canLoginWithMethod(String email, AuthProvider authProvider) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        // Nếu user đã được merge hoặc có cùng auth provider
        return user.getAuthProvider() == authProvider ||
                user.getAuthProvider() == AuthProvider.EMAIL; // EMAIL users có thể login bằng Google
    }
}
