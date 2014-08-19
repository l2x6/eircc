/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.l2x6.eircc.core.model.IrcAccountsStatistics;
import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcChannelUser;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.IrcObject;
import org.l2x6.eircc.core.model.IrcUser;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcImages {

    public enum ImageKey {
        ACCOUNT("account.gif", IrcAccount.class), //
        ACCOUNT_NEW("account-new.png"), //
        BLUE_BALL_OVERLAY("blue-ball-overlay.png"), //
        CHANNEL("channel.gif", IrcChannel.class), //
        CONNECT("connect.gif"), //
        DISCONNECT("disconnect.gif"), //
        GREEN_BALL_OVERLAY("green-ball-overlay.png"), //
        IRC_CLIENT("eircc.png"), //
        IRC_CLIENT_DISABLED(IRC_CLIENT, SWT.IMAGE_GRAY), //
        NEW_OVERLAY("new-overlay.png"), //
        REFRESH("refresh.gif"), USER("user.gif", IrcUser.class), //
        SMILEY_OVERLAY("smiley-overlay.png"), //
        WARNING_OVERLAY("warning-overlay.png");

        private static final Map<Class<?>, ImageKey> CLASS_LOOKUP;

        static {
            ImageKey[] imageKeys = ImageKey.values();
            Map<Class<?>, ImageKey> m = new HashMap<Class<?>, IrcImages.ImageKey>(imageKeys.length + imageKeys.length
                    / 2 + 1);
            for (ImageKey imageKey : imageKeys) {
                if (imageKey.modelClass != null) {
                    m.put(imageKey.modelClass, imageKey);
                }
            }
            /* both IrcUser and IrcChannelUser will have the same icon */
            m.put(IrcChannelUser.class, USER);
            CLASS_LOOKUP = Collections.unmodifiableMap(m);
        }

        public static ImageKey fromModelClass(Class<?> cl) {
            return CLASS_LOOKUP.get(cl);
        }

        private final Class<?> modelClass;
        private final String path;
        private final String key;
        private final int flags;

        private static String createKey(String path, int flags) {
            return "f" + flags + "-" + path;
        }

        public int getFlags() {
            return flags;
        }

        private ImageKey(ImageKey imageKey, int flags) {
            this.path = imageKey.path;
            this.modelClass = imageKey.modelClass;
            this.flags = flags;
            this.key = createKey(path, flags);
        }

        /**
         * @param path
         */
        private ImageKey(String path) {
            this.path = path;
            this.modelClass = null;
            this.flags = SWT.NONE;
            this.key = createKey(path, flags);
        }

        /**
         * @param path
         * @param modelClass
         */
        private ImageKey(String path, Class<?> modelClass) {
            this.path = path;
            this.modelClass = modelClass;
            this.flags = SWT.NONE;
            this.key = createKey(path, flags);
        }

        public String getAbsolutePath() {
            return "/icons/" + path;
        }

        public String getPath() {
            return path;
        }

        public String getKey() {
            return key;
        }

    }

    private static final IrcImages INSTANCE = new IrcImages();

    public static IrcImages getInstance() {
        return INSTANCE;
    }

    private static String getKey(ImageKey[] overlays) {
        int i = 0;
        StringBuilder sb = new StringBuilder(overlays[i++].getKey());
        for (; i < overlays.length; i++) {
            ImageKey imageKey = overlays[i];
            if (imageKey != null) {
                sb.append("_q").append(i - 1).append('_').append(imageKey.getKey());
            }
        }
        return sb.toString();
    }

    private final ImageRegistry imageRegistry = new ImageRegistry();

    /**
     *
     */
    public IrcImages() {

        for (ImageKey imageKey : ImageKey.values()) {
            ImageDescriptor imageDescriptor = ImageDescriptor.createFromFile(this.getClass(),
                    imageKey.getAbsolutePath());
            if (imageKey.getFlags() != SWT.NONE) {
                imageDescriptor = ImageDescriptor.createWithFlags(imageDescriptor, imageKey.getFlags());
            }
            imageRegistry.put(imageKey.getKey(), imageDescriptor);
        }

    }

    public void dispose() {
        imageRegistry.dispose();
    }

    public Image getImage(Class<?> cl) {
        return imageRegistry.get(ImageKey.fromModelClass(cl).getKey());
    }

    public Image getImage(ImageKey imageKey) {
        return imageRegistry.get(imageKey.getKey());
    }

    public Image getImage(ImageKey[] overlays) {
        String key = getKey(overlays);
        getImageDescriptor(key, overlays);
        return imageRegistry.get(key);
    }

    public Image[] getFlashingImage(IrcModel model) {
        IrcAccountsStatistics stats = model.getAccountsStatistics();
        if (stats.hasChannelsNamingMe()) {
            return new Image[] { getImage(getOverlays(stats, true)), getImage(getOverlays(stats, false)) };
        } else {
            return new Image[] { getImage(getOverlays(stats, true)) };
        }
    }

    public Image getImage(IrcObject element) {
        if (element == null) {
            return null;
        }
        if (element instanceof IrcAccount) {
            return getImage(getOverlays((IrcAccount) element));
        } else if (element instanceof IrcChannel) {
            return getImage(getOverlays((IrcChannel) element));
        } else if (element instanceof IrcModel) {
            return getImage(getOverlays(((IrcModel) element).getAccountsStatistics(), true));
        }
        return getImage(element.getClass());
    }

    public ImageDescriptor getImageDescriptor(ImageKey imageKey) {
        if (imageKey == null) {
            return null;
        }
        return imageRegistry.getDescriptor(imageKey.getKey());
    }

    public ImageDescriptor getImageDescriptor(ImageKey imageKey, ImageKey overlayKey, int quadrant) {
        ImageKey[] overlays = new ImageKey[6];
        overlays[0] = imageKey;
        overlays[quadrant + 1] = overlayKey;
        String key = getKey(overlays);
        return getImageDescriptor(key, overlays);
    }

    public ImageDescriptor getImageDescriptor(IrcObject element) {
        if (element == null) {
            return null;
        }
        ImageKey[] overlays;
        if (element instanceof IrcAccount) {
            overlays = getOverlays((IrcAccount) element);
        } else if (element instanceof IrcChannel) {
            overlays = getOverlays((IrcChannel) element);
        } else {
            return getImageDescriptor(ImageKey.fromModelClass(element.getClass()));
        }
        String key = getKey(overlays);
        return getImageDescriptor(key, overlays);
    }

    public ImageDescriptor getImageDescriptor(String key, ImageKey[] overlays) {
        ImageDescriptor result = imageRegistry.getDescriptor(key);
        if (result == null) {
            switch (overlays.length) {
            case 1:
                result = getImageDescriptor(overlays[0]);
                break;
            case 6:
                result = new DecorationOverlayIcon(getImage(overlays[0]), toOverlayDescriptors(overlays));
                break;
            default:
                throw new IllegalArgumentException("Unexpected overlays lenght.");
            }
            imageRegistry.put(key, result);
        }
        return result;
    }

    public ImageDescriptor getImageDescriptorNew(ImageKey imageKey) {
        return getImageDescriptor(imageKey, ImageKey.NEW_OVERLAY, IDecoration.TOP_RIGHT);
    }

    private ImageKey[] getOverlays(IrcAccount account) {
        boolean hasOverlays = false;
        ImageKey topLeftOverlay = null;
        ImageKey topRightOverlay = null;
        ImageKey bottomLeftOverlay = null;
        ImageKey bottomRightOverlay = null;
        ImageKey underlay = null;
        switch (account.getState()) {
        case ONLINE:
            topRightOverlay = ImageKey.GREEN_BALL_OVERLAY;
            hasOverlays = true;
            break;
        case OFFLINE:
            break;
        case OFFLINE_AFTER_ERROR:
            topLeftOverlay = ImageKey.WARNING_OVERLAY;
            hasOverlays = true;
            break;
        }
        if (hasOverlays) {
            return new ImageKey[] { ImageKey.ACCOUNT, topLeftOverlay, topRightOverlay, bottomLeftOverlay,
                    bottomRightOverlay, underlay };
        } else {
            return new ImageKey[] { ImageKey.ACCOUNT };
        }

    }

    private ImageKey[] getOverlays(IrcChannel channel) {
        boolean hasOverlays = false;
        ImageKey topLeftOverlay = null;
        ImageKey topRightOverlay = null;
        ImageKey bottomLeftOverlay = null;
        ImageKey bottomRightOverlay = null;
        ImageKey underlay = null;
        IrcLog log = channel.getLog();
        if (log != null) {
            switch (log.getState()) {
            case ME_NAMED:
                topRightOverlay = ImageKey.SMILEY_OVERLAY;
                hasOverlays = true;
                break;
            case UNREAD_MESSAGES:
                topRightOverlay = ImageKey.BLUE_BALL_OVERLAY;
                hasOverlays = true;
                break;
            case NONE:
                if (channel.isJoined()) {
                    topRightOverlay = ImageKey.GREEN_BALL_OVERLAY;
                    hasOverlays = true;
                }
                break;
            }
        } else if (channel.isJoined()) {
            topRightOverlay = ImageKey.GREEN_BALL_OVERLAY;
            hasOverlays = true;
        }

        if (hasOverlays) {
            return new ImageKey[] { ImageKey.CHANNEL, topLeftOverlay, topRightOverlay, bottomLeftOverlay,
                    bottomRightOverlay, underlay };
        } else {
            return new ImageKey[] { ImageKey.CHANNEL };
        }
    }

    /**
     * @param element
     * @return
     */
    private ImageKey[] getOverlays(IrcAccountsStatistics stats, boolean withUnseenBall) {

        /*
         * colored or desaturated base image depending on whether there are
         * channels online
         */
        ImageKey base = stats.getChannelsOnline() > 0 ? ImageKey.IRC_CLIENT : ImageKey.IRC_CLIENT_DISABLED;
        ImageKey topLeftOverlay = stats.hasChannelsOfflineAfterError() ? ImageKey.WARNING_OVERLAY : null;
        ImageKey topRightOverlay = null;
        ImageKey bottomLeftOverlay = null;
        ImageKey bottomRightOverlay = null;
        ImageKey underlay = null;

        if (stats.hasChannelsNamingMe()) {
            topRightOverlay = withUnseenBall ? ImageKey.SMILEY_OVERLAY : null;
        } else if (stats.hasChannelsWithUnreadMessages()) {
            topRightOverlay = ImageKey.BLUE_BALL_OVERLAY;
        }

        boolean hasOverlays = topLeftOverlay != null || topRightOverlay != null || bottomLeftOverlay != null
                || bottomRightOverlay != null || underlay != null;

        if (hasOverlays) {
            return new ImageKey[] { base, topLeftOverlay, topRightOverlay, bottomLeftOverlay, bottomRightOverlay,
                    underlay };
        } else {
            return new ImageKey[] { base };
        }
    }

    private ImageDescriptor[] toOverlayDescriptors(ImageKey[] overlays) {
        int i = 1;
        return new ImageDescriptor[] { getImageDescriptor(overlays[i++]), getImageDescriptor(overlays[i++]),
                getImageDescriptor(overlays[i++]), getImageDescriptor(overlays[i++]), getImageDescriptor(overlays[i++]) };
    }

}
