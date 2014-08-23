/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.commands;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.l2x6.eircc.core.IrcController;
import org.l2x6.eircc.core.IrcException;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcAccount.IrcAccountState;
import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.ui.EirccUi;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcConnectHandler extends AbstractHandler {

    /**
     *
     */
    public IrcConnectHandler() {
        super();
    }

    /**
     * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
        Iterator<?> iterator = selection.iterator();
        while (iterator.hasNext()) {
            Object element = iterator.next();
            if (element instanceof IrcChannel) {
                IrcChannel ch = (IrcChannel) element;
                if (!ch.isJoined()) {
                    try {
                        IrcController.getInstance().joinChannel(ch);
                    } catch (IrcException e) {
                        EirccUi.log(e);
                    }
                }
            } else if (element instanceof IrcAccount) {
                IrcAccount account = (IrcAccount) element;
                if (account.getState() != IrcAccountState.ONLINE) {
                    try {
                        IrcController.getInstance().connect(account);
                    } catch (IrcException e) {
                        EirccUi.log(e);
                    }
                }
            }
        }
        return null;
    }

}
