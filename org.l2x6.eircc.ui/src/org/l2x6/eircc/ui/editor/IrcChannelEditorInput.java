/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.ui.IrcImages;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcChannelEditorInput implements IEditorInput {
    private final IrcChannel channel;
    private String toolTipText;

    /**
     * @param channel
     */
    public IrcChannelEditorInput(IrcChannel channel) {
        super();
        this.channel = channel;
        this.toolTipText = channel.getName() + "@"+ channel.getAccount().getLabel();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof IrcChannelEditorInput) {
            return ((IrcChannelEditorInput) other).channel.equals(channel);
        }
        return false;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#exists()
     */
    @Override
    public boolean exists() {
        return true;
    }

    /**
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        return null;
    }

    public IrcChannel getChannel() {
        return channel;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return IrcImages.getInstance().getImageDescriptor(channel);
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getName()
     */
    @Override
    public String getName() {
        return channel.getName();
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getPersistable()
     */
    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getToolTipText()
     */
    @Override
    public String getToolTipText() {
        return toolTipText;
    }

    @Override
    public int hashCode() {
        return channel.hashCode();
    }

}
