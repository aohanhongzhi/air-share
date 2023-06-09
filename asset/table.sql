create table if not exists file_model
(
    id          INTEGER not null
        constraint file_model_pk
            primary key autoincrement,
    file_name   TEXT,
    file_path   TEXT,
    file_md5    TEXT,
    create_time timestamp,
    file_size   INTEGER,
    file_uuid   TEXT,
    server_name varchar(255)
);

