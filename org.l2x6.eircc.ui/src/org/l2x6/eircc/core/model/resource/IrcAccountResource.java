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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcAccountResource {
    public static final String CHANNELS_FOLDER_SUFFIX = "-channels";
    public static final String FILE_EXTENSION = ".account.properties";
    /**  */
    public static final String USERS_FOLDER_SUFFIX = "-users";

    /**
     * @param f
     * @return
     */
    public static String getAccountName(IFile f) {
        String fileName = f.getName();
        String result = fileName.substring(0, fileName.length() - IrcAccountResource.FILE_EXTENSION.length());
        return result;
    }

    /**
     * @param accountChannelsFolder
     * @return
     */
    public static String getAccountName(IResource accountChannelsFolder) {
        String name = accountChannelsFolder.getName();
        return name.substring(0, name.length() - IrcAccountResource.CHANNELS_FOLDER_SUFFIX.length());
    }

    public static String getAccountNameFromChannelsFolder(IPath path) {
        String name = path.lastSegment();
        return name.substring(0, name.length() - CHANNELS_FOLDER_SUFFIX.length());
    }

    /**
     * @param resource
     * @return
     */
    public static boolean isAccountChannelsFolder(IResource resource) {
        return resource.getType() == IResource.FOLDER
                && resource.getName().endsWith(IrcAccountResource.CHANNELS_FOLDER_SUFFIX);
    }

    /**
     * @param f
     * @return
     */
    public static boolean isAccountFile(IResource r) {
        return r.getType() == IResource.FILE && r.getName().endsWith(IrcAccountResource.FILE_EXTENSION);
    }

    public static boolean isChannelsFolder(IResource resource) {
        return resource.getType() == IResource.FOLDER && resource.getName().endsWith(CHANNELS_FOLDER_SUFFIX);
    }

    private final String accountName;
    private final IFile accountPropertyFile;

    private final Map<String, IrcChannelResource> channelResources;

    private final IFolder channelsFolder;

    private final IrcRootResource rootResource;
    private final IFolder usersFolder;

    /**
     * @param rootResource
     * @param accountName
     * @param channelsFolder
     * @throws CoreException
     */
    public IrcAccountResource(IrcRootResource rootResource, IFolder channelsFolder) throws IrcResourceException {
        super();
        this.rootResource = rootResource;
        this.accountName = getAccountName(channelsFolder);
        ;
        this.channelsFolder = channelsFolder;
        IContainer parent = channelsFolder.getParent();
        this.usersFolder = parent.getFolder(new Path(accountName + IrcAccountResource.USERS_FOLDER_SUFFIX));
        this.accountPropertyFile = parent.getFile(new Path(accountName + FILE_EXTENSION));
        this.channelResources = collectChannelResources();
    }

    /**
     * @return
     * @throws CoreException
     */
    private Map<String, IrcChannelResource> collectChannelResources() throws IrcResourceException {
        Map<String, IrcChannelResource> result = new HashMap<String, IrcChannelResource>();
        try {
            for (IResource r : channelsFolder.members()) {
                if (IrcChannelResource.isChannelLogsFolder(r)) {
                    IrcChannelResource channelResource = new IrcChannelResource(this, (IFolder) r);
                    result.put(channelResource.getChannelName(), channelResource);
                }
            }
        } catch (CoreException e) {
            throw new IrcResourceException(e);
        }
        return result;
    }

    public String getAccountName() {
        return accountName;
    }

    /**
     * @return
     */
    public IFile getAccountPropertyFile() {
        return accountPropertyFile;
    }

    /**
     * @param logsFolder
     * @throws IrcResourceException
     */
    public IrcChannelResource getChannelResource(IFolder logsFolder) throws IrcResourceException {
        String channelName = IrcChannelResource.getChannelName(logsFolder);
        IrcChannelResource result = getChannelResource(channelName);
        if (result == null) {
            if (IrcChannelResource.isChannelLogsFolder(logsFolder)) {
                result = new IrcChannelResource(this, logsFolder);
                channelResources.put(result.getChannelName(), result);
            }
        }
        if (result == null) {
            throw new IrcResourceException("Cannot create channel resource for " + logsFolder.getFullPath() + ".");
        }
        return result;
    }

    /**
     * @param channelName
     * @return
     */
    public IrcChannelResource getChannelResource(String channelName) {
        return channelResources.get(channelName);
    }

    public IFolder getChannelsFolder() {
        return channelsFolder;
    }

    /**
     * @param name
     * @return
     * @throws IrcResourceException
     */
    public IrcChannelResource getOrCreateChannelResource(String channelName) throws IrcResourceException {
        IrcChannelResource result = channelResources.get(channelName);
        if (result == null) {
            IFolder logsFolder = IrcChannelResource.getChannelLogsFolder(channelsFolder, channelName);
            result = new IrcChannelResource(this, logsFolder);
            channelResources.put(result.getChannelName(), result);
        }
        return result;
    }

    public IrcRootResource getRootResource() {
        return rootResource;
    }

    public IFolder getUsersFolder() {
        return usersFolder;
    }

}
