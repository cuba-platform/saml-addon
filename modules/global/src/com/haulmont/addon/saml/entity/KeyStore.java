package com.haulmont.addon.saml.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Listeners;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Listeners("keystoreListener")
@NamePattern("%s : %s|login,description")
@Table(name = "SAMLADDON_KEY_STORE")
@Entity(name = "samladdon$KeyStore")
public class KeyStore extends StandardEntity {
    private static final long serialVersionUID = 420054615939249553L;

    @NotNull
    @Column(name = "LOGIN", nullable = false, length = 100)
    protected String login;

    @NotNull
    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "KEY_ID")
    protected FileDescriptor key;

    @NotNull
    @Column(name = "PASSWORD", nullable = false, length = 100)
    protected String password;

    @Column(name = "DESCRIPTION")
    protected String description;

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setKey(FileDescriptor key) {
        this.key = key;
    }

    public FileDescriptor getKey() {
        return key;
    }


    public void setLogin(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

}