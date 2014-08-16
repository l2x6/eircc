/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.client;


/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public interface IrcNickGenerator {
    public static IrcNickGenerator DEFAULT = new IrcNickGenerator() {
        private static final int MAX_UNDERSCORES = 16;
        public String newNick(String oldNick) {
            if (oldNick == null) {
                return null;
            }
            int underscores = 0;
            for (int i = oldNick.length() -1; i >= 0; i--) {
                if (oldNick.charAt(i) == '_') {
                    underscores++;
                }
            }
            if (underscores > MAX_UNDERSCORES) {
                return null;
            }
            return oldNick + "_";
        }
    };

    String newNick(String oldNick);
}
