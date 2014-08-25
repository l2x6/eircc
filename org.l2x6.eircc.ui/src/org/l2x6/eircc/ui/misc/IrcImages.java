/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.misc;

import java.awt.Dimension;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.gmf.runtime.draw2d.ui.render.RenderInfo;
import org.eclipse.gmf.runtime.draw2d.ui.render.RenderedImage;
import org.eclipse.gmf.runtime.draw2d.ui.render.factory.RenderedImageFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcAccountsStatistics;
import org.l2x6.eircc.core.model.IrcChannelUser;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.IrcObject;
import org.l2x6.eircc.core.model.IrcUser;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcImages {
    private static class GmfRenderInfo implements RenderInfo {
        private final ImageSize size;

        /**
         * @param size
         */
        public GmfRenderInfo(ImageSize size) {
            super();
            this.size = size;
        }

        @Override
        public RGB getBackgroundColor() {
            return null;
        }

        @Override
        public RGB getForegroundColor() {
            return null;
        }

        @Override
        public int getHeight() {
            return size.getHeight();
        }

        @Override
        public int getWidth() {
            return size.getWidth();
        }

        @Override
        public void setValues(int width, int height, boolean maintainAspectRatio, boolean antialias, RGB background,
                RGB foreground) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean shouldAntiAlias() {
            return true;
        }

        @Override
        public boolean shouldMaintainAspectRatio() {
            return false;
        }
    }

    public class ImageImageDescriptor extends ImageDescriptor {
        private Image fImage;

        /**
         * Constructor for ImagImageDescriptor.
         */
        public ImageImageDescriptor(Image image) {
            super();
            fImage = image;
        }

        /*
         * (non-Javadoc)
         * 
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object obj) {
            return (obj != null) && getClass().equals(obj.getClass())
                    && fImage.equals(((ImageImageDescriptor) obj).fImage);
        }

        /*
         * (non-Javadoc)
         * 
         * @see ImageDescriptor#getImageData()
         */
        @Override
        public ImageData getImageData() {
            return fImage.getImageData();
        }

        /*
         * (non-Javadoc)
         * 
         * @see Object#hashCode()
         */
        @Override
        public int hashCode() {
            return fImage.hashCode();
        }
    }

    public enum ImageKey {
        ACCOUNT("account.gif", IrcAccount.class), //
        ACCOUNT_NEW("account-new.png"), //
        BLUE_BALL_OVERLAY("blue-ball-overlay.png", null, SWT.NONE, "blue-ball-overlay.svg", ImageSize._7x7), //
        CHANNEL("channel.gif", AbstractIrcChannel.class), //
        CONNECT("connect.gif"), //
        DISCONNECT("disconnect.gif"), //
        GREEN_BALL_OVERLAY("green-ball-overlay.png", null, SWT.NONE, "green-ball-overlay.svg", ImageSize._7x7), //
        IRC_CLIENT("eircc.png", IrcModel.class, SWT.NONE, "eircc.svg", ImageSize._16x16), //
        IRC_CLIENT_DISABLED(IRC_CLIENT, SWT.IMAGE_GRAY), //
        JOIN_CHANNEL("join-channel.gif"), //
        LEAVE_CHANNEL("leave-channel.gif"), //
        NEW_OVERLAY("new-overlay.png"), //
        REFRESH("refresh.gif"), //
        SMILEY_OVERLAY("smiley-overlay.png", null, SWT.NONE, "smiley-overlay.svg", ImageSize._7x7), //
        USER("user.gif", IrcUser.class), //
        WARNING_OVERLAY("warning-overlay.gif", null, SWT.NONE, "warning-overlay.svg", ImageSize._7x8);

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

        private static StringBuilder appendKey(StringBuilder sb, String name, int flags, ImageSize size) {
            return sb.append("f").append(flags).append("-").append(name).append("-").append(size.getWidth())
                    .append("x").append(size.getHeight());
        }

        public static ImageKey fromModelClass(Class<?> cl) {
            return CLASS_LOOKUP.get(cl);
        }

        private final int flags;

        private final String key16x16;
        private final Class<?> modelClass;
        private final String path16x16;
        private final String pathSvg;

        /**
         * As stupid as the name of this field may sound... this is a width and
         * height of the image in pixels. 16x16 is only the form factor, whereas
         * the image may be an overlay for this form factor that in reality has
         * 7x7 pixels.
         */
        private final ImageSize size16x16;

        private ImageKey(ImageKey imageKey, int flags) {
            this(imageKey.path16x16, imageKey.modelClass, flags, imageKey.pathSvg, imageKey.size16x16);
        }

        /**
         * @param path
         */
        private ImageKey(String path16x16) {
            this(path16x16, null, SWT.NONE, null, ImageSize._16x16);
        }

        /**
         * @param path
         * @param modelClass
         */
        private ImageKey(String path16x16, Class<?> modelClass) {
            this(path16x16, modelClass, SWT.NONE, null, ImageSize._16x16);
        }

        private ImageKey(String path16x16, Class<?> modelClass, int flags, String pathSvg, ImageSize size16x16) {
            this.path16x16 = path16x16;
            this.modelClass = modelClass;
            this.flags = flags;
            this.pathSvg = pathSvg;
            this.size16x16 = size16x16;
            this.key16x16 = appendKey(new StringBuilder(), name(), flags, ImageSize._16x16).toString();
        }

        public void appendKey(StringBuilder sb, ImageSize size) {
            if (size == ImageSize._16x16) {
                sb.append(key16x16);
            } else {
                appendKey(sb, name(), flags, size);
            }
        }

        public String getAbsolutePath() {
            return "/icons/" + path16x16;
        }

        public String getAbsolutePathSvg() {
            return "/icons-src/" + pathSvg;
        }

        public int getFlags() {
            return flags;
        }

        /**
         * @param size
         * @return
         */
        public String getKey(ImageSize size) {
            return appendKey(new StringBuilder(), name(), flags, size).toString();
        }

        public String getKey16x16() {
            return key16x16;
        }

        public String getPath16x16() {
            return path16x16;
        }

        public ImageSize getSize16x16() {
            return size16x16;
        }
    }

    public static class ImageSize {
        public static final ImageSize _16x16 = new ImageSize(16, 16);
        public static final ImageSize _7x7 = new ImageSize(7, 7);
        public static final ImageSize _7x8 = new ImageSize(7, 8);
        private final int height;
        private final int width;

        /**
         * @param trayIconSize
         */
        public ImageSize(Dimension dimension) {
            this.width = dimension.width;
            this.height = dimension.height;
        }

        /**
         * @param width
         * @param height
         */
        public ImageSize(int width, int height) {
            super();
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ImageSize other = (ImageSize) obj;
            if (height != other.height)
                return false;
            if (width != other.width)
                return false;
            return true;
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + height;
            result = prime * result + width;
            return result;
        }

        public ImageSize scaleToSquare(int size) {
            return new ImageSize(width * size / ImageSize._16x16.getWidth(), height * size
                    / ImageSize._16x16.getHeight());
        }

        @Override
        public String toString() {
            return "" + width + "x" + height;
        }

    }

    private static final IrcImages INSTANCE = new IrcImages();

    public static Image fromSvg(URL url, ImageSize size) {
        RenderInfo ri = new GmfRenderInfo(size);
        RenderedImage renderedImage = RenderedImageFactory.getInstance(url, ri);
        return renderedImage.getSWTImage();
    }

    public static IrcImages getInstance() {
        return INSTANCE;
    }

    private static String getKey(ImageKey[] overlays, ImageSize size) {
        int i = 0;
        StringBuilder sb = new StringBuilder();
        overlays[i++].appendKey(sb, size);
        for (; i < overlays.length; i++) {
            ImageKey imageKey = overlays[i];
            if (imageKey != null) {
                sb.append("_q").append(i - 1).append('_').append(imageKey.getKey16x16());
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
            imageRegistry.put(imageKey.getKey16x16(), imageDescriptor);
        }

    }

    public void dispose() {
        imageRegistry.dispose();
    }

    public Image[] getFlashingImage(IrcModel model, ImageSize size) {
        IrcAccountsStatistics stats = model.getAccountsStatistics();
        if (stats.hasChannelsNamingMe()) {
            return new Image[] { getImage(getOverlays(stats, true), size), getImage(getOverlays(stats, false), size) };
        } else {
            return new Image[] { getImage(getOverlays(stats, true), size) };
        }
    }

    public Image getImage(Class<?> cl) {
        return imageRegistry.get(ImageKey.fromModelClass(cl).getKey16x16());
    }

    public Image getImage(ImageKey imageKey) {
        return imageRegistry.get(imageKey.getKey16x16());
    }

    public Image getImage(ImageKey[] overlays) {
        return getImage(overlays, ImageSize._16x16);
    }

    public Image getImage(ImageKey[] overlays, ImageSize size) {
        String key = getKey(overlays, size);
        getImageDescriptor(key, overlays, size);
        return imageRegistry.get(key);
    }

    public Image getImage(IrcObject element) {
        if (element == null) {
            return null;
        }
        if (element instanceof IrcAccount) {
            return getImage(getOverlays((IrcAccount) element));
        } else if (element instanceof AbstractIrcChannel) {
            return getImage(getOverlays((AbstractIrcChannel) element));
        } else if (element instanceof IrcModel) {
            return getImage(getOverlays(((IrcModel) element).getAccountsStatistics(), true));
        }
        return getImage(element.getClass());
    }

    public ImageDescriptor getImageDescriptor(ImageKey imageKey) {
        return getImageDescriptor(imageKey, ImageSize._16x16);
    }

    public ImageDescriptor getImageDescriptor(ImageKey imageKey, ImageKey overlayKey, ImageSize size, int quadrant) {
        ImageKey[] overlays = new ImageKey[6];
        overlays[0] = imageKey;
        overlays[quadrant + 1] = overlayKey;
        String key = getKey(overlays, size);
        return getImageDescriptor(key, overlays, size);
    }

    public ImageDescriptor getImageDescriptor(ImageKey imageKey, ImageSize size) {
        if (imageKey == null) {
            return null;
        }
        String key = imageKey.getKey(size);
        ImageDescriptor result = imageRegistry.getDescriptor(key);
        if (result == null) {
            /* this should happen for non-16x16 images only */
            String svgPath = imageKey.getAbsolutePathSvg();
            if (svgPath != null) {
                URL url = getClass().getClassLoader().getResource(imageKey.getAbsolutePathSvg());
                if (url == null) {
                    return null;
                }
                Image image = fromSvg(url, imageKey.getSize16x16().scaleToSquare(size.getWidth()));
                if (imageKey.getFlags() != SWT.NONE) {
                    result = ImageDescriptor.createWithFlags(new ImageImageDescriptor(image), imageKey.getFlags());
                } else {
                    result = new ImageImageDescriptor(image);
                }
                imageRegistry.put(key, result);
            }
        }
        return result;
    }

    public ImageDescriptor getImageDescriptor(IrcObject element) {
        return getImageDescriptor(element, ImageSize._16x16);
    }

    public ImageDescriptor getImageDescriptor(IrcObject element, ImageSize size) {
        if (element == null) {
            return null;
        }
        ImageKey[] overlays;
        if (element instanceof IrcAccount) {
            overlays = getOverlays((IrcAccount) element);
        } else if (element instanceof AbstractIrcChannel) {
            overlays = getOverlays((AbstractIrcChannel) element);
        } else {
            return getImageDescriptor(ImageKey.fromModelClass(element.getClass()), size);
        }
        String key = getKey(overlays, size);
        return getImageDescriptor(key, overlays, size);
    }

    public ImageDescriptor getImageDescriptor(String key, ImageKey[] overlays, ImageSize size) {
        ImageDescriptor result = imageRegistry.getDescriptor(key);
        if (result == null) {
            switch (overlays.length) {
            case 1:
                result = getImageDescriptor(overlays[0], size);
                break;
            case 6:
                result = new DecorationOverlayIcon(getImage(overlays[0]), toOverlayDescriptors(overlays, size));
                imageRegistry.put(key, result);
                break;
            default:
                throw new IllegalArgumentException("Unexpected overlays length.");
            }
        }
        return result;
    }

    private ImageKey[] getOverlays(AbstractIrcChannel channel) {
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

    private ImageDescriptor[] toOverlayDescriptors(ImageKey[] overlays, ImageSize size) {
        int i = 1;
        return new ImageDescriptor[] { getImageDescriptor(overlays[i++], size),
                getImageDescriptor(overlays[i++], size), getImageDescriptor(overlays[i++], size),
                getImageDescriptor(overlays[i++], size), getImageDescriptor(overlays[i++], size) };
    }

}
