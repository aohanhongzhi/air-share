package hxy.dragon.entity.request;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Verification code login request
 *
 * @author houxiaoyi
 */
@Data
public class VerificationCodeLoginRequest {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email format is invalid")
    private String email;

    @NotBlank(message = "Verification code cannot be empty")
    private String verificationCode;
}