/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui;

import java.util.ResourceBundle;

import org.eclipse.osgi.util.NLS;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcUiMessages extends NLS {

    public static String Account;
    public static String Account_Connect_Automatically;

    public static String Account_Created_on;

    public static String Account_Host;

    public static String Account_Label;

    public static String Account_Name;
    public static String Account_Nick;
    public static String Account_Password;
    public static String Account_Port;
    public static String Account_Quit_Message;
    public static String Account_SOCKS_Proxy_Host;
    public static String Account_SOCKS_Proxy_Port;
    public static String Account_Use_SSL;
    public static String Account_Username;
    public static String AccountPage_title;
    public static String AddIrcAccountAction_label;
    private static final String BUNDLE_FOR_CONSTRUCTED_KEYS = "org.eclipse.ui.texteditor.ConstructedEditorMessages";//$NON-NLS-1$
    private static final String BUNDLE_NAME = "org.l2x6.eircc.ui.IrcUiMessages";//$NON-NLS-1$

    public static String Channel_Connected;
    public static String Channel_Disconnected;
    public static String Channel_You_have_unseen_messages;

    public static String ConnectIrcChannelAction_label;

    public static String Console_Account_Log;

    public static String DisconnectIrcChannelAction_label;
    public static String Eclipse_IRC_Client;

    public static String Editor_statusline_error_label;
    public static String Editor_statusline_position_pattern;
    public static String Editor_statusline_state_readonly_label;
    public static String Error_Input_in_field_x_required;

    private static ResourceBundle fgBundleForConstructedKeys = ResourceBundle.getBundle(BUNDLE_FOR_CONSTRUCTED_KEYS);
    public static String FileLabelProvider_count_format;
    public static String FileLabelProvider_line_number;

    public static String FileLabelProvider_removed_resource_label;
    public static String FileSearchPage_limited_format_files;
    public static String FileSearchPage_limited_format_matches;
    public static String FileSearchPage_open_file_dialog_title;
    public static String FileSearchPage_open_file_failed;
    public static String FileSearchPage_sort_by_label;
    public static String FileSearchPage_sort_name_label;

    public static String FileSearchPage_sort_path_label;

    public static String FileSearchQuery_channels;
    public static String FileSearchQuery_from;
    public static String FileSearchQuery_pluralPattern;
    public static String FileSearchQuery_pluralPattern_fileNameSearch;
    public static String FileSearchQuery_pluralPatternWithFileExt;

    public static String FileSearchQuery_singularLabel;
    public static String FileSearchQuery_singularLabel_fileNameSearch;
    public static String FileSearchQuery_singularPatternWithFileExt;
    public static String FileSearchQuery_time;
    public static String IrcAccountsView_serverChannelsLabel_text;
    public static String IrcChannelOutlinePage_watchThisUser;
    public static String IrcChannelOutlinePage_watchThisChannel;

    public static String IrcChannelOutlinePage_Open_Private_Chat;
    public static String IrcChannelsSelector_delimiter;
    public static String IrcClient_commandExecutionException;
    public static String IrcClient_commandTimeOut;
    public static String IrcEditor_File_x_does_not_exist;
    public static String IrcNotificationsPreferencePage_addNickPatternText;

    public static String IrcNotificationsPreferencePage_addNickPatternTitle;
    public static String IrcNotificationsPreferencePage_cannotBeEmpty;
    public static String IrcNotificationsPreferencePage_invalidPattern;
    public static String IrcNotificationsPreferencePage_senderBasedNotification;

    public static String IrcSearchPage_anyTime;
    public static String IrcSearchPage_caseSensitive;
    public static String IrcSearchPage_channelNamePatterns_hint;
    public static String IrcSearchPage_containingText_hint;
    public static String IrcSearchPage_containingText_text;
    public static String IrcSearchPage_custom;
    public static String IrcSearchPage_fromNick;
    public static String IrcSearchPage_fromNickAssist;
    public static String IrcSearchPage_ignore;

    public static String IrcSearchPage_ignoreMessagesFromMe;
    public static String IrcSearchPage_ignoreSystemMessages;
    public static String IrcSearchPage_inChannels;
    public static String IrcSearchPage_inChannelsAssist;
    public static String IrcSearchPage_lastMonth;
    public static String IrcSearchPage_lastWeek;
    public static String IrcSearchPage_lastYear;
    public static String IrcSearchPage_messageTime;

    public static String IrcSearchPage_regularExpression;

    public static String IrcSearchPage_Search_in_Channels_label;

    public static String IrcSearchPage_searchproblems_message;

    public static String IrcSearchPage_searchproblems_title;

    public static String IrcSearchPage_Select_button;

    public static String IrcSearchPage_today;

    public static String IrcSearchPage_wholeWord;

    public static String IrcSearchQuery_label;

    public static String IrcServersView_empty;

    public static String JoinIrcChannelAction_label;

    public static String LeaveIrcChannelAction_label;

    public static String ListChannelsAction_label;

    public static String Message_x_is_known_as_y;
    public static String Message_x_joined;
    public static String Message_x_left;
    public static String Message_x_left_with_message;
    public static String Message_You_are_known_as_x;
    public static String Message_You_joined_as_nick;
    public static String Message_You_left;
    public static String OpenWithMenu_label;
    public static String PromptAndJoinChannelAction_label;
    public static String PromptAndJoinChannelAction_shouldStartWithHash;
    public static String TextSearchEngine_statusMessage;
    public static String TextSearchVisitor_canceled;
    public static String TextSearchVisitor_filesearch_task_label;
    public static String TextSearchVisitor_progress_updating_job;
    public static String TextSearchVisitor_scanning;
    public static String TextSearchVisitor_textsearch_task_label;
    public static String IrcNotificationsPreferencePage_watchedChannels;
    static {
        NLS.initializeMessages(BUNDLE_NAME, IrcUiMessages.class);
    }

    /**
     * Returns the message bundle which contains constructed keys.
     *
     * @since 3.1
     * @return the message bundle
     */
    public static ResourceBundle getBundleForConstructedKeys() {
        return fgBundleForConstructedKeys;
    }

    private IrcUiMessages() {
    }
}
