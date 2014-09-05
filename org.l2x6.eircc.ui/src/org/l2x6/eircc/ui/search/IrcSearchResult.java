/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.ui.editor.IrcChannelEditorInput;
import org.l2x6.eircc.ui.misc.IrcImages;
import org.l2x6.eircc.ui.misc.IrcImages.ImageKey;

/**
 * Adapted from {@code org.eclipse.search.internal.ui.text.FileSearchResult} as
 * available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter, IFileMatchAdapter {
    private final Match[] EMPTY_ARR = new Match[0];

    private IrcSearchQuery query;

    public IrcSearchResult(IrcSearchQuery query) {
        this.query = query;
    }

    public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
        IEditorInput ei = editor.getEditorInput();
        if (ei instanceof IFileEditorInput) {
            IFileEditorInput fi = (IFileEditorInput) ei;
            return getMatches(fi.getFile());
        } else if (ei instanceof IrcChannelEditorInput) {
            IrcChannelEditorInput chei = (IrcChannelEditorInput) ei;
            AbstractIrcChannel ch = chei.getChannel();
            IrcLog log = ch.getLog();
            if (log != null) {
                IFile f = ResourcesPlugin.getWorkspace().getRoot().getFile(log.getPath());
                return getMatches(f);
            }
        }
        return EMPTY_ARR;
    }

    public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
        return getMatches(file);
    }

    public IEditorMatchAdapter getEditorMatchAdapter() {
        return this;
    }

    public IFile getFile(Object element) {
        if (element instanceof IFile)
            return (IFile) element;
        return null;
    }

    public IFileMatchAdapter getFileMatchAdapter() {
        return this;
    }

    public ImageDescriptor getImageDescriptor() {
        return IrcImages.getInstance().getImageDescriptor(ImageKey.SEARCH);
    }

    public String getLabel() {
        return query.getResultLabel(getMatchCount());
    }

    public IrcSearchQuery getQuery() {
        return query;
    }

    public String getTooltip() {
        return getLabel();
    }

    public boolean isShownInEditor(Match match, IEditorPart editor) {
        IEditorInput ei = editor.getEditorInput();
        if (ei instanceof IFileEditorInput) {
            IFileEditorInput fi = (IFileEditorInput) ei;
            return match.getElement().equals(fi.getFile());
        } else if (ei instanceof IrcChannelEditorInput && match.getElement() instanceof IFile) {
            IrcChannelEditorInput chei = (IrcChannelEditorInput) ei;
            IFile f = (IFile) match.getElement();
            AbstractIrcChannel ch = chei.getChannel();
            IrcLog log = ch.getLog();
            return log != null && log.getPath().equals(f.getFullPath());
        }
        return false;
    }
}
