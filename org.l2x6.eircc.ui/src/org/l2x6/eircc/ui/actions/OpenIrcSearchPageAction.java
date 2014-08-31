/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.misc.IrcUiUtils;
import org.l2x6.eircc.ui.search.IrcSearchPage;

/**
 * Opens the Search Dialog.
 */
public class OpenIrcSearchPageAction implements IWorkbenchWindowActionDelegate {

    private IWorkbenchWindow fWindow;

    public OpenIrcSearchPageAction() {
    }

    public void dispose() {
        fWindow= null;
    }

    public void init(IWorkbenchWindow window) {
        fWindow= window;
    }

    public void run(IAction action) {
        if (fWindow == null || fWindow.getActivePage() == null) {
            IrcUiUtils.beep();
            EirccUi.log("Could not open the search dialog - for some reason the window handle was null"); //$NON-NLS-1$
            return;
        }
        NewSearchUI.openSearchDialog(fWindow, IrcSearchPage.ID);
    }

    public void selectionChanged(IAction action, ISelection selection) {
        /* nothing to do */
    }

}
