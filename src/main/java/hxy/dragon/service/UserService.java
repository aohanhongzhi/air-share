package hxy.dragon.service;

import hxy.dragon.dao.model.UserModel;
import hxy.dragon.entity.request.UpdatePasswordRequest;
import hxy.dragon.entity.request.UpdateUsernameRequest;
import hxy.dragon.entity.request.UserLoginRequest;
import hxy.dragon.entity.request.UserRegisterRequest;
import hxy.dragon.entity.request.VerificationCodeLoginRequest;
import hxy.dragon.entity.response.UserLoginResponse;

/**
 * User service interface
 *
 * @author houxiaoyi
 */
public interface UserService {

    /**
     * Register new user
     *
     * @param request registration request
     * @return registered user
     */
    UserModel register(UserRegisterRequest request);

    /**
     * User login
     *
     * @param request login request
     * @return login response with token
     */
    UserLoginResponse login(UserLoginRequest request);

    /**
     * User login with verification code
     *
     * @param request verification code login request
     * @return login response with token
     */
    UserLoginResponse loginWithVerificationCode(VerificationCodeLoginRequest request);

    /**
     * Find user by username or email
     *
     * @param usernameOrEmail username or email
     * @return user model
     */
    UserModel findByUsernameOrEmail(String usernameOrEmail);

    /**
     * Find user by ID
     *
     * @param userId user ID
     * @return user model
     */
    UserModel findById(Long userId);

    /**
     * Check if username exists
     *
     * @param username username to check
     * @return true if exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     *
     * @param email email to check
     * @return true if exists
     */
    boolean existsByEmail(String email);

    /**
     * Update username
     *
     * @param userId user ID
     * @param request update username request
     * @return updated user
     */
    UserModel updateUsername(Long userId, UpdateUsernameRequest request);

    /**
     * Update password
     *
     * @param userId user ID
     * @param request update password request
     * @return updated user
     */
    UserModel updatePassword(Long userId, UpdatePasswordRequest request);
}