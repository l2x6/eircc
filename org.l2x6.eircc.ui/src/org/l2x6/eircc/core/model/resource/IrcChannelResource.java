/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model.resource;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.l2x6.eircc.core.util.IrcUtils;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcChannelResource {

    /**  */
    public static final String FILE_EXTENSION = ".channel.properties";

    public static final String LOGS_FOLDER_SUFFIX = "-logs";

    public static IFolder getChannelLogsFolder(IFolder channelsFolder, String channelName) {
        return channelsFolder.getFolder(channelName + IrcChannelResource.LOGS_FOLDER_SUFFIX);
    }

    public static String getChannelName(IFolder logsFolder) {
        String name = logsFolder.getName();
        return name.substring(0, name.length() - IrcChannelResource.LOGS_FOLDER_SUFFIX.length());
    }

    public static String getChannelName(IPath logsFolderPath) {
        String name = logsFolderPath.lastSegment();
        return name.substring(0, name.length() - IrcChannelResource.LOGS_FOLDER_SUFFIX.length());
    }

    /**
     * @param channelPropsFile
     * @return
     */
    public static String getChannelNameFromChannelPropsFile(IFile channelPropsFile) {
        String fName = channelPropsFile.getName();
        return fName.substring(0, fName.length() - IrcChannelResource.FILE_EXTENSION.length());
    }

    /**
     * @param f
     * @return
     */
    public static boolean isChannelFile(IResource f) {
        return f.getType() == IResource.FILE && f.getName().endsWith(IrcChannelResource.FILE_EXTENSION);
    }

    /**
     * @param resource
     * @return
     */
    public static boolean isChannelLogsFolder(IResource resource) {
        return resource.getType() == IResource.FOLDER
                && resource.getName().endsWith(IrcChannelResource.LOGS_FOLDER_SUFFIX);
    }

    public static boolean isP2pChannel(IPath path) {
        return isP2pChannel(path.lastSegment());
    }

    public static boolean isP2pChannel(String channelName) {
        return !channelName.startsWith("#");
    }

    private final IrcAccountResource accountResource;

    private final String channelName;

    private IFile channelPropertyFile;

    private final SortedMap<OffsetDateTime, IrcLogResource> logResources;
    private final IFolder logsFolder;

    private final boolean p2p;

    /**
     * S
     *
     * @param accountResource
     * @param logsFolder
     * @throws CoreException
     */
    public IrcChannelResource(IrcAccountResource accountResource, IFolder logsFolder) throws IrcResourceException {
        super();
        this.accountResource = accountResource;
        this.logsFolder = logsFolder;
        this.channelName = IrcChannelResource.getChannelName(logsFolder);
        this.p2p = IrcChannelResource.isP2pChannel(channelName);
        this.logResources = collectLogResources();
    }

    /**
     * @return
     * @throws CoreException
     */
    private SortedMap<OffsetDateTime, IrcLogResource> collectLogResources() throws IrcResourceException {
        SortedMap<OffsetDateTime, IrcLogResource> result = new TreeMap<OffsetDateTime, IrcLogResource>();
        try {
            if (!logsFolder.exists()) {
                IProgressMonitor monitor = new NullProgressMonitor();
                IrcUtils.mkdirs(logsFolder, monitor);
            }
            for (IResource r : logsFolder.members()) {
                if (IrcLogResource.isLogFile(r)) {
                    OffsetDateTime time = IrcLogResource.getTime(r.getFullPath());
                    IrcLogResource logResource = new IrcLogResource(this, time, (IFile) r);
                    result.put(time, logResource);
                }
            }
        } catch (CoreException e) {
            throw new IrcResourceException(e);
        }
        return result;
    }

    public IrcAccountResource getAccountResource() {
        return accountResource;
    }

    /**
     * @return
     * @throws IrcResourceException
     */
    public IrcLogResource getActiveLogResource() throws IrcResourceException {
        refresh();
        if (logResources.isEmpty() || shouldRotate()) {
            return rotate();
        } else {
            return getLastLogResource();
        }
    }

    public String getChannelName() {
        return channelName;
    }

    /**
     * @return
     */
    public IFile getChannelPropertyFile() {
        if (channelPropertyFile == null) {
            channelPropertyFile = accountResource.getChannelsFolder().getFile(
                    channelName + IrcChannelResource.FILE_EXTENSION);
        }
        return channelPropertyFile;
    }

    /**
     * @return
     */
    public IrcLogResource getLastLogResource() {
        return logResources.isEmpty() ? null : logResources.get(logResources.lastKey());
    }

    /**
     * @param logFile
     * @return
     * @throws IrcResourceException
     */
    public IrcLogResource getLogResource(IFile logFile) throws IrcResourceException {
        OffsetDateTime time = IrcLogResource.getTime(logFile);
        IrcLogResource result = getOrCreateLogResource(time);
        if (result == null) {
            throw new IrcResourceException("Cannot create IRC log resource for " + logFile.getFullPath() + ".");
        }
        return result;
    }

    /**
     * @param time
     * @return
     */
    public IrcLogResource getLogResource(OffsetDateTime time) {
        return logResources.get(time);
    }

    public SortedMap<OffsetDateTime, IrcLogResource> getLogResources() {
        return logResources;
    }

    public IFolder getLogsFolder() {
        return logsFolder;
    }

    /**
     * @param truncatedTo
     * @return
     * @throws IrcResourceException
     */
    public IrcLogResource getOrCreateLogResource(OffsetDateTime time) throws IrcResourceException {
        refresh();
        IrcLogResource result = getLogResource(time);
        if (result == null) {
            result = new IrcLogResource(this, time);
            logResources.put(time, result);
        }
        return result;
    }

    /**
     * @return
     */
    public boolean isP2p() {
        return p2p;
    }

    /**
     * @throws CoreException
     *
     */
    public void refresh() throws IrcResourceException {
        try {
            //logsFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

            for (Iterator<IrcLogResource> it = logResources.values().iterator(); it.hasNext();) {
                IrcLogResource log = it.next();
                if (!log.exists()) {
                    it.remove();
                }
            }
            for (IResource r : logsFolder.members()) {
                if (IrcLogResource.isLogFile(r)) {
                    OffsetDateTime time = IrcLogResource.getTime(r.getFullPath());
                    if (!logResources.containsKey(time)) {
                        IrcLogResource logResource = new IrcLogResource(this, time, (IFile) r);
                        logResources.put(time, logResource);
                    }
                }
            }
        } catch (CoreException e) {
            throw new IrcResourceException(e);
        }
    }

    /**
     * @return
     * @throws IrcResourceException
     */
    private IrcLogResource rotate() throws IrcResourceException {
        OffsetDateTime time = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        IrcLogResource result = getOrCreateLogResource(time);
        return result;
    }

    /**
     * @return
     */
    private boolean shouldRotate() {
        IrcLogResource lastLogResource = getLastLogResource();
        if (lastLogResource == null) {
            return true;
        }
        long logDay = lastLogResource.getTime().truncatedTo(ChronoUnit.DAYS).toEpochSecond();
        long today = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS).toEpochSecond();
        return today != logDay;
    }

}
