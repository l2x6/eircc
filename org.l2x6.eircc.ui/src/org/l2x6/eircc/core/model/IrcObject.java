/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import org.eclipse.core.runtime.IPath;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public abstract class IrcObject extends IrcBase {
    protected final IrcModel model;
    protected final IPath parentFolderPath;

    protected IrcObject(IrcModel model, IPath parentFolderPath) {
        super();
        this.parentFolderPath = parentFolderPath;
        this.model = model;
    }


    public IrcModel getModel() {
        return model;
    }

    public IPath getParentFolderPath() {
        return parentFolderPath;
    }

}
