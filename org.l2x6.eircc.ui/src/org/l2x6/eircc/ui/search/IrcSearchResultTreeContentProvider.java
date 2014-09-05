/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.text.Match;

/**
 * Adapted from {@code org.eclipse.search.internal.ui.text.FileTreeContentProvider} as
 * available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSearchResultTreeContentProvider implements ITreeContentProvider, IrcSearchContentProvider {

    private Map<Object, Set<Object>> childrenMap;

    private final Object[] EMPTY_ARR = new Object[0];
    private IrcSearchResult searchResult;
    private IrcSearchResultPage searchResultPage;
    private AbstractTreeViewer treeViewer;

    IrcSearchResultTreeContentProvider(IrcSearchResultPage page, AbstractTreeViewer viewer) {
        searchResultPage = page;
        treeViewer = viewer;
    }

    public void clear() {
        initialize(searchResult);
        treeViewer.refresh();
    }

    public void dispose() {
    }

    /**
     * @see org.l2x6.eircc.ui.search.IrcSearchContentProvider#elementsChanged(java.lang.Object[])
     */
    public synchronized void elementsChanged(Object[] updatedElements) {
        for (int i = 0; i < updatedElements.length; i++) {
            if (!(updatedElements[i] instanceof IrcMessageMatches)) {
                // change events to elements are reported in file search
                if (searchResult.getMatchCount(updatedElements[i]) > 0)
                    insert(updatedElements[i], true);
                else
                    remove(updatedElements[i], true);
            } else {
                // change events to line elements are reported in text search
                IrcMessageMatches lineElement = (IrcMessageMatches) updatedElements[i];
                int nMatches = lineElement.getNumberOfMatches(searchResult);
                if (nMatches > 0) {
                    if (hasChild(lineElement.getParent(), lineElement)) {
                        treeViewer.update(new Object[] { lineElement, lineElement.getParent() }, null);
                    } else {
                        insert(lineElement, true);
                    }
                } else {
                    remove(lineElement, true);
                }
            }
        }
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object parentElement) {
        Set<Object> children = childrenMap.get(parentElement);
        if (children == null)
            return EMPTY_ARR;
        return children.toArray();
    }

    private int getElementLimit() {
        return searchResultPage.getElementLimit().intValue();
    }

    public Object[] getElements(Object inputElement) {
        Object[] children = getChildren(inputElement);
        int elementLimit = getElementLimit();
        if (elementLimit != -1 && elementLimit < children.length) {
            Object[] limitedChildren = new Object[elementLimit];
            System.arraycopy(children, 0, limitedChildren, 0, elementLimit);
            return limitedChildren;
        }
        return children;
    }

    public Object getParent(Object element) {
        if (element instanceof IProject)
            return null;
        if (element instanceof IResource) {
            IResource resource = (IResource) element;
            return resource.getParent();
        }
        if (element instanceof IrcMessageMatches) {
            return ((IrcMessageMatches) element).getParent();
        }

        if (element instanceof IrcMatch) {
            IrcMatch match = (IrcMatch) element;
            return match.getMessageMatches();
        }
        return null;
    }

    private boolean hasChild(Object parent, Object child) {
        Set<Object> children = childrenMap.get(parent);
        return children != null && children.contains(child);
    }

    public boolean hasChildren(Object element) {
        return getChildren(element).length > 0;
    }

    private boolean hasMatches(Object element) {
        if (element instanceof IrcMessageMatches) {
            IrcMessageMatches lineElement = (IrcMessageMatches) element;
            return lineElement.getNumberOfMatches(searchResult) > 0;
        }
        return searchResult.getMatchCount(element) > 0;
    }

    private synchronized void initialize(IrcSearchResult result) {
        searchResult = result;
        childrenMap = new HashMap<Object, Set<Object>>();
        boolean showLineMatches = !searchResult.getQuery().isFileLevelSearch();

        if (result != null) {
            Object[] elements = result.getElements();
            for (int i = 0; i < elements.length; i++) {
                if (showLineMatches) {
                    Match[] matches = result.getMatches(elements[i]);
                    for (int j = 0; j < matches.length; j++) {
                        insert(((IrcMatch) matches[j]).getMessageMatches(), false);
                    }
                } else {
                    insert(elements[i], false);
                }
            }
        }
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput instanceof IrcSearchResult) {
            initialize((IrcSearchResult) newInput);
        }
    }

    private void insert(Object child, boolean refreshViewer) {
        Object parent = getParent(child);
        while (parent != null) {
            if (insertChild(parent, child)) {
                if (refreshViewer)
                    treeViewer.add(parent, child);
            } else {
                if (refreshViewer)
                    treeViewer.refresh(parent);
                return;
            }
            child = parent;
            parent = getParent(child);
        }
        if (insertChild(searchResult, child)) {
            if (refreshViewer)
                treeViewer.add(searchResult, child);
        }
    }

    /**
     * Adds the child to the parent.
     *
     * @param parent
     *            the parent
     * @param child
     *            the child
     * @return <code>true</code> if this set did not already contain the
     *         specified element
     */
    private boolean insertChild(Object parent, Object child) {
        Set<Object> children = childrenMap.get(parent);
        if (children == null) {
            children = new HashSet<Object>();
            childrenMap.put(parent, children);
        }
        return children.add(child);
    }

    private void remove(Object element, boolean refreshViewer) {
        // precondition here: fResult.getMatchCount(child) <= 0

        if (hasChildren(element)) {
            if (refreshViewer)
                treeViewer.refresh(element);
        } else {
            if (!hasMatches(element)) {
                childrenMap.remove(element);
                Object parent = getParent(element);
                if (parent != null) {
                    removeFromSiblings(element, parent);
                    remove(parent, refreshViewer);
                } else {
                    removeFromSiblings(element, searchResult);
                    if (refreshViewer)
                        treeViewer.refresh();
                }
            } else {
                if (refreshViewer) {
                    treeViewer.refresh(element);
                }
            }
        }
    }

    private void removeFromSiblings(Object element, Object parent) {
        Set<Object> siblings = childrenMap.get(parent);
        if (siblings != null) {
            siblings.remove(element);
        }
    }
}
