/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.views;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.l2x6.eircc.core.model.IrcObject;
import org.l2x6.eircc.ui.IrcImages;

public class IrcLabelProvider extends LabelProvider {

    private static final IrcLabelProvider INSTANCE = new IrcLabelProvider();

    public static IrcLabelProvider getInstance() {
        return INSTANCE;
    }

    /**
     *
     */
    public IrcLabelProvider() {
        super();
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof IrcObject) {
            return IrcImages.getInstance().getImage((IrcObject) element);
        }
        return null;
    }

}