/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.search.core.text.TextSearchScope;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.l2x6.eircc.core.model.PlainIrcMessage;
import org.l2x6.eircc.core.model.resource.IrcChannelResource;
import org.l2x6.eircc.core.model.resource.IrcLogResource;
import org.l2x6.eircc.core.util.IrcLogReader;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.search.IrcSearchPage.IrcSearchPatternData;
import org.l2x6.eircc.ui.search.IrcSearchPage.TimeSpan;
import org.l2x6.eircc.ui.search.IrcSearchQuery.IrcSearchResultCollector;

/**
 * Adapted from {@code org.eclipse.search.internal.core.text.TextSearchVisitor}
 * as available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSearchVisitor {

    private static final Comparator<IFile> BY_DATE_DESC = new Comparator<IFile>() {
        @Override
        public int compare(IFile o1, IFile o2) {
            return o1.getFullPath().toString().compareTo(o2.getFullPath().toString());
        }
    };
    private final List<String> channels;
    private final IrcSearchResultCollector collector;

    private IFile currentFile;

    private final boolean isFileLevelSearch;
    private String lastChannel;
    private OffsetDateTime lastFileDate;

    private final Matcher matcher;
    private final List<String> nickPrefixes;
    private int numberOfFilesToScan;
    private int numberOfScannedFiles;
    private IProgressMonitor progressMonitor;
    private IrcSearchPatternData searchData;
    private final MultiStatus status;
    private final OffsetDateTime timeStart;

    public IrcSearchVisitor(boolean isFileLevelSearch, IrcSearchResultCollector collector, Pattern searchPattern,
            IrcSearchPatternData searchData) {
        this.collector = collector;
        this.isFileLevelSearch = isFileLevelSearch;
        this.status = new MultiStatus(NewSearchUI.PLUGIN_ID, IStatus.OK, IrcUiMessages.TextSearchEngine_statusMessage,
                null);

        this.matcher = searchPattern.pattern().length() == 0 ? null : searchPattern.matcher(new String());
        this.searchData = searchData;
        if (searchData.timeSpan == null || searchData.timeSpan == TimeSpan.A_ANY_TIME) {
            timeStart = null;
        } else {
            timeStart = searchData.timeSpan.getStart();
        }
        this.nickPrefixes = new ArrayList<String>();
        if (searchData.nickPrefixes != null) {
            StringTokenizer st = new StringTokenizer(searchData.nickPrefixes, " \t\n\r");
            while (st.hasMoreTokens()) {
                this.nickPrefixes.add(st.nextToken().toLowerCase());
            }
        }

        this.channels = new ArrayList<String>();
        if (searchData.channels != null) {
            StringTokenizer st = new StringTokenizer(searchData.channels, " \t\n\r");
            while (st.hasMoreTokens()) {
                channels.add(st.nextToken().toLowerCase());
            }
        }

    }

    /**
     * @return returns a map from IFile to IDocument for all open, dirty editors
     */
    private Map<IFile, IDocument> evalNonFileBufferDocuments() {
        Map<IFile, IDocument> result = new HashMap<IFile, IDocument>();
        IWorkbench workbench = PlatformUI.getWorkbench();
        ;
        IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
        for (int i = 0; i < windows.length; i++) {
            IWorkbenchPage[] pages = windows[i].getPages();
            for (int x = 0; x < pages.length; x++) {
                IEditorReference[] editorRefs = pages[x].getEditorReferences();
                for (int z = 0; z < editorRefs.length; z++) {
                    IEditorPart ep = editorRefs[z].getEditor(false);
                    if (ep instanceof ITextEditor && ep.isDirty()) { // only
                                                                     // dirty
                                                                     // editors
                        evaluateTextEditor(result, ep);
                    }
                }
            }
        }
        return result;
    }

    private void evaluateTextEditor(Map<IFile, IDocument> result, IEditorPart ep) {
        IEditorInput input = ep.getEditorInput();
        if (input instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) input).getFile();
            if (!result.containsKey(file)) { // take the first editor found
                ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
                ITextFileBuffer textFileBuffer = bufferManager
                        .getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
                if (textFileBuffer != null) {
                    // file buffer has precedence
                    result.put(file, textFileBuffer.getDocument());
                } else {
                    // use document provider
                    IDocument document = ((ITextEditor) ep).getDocumentProvider().getDocument(input);
                    if (document != null) {
                        result.put(file, document);
                    }
                }
            }
        }
    }

    private void locateMatches(IFile file, PlainIrcMessage message) throws CoreException {
        if (matcher == null) {
            /* no text pattern accept the whole message */
            boolean res = collector.acceptPatternMatch(file, message, 0, 0);
            if (!res) {
                return; // no further reporting requested
            }
        } else {
            /* text pattern matching */
            matcher.reset(message.getText());
            int k = 0;
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                if (end != start) { // don't report 0-length matches
                    boolean res = collector.acceptPatternMatch(file, message, start, end - start);
                    if (!res) {
                        return; // no further reporting requested
                    }
                }
                if (k++ == 20) {
                    if (progressMonitor.isCanceled()) {
                        throw new OperationCanceledException(IrcUiMessages.TextSearchVisitor_canceled);
                    }
                    k = 0;
                }
            }
        }
    }

    /**
     * @param file
     * @return
     */
    private boolean matchesChannelsAndTime(IFile file) {
        if (channels.isEmpty()) {
            return true;
        }
        IContainer logsFolder = file.getParent();
        String channel = null;
        try {
            if (IrcChannelResource.isChannelLogsFolder(logsFolder)) {
                channel = IrcChannelResource.getChannelName((IFolder) logsFolder);
                if (channel == null) {
                    return false;
                }
                channel = channel.toLowerCase();
                for (String expectedChannel : channels) {
                    if (channel.equals(expectedChannel)) {
                        boolean result = matchesTime(file, channel, lastChannel);
                        return result;
                    }
                }
            }
        } finally {
            this.lastChannel = channel;
        }

        return false;
    }

    /**
     * @return
     */
    private boolean matchesFrom(PlainIrcMessage message) {
        if (nickPrefixes.isEmpty()) {
            return true;
        }
        String nick = message.getNick();
        if (nick == null) {
            return false;
        }
        nick = nick.toLowerCase();
        for (String nickPrefix : nickPrefixes) {
            if (nick.startsWith(nickPrefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesTime(IFile file, String channel, String lastChannel) {
        if (timeStart == null) {
            return true;
        } else {
            OffsetDateTime fileDate = IrcLogResource.getTime(file.getFullPath());
            if (!channel.equals(lastChannel)) {
                lastFileDate = null;
            }
            boolean result = lastFileDate == null || lastFileDate.isAfter(timeStart);
            lastFileDate = fileDate;
            return result;
        }
    }

    /**
     * @param message
     * @return
     */
    private boolean matchesTime(PlainIrcMessage message) {
        if (timeStart == null) {
            return true;
        }
        return !timeStart.isAfter(message.getArrivedAt());
    }

    public boolean processFile(IFile file, Map<IFile, IDocument> documentsInEditors) {
        if (matchesChannelsAndTime(file)) {
            if (isFileLevelSearch) {
                collector.acceptFile(file);
            } else {
                IDocument document = documentsInEditors.get(file);
                IrcLogReader reader = null;
                try {
                    if (document != null) {
                        reader = new IrcLogReader(document, file.toString(), IrcChannelResource.isP2pChannel(file.getFullPath()));
                    } else {
                        reader = new IrcLogReader(file.getContents(), file.toString(), IrcChannelResource.isP2pChannel(file
                                .getFullPath()));
                    }
                    while (reader.hasNext()) {
                        PlainIrcMessage message = reader.next();
                        if ((!searchData.ignoreSystemMessages || !message.isSystemMessage())
                                && (!searchData.ignoreMessagesFromMe || !message.isFromMe()) && matchesFrom(message)
                                && matchesTime(message)) {
                            locateMatches(file, message);
                        }
                    }
                } catch (IOException | CoreException e1) {
                    EirccUi.log(e1);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            EirccUi.log(e);
                        }
                    }
                }
            }
        }
        numberOfScannedFiles++;
        if (progressMonitor.isCanceled())
            throw new OperationCanceledException(IrcUiMessages.TextSearchVisitor_canceled);

        return true;
    }

    private void processFiles(IFile[] files) {

        Arrays.sort(files, BY_DATE_DESC);

        final Map<IFile, IDocument> documentsInEditors;
        if (PlatformUI.isWorkbenchRunning())
            documentsInEditors = evalNonFileBufferDocuments();
        else
            documentsInEditors = Collections.emptyMap();

        for (int i = 0; i < files.length; i++) {
            currentFile = files[i];
            boolean res = processFile(currentFile, documentsInEditors);
            if (!res)
                break;
        }
    }

    public IStatus search(IFile[] files, IProgressMonitor monitor) {
        progressMonitor = monitor == null ? new NullProgressMonitor() : monitor;
        numberOfScannedFiles = 0;
        numberOfFilesToScan = files.length;
        currentFile = null;

        Job monitorUpdateJob = new Job(IrcUiMessages.TextSearchVisitor_progress_updating_job) {
            private int fLastNumberOfScannedFiles = 0;

            public IStatus run(IProgressMonitor inner) {
                while (!inner.isCanceled()) {
                    IFile file = currentFile;
                    if (file != null) {
                        String fileName = file.getName();
                        Object[] args = { fileName, new Integer(numberOfScannedFiles), new Integer(numberOfFilesToScan) };
                        progressMonitor.subTask(MessageFormat.format(IrcUiMessages.TextSearchVisitor_scanning, args));
                        int steps = numberOfScannedFiles - fLastNumberOfScannedFiles;
                        progressMonitor.worked(steps);
                        fLastNumberOfScannedFiles += steps;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        return Status.OK_STATUS;
                    }
                }
                return Status.OK_STATUS;
            }
        };

        try {
            String taskName = matcher == null ? IrcUiMessages.TextSearchVisitor_filesearch_task_label : MessageFormat
                    .format(IrcUiMessages.TextSearchVisitor_textsearch_task_label, matcher.pattern().pattern());
            progressMonitor.beginTask(taskName, numberOfFilesToScan);
            monitorUpdateJob.setSystem(true);
            monitorUpdateJob.schedule();
            try {
                collector.beginReporting();
                processFiles(files);
                return status;
            } finally {
                monitorUpdateJob.cancel();
            }
        } finally {
            progressMonitor.done();
            collector.endReporting();
        }
    }

    public IStatus search(TextSearchScope scope, IProgressMonitor monitor) {
        return search(scope.evaluateFilesInScope(status), monitor);
    }

}
