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
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.OverviewRuler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
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
public class IrcLogEditor extends TextEditor {

    public static final String ID = "org.l2x6.eircc.ui.editor.IrcLogEditor";
    private static final DateTimeFormatter TITLE_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2).toFormatter();
    private String channelName;
    private boolean isP2pChannel;
    private OffsetDateTime lastMessageTime;

    protected IrcLogViewer logViewer;

    private IPath path;

    private String tooltip;

    @Override
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
        fAnnotationAccess= getAnnotationAccess();
        fOverviewRuler= createOverviewRuler(getSharedColors());

        this.logViewer = new IrcLogViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
        // ensure decoration support has been created and configured.
        getSourceViewerDecorationSupport(this.logViewer);

        return this.logViewer;
    }
    protected boolean isOverviewRulerVisible() {
        return true;
    }

    protected boolean isLineNumberRulerVisible() {
        return false;
    }

    /**
     * Returns the overview ruler.
     *
     * @return the overview ruler
     */
    protected IOverviewRuler getOverviewRuler() {
        if (fOverviewRuler == null)
            fOverviewRuler= createOverviewRuler(getSharedColors());
        return fOverviewRuler;
    }


    protected IOverviewRuler createOverviewRuler(ISharedTextColors sharedColors) {
        IOverviewRuler ruler= new OverviewRuler(getAnnotationAccess(), VERTICAL_RULER_WIDTH, sharedColors);

        Iterator e= fAnnotationPreferences.getAnnotationPreferences().iterator();
        while (e.hasNext()) {
            AnnotationPreference preference= (AnnotationPreference) e.next();
            if (preference.contributesToHeader())
                ruler.addHeaderAnnotationType(preference.getAnnotationType());
        }
        return ruler;
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
            setSite(site);
            setInput(input);
            IPathEditorInput pathInput = (IPathEditorInput) input;
            path = pathInput.getPath();
            isP2pChannel = AbstractIrcChannel.isP2pChannel(path);
            updateTitle();
        } else {
            throw new PartInitException("IPathEditorInput expected.");
        }
    }

    @Override
    protected void initializeEditor() {
        super.initializeEditor();
        setSourceViewerConfiguration(new IrcLogEditorConfiguration(getPreferenceStore()));
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
        if (path != null) {
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
