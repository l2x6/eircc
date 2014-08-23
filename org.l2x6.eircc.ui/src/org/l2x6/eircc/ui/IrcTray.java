/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui;

import java.awt.SystemTray;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEventListener;
import org.l2x6.eircc.ui.IrcImages.ImageSize;
import org.l2x6.eircc.ui.views.IrcLabelProvider;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcTray implements IrcModelEventListener {

    private class Flasher implements Runnable {
        private int flashIndex = 0;
        private final Image[] images;

        /**
         * @param images
         */
        public Flasher(Image[] images) {
            this.images = images;
        }

        /**
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            /* make sure that this is not an obsolete flasher */
            if (IrcTray.this.flasher == this && trayItem != null && !trayItem.isDisposed()) {
                trayItem.setImage(images[flashIndex]);
                flashIndex++;
                flashIndex %= images.length;
                /* and schedule the next flash */
                tray.getDisplay().timerExec(flashingInterval, this);
            }
        }

    }

    private static final IrcTray INSTANCE = new IrcTray();

    public static IrcTray getInstance() {
        return INSTANCE;
    }

    private Flasher flasher;

    private int flashingInterval = 500;
    private Tray tray;
    private TrayItem trayItem;

    /**
     *
     */
    public IrcTray() {
        super();

        tray = Display.getDefault().getSystemTray();
        if (tray != null) {
            trayItem = new TrayItem(tray, SWT.NONE);
            update();
            IrcModel.getInstance().addModelEventListener(this);
        }
    }

    public void dispose() {
        IrcModel.getInstance().removeModelEventListener(this);
        flasher = null;
        trayItem.dispose();
        trayItem = null;
        tray = null;
    }

    /**
     * @see org.l2x6.eircc.core.model.event.IrcModelEventListener#handle(org.l2x6.eircc.core.model.event.IrcModelEvent)
     */
    @Override
    public void handle(IrcModelEvent e) {
        switch (e.getEventType()) {
        case ACCOUNT_STATE_CHANGED:
        case LOG_STATE_CHANGED:
            update();
            break;
        default:
            break;
        }
    }

    private void update() {
        if (trayItem != null) {
            IrcModel model = IrcModel.getInstance();
            trayItem.setToolTipText(IrcLabelProvider.getInstance().getTooltipText(model));

            /*
             * Find out the icon size required by the tray using AWT. Feel free
             * to replace this with a pure SWT equivalent if you know one
             */
            SystemTray awtTray = SystemTray.getSystemTray();
            ImageSize size = awtTray != null ? new ImageSize(awtTray.getTrayIconSize()) : ImageSize._16x16;
            Image[] images = IrcImages.getInstance().getFlashingImage(model, size);
            if (images.length > 1) {
                this.flasher = new Flasher(images);
                this.flasher.run();
            } else {
                this.flasher = null;
                try {
                    trayItem.setImage(images[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
