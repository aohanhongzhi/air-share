package hxy.dragon.controller;

import hxy.dragon.dao.model.UserModel;
import hxy.dragon.entity.reponse.BaseResponse;
import hxy.dragon.entity.request.EmailVerificationRequest;
import hxy.dragon.entity.request.UserLoginRequest;
import hxy.dragon.entity.request.UserRegisterRequest;
import hxy.dragon.entity.request.VerificationCodeLoginRequest;
import hxy.dragon.entity.response.UserLoginResponse;
import hxy.dragon.service.EmailService;
import hxy.dragon.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

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
            
            // Create session for template-based navigation
            HttpSession session = httpRequest.getSession(true);
            
            // Create authentication for session
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                response.getUsername(),
                null, // Don't store password
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            
            // Set authentication in security context
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(auth);
            
            // Store security context in session
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
            SecurityContextHolder.setContext(securityContext);
            
            log.info("Session created for user: {}", response.getUsername());
            
            return BaseResponse.success("Login successful", response);
        } catch (Exception e) {
            log.error("User login failed", e);
            return BaseResponse.error("Login failed: " + e.getMessage());
        }
    }

    /**
     * Send login verification code for existing users
     */
    @PostMapping("/send-login-verification-code")
    public BaseResponse<String> sendLoginVerificationCode(@Validated @RequestBody EmailVerificationRequest request) {
        try {
            // Check if email is registered
            if (!userService.existsByEmail(request.getEmail())) {
                return BaseResponse.error("Email is not registered");
            }

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
            
            // Create session for template-based navigation
            HttpSession session = httpRequest.getSession(true);
            
            // Create authentication for session
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                response.getUsername(),
                null, // Don't store password
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            
            // Set authentication in security context
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(auth);
            
            // Store security context in session
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
            SecurityContextHolder.setContext(securityContext);
            
            log.info("Session created for user: {}", response.getUsername());
            
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
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
                log.info("Session invalidated for logout");
            }
            
            SecurityContextHolder.clearContext();
            
            return BaseResponse.success("Logout successful");
        } catch (Exception e) {
            log.error("Logout failed", e);
            return BaseResponse.error("Logout failed: " + e.getMessage());
        }
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
}