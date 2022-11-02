create table file_model
(
    file_name   TEXT,
    file_path   TEXT,
    file_md5    TEXT,
    id          INTEGER not null
        constraint file_model_pk
            primary key autoincrement,
    create_time timestamp,
    file_size   INTEGER,
    file_uuid   TEXT,
    server_name varchar(255)
);

