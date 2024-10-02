/*
 * Copyright (c) 2024 ServiceNow, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice (including the next paragraph)
 * shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.snc.secres.tool.common.logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ByteStreamLogger {
    private class ByteBufferInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            ByteStreamLogger.this.buf.flip();
            int result = -1;
            if (ByteStreamLogger.this.buf.limit() > 0) {
                result = ByteStreamLogger.this.buf.get() & 0xFF;
            }
            ByteStreamLogger.this.buf.compact();
            return result;
        }

        @Override
        public int read(final byte[] bytes, final int off, final int len) throws IOException {
            ByteStreamLogger.this.buf.flip();
            int result = -1;
            if (ByteStreamLogger.this.buf.limit() > 0) {
                result = Math.min(len, ByteStreamLogger.this.buf.limit());
                ByteStreamLogger.this.buf.get(bytes, off, result);
            }
            ByteStreamLogger.this.buf.compact();
            return result;
        }
    }

    private static final int BUFFER_SIZE = 1024;
    private final Logger logger;
    private final Level level;
    private final InputStreamReader reader;
    private final char[] msgBuf = new char[BUFFER_SIZE];
    private final StringBuilder msg = new StringBuilder();
    private boolean closed;

    private final ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);

    public ByteStreamLogger(final Logger logger, final Level level, final Charset charset) {
        this.logger = logger;
        this.level = level == null ? logger.getLevel() : level;
        this.reader = new InputStreamReader(new ByteBufferInputStream(), charset == null ? Charset.defaultCharset() : charset);
    }

    public void close() {
        synchronized (this.msg) {
            this.closed = true;
            logEnd();
        }
    }

    private void extractMessages() throws IOException {
        if (this.closed) {
            return;
        }
        int read = this.reader.read(this.msgBuf);
        while (read > 0) {
            int off = 0;
            for (int pos = 0; pos < read; pos++) {
                switch (this.msgBuf[pos]) {
                    case '\r':
                        this.msg.append(this.msgBuf, off, pos - off);
                        off = pos + 1;
                        break;
                    case '\n':
                        this.msg.append(this.msgBuf, off, pos - off);
                        off = pos + 1;
                        log();
                        break;
                }
            }
            this.msg.append(this.msgBuf, off, read - off);
            read = this.reader.read(this.msgBuf);
        }
    }

    private void log() {
        // convert to string now so async loggers work
        this.logger.log(this.level, this.msg.toString());
        this.msg.setLength(0);
    }

    private void logEnd() {
        if (this.msg.length() > 0) {
            log();
        }
    }

    public void put(final byte[] b, final int off, final int len) throws IOException {
        int curOff = off;
        int curLen = len;
        if (curLen >= 0) {
            synchronized (this.msg) {
                while (curLen > this.buf.remaining()) {
                    final int remaining = this.buf.remaining();
                    this.buf.put(b, curOff, remaining);
                    curLen -= remaining;
                    curOff += remaining;
                    extractMessages();
                }
                this.buf.put(b, curOff, curLen);
                extractMessages();
            }
        } else {
            logEnd();
        }
    }

    public void put(final int b) throws IOException {
        if (b >= 0) {
            synchronized (this.msg) {
                this.buf.put((byte) (b & 0xFF));
                extractMessages();
            }
        } else {
            logEnd();
        }
    }
}
