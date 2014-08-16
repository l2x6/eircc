/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.client;

import org.l2x6.eircc.core.model.IrcAccount;
import org.schwering.irc.lib.TrafficLogger;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public interface TrafficLoggerFactory {
    TrafficLogger createTrafficLogger(IrcAccount account);
}
