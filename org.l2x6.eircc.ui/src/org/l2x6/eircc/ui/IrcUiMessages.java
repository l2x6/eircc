/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui;

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
    public static String Account_Use_SSL;
    public static String Account_Username;
    public static String AccountPage_title;
    public static String AddIrcAccountAction_label;
    private static final String BUNDLE_NAME = "org.l2x6.eircc.ui.IrcUiMessages";//$NON-NLS-1$

    public static String Channel_Connected;
    public static String Channel_Disconnected;
    public static String Channel_You_have_unseen_messages;

    public static String ConnectIrcChannelAction_label;

    public static String Console_Account_Log;

    public static String DisconnectIrcChannelAction_label;
    public static String Eclipse_IRC_Client;

    public static String Error_Input_in_field_x_required;
    public static String IrcAccountsView_serverChannelsLabel_text;
    public static String IrcChannelOutlinePage_Open_Private_Chat;
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

    static {
        NLS.initializeMessages(BUNDLE_NAME, IrcUiMessages.class);
    }

    private IrcUiMessages() {
    }
}
