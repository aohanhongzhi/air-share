package hxy.dragon.dao.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Email verification model
 *
 * @author houxiaoyi
 */
@Data
@TableName("email_verification")
public class EmailVerificationModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String email;

    private String verificationCode;

    private Boolean used;

    private LocalDateTime createTime;

    private LocalDateTime expireTime;
}