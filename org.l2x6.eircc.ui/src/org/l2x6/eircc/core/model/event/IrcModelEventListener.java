/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model.event;

import java.util.Comparator;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public interface IrcModelEventListener {
    Comparator<IrcModelEventListener> COMPARATOR = new Comparator<IrcModelEventListener>() {
        @Override
        public int compare(IrcModelEventListener o1, IrcModelEventListener o2) {
            return Float.compare(o1.getOrderingKey(), o2.getOrderingKey());
        }
    };

    default float getOrderingKey() {
        return 0f;
    }

    void handle(IrcModelEvent e);
}
