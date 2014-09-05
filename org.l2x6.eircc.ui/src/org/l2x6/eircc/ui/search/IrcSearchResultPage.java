/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.editor.IrcDefaultMessageFormatter.TimeStyle;
import org.l2x6.eircc.ui.search.IrcSearchLabelProvider.LabelOrder;

/**
 * Adapted from {@code org.eclipse.search.internal.ui.text.FileSearchPage} as
 * available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSearchResultPage extends AbstractTextSearchViewPage implements IAdaptable {

    public static class DecoratorIgnoringViewerSorter extends ViewerComparator {
        private final ILabelProvider fLabelProvider;

        public DecoratorIgnoringViewerSorter(ILabelProvider labelProvider) {
            fLabelProvider = labelProvider;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.jface.viewers.ViewerComparator#category(java.lang.Object)
         */
        public int category(Object element) {
            if (element instanceof IContainer) {
                return 1;
            }
            return 2;
        }

        @SuppressWarnings("unchecked")
        public int compare(Viewer viewer, Object e1, Object e2) {
            int cat1 = category(e1);
            int cat2 = category(e2);

            if (cat1 != cat2) {
                return cat1 - cat2;
            }

            if (e1 instanceof IrcMessageMatches && e2 instanceof IrcMessageMatches) {
                IrcMessageMatches m1 = (IrcMessageMatches) e1;
                IrcMessageMatches m2 = (IrcMessageMatches) e2;
                return m1.getOffset() - m2.getOffset();
            }

            String name1 = fLabelProvider.getText(e1);
            String name2 = fLabelProvider.getText(e2);
            if (name1 == null)
                name1 = "";//$NON-NLS-1$
            if (name2 == null)
                name2 = "";//$NON-NLS-1$
            return getComparator().compare(name1, name2);
        }
    }

    private static final int DEFAULT_ELEMENT_LIMIT = 1000;
    private static final String KEY_LIMIT = "org.eclipse.search.resultpage.limit"; //$NON-NLS-1$

    private static final String KEY_SORTING = "org.eclipse.search.resultpage.sorting"; //$NON-NLS-1$

    private static final IShowInTargetList SHOW_IN_TARGET_LIST = new IShowInTargetList() {
        public String[] getShowInTargetIds() {
            return SHOW_IN_TARGETS;
        }
    };
    @SuppressWarnings("deprecation")
    private static final String[] SHOW_IN_TARGETS = new String[] { IPageLayout.ID_RES_NAV };
    private ActionGroup searchActionGroup;
    private IrcSearchContentProvider contentProvider;
    private LabelOrder labelOrder;

    private final SortIrcSearchResultsAction sortByNameAction;
    private final SortIrcSearchResultsAction sortByPathAction;

    public IrcSearchResultPage() {
        sortByNameAction = new SortIrcSearchResultsAction(IrcUiMessages.FileSearchPage_sort_name_label, this,
                LabelOrder.SHOW_LABEL_PATH);
        sortByPathAction = new SortIrcSearchResultsAction(IrcUiMessages.FileSearchPage_sort_path_label, this,
                LabelOrder.SHOW_PATH_LABEL);

        setElementLimit(new Integer(DEFAULT_ELEMENT_LIMIT));
    }

    private void addSortActions(IMenuManager mgr) {
        if (getLayout() != FLAG_LAYOUT_FLAT)
            return;
        MenuManager sortMenu = new MenuManager(IrcUiMessages.FileSearchPage_sort_by_label);
        sortMenu.add(sortByNameAction);
        sortMenu.add(sortByPathAction);

        sortByNameAction.setChecked(labelOrder == sortByNameAction.getSortOrder());
        sortByPathAction.setChecked(labelOrder == sortByPathAction.getSortOrder());

        mgr.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, sortMenu);
    }

    protected void clear() {
        if (contentProvider != null)
            contentProvider.clear();
    }

    protected void configureTableViewer(TableViewer viewer) {
        viewer.setUseHashlookup(true);
        IrcSearchLabelProvider innerLabelProvider = new IrcSearchLabelProvider(this, labelOrder, TimeStyle.DATE_TIME);
        viewer.setLabelProvider(new IrcSearchDecoratingLabelProvider(innerLabelProvider));
        viewer.setContentProvider(new IrcSearchResultTableContentProvider(this));
        viewer.setComparator(new DecoratorIgnoringViewerSorter(innerLabelProvider));
        contentProvider = (IrcSearchContentProvider) viewer.getContentProvider();
    }

    protected void configureTreeViewer(TreeViewer viewer) {
        viewer.setUseHashlookup(true);
        IrcSearchLabelProvider innerLabelProvider = new IrcSearchLabelProvider(this, LabelOrder.SHOW_LABEL,
                TimeStyle.TIME);
        viewer.setLabelProvider(new IrcSearchDecoratingLabelProvider(innerLabelProvider));
        viewer.setContentProvider(new IrcSearchResultTreeContentProvider(this, viewer));
        viewer.setComparator(new DecoratorIgnoringViewerSorter(innerLabelProvider));
        contentProvider = (IrcSearchContentProvider) viewer.getContentProvider();
    }

    public void dispose() {
        searchActionGroup.dispose();
        super.dispose();
    }

    protected void elementsChanged(Object[] objects) {
        if (contentProvider != null)
            contentProvider.elementsChanged(objects);
    }

    @SuppressWarnings("unchecked")
    protected void evaluateChangedElements(Match[] matches, @SuppressWarnings("rawtypes") Set changedElements) {
        if (showLineMatches()) {
            for (int i = 0; i < matches.length; i++) {
                changedElements.add(((IrcMatch) matches[i]).getMessageMatches());
            }
        } else {
            super.evaluateChangedElements(matches, changedElements);
        }
    }

    protected void fillContextMenu(IMenuManager mgr) {
        super.fillContextMenu(mgr);
        addSortActions(mgr);
        searchActionGroup.setContext(new ActionContext(getSite().getSelectionProvider().getSelection()));
        searchActionGroup.fillContextMenu(mgr);
    }

    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        if (IShowInTargetList.class.equals(adapter)) {
            return SHOW_IN_TARGET_LIST;
        }

        if (adapter == IShowInSource.class) {
            ISelectionProvider selectionProvider = getSite().getSelectionProvider();
            if (selectionProvider == null)
                return null;

            ISelection selection = selectionProvider.getSelection();
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection structuredSelection = ((StructuredSelection) selection);
                final Set<IResource> newSelection = new HashSet<IResource>(structuredSelection.size());
                Iterator<?> iter = structuredSelection.iterator();
                while (iter.hasNext()) {
                    Object element = iter.next();
                    if (element instanceof IrcMessageMatches) {
                        newSelection.add(((IrcMessageMatches) element).getParent());
                    } else if (element instanceof IResource) {
                        newSelection.add((IResource) element);
                    }
                }

                return new IShowInSource() {
                    public ShowInContext getShowInContext() {
                        return new ShowInContext(null, new StructuredSelection(new ArrayList<IResource>(newSelection)));
                    }
                };
            }
            return null;
        }

        return null;
    }

    public int getDisplayedMatchCount(Object element) {
        if (showLineMatches()) {
            if (element instanceof IrcMessageMatches) {
                IrcMessageMatches lineEntry = (IrcMessageMatches) element;
                return lineEntry.getNumberOfMatches(getInput());
            }
            return 0;
        }
        return super.getDisplayedMatchCount(element);
    }

    public Match[] getDisplayedMatches(Object element) {
        if (showLineMatches()) {
            if (element instanceof IrcMessageMatches) {
                IrcMessageMatches lineEntry = (IrcMessageMatches) element;
                return lineEntry.getMatches();
            }
            return new Match[0];
        }
        return super.getDisplayedMatches(element);
    }

    public String getLabel() {
        String label = super.getLabel();
        StructuredViewer viewer = getViewer();
        if (viewer instanceof TableViewer) {
            TableViewer tv = (TableViewer) viewer;

            AbstractTextSearchResult result = getInput();
            if (result != null) {
                int itemCount = ((IStructuredContentProvider) tv.getContentProvider()).getElements(getInput()).length;
                if (showLineMatches()) {
                    int matchCount = getInput().getMatchCount();
                    if (itemCount < matchCount) {
                        return MessageFormat.format(IrcUiMessages.FileSearchPage_limited_format_matches, new Object[] {
                                label, new Integer(itemCount), new Integer(matchCount) });
                    }
                } else {
                    int fileCount = getInput().getElements().length;
                    if (itemCount < fileCount) {
                        return MessageFormat.format(IrcUiMessages.FileSearchPage_limited_format_files, new Object[] {
                                label, new Integer(itemCount), new Integer(fileCount) });
                    }
                }
            }
        }
        return label;
    }

    public StructuredViewer getViewer() {
        return super.getViewer();
    }

    protected void handleOpen(OpenEvent event) {
        if (showLineMatches()) {
            Object firstElement = ((IStructuredSelection) event.getSelection()).getFirstElement();
            if (firstElement instanceof IFile) {
                if (getDisplayedMatchCount(firstElement) == 0) {
                    try {
                        open(getSite().getPage(), (IFile) firstElement, false);
                    } catch (PartInitException e) {
                        ErrorDialog.openError(getSite().getShell(),
                                IrcUiMessages.FileSearchPage_open_file_dialog_title,
                                IrcUiMessages.FileSearchPage_open_file_failed, e.getStatus());
                    }
                    return;
                }
            }
        }
        super.handleOpen(event);
    }

    @SuppressWarnings("restriction")
    public void init(IPageSite site) {
        super.init(site);
        IMenuManager menuManager = site.getActionBars().getMenuManager();
        menuManager.appendToGroup(IContextMenuConstants.GROUP_PROPERTIES,
                new org.eclipse.search2.internal.ui.OpenSearchPreferencesAction());
    }

    public void restoreState(IMemento memento) {
        super.restoreState(memento);
        try {
            String val = getSettings().get(KEY_SORTING);
            if (val == null) {
                labelOrder = sortByNameAction.getSortOrder();
            } else {
                labelOrder = LabelOrder.valueOf(val);
            }
        } catch (IllegalArgumentException e) {
            labelOrder = sortByNameAction.getSortOrder();
        }
        int elementLimit = DEFAULT_ELEMENT_LIMIT;
        try {
            elementLimit = getSettings().getInt(KEY_LIMIT);
        } catch (NumberFormatException e) {
        }
        if (memento != null) {
            String str = memento.getString(KEY_SORTING);
            if (str != null) {
                try {
                    labelOrder = LabelOrder.valueOf(str);
                } catch (IllegalArgumentException e) {
                    labelOrder = sortByNameAction.getSortOrder();
                }
            }

            Integer value = memento.getInteger(KEY_LIMIT);
            if (value != null) {
                elementLimit = value.intValue();
            }
        }
        setElementLimit(new Integer(elementLimit));
    }

    public void saveState(IMemento memento) {
        super.saveState(memento);
        memento.putString(KEY_SORTING, labelOrder.name());
        memento.putInteger(KEY_LIMIT, getElementLimit().intValue());
    }

    public void setElementLimit(Integer elementLimit) {
        super.setElementLimit(elementLimit);
        int limit = elementLimit.intValue();
        getSettings().put(KEY_LIMIT, limit);
    }

    public void setSortOrder(LabelOrder sortOrder) {
        labelOrder = sortOrder;
        IrcSearchDecoratingLabelProvider lpWrapper = (IrcSearchDecoratingLabelProvider) getViewer().getLabelProvider();
        ((IrcSearchLabelProvider) lpWrapper.getStyledStringProvider()).setOrder(sortOrder);
        getViewer().refresh();
        getSettings().put(KEY_SORTING, labelOrder.name());
    }

    public void setViewPart(ISearchResultViewPart part) {
        super.setViewPart(part);
        searchActionGroup = new IrcSearchActionGroup(part);
    }

    private boolean showLineMatches() {
        AbstractTextSearchResult input = getInput();
        return getLayout() == FLAG_LAYOUT_TREE && input != null
                && !((IrcSearchQuery) input.getQuery()).isFileLevelSearch();
    }

    protected void showMatch(Match match, int offset, int length, boolean activate) throws PartInitException {
        IFile file = (IFile) match.getElement();
        IWorkbenchPage page = getSite().getPage();
        if (offset >= 0 && length != 0) {
            openAndSelect(page, file, offset, length, activate);
        } else {
            open(page, file, activate);
        }
    }

}
