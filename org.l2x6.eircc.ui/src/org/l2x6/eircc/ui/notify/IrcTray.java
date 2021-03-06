/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.notify;

import java.awt.SystemTray;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.l2x6.eircc.core.model.IrcAccountsStatistics;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.resource.IrcResourceException;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.misc.IrcImages;
import org.l2x6.eircc.ui.misc.IrcImages.ImageSize;
import org.l2x6.eircc.ui.views.IrcLabelProvider;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcTray {

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
            trayItem.addListener(SWT.DefaultSelection, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    try {
                        EirccUi.getDefault().revealHottest();
                    } catch (IrcResourceException | CoreException | IOException | BadLocationException e) {
                        EirccUi.log(e);
                    }
                }
            });
            update();
        }
    }

    public void dispose() {
        flasher = null;
        trayItem.dispose();
        trayItem = null;
        tray = null;
    }

    public void update() {
        if (trayItem != null) {
            IrcModel model = EirccUi.getDefault().getModel();
            IrcAccountsStatistics stats = model.getAccountsStatistics();
            trayItem.setToolTipText(IrcLabelProvider.getInstance().getTooltipText(stats));

            /*
             * Find out the icon size required by the tray using AWT. Feel free
             * to replace this with a pure SWT equivalent if you know one
             */
            SystemTray awtTray = SystemTray.getSystemTray();
            ImageSize size = awtTray != null ? new ImageSize(awtTray.getTrayIconSize()) : ImageSize._16x16;
            Image[] images = IrcImages.getInstance().getFlashingImage(stats, size);
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
