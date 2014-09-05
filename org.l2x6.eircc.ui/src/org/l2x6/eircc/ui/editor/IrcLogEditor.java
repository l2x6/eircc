/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.PlainIrcMessage;
import org.l2x6.eircc.core.util.IrcLogReader;
import org.l2x6.eircc.ui.EirccUi;

/**
 * In fact a read-only viewer, but technically an Eclipse Editor.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcLogEditor extends EditorPart {

    public static final String ID = "org.l2x6.eircc.ui.editor.IrcLogEditor";
    private static final DateTimeFormatter TITLE_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2).toFormatter();
    private String channelName;
    private boolean isP2pChannel;
    private OffsetDateTime lastMessageTime;

    private IrcLogViewer logViewer;

    private IPath path;

    private String tooltip;

    /**
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        logViewer = new IrcLogViewer(parent);
        try {
            reload();
        } catch (IOException | CoreException e) {
            EirccUi.log(e);
        }
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {
    }

    @Override
    public String getTitleToolTip() {
        return tooltip == null ? super.getTitleToolTip() : tooltip;
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (input instanceof IPathEditorInput) {
            setInput(input);
            setSite(site);
            IPathEditorInput pathInput = (IPathEditorInput) input;
            path = pathInput.getPath();
            isP2pChannel = AbstractIrcChannel.isP2pChannel(path);
            updateTitle();
        } else {
            throw new PartInitException("IPathEditorInput expected.");
        }
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    private void reload() throws IOException, CoreException {
        logViewer.clear();
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        IFile file = ws.getRoot().getFileForLocation(path);
        try (IrcLogReader reader = new IrcLogReader(file.getContents(), isP2pChannel)) {
            while (reader.hasNext()) {
                PlainIrcMessage m = reader.next();
                logViewer.appendMessage(m);
                lastMessageTime = m.getArrivedAt();
            }
        }
        updateTitle();
    }

    /**
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        logViewer.setFocus();
    }

    /**
    *
    */
    private void updateTitle() {
        OffsetDateTime d = IrcLog.getDate(path);
        String t = d.format(DateTimeFormatter.ISO_LOCAL_DATE) + ' ' + d.format(TITLE_DATE_FORMATTER) + ' '
                + channelName;
        setPartName(t);

        if (lastMessageTime == null) {
            tooltip = t;
        } else {
            String start = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String end = lastMessageTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
            if (start.equals(end)) {
                tooltip = start + ' ' + d.format(TITLE_DATE_FORMATTER) + " - "
                        + lastMessageTime.format(TITLE_DATE_FORMATTER) + ' ' + channelName;
            } else {
                tooltip = start + ' ' + d.format(TITLE_DATE_FORMATTER) + " - " + end + ' '
                        + lastMessageTime.format(TITLE_DATE_FORMATTER) + ' ' + channelName;
            }
        }
    }

}
