/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.misc;

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Text;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class NumericTextVerifier implements VerifyListener {
    @Override
    public void verifyText(VerifyEvent e) {
        final String oldText = ((Text)e.widget).getText();
        final String newS = oldText.substring(0, e.start) + e.text + oldText.substring(e.end);
        try {
            Integer.parseInt(newS);
        } catch (final NumberFormatException numberFormatException) {
            e.doit = false;
        }
    }
}