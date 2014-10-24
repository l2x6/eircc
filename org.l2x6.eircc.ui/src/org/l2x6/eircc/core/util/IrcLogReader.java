/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.util;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import org.eclipse.jface.text.IDocument;
import org.l2x6.eircc.core.model.PlainIrcMessage;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcLogReader implements Closeable {

    public static class IrcLogChunk {
        private final StringBuilder buffer;

        /**
         * @param buffer
         */
        public IrcLogChunk(StringBuilder buffer) {
            super();
            this.buffer = buffer;
        }

        public String rest() {
            return buffer.toString();
        }

        public String tail(char delimiter) {
            int lastDelim = buffer.lastIndexOf(String.valueOf(delimiter));
            String result = buffer.substring(lastDelim + 1);
            buffer.setLength(lastDelim);
            return result;
        }
    }

    public static class IrcLogReaderException extends Exception {

        /**  */
        private static final long serialVersionUID = -3533362196506404856L;

        /**
         * @param message
         */
        public IrcLogReaderException(String message) {
            super(message);
        }

        /**
         * @param message
         * @param cause
         */
        public IrcLogReaderException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * @param cause
         */
        public IrcLogReaderException(Throwable cause) {
            super(cause);
        }

    }

    public static class PlainIrcMessageIterator implements Iterator<PlainIrcMessage> {
        private final ExceptionHandler exceptionHandler;

        private final IrcLogReader reader;
        /**
         * @param reader
         * @param exceptionHandler
         */
        public PlainIrcMessageIterator(IrcLogReader reader, ExceptionHandler exceptionHandler) {
            super();
            this.reader = reader;
            this.exceptionHandler = exceptionHandler;
        }
        /**
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            try {
                return reader.hasNext();
            } catch (Exception e) {
                exceptionHandler.handle(e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @see java.util.Iterator#next()
         */
        @Override
        public PlainIrcMessage next() {
            try {
                return reader.next();
            } catch (Exception e) {
                exceptionHandler.handle(e);
                throw new RuntimeException(e);
            }
        }

    }

    public static void append(CharSequence token, Appendable out, char delimiter) throws IOException {
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            switch (ch) {
            case '\n':
                out.append("\n ");
                break;
            case '\r':
                /* ignore */
                break;
            default:
                out.append(ch);
                break;
            }
        }
        out.append(delimiter);
    }

    private final CountedPushbackReader in;
    private final boolean isP2pChannel;

    private final String source;

    public IrcLogReader(IDocument document, String source, boolean isP2pChannel) throws UnsupportedEncodingException,
            FileNotFoundException {
        this(new DocumenReader(document), source, isP2pChannel);
    }

    /**
     * @param isP2pChannel
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     *
     */
    public IrcLogReader(InputStream inputStream, String source, boolean isP2pChannel)
            throws UnsupportedEncodingException, FileNotFoundException {
        this(new InputStreamReader(inputStream, "utf-8"), source, isP2pChannel);
    }

    public IrcLogReader(Reader input, String source, boolean isP2pChannel) throws UnsupportedEncodingException,
            FileNotFoundException {
        this.in = new CountedPushbackReader(input, 2);
        this.isP2pChannel = isP2pChannel;
        this.source = source;
    }

    /**
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
    }

    /**
     * @return
     */
    public int getCharCount() {
        return in.getCharCount();
    }

    public int getLineIndex() {
        return in.getLineIndex();
    }

    public String getSource() {
        return source;
    }

    public boolean hasNext() throws IOException {
        int ch = in.read();
        in.unread(ch);
        return ch >= 0;
    }

    public Iterator<PlainIrcMessage> iterator(ExceptionHandler exceptionHandler) {
        return new PlainIrcMessageIterator(this, exceptionHandler);
    }

    public PlainIrcMessage next() throws IrcLogReaderException, IOException {
        if (!hasNext()) {
            throw new IllegalStateException("No more IrcMessages to return.");
        } else {
            return new PlainIrcMessage(this, isP2pChannel);
        }
    }

    public IrcLogChunk readChunk(char delimiter, char multilineMarker) throws IOException {
        StringBuilder result = new StringBuilder();
        int ch;
        while (true) {
            if ((ch = in.read()) >= 0) {
            } else {
                break;
            }
            if (ch == '\n') {
                int ch2 = in.read();
                if (ch2 == multilineMarker) {
                    result.append('\n');
                } else if (ch2 == -1) {
                    break;
                } else {
                    in.unread(ch2);
                    if (ch == delimiter) {
                        break;
                    }
                }
            } else if (ch == delimiter) {
                break;
            } else {
                result.append((char) ch);
            }
        }
        return new IrcLogChunk(result);
    }

    public String readToken(char delimiter, char multilineMarker) throws IOException {
        return readChunk(delimiter, multilineMarker).rest();
    }

}
