package hxy.dragon.config.db;

import org.apache.ibatis.jdbc.SqlRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @description 初始化表
 * @date 2022/4/14
 */
@Component
public class InitTable {

    private static final Logger log = LoggerFactory.getLogger(InitTable.class);

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void createTable() {
        try {
            Connection connection = dataSource.getConnection();
            SqlRunner sqlRunner = new SqlRunner(connection);

            // 分割并执行每个SQL语句
            String[] sqlStatements = {
                """
                create table if not exists file_model
                (
                    id          INTEGER not null
                        constraint file_model_pk
                            primary key autoincrement,
                    file_name   TEXT,
                    file_path   TEXT,
                    file_md5    varchar(255),
                    create_time timestamp,
                    file_size   INTEGER,
                    file_uuid   varchar(255),
                    server_name varchar(255)
                )
                """,
                """
                create table if not exists user_model
                (
                    id              INTEGER not null
                        constraint user_model_pk
                            primary key autoincrement,
                    username        varchar(50) not null unique,
                    email           varchar(100) not null unique,
                    password        varchar(255)  null,
                    enabled         boolean default true,
                    email_verified  boolean default false,
                    create_time     timestamp default current_timestamp,
                    update_time     timestamp default current_timestamp,
                    last_login_time timestamp
                )
                """,
                """
                create table if not exists email_verification
                (
                    id                INTEGER not null
                        constraint email_verification_pk
                            primary key autoincrement,
                    email             varchar(100) not null,
                    verification_code varchar(10) not null,
                    used              boolean default false,
                    create_time       timestamp default current_timestamp,
                    expire_time       timestamp not null
                )
                """,
                "create index if not exists idx_user_username on user_model(username)",
                "create index if not exists idx_user_email on user_model(email)",
                "create index if not exists idx_email_verification_email on email_verification(email)",
                "create index if not exists idx_email_verification_code on email_verification(verification_code)"
            };

            for (String sql : sqlStatements) {
                try {
                    log.info("执行SQL: {}", sql.trim());
                    sqlRunner.run(sql);
                    log.info("SQL执行成功");
                } catch (SQLException e) {
                    log.warn("SQL执行失败 (可能表或索引已存在): {}", e.getMessage());
                }
            }

        } catch (SQLException e) {
            log.error("数据库连接错误: {}", e.getMessage(), e);
        }
    }
}
