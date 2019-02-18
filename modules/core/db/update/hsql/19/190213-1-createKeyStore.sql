create table SAMLADDON_KEY_STORE (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    LOGIN varchar(100) not null,
    KEY_ID varchar(36),
    PASSWORD varchar(100) not null,
    DESCRIPTION varchar(255),
    --
    primary key (ID)
);
