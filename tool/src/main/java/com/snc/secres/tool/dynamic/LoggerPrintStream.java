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

package com.snc.secres.tool.dynamic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.snc.secres.tool.common.logging.InternalPrintStream;

public class LoggerPrintStream extends PrintStream {
    private final InternalPrintStream psLogger;

    public LoggerPrintStream(final Logger logger) throws UnsupportedEncodingException {
        this(logger, Level.INFO);
    }

    public LoggerPrintStream(final Logger logger, final Level level) throws UnsupportedEncodingException {
        this(logger, false, Charset.defaultCharset(), level);
    }

    protected LoggerPrintStream(final Logger logger, final boolean autoFlush, final Charset charset, final Level level)
            throws UnsupportedEncodingException {
        super(new PrintStream(new ByteArrayOutputStream()));
        psLogger = new InternalPrintStream(logger, autoFlush, charset, level);
    }

    @Override
    public LoggerPrintStream append(final char c) {
        psLogger.append(c);
        return this;
    }

    @Override
    public LoggerPrintStream append(final CharSequence csq) {
        psLogger.append(csq);
        return this;
    }

    @Override
    public LoggerPrintStream append(final CharSequence csq, final int start, final int end) {
        psLogger.append(csq, start, end);
        return this;
    }

    @Override
    public boolean checkError() {
        return psLogger.checkError();
    }

    @Override
    public void close() {
        psLogger.close();
    }

    @Override
    public void flush() {
        psLogger.flush();
    }

    @Override
    public LoggerPrintStream format(final Locale l, final String format, final Object... args) {
        psLogger.format(l, format, args);
        return this;
    }

    @Override
    public LoggerPrintStream format(final String format, final Object... args) {
        psLogger.format(format, args);
        return this;
    }

    @Override
    public void print(final boolean b) {
        psLogger.print(b);
    }

    @Override
    public void print(final char c) {
        psLogger.print(c);
    }

    @Override
    public void print(final char[] s) {
        psLogger.print(s);
    }

    @Override
    public void print(final double d) {
        psLogger.print(d);
    }

    @Override
    public void print(final float f) {
        psLogger.print(f);
    }

    @Override
    public void print(final int i) {
        psLogger.print(i);
    }

    @Override
    public void print(final long l) {
        psLogger.print(l);
    }

    @Override
    public void print(final Object obj) {
        psLogger.print(obj);
    }

    @Override
    public void print(final String s) {
        psLogger.print(s);
    }

    @Override
    public LoggerPrintStream printf(final Locale l, final String format, final Object... args) {
        psLogger.printf(l, format, args);
        return this;
    }

    @Override
    public LoggerPrintStream printf(final String format, final Object... args) {
        psLogger.printf(format, args);
        return this;
    }

    @Override
    public void println() {
        psLogger.println();
    }

    @Override
    public void println(final boolean x) {
        psLogger.println(x);
    }

    @Override
    public void println(final char x) {
        psLogger.println(x);
    }

    @Override
    public void println(final char[] x) {
        psLogger.println(x);
    }

    @Override
    public void println(final double x) {
        psLogger.println(x);
    }

    @Override
    public void println(final float x) {
        psLogger.println(x);
    }

    @Override
    public void println(final int x) {
        psLogger.println(x);
    }

    @Override
    public void println(final long x) {
        psLogger.println(x);
    }

    @Override
    public void println(final Object x) {
        psLogger.println(x);
    }

    @Override
    public void println(final String x) {
        psLogger.println(x);
    }

    @Override
    public String toString() {
        return LoggerPrintStream.class.getSimpleName() + psLogger.toString();
    }

    @Override
    public void write(final byte[] b) throws IOException {
        psLogger.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) {
        psLogger.write(b, off, len);
    }

    @Override
    public void write(final int b) {
        psLogger.write(b);
    }
}
