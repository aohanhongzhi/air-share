package hxy.dragon.entity.request;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Email verification request
 *
 * @author houxiaoyi
 */
@Data
public class EmailVerificationRequest {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email format is invalid")
    private String email;
}