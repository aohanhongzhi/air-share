package hxy.dragon.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hxy.dragon.dao.model.EmailVerificationModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Email verification mapper interface
 *
 * @author houxiaoyi
 */
@Mapper
public interface EmailVerificationMapper extends BaseMapper<EmailVerificationModel> {

    /**
     * Find valid verification code by email and code
     */
    @Select("SELECT * FROM email_verification WHERE email = #{email} AND verification_code = #{code} AND used = false AND expire_time > CURRENT_TIMESTAMP ORDER BY create_time DESC LIMIT 1")
    EmailVerificationModel findValidVerification(@Param("email") String email, @Param("code") String code);

    /**
     * Mark verification code as used
     */
    @Update("UPDATE email_verification SET used = true WHERE id = #{id}")
    int markAsUsed(@Param("id") Long id);

    /**
     * Delete expired verification codes
     */
    @Update("DELETE FROM email_verification WHERE expire_time <= CURRENT_TIMESTAMP")
    int deleteExpiredCodes();
}