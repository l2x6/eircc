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
import java.time.temporal.TemporalAmount;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
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
import org.eclipse.swt.widgets.Display;
import org.l2x6.eircc.core.model.PlainIrcMessage;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.editor.IrcDefaultMessageFormatter;
import org.l2x6.eircc.ui.editor.IrcSearchMessageFormatter;
import org.l2x6.eircc.ui.editor.IrcSystemMessageFormatter;
import org.l2x6.eircc.ui.misc.Colors;
import org.l2x6.eircc.ui.misc.ExtendedTextStyle;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcPreferences {

    /**
     * Preference keys and their default values.
     *
     * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
     *
     */
    public enum PreferenceKey {

        TRACKED_NICKS("tracked.nicks", ""); //$NON-NLS-1$

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

    private static final int DEFAULT_EDITOR_LOOK_BACK_LINE_LIMIT = 512;

    private static final TemporalAmount DEFAULT_EDITOR_LOOK_BACK_TIME_SPAN = Duration.ofHours(24);
    private static final Duration DEFAULT_PING_INTERVAL = Duration.ofMinutes(1);
    private static final IrcPreferences INSTANCE = new IrcPreferences();

    public static final char NICKS_DELIMITER = ' ';

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

    public static IrcPreferences getInstance() {
        return INSTANCE;
    }

    private final IrcDefaultMessageFormatter defaultFormatter = new IrcDefaultMessageFormatter(this);

    private final ExtendedTextStyle messageTimeStyle;

    private IrcSearchMessageFormatter searchFormatter = new IrcSearchMessageFormatter(this);
    private final IrcSystemMessageFormatter systemFormatter = new IrcSystemMessageFormatter(this);

    private final ExtendedTextStyle systemMessageStyle;
    private Map<String, Pattern> trackedNicks;
    private final IrcUserStyler[] userStylers;

    private final ExtendedTextStyle[] userStyles;

    private final ExtendedTextStyle[] userStylesNamingMe;

    /**
     *
     */
    public IrcPreferences() {
        super();
        systemMessageStyle = new ExtendedTextStyle(Display.getDefault().getSystemColor(
                SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
        messageTimeStyle = new ExtendedTextStyle(Display.getDefault().getSystemColor(
                SWT.COLOR_TITLE_INACTIVE_FOREGROUND));

        Colors colors = Colors.getInstance();
        ColorRegistry reg = JFaceResources.getColorRegistry();
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

    public void addTrackedNickPattern(String nick) {
        getTrackedNicks().put(nick, Pattern.compile(nick));

        StringBuilder sb = new StringBuilder();
        for (String n : trackedNicks.keySet()) {
            if (sb.length() > 0) {
                sb.append(NICKS_DELIMITER);
            }
            sb.append(n);
        }
        setString(PreferenceKey.TRACKED_NICKS, sb.toString());
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

    public int getEditorLookBackLineLimit() {
        return DEFAULT_EDITOR_LOOK_BACK_LINE_LIMIT;
    }

    public TemporalAmount getEditorLookBackTimeSpan() {
        return DEFAULT_EDITOR_LOOK_BACK_TIME_SPAN;
    }

    /**
     * @param m
     * @return
     */
    public IrcDefaultMessageFormatter getFormatter(PlainIrcMessage m) {
        if (m.isSystemMessage()) {
            return systemFormatter;
        }
        return defaultFormatter;
    }

    public ExtendedTextStyle getMessageTimeStyle() {
        return messageTimeStyle;
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

    private Map<String, Pattern> getTrackedNicks() {
        if (trackedNicks == null) {
            trackedNicks = new TreeMap<String, Pattern>();
            String str = getString(PreferenceKey.TRACKED_NICKS);
            StringTokenizer st = new StringTokenizer(str, "\n\t\r" + NICKS_DELIMITER);
            while (st.hasMoreTokens()) {
                String nick = st.nextToken();
                trackedNicks.put(nick, Pattern.compile(nick));
            }
        }
        return trackedNicks;
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

    public void setString(PreferenceKey key, String value) {
        IPreferenceStore store = EirccUi.getDefault().getPreferenceStore();
        store.setValue(key.toString(), value);
        // InstanceScope.INSTANCE.getNode(EirccUi.PLUGIN_ID).put();
    }

    /**
     * @param user
     * @return
     */
    public boolean shouldPlaySoundOnMessageFromNick(String nick) {
        if (nick != null) {
            for (Pattern pattern : getTrackedNicks().values()) {
                if (pattern.matcher(nick).matches()) {
                    return true;
                }
            }
        }
        return false;
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
