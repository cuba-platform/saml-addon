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

import com.haulmont.addon.saml.entity.SamlConnection;
import com.haulmont.addon.saml.web.security.saml.SamlCommunicationService;
import com.haulmont.cuba.gui.app.core.file.FileDownloadHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.EditAction;
import com.haulmont.cuba.gui.components.actions.ItemTrackingAction;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.security.entity.EntityOp;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author adiatullin
 */
public class SamlConnectionBrowse extends AbstractLookup {

    @Inject
    protected SamlCommunicationService samlCommunicationService;

    @Inject
    protected CollectionDatasource<SamlConnection, UUID> samlConnectionsDs;
    @Inject
    protected Table<SamlConnection> samlConnectionsTable;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initActions();
        initTableColumns();
    }

    protected void initActions() {
        ItemTrackingAction activationAction = new ItemTrackingAction(samlConnectionsTable, "activation") {
            @Override
            public void actionPerform(Component component) {
                Set<SamlConnection> connections = samlConnectionsTable.getSelected();
                if (!CollectionUtils.isEmpty(connections)) {
                    try {
                        for (SamlConnection connection : connections) {
                            samlCommunicationService.setActive(!Boolean.TRUE.equals(connection.getActive()), connection);
                        }
                        showNotification(getMessage("captions.actionPerformed"), NotificationType.HUMANIZED);
                    } finally {
                        samlConnectionsDs.refresh();
                    }
                }
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set<SamlConnection> connections = samlConnectionsTable.getSelected();
                    if (!CollectionUtils.isEmpty(connections)) {
                        return security.isEntityOpPermitted(samlConnectionsDs.getMetaClass(), EntityOp.UPDATE);
                    }
                }
                return false;
            }
        };
        activationAction.setIcon(CubaIcon.OK.source());
        activationAction.setCaption(getMessage("actions.activate"));
        samlConnectionsTable.addAction(activationAction);

        final EditAction editAction = (EditAction) samlConnectionsTable.getActionNN(EditAction.ACTION_ID);
        final RemoveAction removeAction = (RemoveAction) samlConnectionsTable.getActionNN(RemoveAction.ACTION_ID);

        samlConnectionsDs.addItemChangeListener(e -> {
            SamlConnection connection = samlConnectionsTable.getSingleSelected();
            Set<SamlConnection> connections = samlConnectionsTable.getSelected();

            editAction.setEnabled(connections.size() == 1);
            removeAction.setEnabled(checkAllIsNotActive(connections));
            activationAction.setEnabled(checkAllHaveSameStatus(connections));

            if (connection == null) {
                activationAction.setCaption(getMessage("actions.activate"));
                activationAction.setIcon(CubaIcon.OK.source());
            } else {
                boolean active = Boolean.TRUE.equals(connection.getActive());
                activationAction.setCaption(active ? getMessage("actions.deactivate") : getMessage("actions.activate"));
                activationAction.setIcon(active ? CubaIcon.CANCEL.source() : CubaIcon.OK.source());
            }
        });

        editAction.setAfterCommitHandler(entity -> samlConnectionsDs.refresh());
    }

    protected boolean checkAllIsNotActive(Set<SamlConnection> connections) {
        if (!CollectionUtils.isEmpty(connections)) {
            for (SamlConnection connection : connections) {
                if (Boolean.TRUE.equals(connection.getActive())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    protected boolean checkAllHaveSameStatus(Set<SamlConnection> connections) {
        if (!CollectionUtils.isEmpty(connections)) {
            boolean value = Boolean.TRUE.equals(((SamlConnection) CollectionUtils.get(connections, 0)).getActive());
            for (SamlConnection connection : connections) {
                boolean current = Boolean.TRUE.equals(connection.getActive());
                if (current != value) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    protected void initTableColumns() {
        FileDownloadHelper.initGeneratedColumn(samlConnectionsTable, "idpMetadata");
    }
}