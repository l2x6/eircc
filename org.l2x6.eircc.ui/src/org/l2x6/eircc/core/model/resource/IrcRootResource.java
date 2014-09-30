/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model.resource;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcRootResource {
    private final Map<String, IrcAccountResource> accountResources;
    private final IDocumentProvider documentProvider;
    private final IProject project;

    /**
     * @param project
     * @throws CoreException
     */
    public IrcRootResource(IProject project, IDocumentProvider documentProvider) throws IrcResourceException {
        super();
        this.project = project;
        this.documentProvider = documentProvider;
        refresh();
        this.accountResources = collectAccountResources();
    }

    private Map<String, IrcAccountResource> collectAccountResources() throws IrcResourceException {
        Map<String, IrcAccountResource> result = new HashMap<String, IrcAccountResource>();
        try {
            for (IResource r : project.members()) {
                if (IrcAccountResource.isAccountChannelsFolder(r)) {
                    IrcAccountResource accountResource = new IrcAccountResource(this, (IFolder) r);
                    result.put(accountResource.getAccountName(), accountResource);
                }
            }
        } catch (CoreException e) {
            throw new IrcResourceException(e);
        }
        return result;
    }

    /**
     * @param accountChannelsFolder
     * @return
     * @throws IrcResourceException
     */
    public IrcAccountResource getAccountResource(IFolder accountChannelsFolder) throws IrcResourceException {
        String accountName = IrcAccountResource.getAccountName(accountChannelsFolder);
        IrcAccountResource result = getAccoutResource(accountName);
        if (result == null) {
            if (IrcAccountResource.isAccountChannelsFolder(accountChannelsFolder)) {
                result = new IrcAccountResource(this, accountChannelsFolder);
                accountResources.put(result.getAccountName(), result);
            }
        }
        if (result == null) {
            throw new IrcResourceException("Cannot create account resource for " + accountChannelsFolder.getFullPath()
                    + ".");
        }
        return result;
    }

    public IrcAccountResource getAccoutResource(String accountName) {
        return accountResources.get(accountName);
    }

    public IDocumentProvider getDocumentProvider() {
        return documentProvider;
    }

    /**
     * @param logPath
     * @return
     * @throws IrcResourceException
     */
    public IrcLogResource getLogResource(IFile logFile) throws IrcResourceException {
        IFolder logsFolder = (IFolder) logFile.getParent();
        IFolder accountChannelsFolder = (IFolder) logsFolder.getParent();
        IrcAccountResource accountResource = getAccountResource(accountChannelsFolder);
        IrcChannelResource channelResource = accountResource.getChannelResource(logsFolder);
        return channelResource.getLogResource(logFile);
    }

    public IProject getProject() {
        return project;
    }

    /**
     * @throws IrcResourceException
     *
     */
    public void refresh() throws IrcResourceException {
        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (CoreException e) {
            throw new IrcResourceException(e);
        }
    }

}
