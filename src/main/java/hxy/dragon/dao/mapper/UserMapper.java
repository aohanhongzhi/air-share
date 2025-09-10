package hxy.dragon.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hxy.dragon.dao.model.UserModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * User mapper interface
 *
 * @author houxiaoyi
 */
@Mapper
public interface UserMapper extends BaseMapper<UserModel> {

    /**
     * Find user by username or email
     */
    @Select("SELECT * FROM user_model WHERE username = #{usernameOrEmail} OR email = #{usernameOrEmail}")
    UserModel findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    /**
     * Find user by email
     */
    @Select("SELECT * FROM user_model WHERE email = #{email}")
    UserModel findByEmail(@Param("email") String email);

    /**
     * Find user by username
     */
    @Select("SELECT * FROM user_model WHERE username = #{username}")
    UserModel findByUsername(@Param("username") String username);
}