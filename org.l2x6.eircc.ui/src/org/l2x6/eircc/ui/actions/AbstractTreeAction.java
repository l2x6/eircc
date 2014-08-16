/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public abstract class AbstractTreeAction extends Action implements Listener {
    protected final Tree tree;

    public AbstractTreeAction(Tree tree) {
        this.tree = tree;
        setEnabled(false);
        tree.addListener(SWT.Selection, this);
    }

    public void dispose() {
        if (!tree.isDisposed()) {
            tree.removeListener(SWT.Selection, this);
        }
    }

    /**
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    @Override
    public void handleEvent(Event event) {
        updateEnablement();
    }


    public abstract void updateEnablement();

}
