alter table SAMLADDON_SAML_CONNECTION alter column CODE set null ;
alter table SAMLADDON_SAML_CONNECTION add SSO_PATH varchar(100) ^
update SAMLADDON_SAML_CONNECTION set SSO_PATH = '' where SSO_PATH is null ;
alter table SAMLADDON_SAML_CONNECTION alter column SSO_PATH set not null ;
alter table SAMLADDON_SAML_CONNECTION drop column CODE cascade ;

