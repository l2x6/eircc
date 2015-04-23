/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.views;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.l2x6.eircc.core.client.TrafficLoggerFactory;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCExceptionHandler;
import org.schwering.irc.lib.IRCTrafficLogger;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcConsole implements TrafficLoggerFactory {
    public static class ConsoleTrafficLogger implements IRCTrafficLogger, IRCExceptionHandler {
        private MessageConsole console;
        private final MessageConsoleStream consoleStream;

        /**
         * @param consoleStream
         */
        public ConsoleTrafficLogger(MessageConsole console) {
            super();
            this.consoleStream = console.newMessageStream();
            this.console = console;
        }

        /**
         * @see org.schwering.irc.lib.IRCExceptionHandler#exception(org.schwering.irc.lib.IRCConnection, java.lang.Throwable)
         */
        @Override
        public void exception(IRCConnection arg0, Throwable e) {
            try {
                reveal();
                StringWriter w = new StringWriter();
                PrintWriter pw = new PrintWriter(w);
                e.printStackTrace(pw);
                pw.flush();
                consoleStream.write(w.toString());
            } catch (IOException ignored) {
            }
        }

        /**
         * @see org.schwering.irc.lib.TrafficLogger#in(java.lang.String)
         */
        @Override
        public void in(String line) {
            reveal();
            consoleStream.println("> " + line);
        }

        /**
         * @see org.schwering.irc.lib.TrafficLogger#out(java.lang.String)
         */
        @Override
        public void out(String line) {
            reveal();
            consoleStream.println("< " + line);
        }

        public void reveal() {
            if (Display.getCurrent() == null) {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        uiReveal();
                    }
                });
            } else {
                uiReveal();
            }
        }

        public void uiReveal() {
            IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (win != null) {
                IWorkbenchPage page = win.getActivePage();
                if (page != null) {
                    try {
                        IConsoleView view = (IConsoleView) page.findView(IConsoleConstants.ID_CONSOLE_VIEW);
                        if (view == null) {
                            /*
                             * this steals the focus so let us use it as little
                             * as possible
                             */
                            view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
                            view.display(console);
                        }
                    } catch (PartInitException e) {
                        EirccUi.log(e);
                    }
                }
            }
        }
    }

    private static final IrcConsole INSTANCE = new IrcConsole();

    public static IrcConsole getInstance() {
        return INSTANCE;
    }

    /**
     * @param account
     * @return
     */
    private static String getLabel(IrcAccount account) {
        return account.getLabel() + IrcUiMessages.Console_Account_Log;
    }

    private final Map<String, ConsoleTrafficLogger> loggerMap = new HashMap<String, ConsoleTrafficLogger>();

    /**
     * @see org.l2x6.eircc.core.client.TrafficLoggerFactory#getExceptionHandler(org.l2x6.eircc.core.model.IrcAccount)
     */
    @Override
    public IRCExceptionHandler getExceptionHandler(IrcAccount account) {
        synchronized (this.loggerMap) {
            ConsoleTrafficLogger result = this.loggerMap.get(account.getName());
            if (result == null) {
                MessageConsole console = getOrCreateConsole(getLabel(account));
                result = new ConsoleTrafficLogger(console);
                this.loggerMap.put(account.getName(), result);
            }
            return result;
        }
    }

    private MessageConsole getOrCreateConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++)
            if (name.equals(existing[i].getName()))
                return (MessageConsole) existing[i];
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[] { myConsole });
        return myConsole;
    }

    /**
     * @see org.l2x6.eircc.core.client.TrafficLoggerFactory#getTrafficLogger(org.l2x6.eircc.core.model.IrcAccount)
     */
    @Override
    public IRCTrafficLogger getTrafficLogger(IrcAccount account) {
        synchronized (this.loggerMap) {
            ConsoleTrafficLogger result = this.loggerMap.get(account.getName());
            if (result == null) {
                MessageConsole console = getOrCreateConsole(getLabel(account));
                result = new ConsoleTrafficLogger(console);
                this.loggerMap.put(account.getName(), result);
            }
            return result;
        }
    }

}
