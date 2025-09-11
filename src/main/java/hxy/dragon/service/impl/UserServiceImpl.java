package hxy.dragon.service.impl;

import hxy.dragon.dao.mapper.UserMapper;
import hxy.dragon.dao.model.UserModel;
import hxy.dragon.entity.request.UserLoginRequest;
import hxy.dragon.entity.request.UserRegisterRequest;
import hxy.dragon.entity.request.VerificationCodeLoginRequest;
import hxy.dragon.entity.response.UserLoginResponse;
import hxy.dragon.service.EmailService;
import hxy.dragon.service.UserService;
import hxy.dragon.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * User service implementation
 *
 * @author houxiaoyi
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserModel register(UserRegisterRequest request) {
        // Check if username already exists
        if (existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Verify email verification code
        if (!emailService.verifyCode(request.getEmail(), request.getVerificationCode())) {
            throw new RuntimeException("Invalid or expired verification code");
        }

        // Create new user
        UserModel user = new UserModel();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // Save user to database
        userMapper.insert(user);

        log.info("User registered successfully: {}", request.getUsername());
        return user;
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request) {
        // Find user by username or email
        UserModel user = findByUsernameOrEmail(request.getUsernameOrEmail());
        
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (!user.getEnabled()) {
            throw new RuntimeException("User account is disabled");
        }

        if (!user.getEmailVerified()) {
            throw new RuntimeException("Email not verified");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Update last login time
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getEmail());

        log.info("User logged in successfully: {}", user.getUsername());
        
        return new UserLoginResponse(token, user.getUsername(), user.getEmail(), user.getId());
    }

    @Override
    @Transactional
    public UserLoginResponse loginWithVerificationCode(VerificationCodeLoginRequest request) {
        // Verify verification code first
        if (!emailService.verifyCode(request.getEmail(), request.getVerificationCode())) {
            throw new RuntimeException("Invalid or expired verification code");
        }

        // Find user by email
        UserModel user = userMapper.findByEmail(request.getEmail());
        
        // If user doesn't exist, create a new user automatically
        if (user == null) {
            log.info("User not found for email: {}, creating new user", request.getEmail());
            user = createUserFromEmail(request.getEmail());
        }

        if (!user.getEnabled()) {
            throw new RuntimeException("User account is disabled");
        }

        // Update last login time
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getEmail());

        log.info("User logged in with verification code successfully: {}", user.getUsername());
        
        return new UserLoginResponse(token, user.getUsername(), user.getEmail(), user.getId());
    }

    /**
     * Create a new user from email during verification code login
     */
    private UserModel createUserFromEmail(String email) {
        // Generate a unique username from email
        String baseUsername = email.substring(0, email.indexOf('@'));
        String username = generateUniqueUsername(baseUsername);
        
        // Create new user without password
        UserModel user = new UserModel();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(null); // No password for verification code created users
        user.setEnabled(true);
        user.setEmailVerified(true); // Email is verified through verification code
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // Save user to database
        userMapper.insert(user);

        log.info("New user created automatically during verification code login: {}", username);
        return user;
    }

    /**
     * Generate a unique username by appending numbers if needed
     */
    private String generateUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 1;
        
        // Keep trying until we find a unique username
        while (existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }
        
        return username;
    }

    @Override
    public UserModel findByUsernameOrEmail(String usernameOrEmail) {
        return userMapper.findByUsernameOrEmail(usernameOrEmail);
    }

    @Override
    public UserModel findById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.findByUsername(username) != null;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userMapper.findByEmail(email) != null;
    }
}