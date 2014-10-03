/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.prefs;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    /**
     *
     */
    public IrcPreferencePage() {
        super();
    }

    /**
     * @param title
     */
    public IrcPreferencePage(String title) {
        super(title);
    }

    /**
     * @param title
     * @param image
     */
    public IrcPreferencePage(String title, ImageDescriptor image) {
        super(title, image);
    }

    /**
     * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        return null;
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {
    }

}
