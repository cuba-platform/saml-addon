-- begin SAMLADDON_SAML_CONNECTION
create table SAMLADDON_SAML_CONNECTION (
    ID uuid,
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
    SSO_PATH varchar(100) not null,
    SP_ID varchar(255) not null,
    ACTIVE boolean,
    IDP_METADATA_URL varchar(255),
    IDP_METADATA_ID uuid,
    METADATA_TRUST_CHECK boolean,
    DEFAULT_GROUP_ID uuid not null,
    PROCESSING_SERVICE varchar(255) not null,
    KEYSTORE_ID uuid,
    --
    primary key (ID)
)^
-- end SAMLADDON_SAML_CONNECTION
-- begin SAMLADDON_KEY_STORE
create table SAMLADDON_KEY_STORE (
    ID uuid,
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
    KEYSTORE_PASSWORD varchar(100),
    DESCRIPTION varchar(255),
    --
    primary key (ID)
)^
-- end SAMLADDON_KEY_STORE


-- begin SAMLADDON_SAML_CONNECTION
alter table SAMLADDON_SAML_CONNECTION add constraint FK_SAMLADDON_SAML_CONNECTION_ON_IDP_METADATA foreign key (IDP_METADATA_ID) references SYS_FILE(ID)^
alter table SAMLADDON_SAML_CONNECTION add constraint FK_SAMLADDON_SAML_CONNECTION_ON_DEFAULT_GROUP foreign key (DEFAULT_GROUP_ID) references SEC_GROUP(ID)^
alter table SAMLADDON_SAML_CONNECTION add constraint FK_SAMLADDON_SAML_CONNECTION_ON_KEYSTORE foreign key (KEYSTORE_ID) references SAMLADDON_KEY_STORE(ID)^
create index IDX_SAMLADDON_SAML_CONNECTION_ON_IDP_METADATA on SAMLADDON_SAML_CONNECTION (IDP_METADATA_ID)^
create index IDX_SAMLADDON_SAML_CONNECTION_ON_DEFAULT_GROUP on SAMLADDON_SAML_CONNECTION (DEFAULT_GROUP_ID)^
create index IDX_SAMLADDON_SAML_CONNECTION_ON_KEYSTORE on SAMLADDON_SAML_CONNECTION (KEYSTORE_ID)^
-- end SAMLADDON_SAML_CONNECTION
-- begin SAMLADDON_KEY_STORE
alter table SAMLADDON_KEY_STORE add constraint FK_SAMLADDON_KEY_STORE_ON_KEY foreign key (KEY_ID) references SYS_FILE(ID)^
create index IDX_SAMLADDON_KEY_STORE_ON_KEY on SAMLADDON_KEY_STORE (KEY_ID)^
-- end SAMLADDON_KEY_STORE
