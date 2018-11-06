
create table SAMLADDON_SAML_CONNECTION (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255) not null,
    CODE varchar(100) not null,
    SP_ID varchar(255) not null,
    ACTIVE boolean default false,
    KEYSTORE_ID uuid,
    KEYSTORE_LOGIN varchar(255),
    KEYSTORE_PASSWORD varchar(255),
    IDP_METADATA_URL varchar(255) not null,
    DEFAULT_GROUP_ID uuid not null,
    PROCESSING_SERVICE varchar(255) not null,
    --
    primary key (ID)
)^
alter table SAMLADDON_SAML_CONNECTION add constraint FK_SAMLADDON_SAML_CONNECTION_ON_KEYSTORE foreign key (KEYSTORE_ID) references SYS_FILE(ID)^
create index IDX_SAMLADDON_SAML_CONNECTION_ON_KEYSTORE on SAMLADDON_SAML_CONNECTION (KEYSTORE_ID)^

alter table SAMLADDON_SAML_CONNECTION add constraint FK_SAMLADDON_SAML_CONNECTION_ON_DEFAULT_GROUP foreign key (DEFAULT_GROUP_ID) references SEC_GROUP(ID)^
create index IDX_SAMLADDON_SAML_CONNECTION_ON_DEFAULT_GROUP on SAMLADDON_SAML_CONNECTION (DEFAULT_GROUP_ID)^

----------------------------------------------------------------------------------------------------------------
