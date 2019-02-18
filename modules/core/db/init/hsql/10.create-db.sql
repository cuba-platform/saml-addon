-- begin SAMLADDON_SAML_CONNECTION
create table SAMLADDON_SAML_CONNECTION (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255) not null,
    CREATE_USERS boolean,
    CODE varchar(100) not null,
    SP_ID varchar(255) not null,
    ACTIVE boolean,
    IDP_METADATA_URL varchar(255),
    IDP_METADATA_ID varchar(36),
    DEFAULT_GROUP_ID varchar(36) not null,
    PROCESSING_SERVICE varchar(255) not null,
    KEYSTORE_ID varchar(36),
    --
    primary key (ID)
)^
-- end SAMLADDON_SAML_CONNECTION
-- begin SAMLADDON_KEY_STORE
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
)^
-- end SAMLADDON_KEY_STORE
