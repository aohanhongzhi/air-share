package hxy.dragon.entity.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User login response
 *
 * @author houxiaoyi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponse {

    private String token;

    private String refreshToken;

    private String username;

    private String email;

    private Long userId;
}