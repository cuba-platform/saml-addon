
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
    CODE varchar(100) not null,
    SP_ID varchar(255) not null,
    ACTIVE boolean,
    CREATE_USERS boolean,
    KEYSTORE_ID varchar(36),
    KEYSTORE_LOGIN varchar(255),
    KEYSTORE_PASSWORD varchar(255),
    IDP_METADATA_URL varchar(255),
    IDP_METADATA_ID varchar(36),
    DEFAULT_GROUP_ID varchar(36) not null,
    PROCESSING_SERVICE varchar(255) not null,
    --
    primary key (ID)
)^

alter table SAMLADDON_SAML_CONNECTION add constraint FK_SAMLADDON_SAML_CONNECTION_ON_KEYSTORE foreign key (KEYSTORE_ID) references SYS_FILE(ID)^
create index IDX_SAMLADDON_SAML_CONNECTION_ON_KEYSTORE on SAMLADDON_SAML_CONNECTION (KEYSTORE_ID)^

alter table SAMLADDON_SAML_CONNECTION add constraint FK_SAMLADDON_SAML_CONNECTION_ON_DEFAULT_GROUP foreign key (DEFAULT_GROUP_ID) references SEC_GROUP(ID)^
create index IDX_SAMLADDON_SAML_CONNECTION_ON_DEFAULT_GROUP on SAMLADDON_SAML_CONNECTION (DEFAULT_GROUP_ID)^

alter table SAMLADDON_SAML_CONNECTION add constraint FK_SAMLADDON_SAML_CONNECTION_ON_IDP_METADATA foreign key (IDP_METADATA_ID) references SYS_FILE(ID)^
create index IDX_SAMLADDON_SAML_CONNECTION_ON_IDP_METADATA on SAMLADDON_SAML_CONNECTION (IDP_METADATA_ID)^

----------------------------------------------------------------------------------------------------------------
