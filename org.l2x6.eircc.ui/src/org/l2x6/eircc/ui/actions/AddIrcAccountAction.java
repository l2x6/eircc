/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardDescriptor;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.misc.IrcImages;
import org.l2x6.eircc.ui.misc.NewIrcAccountWizard;
import org.l2x6.eircc.ui.misc.IrcImages.ImageKey;
import org.l2x6.eircc.ui.IrcUiMessages;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class AddIrcAccountAction extends Action {

    public AddIrcAccountAction() {
        setText(IrcUiMessages.AddIrcAccountAction_label);
        setImageDescriptor(IrcImages.getInstance().getImageDescriptor(ImageKey.ACCOUNT_NEW));
    }

    public void run() {
        try {
            IWizardDescriptor descriptor = PlatformUI.getWorkbench().getNewWizardRegistry()
                    .findWizard(NewIrcAccountWizard.ID);
            IWizard wizard = descriptor.createWizard();
            WizardDialog wd = new WizardDialog(EirccUi.getShell(), wizard);
            wd.setTitle(wizard.getWindowTitle());
            wd.open();
        } catch (CoreException e) {
            EirccUi.log(e);
        }
    }

}
