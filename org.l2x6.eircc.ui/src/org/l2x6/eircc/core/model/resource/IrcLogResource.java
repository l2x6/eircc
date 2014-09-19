/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model.resource;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.l2x6.eircc.core.util.IrcUtils;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcLogResource {

    public static final String FILE_EXTENSION = ".irc.log";

    /**
     * @param channelResource
     * @param time
     * @return
     */
    private static IFile getLogFile(IrcChannelResource channelResource, OffsetDateTime time) {
        String name = time.toString() + IrcLogResource.FILE_EXTENSION;
        IPath path = channelResource.getLogsFolder().getFullPath().append(name);
        IFile file = channelResource.getAccountResource().getRootResource().getProject().getWorkspace().getRoot().getFile(path);
        return file;
    }

    public static OffsetDateTime getTime(IFile logFile) {
        String name = logFile.getName();
        String str = name.substring(0, name.length() - FILE_EXTENSION.length());
        return OffsetDateTime.parse(str);
    }

    public static OffsetDateTime getTime(IPath path) {
        String name = path.lastSegment();
        String str = name.substring(0, name.length() - FILE_EXTENSION.length());
        return OffsetDateTime.parse(str);
    }

    /**
     * @param f
     * @return
     */
    public static boolean isLogFile(IResource f) {
        return f.getType() == IResource.FILE && f.getName().endsWith(FILE_EXTENSION);
    }
    private ITextFileBuffer buffer;
    private final IrcChannelResource channelResource;

    private final IFile logFile;

    private final OffsetDateTime time;

    /**
     * @param ircChannelResource
     * @param time2
     * @throws CoreException
     */
    public IrcLogResource(IrcChannelResource channelResource, OffsetDateTime time) throws IrcResourceException {
        this(channelResource, time, getLogFile(channelResource, time));
    }

    /**
     * @param channelResource
     * @param time
     * @param logFile
     * @throws IrcResourceException
     */
    public IrcLogResource(IrcChannelResource channelResource, OffsetDateTime time, IFile logFile) throws IrcResourceException {
        this.channelResource = channelResource;
        this.time = time;
        this.logFile = logFile;
        if (!logFile.exists()) {
            IProgressMonitor monitor = new NullProgressMonitor();
            try {
                IrcUtils.mkdirs(logFile.getParent(), monitor);
                logFile.create(new ByteArrayInputStream(new byte[0]), true, monitor);
                logFile.refreshLocal(IResource.DEPTH_ONE, new SubProgressMonitor(monitor, 1));
            } catch (CoreException e) {
                throw new IrcResourceException(e);
            }
        }
    }

    /**
     * @param monitor
     * @throws IrcResourceException
     *
     */
    public void commitBuffer(IProgressMonitor monitor) throws CoreException {
        if (buffer == null) {
            throw new IllegalStateException("Cannot commit a null buffer.");
        }
        buffer.commit(new SubProgressMonitor(monitor, 1), false);
        logFile.refreshLocal(IResource.DEPTH_ONE, new SubProgressMonitor(monitor, 1));
    }

    private void ensureBufferConnected(IProgressMonitor monitor) throws CoreException {
        if (buffer == null) {
            IFile file = logFile;
            ITextFileBufferManager fbm = FileBuffers.getTextFileBufferManager();
            IPath path = file.getFullPath();
            buffer = fbm.getTextFileBuffer(path, LocationKind.NORMALIZE);
            if (buffer == null) {
                fbm.connect(path, LocationKind.IFILE, monitor);
                buffer = fbm.getTextFileBuffer(path, LocationKind.NORMALIZE);
            }
        }
    }

    /**
     * @return
     */
    public boolean exists() {
        return logFile.exists();
    }

    public IrcChannelResource getChannelResource() {
        return channelResource;
    }

    /**
     * @param monitor
     * @return
     * @throws IrcResourceException
     */
    public IDocument getDocument(IProgressMonitor monitor) throws CoreException {
        ensureBufferConnected(monitor);
        IDocument document = buffer.getDocument();
        return document;
    }

    /**
     * @return
     */
    public IFile getLogFile() {
        return logFile;
    }

    public OffsetDateTime getTime() {
        return time;
    }

    public boolean isLast() {
        try {
            channelResource.refresh();
        } catch (IrcResourceException e) {
            throw new RuntimeException(e);
        }
        return channelResource.getLastLogResource() == this;
    }

}
