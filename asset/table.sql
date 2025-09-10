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

-- User table for authentication
create table if not exists user_model
(
    id              INTEGER not null
        constraint user_model_pk
            primary key autoincrement,
    username        varchar(50) not null unique,
    email           varchar(100) not null unique,
    password        varchar(255) not null,
    enabled         boolean default true,
    email_verified  boolean default false,
    create_time     timestamp default current_timestamp,
    update_time     timestamp default current_timestamp,
    last_login_time timestamp
);

-- Email verification table
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
);

-- Create indexes for better performance
create index if not exists idx_user_username on user_model(username);
create index if not exists idx_user_email on user_model(email);
create index if not exists idx_email_verification_email on email_verification(email);
create index if not exists idx_email_verification_code on email_verification(verification_code);

