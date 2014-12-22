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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.l2x6.eircc.core.util.IrcUtils;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcLogResource implements ISynchronizable {

    public static final String FILE_EXTENSION = ".irc.log";

    /**
     * @param channelResource
     * @param time
     * @return
     */
    private static IFile getLogFile(IrcChannelResource channelResource, OffsetDateTime time) {
        String name = time.toString() + IrcLogResource.FILE_EXTENSION;
        IPath path = channelResource.getLogsFolder().getFullPath().append(name);
        IFile file = channelResource.getAccountResource().getRootResource().getProject().getWorkspace().getRoot()
                .getFile(path);
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

    private final IrcChannelResource channelResource;

    private IDocument document;

    private final FileEditorInput editorInput;

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
    public IrcLogResource(IrcChannelResource channelResource, OffsetDateTime time, IFile logFile)
            throws IrcResourceException {
        this.channelResource = channelResource;
        this.time = time;
        this.logFile = logFile;
        this.editorInput = new FileEditorInput(logFile);
        try {
            if (!logFile.exists()) {
                IProgressMonitor monitor = new NullProgressMonitor();
                IrcUtils.mkdirs(logFile.getParent(), monitor);
                logFile.create(new ByteArrayInputStream(new byte[0]), true, monitor);
                //logFile.refreshLocal(IResource.DEPTH_ONE, new SubProgressMonitor(monitor, 1));
            }
            IDocumentProvider documentProvider = channelResource.getAccountResource().getRootResource()
                    .getDocumentProvider();
            documentProvider.connect(editorInput);
        } catch (CoreException e) {
            throw new IrcResourceException(e);
        }
    }

    public void discard() {
        document = null;
        IDocumentProvider documentProvider = channelResource.getAccountResource().getRootResource()
                .getDocumentProvider();
        documentProvider.disconnect(editorInput);
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

    public IDocument getDocument() {
        if (document == null) {
            IDocumentProvider documentProvider = channelResource.getAccountResource().getRootResource()
                    .getDocumentProvider();
            this.document = documentProvider.getDocument(editorInput);
        }
        return document;
    }

    public FileEditorInput getEditorInput() {
        return editorInput;
    }

    /**
     * @see org.eclipse.jface.text.ISynchronizable#getLockObject()
     */
    @Override
    public Object getLockObject() {
        IDocument doc = getDocument();
        if (doc instanceof ISynchronizable) {
            return ((ISynchronizable) doc).getLockObject();
        }
        return this;
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

    /**
     * @see org.eclipse.jface.text.ISynchronizable#setLockObject(java.lang.Object)
     */
    @Override
    public void setLockObject(Object lockObject) {
        throw new UnsupportedOperationException();
    }

}
