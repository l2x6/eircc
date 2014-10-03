/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.notify;

import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.IrcLog.LogState;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEventListener;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.prefs.IrcPreferences;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcNotificationController implements IrcModelEventListener {

    private static final IrcNotificationController INSTANCE = new IrcNotificationController();
    public static IrcNotificationController getInstance() {
        return INSTANCE;
    }
    private final IrcSoundNotifier soundNotifier;

    private final IrcTray tray;

    /**
     *
     */
    public IrcNotificationController() {
        super();
        this.soundNotifier = new IrcSoundNotifier();
        this.tray = new IrcTray();

        IrcModel.getInstance().addModelEventListener(this);
    }

    public void dispose() {
        IrcModel.getInstance().removeModelEventListener(this);
        tray.dispose();
    }

    /**
     * @see org.l2x6.eircc.core.model.event.IrcModelEventListener#handle(org.l2x6.eircc.core.model.event.IrcModelEvent)
     */
    @Override
    public void handle(IrcModelEvent e) {
        IrcPreferences prefs = IrcPreferences.getInstance();

        try {
            switch (e.getEventType()) {
            case NEW_MESSAGE:
                IrcMessage m = (IrcMessage) e.getModelObject();
                if (m.isMeNamed() && m.getLog().getState() == LogState.ME_NAMED && prefs.shouldPlaySoundOnNamingMe()) {
                    soundNotifier.meNamed();
                } else if (!m.isFromMe() && m.getUser() != null
                        && prefs.shouldPlaySoundOnMessageFromNick(m.getUser().getNick())) {
                    soundNotifier.messageFromTrackedUser();
                }
                break;
            case ACCOUNT_STATE_CHANGED:
            case LOG_STATE_CHANGED:
                tray.update();
                break;
            default:
                break;
            }
        } catch (Exception e1) {
            EirccUi.log(e1);
        }
    }

}
