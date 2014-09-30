/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.OpenFileAction;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.l2x6.eircc.ui.IrcUiMessages;

/**
 * Adapted from
 * {@code org.eclipse.search.internal.ui.text.NewTextSearchActionGroup} as
 * available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSearchActionGroup extends ActionGroup {

    private OpenFileAction fOpenAction;
    private PropertyDialogAction fOpenPropertiesDialog;
    private IWorkbenchPage fPage;
    private ISelectionProvider fSelectionProvider;

    public IrcSearchActionGroup(IViewPart part) {
        Assert.isNotNull(part);
        IWorkbenchPartSite site = part.getSite();
        fSelectionProvider = site.getSelectionProvider();
        fPage = site.getPage();
        fOpenPropertiesDialog = new PropertyDialogAction(site, fSelectionProvider);
        fOpenAction = new OpenFileAction(fPage);
        ISelection selection = fSelectionProvider.getSelection();

        if (selection instanceof IStructuredSelection)
            fOpenPropertiesDialog.selectionChanged((IStructuredSelection) selection);
        else
            fOpenPropertiesDialog.selectionChanged(selection);

    }

    private void addOpenWithMenu(IMenuManager menu, IStructuredSelection selection) {
        if (selection == null)
            return;

        fOpenAction.selectionChanged(selection);
        if (fOpenAction.isEnabled()) {
            menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpenAction);
        }

        if (selection.size() != 1) {
            return;
        }

        Object o = selection.getFirstElement();
        if (!(o instanceof IAdaptable))
            return;

        // Create menu
        IMenuManager submenu = new MenuManager(IrcUiMessages.OpenWithMenu_label);
        submenu.add(new OpenWithMenu(fPage, (IAdaptable) o));

        // Add the submenu.
        menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, submenu);
    }

    /*
     * (non-Javadoc) Method declared in ActionGroup
     */
    public void fillActionBars(IActionBars actionBar) {
        super.fillActionBars(actionBar);
        setGlobalActionHandlers(actionBar);
    }

    public void fillContextMenu(IMenuManager menu) {
        // view must exist if we create a context menu for it.

        ISelection selection = getContext().getSelection();
        if (selection instanceof IStructuredSelection) {
            addOpenWithMenu(menu, (IStructuredSelection) selection);
            if (fOpenPropertiesDialog != null && fOpenPropertiesDialog.isEnabled()
                    && fOpenPropertiesDialog.isApplicableForSelection((IStructuredSelection) selection))
                menu.appendToGroup(IContextMenuConstants.GROUP_PROPERTIES, fOpenPropertiesDialog);
        }

    }

    private void setGlobalActionHandlers(IActionBars actionBars) {
        actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), fOpenPropertiesDialog);
    }
}
