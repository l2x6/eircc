/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

/**
 * Adapted from {@code org.eclipse.search.internal.ui.text.FileTableContentProvider} as
 * available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSearchResultTableContentProvider implements IStructuredContentProvider, IrcSearchContentProvider {

    private final Object[] EMPTY_ARR = new Object[0];

    private final IrcSearchResultPage searchResultPage;
    private IrcSearchResult searchResult;

    public IrcSearchResultTableContentProvider(IrcSearchResultPage page) {
        this.searchResultPage = page;
    }

    public void clear() {
        getViewer().refresh();
    }

    public void dispose() {
        // nothing to do
    }

    public void elementsChanged(Object[] updatedElements) {
        TableViewer viewer = getViewer();
        int elementLimit = getElementLimit();
        boolean tableLimited = elementLimit != -1;
        for (int i = 0; i < updatedElements.length; i++) {
            if (searchResult.getMatchCount(updatedElements[i]) > 0) {
                if (viewer.testFindItem(updatedElements[i]) != null)
                    viewer.update(updatedElements[i], null);
                else {
                    if (!tableLimited || viewer.getTable().getItemCount() < elementLimit)
                        viewer.add(updatedElements[i]);
                }
            } else
                viewer.remove(updatedElements[i]);
        }
    }

    private int getElementLimit() {
        return searchResultPage.getElementLimit().intValue();
    }

    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof IrcSearchResult) {
            int elementLimit = getElementLimit();
            Object[] elements = ((IrcSearchResult) inputElement).getElements();
            if (elementLimit != -1 && elements.length > elementLimit) {
                Object[] shownElements = new Object[elementLimit];
                System.arraycopy(elements, 0, shownElements, 0, elementLimit);
                return shownElements;
            }
            return elements;
        }
        return EMPTY_ARR;
    }

    private TableViewer getViewer() {
        return (TableViewer) searchResultPage.getViewer();
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput instanceof IrcSearchResult) {
            searchResult = (IrcSearchResult) newInput;
        }
    }
}
