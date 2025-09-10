package hxy.dragon.service.impl;

import hxy.dragon.dao.mapper.EmailVerificationMapper;
import hxy.dragon.dao.model.EmailVerificationModel;
import hxy.dragon.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Email service implementation
 *
 * @author houxiaoyi
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailVerificationMapper emailVerificationMapper;

    @Value("${email.from}")
    private String fromEmail;

    @Override
    public String sendVerificationCode(String email) {
        try {
            // Generate 6-digit verification code
            String verificationCode = generateVerificationCode();
            
            // Clean up expired verification codes
            emailVerificationMapper.deleteExpiredCodes();
            
            // Save verification code to database
            EmailVerificationModel emailVerification = new EmailVerificationModel();
            emailVerification.setEmail(email);
            emailVerification.setVerificationCode(verificationCode);
            emailVerification.setUsed(false);
            emailVerification.setCreateTime(LocalDateTime.now());
            emailVerification.setExpireTime(LocalDateTime.now().plusMinutes(10)); // Expire in 10 minutes
            
            emailVerificationMapper.insert(emailVerification);
            
            // Send email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("AirShare Email Verification");
            message.setText("Your verification code is: " + verificationCode + "\n\nThis code will expire in 10 minutes.");
            
            mailSender.send(message);
            
            log.info("Verification code sent to email: {}", email);
            return verificationCode;
            
        } catch (Exception e) {
            log.error("Failed to send verification code to email: {}", email, e);
            throw new RuntimeException("Failed to send verification email");
        }
    }

    @Override
    public boolean verifyCode(String email, String code) {
        try {
            EmailVerificationModel verification = emailVerificationMapper.findValidVerification(email, code);
            
            if (verification != null) {
                // Mark as used
                emailVerificationMapper.markAsUsed(verification.getId());
                log.info("Email verification successful for: {}", email);
                return true;
            }
            
            log.warn("Invalid verification code for email: {}", email);
            return false;
            
        } catch (Exception e) {
            log.error("Error verifying code for email: {}", email, e);
            return false;
        }
    }

    /**
     * Generate 6-digit verification code
     */
    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}