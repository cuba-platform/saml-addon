create table SAMLADDON_KEY_STORE (
    ID uuid not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    LOGIN varchar(100) not null,
    KEY_ID uuid,
    PASSWORD varchar(100) not null,
    DESCRIPTION varchar(255),
    --
    primary key (ID)
);

alter table SAMLADDON_KEY_STORE add constraint FK_SAMLADDON_KEY_STORE_ON_KEY foreign key (KEY_ID) references SYS_FILE(ID);
create index IDX_SAMLADDON_KEY_STORE_ON_KEY on SAMLADDON_KEY_STORE (KEY_ID);

