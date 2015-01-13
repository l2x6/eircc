/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.prefs;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.IrcNotificationLevel;
import org.l2x6.eircc.core.model.IrcNotificationLevelProvider;
import org.l2x6.eircc.core.model.PlainIrcMessage;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.editor.IrcDefaultMessageFormatter;
import org.l2x6.eircc.ui.editor.IrcNotificationMessageFormatter;
import org.l2x6.eircc.ui.editor.IrcSearchMessageFormatter;
import org.l2x6.eircc.ui.editor.IrcSystemMessageFormatter;
import org.l2x6.eircc.ui.misc.Colors;
import org.l2x6.eircc.ui.misc.ExtendedTextStyle;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcPreferences implements IrcNotificationLevelProvider {

    /**
     * Preference keys and their default values.
     *
     * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
     *
     */
    public enum PreferenceKey {

        WATCHED_CHANNELS("watched.channels", ""), WATCHED_NICKS("watched.nicks", ""); //$NON-NLS-1$

        private final Object defaultValue;
        private final String key;

        /**
         * Make sure that the type of {@code defaultValue} is supported by
         * {@link IPreferencesService}.
         *
         * @param value
         * @param defaultValue
         */
        private PreferenceKey(String value, Object defaultValue) {
            this.key = value;
            this.defaultValue = defaultValue;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        @Override
        public String toString() {
            return key;
        }

    }

    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(3);

    private static final long DEFAULT_EDITOR_LOOK_BACK_BYTE_LIMIT = 8 * 1024;
    private static final String DEFAULT_NOTIFICATION_MESSAGE_COLOR_KEY = IrcPreferences.class.getName() + ".defaultNotificationMessageColor";

    private static final Duration DEFAULT_PING_INTERVAL = Duration.ofMinutes(1);

    private static final IrcPreferences INSTANCE = new IrcPreferences();
    private static final IInputValidator PATTERN_VALIDATOR = new IInputValidator() {
        /**
         * Validates the String. Returns null for no error, or an error message
         *
         * @param newText
         *            the String to validate
         * @return String
         */
        public String isValid(String newText) {
            if (newText.isEmpty()) {
                return IrcUiMessages.IrcNotificationsPreferencePage_cannotBeEmpty;
            }

            try {
                Pattern.compile(newText);
            } catch (PatternSyntaxException e) {
                return MessageFormat
                        .format(IrcUiMessages.IrcNotificationsPreferencePage_invalidPattern, e.getMessage());
            }
            return null;
        }
    };

    public static final char WATCHED_OBJECT_DELIMITER = ' ';

    public static IrcPreferences getInstance() {
        return INSTANCE;
    }

    private final IrcDefaultMessageFormatter defaultFormatter = new IrcDefaultMessageFormatter(this);

    private final IrcSystemMessageFormatter errorFormatter = new IrcSystemMessageFormatter(this);
    private final ExtendedTextStyle messageTimeStyle;

    private final IrcNotificationMessageFormatter notificationFormatter = new IrcNotificationMessageFormatter(this);
    private final ExtendedTextStyle notificationMessageStyle;
    private IrcSearchMessageFormatter searchFormatter = new IrcSearchMessageFormatter(this);

    private final IrcSystemMessageFormatter systemFormatter = new IrcSystemMessageFormatter(this);
    private final ExtendedTextStyle systemMessageStyle;
    private final IrcUserStyler[] userStylers;
    private final ExtendedTextStyle[] userStyles;
    private final ExtendedTextStyle[] userStylesNamingMe;

    private Set<String> watchedChannels;

    private Map<String, Pattern> watchedNicks;

    /**
     *
     */
    public IrcPreferences() {
        super();
        ColorRegistry reg = JFaceResources.getColorRegistry();
        systemMessageStyle = new ExtendedTextStyle(Display.getDefault().getSystemColor(
                SWT.COLOR_TITLE_INACTIVE_FOREGROUND));

        reg.put(DEFAULT_NOTIFICATION_MESSAGE_COLOR_KEY, new RGB(254, 128, 0));
        notificationMessageStyle = new ExtendedTextStyle(reg.get(DEFAULT_NOTIFICATION_MESSAGE_COLOR_KEY));
        messageTimeStyle = new ExtendedTextStyle(Display.getDefault().getSystemColor(
                SWT.COLOR_TITLE_INACTIVE_FOREGROUND));

        Colors colors = Colors.getInstance();
        String keyPrefix = this.getClass().getName() + ".user#";
        userStyles = new ExtendedTextStyle[colors.getColorCount()];
        userStylesNamingMe = new ExtendedTextStyle[colors.getColorCount()];
        userStylers = new IrcUserStyler[colors.getColorCount()];
        for (int i = 0; i < userStyles.length; i++) {
            String key = keyPrefix + i;
            reg.put(key, colors.getRGB(i));
            Color c = reg.get(key);
            userStyles[i] = new ExtendedTextStyle(c);
            userStylesNamingMe[i] = new ExtendedTextStyle(c, SWT.BOLD);
            userStylers[i] = new IrcUserStyler(this, i);
        }

    }

    public void addWatchedChannel(String channel) {
        getWatchedChannels().add(channel);
        setCollection(PreferenceKey.WATCHED_CHANNELS, watchedChannels);
    }

    public void addWatchedNickPattern(String nick) {
        getWatchedNicks().put(nick, Pattern.compile(nick));
        setCollection(PreferenceKey.WATCHED_NICKS, watchedNicks.keySet());
    }

    public void dispose() {

    }

    /**
     * @return
     */
    public String getAddresseeSuffix() {
        return ": ";
    }

    /**
     * Calls
     * {@link IPreferencesService#getBoolean(String, String, String, org.eclipse.core.runtime.preferences.IScopeContext[])}
     * using {@link Plugin#ID} and a default ({@code null}) context order.
     *
     * @param key
     * @return
     */
    public boolean getBoolean(PreferenceKey key) {
        IPreferenceStore store = EirccUi.getDefault().getPreferenceStore();
        return store.getBoolean(key.toString());
        // return preferencesService.getBoolean(EirccUi.PLUGIN_ID,
        // key.toString(), ((Boolean) key.getDefaultValue()).booleanValue(),
        // null);
    }

    /**
     * @return
     */
    public boolean getEditorAutoInsert() {
        return true;
    }

    /**
     * @return
     */
    public boolean getEditorAutoPrefixCompletion() {
        return true;
    }

    /**
     * @param m
     * @return
     */
    public IrcDefaultMessageFormatter getFormatter(PlainIrcMessage m) {
        switch (m.getType()) {
        case SYSTEM:
            return systemFormatter;
        case ERROR:
            return errorFormatter;
        case NOTIFICATION:
            return notificationFormatter;
        default:
            return defaultFormatter;
        }
    }

    /**
     * @return
     */
    public long getLookbackTresholdBytes() {
        return DEFAULT_EDITOR_LOOK_BACK_BYTE_LIMIT;
    }

    public ExtendedTextStyle getMessageTimeStyle() {
        return messageTimeStyle;
    }

    /**
     * @param m
     * @return
     */
    public IrcNotificationLevel getNotificationLevel(IrcMessage m) {
        if (m.isMeNamed() && shouldPlaySoundOnNamingMe()) {
            return IrcNotificationLevel.ME_NAMED;
        } else if (!m.isFromMe() && m.getSender() != null && shouldPlaySoundOnMessageFromNick(m.getSender().getNick())) {
            return IrcNotificationLevel.UNREAD_MESSAGES_FROM_A_TRACKED_USER;
        } else if (!m.isFromMe() && m.getUser() != null && shouldPlaySoundOnMessageInChannel(m.getLog().getChannel())) {
            return IrcNotificationLevel.UNREAD_MESSAGES;
        }
        return IrcNotificationLevel.NO_NOTIFICATION;
    }

    /**
     * @return
     */
    public ExtendedTextStyle getNotificationMessageStyle() {
        return notificationMessageStyle;
    }

    public Duration getPingInterval() {
        return DEFAULT_PING_INTERVAL;
    }

    public Duration getPingTimeout() {
        return COMMAND_TIMEOUT;
    }

    /**
     * @param message
     * @return
     */
    public IrcSearchMessageFormatter getSearchFormatter(PlainIrcMessage message) {
        return searchFormatter;
    }

    /**
     * Calls
     * {@link IPreferencesService#getString(String, String, String, org.eclipse.core.runtime.preferences.IScopeContext[])}
     * using {@link Plugin#ID} and a default ({@code null}) context order.
     *
     * @param key
     * @return
     */
    public String getString(PreferenceKey key) {
        IPreferenceStore store = EirccUi.getDefault().getPreferenceStore();
        return store.getString(key.toString());
        // return preferencesService.getString(EirccUi.PLUGIN_ID, , (String)
        // key.getDefaultValue(), null);
    }

    public ExtendedTextStyle getSystemMessageStyle() {
        return systemMessageStyle;
    }

    public ExtendedTextStyle getUserStyle(int index, boolean namingMe) {
        if (index < 0) {
            return null;
        }
        index %= userStylesNamingMe.length;
        return namingMe ? userStylesNamingMe[index] : userStyles[index];
    }

    public IrcUserStyler getUserStyler(int index) {
        index %= userStylers.length;
        return userStylers[index];
    }

    private Set<String> getWatchedChannels() {
        if (watchedChannels == null) {
            watchedChannels = new TreeSet<String>();
            String str = getString(PreferenceKey.WATCHED_CHANNELS);
            StringTokenizer st = new StringTokenizer(str, "\n\t\r" + WATCHED_OBJECT_DELIMITER);
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                watchedChannels.add(token);
            }
        }
        return watchedChannels;
    }

    private Map<String, Pattern> getWatchedNicks() {
        if (watchedNicks == null) {
            watchedNicks = new TreeMap<String, Pattern>();
            String str = getString(PreferenceKey.WATCHED_NICKS);
            StringTokenizer st = new StringTokenizer(str, "\n\t\r" + WATCHED_OBJECT_DELIMITER);
            while (st.hasMoreTokens()) {
                String nick = st.nextToken();
                watchedNicks.put(nick, Pattern.compile(nick));
            }
        }
        return watchedNicks;
    }

    /**
     * @param data
     * @return
     */
    public boolean isWatched(AbstractIrcChannel channel) {
        return channel != null && getWatchedChannels().contains(channel.getName());
    }

    /**
     * @param items
     */
    private void setCollection(PreferenceKey preferenceKey, Collection<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String n : items) {
            if (sb.length() > 0) {
                sb.append(WATCHED_OBJECT_DELIMITER);
            }
            sb.append(n);
        }
        setString(preferenceKey, sb.toString());
    }

    public void setString(PreferenceKey key, String value) {
        IPreferenceStore store = EirccUi.getDefault().getPreferenceStore();
        store.setValue(key.toString(), value);
        // InstanceScope.INSTANCE.getNode(EirccUi.PLUGIN_ID).put();
    }

    /**
     * @param level
     * @return
     */
    public boolean shouldPlaySoundForMessage(IrcMessage m) {
        switch (m.getNotificationLevel()) {
        case NO_NOTIFICATION:
            return false;
        case UNREAD_MESSAGES:
            return shouldPlaySoundOnMessageInChannel(m.getLog().getChannel());
        case UNREAD_MESSAGES_FROM_A_TRACKED_USER:
            return shouldPlaySoundOnMessageFromNick(m.getNick());
        case ME_NAMED:
            return shouldTrayFlashOnNamingMe();
        default:
            return false;
        }
    }

    /**
     * @param user
     * @return
     */
    public boolean shouldPlaySoundOnMessageFromNick(String nick) {
        if (nick != null) {
            for (Pattern pattern : getWatchedNicks().values()) {
                if (pattern.matcher(nick).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param channel
     * @return
     */
    private boolean shouldPlaySoundOnMessageInChannel(AbstractIrcChannel channel) {
        return getWatchedChannels().contains(channel.getName());
    }

    /**
     * @return
     */
    public boolean shouldPlaySoundOnNamingMe() {
        return true;
    }

    /**
     * @return
     */
    public boolean shouldTrayFlashOnNamingMe() {
        return true;
    }

    /**
     * @return
     */
    public String showAddNickPatternDialog(String initialValue) {
        if (initialValue == null) {
            initialValue = "";
        }
        InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(),
                IrcUiMessages.IrcNotificationsPreferencePage_addNickPatternTitle,
                IrcUiMessages.IrcNotificationsPreferencePage_addNickPatternText, initialValue, PATTERN_VALIDATOR);
        if (dialog.open() == Window.OK) {
            return dialog.getValue();
        }
        return null;
    }


}
