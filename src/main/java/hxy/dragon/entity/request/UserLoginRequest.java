package hxy.dragon.entity.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * User login request
 *
 * @author houxiaoyi
 */
@Data
public class UserLoginRequest {

    @NotBlank(message = "Username or email cannot be empty")
    private String usernameOrEmail;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}