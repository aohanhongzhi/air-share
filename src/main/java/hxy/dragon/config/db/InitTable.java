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
            String sql = """
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
                            );
                    """;
            log.info("开始建表 {}", sql);
            sqlRunner.run(sql);
        } catch (SQLException e) {
            log.error("{}", e.getMessage(), e);
        }
    }
}
