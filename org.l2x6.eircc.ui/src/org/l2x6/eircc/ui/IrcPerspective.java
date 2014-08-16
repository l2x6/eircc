/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.l2x6.eircc.ui.views.IrcAccountsView;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcPerspective implements IPerspectiveFactory {

    public void createInitialLayout(IPageLayout layout) {

        String editorArea = layout.getEditorArea();

        IFolderLayout leftFolder= layout.createFolder("left", IPageLayout.LEFT, (float)0.2, editorArea); //$NON-NLS-1$

        leftFolder.addView(IrcAccountsView.ID);

        IFolderLayout bottomFolder= layout.createFolder("bottom", IPageLayout.BOTTOM, (float)0.75, editorArea); //$NON-NLS-1$
        bottomFolder.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);

        layout.addView(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, (float)0.75, editorArea);

        layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);


        // views
        layout.addShowViewShortcut(IrcAccountsView.ID);

        // new actions
        layout.addNewWizardShortcut(NewIrcAccountWizard.ID); //$NON-NLS-1$
    }
}
