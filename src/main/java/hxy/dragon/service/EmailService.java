package hxy.dragon.service;

/**
 * Email service interface
 *
 * @author houxiaoyi
 */
public interface EmailService {

    /**
     * Send verification code to email
     *
     * @param email target email address
     * @return verification code sent
     */
    String sendVerificationCode(String email);

    /**
     * Send login verification code to existing user
     *
     * @param email target email address
     * @return verification code sent
     */
    String sendLoginVerificationCode(String email);

    /**
     * Verify email verification code
     *
     * @param email target email address
     * @param code verification code
     * @return true if verification is successful
     */
    boolean verifyCode(String email, String code);
}