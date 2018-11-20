/*
 * Copyright (c) 2008-2018 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.addon.saml.web.samlconnection;

import com.haulmont.addon.saml.service.SamlService;
import com.haulmont.addon.saml.web.security.saml.SamlCommunicationService;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.addon.saml.entity.SamlConnection;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author adiatullin
 */
public class SamlConnectionEdit extends AbstractEditor<SamlConnection> {
    private static final Logger log = LoggerFactory.getLogger(SamlConnectionEdit.class);

    @Inject
    protected SamlCommunicationService samlCommunicationService;
    @Inject
    protected SamlService samlService;
    @Inject
    protected DataManager dataManager;
    @Inject
    protected BackgroundWorker backgroundWorker;
    @Inject
    protected FileStorageService fileStorageService;

    @Inject
    protected Datasource<SamlConnection> samlConnectionDs;

    @Inject
    protected SourceCodeEditor idpMetadataView;
    @Inject
    protected SourceCodeEditor spMetadataView;
    @Inject
    protected ProgressBar loadingSpMetadataBar;
    @Inject
    protected ProgressBar loadingIdpMetadataBar;
    @Inject
    protected Button spMetadataRefreshBtn;
    @Inject
    protected Button idpMetadataRefreshBtn;
    @Inject
    protected TextField spIdField;
    @Inject
    protected FieldGroup fieldGroup;
    @Inject
    protected LookupField processingServiceField;
    @Inject
    protected TextField idpMetadataUrlField;
    @Inject
    protected UploadField idpMetadataUploadField;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initSpMetadata();
        initIdpMetadata();
        initProcessingServices();
    }

    protected void initSpMetadata() {
        spMetadataRefreshBtn.setAction(new AbstractAction("sp") {
            @Override
            public void actionPerform(Component component) {
                loadingSpMetadataBar.setVisible(true);
                spMetadataRefreshBtn.setEnabled(false);

                final SamlConnection item = getItem();
                BackgroundTask<Integer, MetadataResult> task = new BackgroundTask<Integer, MetadataResult>(getBackgroundTaskTimeout(), SamlConnectionEdit.this) {
                    @Override
                    public MetadataResult run(TaskLifeCycle<Integer> taskLifeCycle) {
                        String metadata = null;
                        String error = null;
                        try {
                            metadata = samlCommunicationService.getSpMetadata(item);
                        } catch (Exception e) {
                            log.error("Failed to load sp metadata", e);
                            error = e.getMessage();
                        }
                        return new MetadataResult(metadata, error);
                    }

                    @Override
                    public void done(MetadataResult result) {
                        loadingSpMetadataBar.setVisible(false);
                        spMetadataRefreshBtn.setEnabled(true);
                        setSpMetadata(result);
                    }
                };
                backgroundWorker.handle(task).execute();
            }
        });
    }

    protected void setSpMetadata(MetadataResult result) {
        String metadata = result == null ? null : result.getMetadata();
        String error = result == null ? null : result.getError();

        String value = metadata;
        if (StringUtils.isEmpty(value)) {
            value = String.format(getMessage("errors.metadataNotAvailable"), error == null ? StringUtils.EMPTY : error);
        }
        spMetadataView.setValue(value);
    }

    protected void initIdpMetadata() {
        idpMetadataRefreshBtn.setAction(new AbstractAction("idp") {
            @Override
            public void actionPerform(Component component) {
                loadingIdpMetadataBar.setVisible(true);
                idpMetadataUrlField.setEnabled(false);
                idpMetadataRefreshBtn.setEnabled(false);

                final SamlConnection item = getItem();
                BackgroundTask<Integer, MetadataResult> task = new BackgroundTask<Integer, MetadataResult>(getBackgroundTaskTimeout(), SamlConnectionEdit.this) {
                    @Override
                    public MetadataResult run(TaskLifeCycle<Integer> taskLifeCycle) {
                        String metadata = null;
                        String error = null;
                        try {
                            metadata = samlCommunicationService.getIdpMetadata(item);
                        } catch (Exception e) {
                            log.error("Failed to load idp metadata", e);
                            error = e.getMessage();
                        }
                        return new MetadataResult(metadata, error);
                    }

                    @Override
                    public void done(MetadataResult result) {
                        loadingIdpMetadataBar.setVisible(false);
                        idpMetadataUrlField.setEnabled(true);
                        idpMetadataRefreshBtn.setEnabled(true);
                        setIdpMetadata(result);
                    }
                };
                backgroundWorker.handle(task).execute();
            }
        });

        samlConnectionDs.addItemPropertyChangeListener(e -> {
            if ("idpMetadataUrl".equals(e.getProperty()) || "idpMetadata".equals(e.getProperty())) {
                setupIdpMetadataView();
            }
        });

        Set<String> extensions = getIdpFilePermittedExtensions();
        if (!CollectionUtils.isEmpty(extensions)) {
            idpMetadataUploadField.setPermittedExtensions(extensions);
        }
    }

    protected Set<String> getIdpFilePermittedExtensions() {
        Set<String> extensions = new HashSet<>();
        extensions.add(".xml");
        extensions.add(".txt");
        return extensions;
    }

    protected void setIdpMetadata(MetadataResult result) {
        String metadata = result == null ? null : result.getMetadata();
        String error = result == null ? null : result.getError();

        String value = metadata;
        if (StringUtils.isEmpty(value)) {
            value = String.format(getMessage("errors.metadataNotAvailable"), error == null ? StringUtils.EMPTY : error);
        }
        idpMetadataView.setValue(value);
    }

    protected int getBackgroundTaskTimeout() {
        return 10;
    }

    protected void initProcessingServices() {
        Map<String, String> processorsMap = samlService.getProcessingServices();
        processingServiceField.setOptionsMap(processorsMap);
    }

    @Override
    protected void postInit() {
        super.postInit();

        setupIdpMetadataView();
    }

    protected void setupIdpMetadataView() {
        String url = getItem().getIdpMetadataUrl();
        FileDescriptor fd = getItem().getIdpMetadata();

        String idpMetadataValue = null;
        if (!StringUtils.isEmpty(url)) {
            idpMetadataRefreshBtn.setEnabled(true);
            idpMetadataUrlField.setEditable(true);
            idpMetadataUploadField.setEnabled(false);

            getItem().setIdpMetadata(null);
        } else if (fd != null) {
            idpMetadataRefreshBtn.setEnabled(false);
            idpMetadataUrlField.setEditable(false);
            idpMetadataUploadField.setEnabled(true);

            getItem().setIdpMetadataUrl(null);

            try {
                idpMetadataValue = new String(fileStorageService.loadFile(fd), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("Failed to read file", e);

                showNotification(String.format(getMessage("errors.fileReadFailed"), e.getMessage()), NotificationType.ERROR);
            }
        } else {
            idpMetadataRefreshBtn.setEnabled(true);
            idpMetadataUrlField.setEditable(true);
            idpMetadataUploadField.setEnabled(true);
        }

        idpMetadataView.setValue(idpMetadataValue);
    }

    @Override
    protected boolean preCommit() {
        if (super.preCommit()) {
            SamlConnection connection = getItem();

            if (!isCorrectCode(connection)) {
                fieldGroup.getFieldNN("code").getComponentNN().requestFocus();
                showNotification(getMessage("errors.incorrectCode"), NotificationType.WARNING);
                return false;
            }
            if (!isIdpMetadataSpecified(connection)) {
                idpMetadataUploadField.requestFocus();
                showNotification(getMessage("errors.emptyIdpMetadata"), NotificationType.WARNING);
                return false;
            }
            if (!isUnique(connection)) {
                fieldGroup.getFieldNN("code").getComponentNN().requestFocus();
                showNotification(getMessage("errors.sameConnectionAlreadyExist"), NotificationType.WARNING);
                return false;
            }

            return true;
        }
        return false;
    }

    protected boolean isCorrectCode(SamlConnection connection) {
        if (StringUtils.isEmpty(connection.getCode())) {
            return false;
        }
        Pattern pattern = Pattern.compile("[\\?\\s\\&]");
        if (pattern.matcher(connection.getCode()).find()) {
            return false;
        }
        return true;
    }

    protected boolean isIdpMetadataSpecified(SamlConnection connection) {
        return !StringUtils.isEmpty(connection.getIdpMetadataUrl()) || connection.getIdpMetadata() != null;
    }

    protected boolean isUnique(SamlConnection connection) {
        LoadContext<SamlConnection> context = LoadContext.create(SamlConnection.class)
                .setQuery(LoadContext.createQuery("select e from samladdon$SamlConnection e where (e.spId = :spId or e.code = :code) and e.id <> :id")
                        .setParameter("spId", connection.getSpId())
                        .setParameter("code", connection.getCode())
                        .setParameter("id", connection.getId())
                        .setMaxResults(1))
                .setView(View.MINIMAL);
        return dataManager.getCount(context) == 0;
    }

    @Override
    protected boolean postCommit(boolean committed, boolean close) {
        if (committed) {
            samlCommunicationService.refresh(getItem());
        }
        return super.postCommit(committed, close);
    }

    protected static class MetadataResult {
        protected String metadata;
        protected String error;

        MetadataResult(String metadata, String error) {
            this.metadata = metadata;
            this.error = error;
        }

        public String getMetadata() {
            return metadata;
        }

        public String getError() {
            return error;
        }
    }
}