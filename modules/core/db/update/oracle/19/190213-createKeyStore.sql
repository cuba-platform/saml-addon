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
    LOGIN varchar2(100) not null,
    KEYSTORE_ID varchar2(32),
    PASSWORD varchar2(100) not null,
    DESCRIPTION varchar2(255),
    --
    primary key (ID)
);

alter table SAMLADDON_KEY_STORE add constraint FK_SAMLADDON_KEY_STORE_ON_KEY foreign key (KEY_ID) references SYS_FILE(ID);
create index IDX_SAMLADDON_KEY_STORE_ON_KEY on SAMLADDON_KEY_STORE (KEY_ID);

