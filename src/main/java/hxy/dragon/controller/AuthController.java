package hxy.dragon.controller;

import hxy.dragon.dao.model.UserModel;
import hxy.dragon.entity.reponse.BaseResponse;
import hxy.dragon.entity.request.EmailVerificationRequest;
import hxy.dragon.entity.request.UpdatePasswordRequest;
import hxy.dragon.entity.request.UpdateUsernameRequest;
import hxy.dragon.entity.request.UserLoginRequest;
import hxy.dragon.entity.request.UserRegisterRequest;
import hxy.dragon.entity.request.VerificationCodeLoginRequest;
import hxy.dragon.entity.response.UserLoginResponse;
import hxy.dragon.service.EmailService;
import hxy.dragon.service.UserService;
import hxy.dragon.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication controller
 *
 * @author houxiaoyi
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Send email verification code
     */
    @PostMapping("/send-verification-code")
    public BaseResponse<String> sendVerificationCode(@Validated @RequestBody EmailVerificationRequest request) {
        try {
            // Check if email is already registered
            if (userService.existsByEmail(request.getEmail())) {
                return BaseResponse.error("Email is already registered");
            }

            String code = emailService.sendVerificationCode(request.getEmail());
            log.info("Verification code sent to: {}", request.getEmail());

            return BaseResponse.success("Verification code sent successfully");
        } catch (Exception e) {
            log.error("Failed to send verification code", e);
            return BaseResponse.error("Failed to send verification code: " + e.getMessage());
        }
    }

    /**
     * User registration
     */
    @PostMapping("/register")
    public BaseResponse<UserModel> register(@Validated @RequestBody UserRegisterRequest request) {
        try {
            UserModel user = userService.register(request);
            log.info("User registered successfully: {}", user.getUsername());

            // Remove password from response
            user.setPassword(null);

            return BaseResponse.success("User registered successfully", user);
        } catch (Exception e) {
            log.error("User registration failed", e);
            return BaseResponse.error("Registration failed: " + e.getMessage());
        }
    }

    /**
     * User login
     */
    @PostMapping("/login")
    public BaseResponse<UserLoginResponse> login(@Validated @RequestBody UserLoginRequest request, HttpServletRequest httpRequest) {
        try {
            UserLoginResponse response = userService.login(request);
            log.info("User logged in successfully: {}", response.getUsername());

            // Stateless login: return tokens only; do not create HTTP session

            return BaseResponse.success("Login successful", response);
        } catch (Exception e) {
            log.error("User login failed", e);
            return BaseResponse.error("Login failed: " + e.getMessage());
        }
    }

    /**
     * Send login verification code (allows for both existing and new users)
     */
    @PostMapping("/send-login-verification-code")
    public BaseResponse<String> sendLoginVerificationCode(@Validated @RequestBody EmailVerificationRequest request) {
        try {
            // Send verification code regardless of whether email is registered
            // This allows for automatic user creation during login
            String code = emailService.sendLoginVerificationCode(request.getEmail());
            log.info("Login verification code sent to: {}", request.getEmail());

            return BaseResponse.success("Login verification code sent successfully");
        } catch (Exception e) {
            log.error("Failed to send login verification code", e);
            return BaseResponse.error("Failed to send login verification code: " + e.getMessage());
        }
    }

    /**
     * User login with verification code
     */
    @PostMapping("/login-with-code")
    public BaseResponse<UserLoginResponse> loginWithCode(@Validated @RequestBody VerificationCodeLoginRequest request, HttpServletRequest httpRequest) {
        try {
            UserLoginResponse response = userService.loginWithVerificationCode(request);
            log.info("User logged in with verification code successfully: {}", response.getUsername());

            // Stateless login with code: return tokens only; do not create HTTP session

            return BaseResponse.success("Login with verification code successful", response);
        } catch (Exception e) {
            log.error("User login with verification code failed", e);
            return BaseResponse.error("Login with verification code failed: " + e.getMessage());
        }
    }

    /**
     * User logout - clear session
     */
    @PostMapping("/logout")
    public BaseResponse<String> logout(HttpServletRequest request) {
        try {
            SecurityContextHolder.clearContext();

            return BaseResponse.success("Logout successful");
        } catch (Exception e) {
            log.error("Logout failed", e);
            return BaseResponse.error("Logout failed: " + e.getMessage());
        }
    }

    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public BaseResponse<UserLoginResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            String username = jwtUtil.extractUsername(refreshToken);
            if (!jwtUtil.validateRefreshToken(refreshToken, username)) {
                return BaseResponse.error("Invalid refresh token");
            }

            Long userId = jwtUtil.extractUserId(refreshToken);
            String email = jwtUtil.extractEmail(refreshToken);

            String newAccessToken = jwtUtil.generateAccessToken(userId, username, email);
            String newRefreshToken = jwtUtil.generateRefreshToken(userId, username, email);

            UserLoginResponse response = new UserLoginResponse(newAccessToken, newRefreshToken, username, email, userId);
            return BaseResponse.success("Token refreshed", response);
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            return BaseResponse.error("Failed to refresh token");
        }
    }

    @Data
    static class RefreshTokenRequest {
        private String refreshToken;
    }

    /**
     * Check if username is available
     */
    @GetMapping("/check-username")
    public BaseResponse<Boolean> checkUsername(@RequestParam String username) {
        try {
            boolean exists = userService.existsByUsername(username);
            return BaseResponse.success(exists ? "Username already exists" : "Username is available", !exists);
        } catch (Exception e) {
            log.error("Error checking username", e);
            return BaseResponse.error("Error checking username");
        }
    }

    /**
     * Check if email is available
     */
    @GetMapping("/check-email")
    public BaseResponse<Boolean> checkEmail(@RequestParam String email) {
        try {
            boolean exists = userService.existsByEmail(email);
            return BaseResponse.success(exists ? "Email already exists" : "Email is available", !exists);
        } catch (Exception e) {
            log.error("Error checking email", e);
            return BaseResponse.error("Error checking email");
        }
    }

    /**
     * Get current user's information
     */
    @GetMapping("/profile")
    public BaseResponse<Map<String, Object>> getUserProfile(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return BaseResponse.error("User not authenticated");
            }

            UserModel user = userService.findById(userId);

            if (user == null) {
                return BaseResponse.error("User not found");
            }

            // Create response with user info and hasPassword flag
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("id", user.getId());
            profileData.put("username", user.getUsername());
            profileData.put("email", user.getEmail());
            profileData.put("enabled", user.getEnabled());
            profileData.put("emailVerified", user.getEmailVerified());
            profileData.put("createTime", user.getCreateTime());
            profileData.put("updateTime", user.getUpdateTime());
            profileData.put("lastLoginTime", user.getLastLoginTime());
            profileData.put("hasPassword", user.getPassword() != null);

            return BaseResponse.success("User profile retrieved successfully", profileData);
        } catch (Exception e) {
            log.error("Error getting user profile", e);
            return BaseResponse.error("Error getting user profile");
        }
    }

    /**
     * Update username
     */
    @PutMapping("/update-username")
    public BaseResponse<UserModel> updateUsername(@Validated @RequestBody UpdateUsernameRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return BaseResponse.error("User not authenticated");
            }

            UserModel currentUser = userService.findById(userId);

            if (currentUser == null) {
                return BaseResponse.error("User not found");
            }

            UserModel updatedUser = userService.updateUsername(currentUser.getId(), request);

            // Remove password from response
            updatedUser.setPassword(null);

            log.info("Username updated successfully: {} -> {}", currentUser.getUsername(), request.getUsername());
            return BaseResponse.success("Username updated successfully", updatedUser);
        } catch (Exception e) {
            log.error("Username update failed", e);
            return BaseResponse.error("Username update failed: " + e.getMessage());
        }
    }

    /**
     * Update password
     */
    @PutMapping("/update-password")
    public BaseResponse<String> updatePassword(@Validated @RequestBody UpdatePasswordRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return BaseResponse.error("User not authenticated");
            }

            UserModel user = userService.findById(userId);

            if (user == null) {
                return BaseResponse.error("User not found");
            }

            userService.updatePassword(user.getId(), request);

            log.info("Password updated successfully for user: {}", user.getUsername());
            return BaseResponse.success("Password updated successfully");
        } catch (Exception e) {
            log.error("Password update failed", e);
            return BaseResponse.error("Password update failed: " + e.getMessage());
        }
    }

    /**
     * Extract user ID from JWT token in request
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        try {
            final String authorizationHeader = request.getHeader("Authorization");

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String jwt = authorizationHeader.substring(7);
                return jwtUtil.extractUserId(jwt);
            }

            return null;
        } catch (Exception e) {
            log.error("Error extracting user ID from request", e);
            return null;
        }
    }
}