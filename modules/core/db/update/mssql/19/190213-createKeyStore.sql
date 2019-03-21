create table SAMLADDON_KEY_STORE (
    ID uniqueidentifier not null,
    CREATE_TS datetime,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS datetime,
    UPDATED_BY varchar(50),
    DELETE_TS datetime,
    DELETED_BY varchar(50),
    --
    LOGIN varchar(100) not null,
    KEYSTORE_ID uniqueidentifier,
    PASSWORD varchar(100) not null,
    DESCRIPTION varchar(255),
    --
    primary key nonclustered (ID)
);

alter table SAMLADDON_KEY_STORE add constraint FK_SAMLADDON_KEY_STORE_ON_KEY foreign key (KEY_ID) references SYS_FILE(ID);
create index IDX_SAMLADDON_KEY_STORE_ON_KEY on SAMLADDON_KEY_STORE (KEY_ID);

