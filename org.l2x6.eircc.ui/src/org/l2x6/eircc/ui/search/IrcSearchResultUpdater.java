/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.search.ui.IQueryListener;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.l2x6.eircc.ui.EirccUi;

/**
 * Adapted from {@code org.eclipse.search.internal.ui.text.SearchResultUpdater} as
 * available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSearchResultUpdater implements IResourceChangeListener, IQueryListener {
    private AbstractTextSearchResult fResult;

    public IrcSearchResultUpdater(AbstractTextSearchResult result) {
        fResult = result;
        NewSearchUI.addQueryListener(this);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    private void handleDelta(IResourceDelta d) {
        try {
            d.accept(new IResourceDeltaVisitor() {
                public boolean visit(IResourceDelta delta) throws CoreException {
                    switch (delta.getKind()) {
                    case IResourceDelta.ADDED:
                        return false;
                    case IResourceDelta.REMOVED:
                        IResource res = delta.getResource();
                        if (res instanceof IFile) {
                            Match[] matches = fResult.getMatches(res);
                            fResult.removeMatches(matches);
                        }
                        break;
                    case IResourceDelta.CHANGED:
                        // handle changed resource
                        break;
                    }
                    return true;
                }
            });
        } catch (CoreException e) {
            EirccUi.log(e);
        }
    }

    public void queryAdded(ISearchQuery query) {
        // don't care
    }

    public void queryFinished(ISearchQuery query) {
        // don't care
    }

    public void queryRemoved(ISearchQuery query) {
        if (fResult.equals(query.getSearchResult())) {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
            NewSearchUI.removeQueryListener(this);
        }
    }

    public void queryStarting(ISearchQuery query) {
        // don't care
    }

    public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta delta = event.getDelta();
        if (delta != null)
            handleDelta(delta);
    }
}
