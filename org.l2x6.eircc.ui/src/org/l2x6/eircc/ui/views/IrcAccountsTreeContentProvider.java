/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.views;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.core.model.IrcModel;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcAccountsTreeContentProvider implements ITreeContentProvider {

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    @Override
    public void dispose() {
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IrcModel) {
            return ((IrcModel) parentElement).getAccounts();
        } else if (parentElement instanceof IrcAccount) {
            return ((IrcAccount) parentElement).getChannels();
        }
        return null;
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
     */
    @Override
    public Object[] getElements(Object inputElement) {
        return ((IrcModel) inputElement).getAccounts();
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    @Override
    public Object getParent(Object element) {
        if (element instanceof IrcChannel) {
            return ((AbstractIrcChannel) element).getAccount();
        }
        return null;
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof IrcModel) {
            return ((IrcModel) element).hasAccounts();
        } else if (element instanceof IrcAccount) {
            return ((IrcAccount) element).hasChannels();
        }
        return false;
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
     *      java.lang.Object, java.lang.Object)
     */
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

}
