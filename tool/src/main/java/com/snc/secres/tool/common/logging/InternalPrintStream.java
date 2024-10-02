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
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal class used primarily to allow location calculations to work properly.
 *
 * @since 2.12
 */
public class InternalPrintStream extends PrintStream {

    public InternalPrintStream(final Logger logger, final boolean autoFlush, final Charset charset, final Level level) throws UnsupportedEncodingException {
        super(new InternalOutputStream(logger, level, ensureNonNull(charset)), autoFlush, ensureNonNull(charset).name());
    }

    private static Charset ensureNonNull(final Charset charset) {
        return charset == null ? Charset.defaultCharset() : charset;
    }

    @Override
    public InternalPrintStream append(final char c) {
        super.append(c);
        return this;
    }

    @Override
    public InternalPrintStream append(final CharSequence csq) {
        super.append(csq);
        return this;
    }

    @Override
    public InternalPrintStream append(final CharSequence csq, final int start, final int end) {
        super.append(csq, start, end);
        return this;
    }

    @Override
    public boolean checkError() {
        return super.checkError();
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void flush() {
        super.flush();
    }

    @Override
    public InternalPrintStream format(final Locale l, final String format, final Object... args) {
        super.format(l, format, args);
        return this;
    }

    @Override
    public InternalPrintStream format(final String format, final Object... args) {
        super.format(format, args);
        return this;
    }

    @Override
    public void print(final boolean b) {
        super.print(b);
    }

    @Override
    public void print(final char c) {
        super.print(c);
    }

    @Override
    public void print(final char[] s) {
        super.print(s);
    }

    @Override
    public void print(final double d) {
        super.print(d);
    }

    @Override
    public void print(final float f) {
        super.print(f);
    }

    @Override
    public void print(final int i) {
        super.print(i);
    }

    @Override
    public void print(final long l) {
        super.print(l);
    }

    @Override
    public void print(final Object obj) {
        super.print(obj);
    }

    @Override
    public void print(final String s) {
        super.print(s);
    }

    @Override
    public InternalPrintStream printf(final Locale l, final String format, final Object... args) {
        super.printf(l, format, args);
        return this;
    }

    @Override
    public InternalPrintStream printf(final String format, final Object... args) {
        super.printf(format, args);
        return this;
    }

    @Override
    public void println() {
        super.println();
    }

    @Override
    public void println(final boolean x) {
        super.println(x);
    }

    @Override
    public void println(final char x) {
        super.println(x);
    }

    @Override
    public void println(final char[] x) {
        super.println(x);
    }

    @Override
    public void println(final double x) {
        super.println(x);
    }

    @Override
    public void println(final float x) {
        super.println(x);
    }

    @Override
    public void println(final int x) {
        super.println(x);
    }

    @Override
    public void println(final long x) {
        super.println(x);
    }

    @Override
    public void println(final Object x) {
        super.println(x);
    }

    @Override
    public void println(final String x) {
        super.println(x);
    }

    @Override
    public String toString() {
        return "{stream=" + this.out + '}';
    }

    @Override
    public void write(final byte[] b) throws IOException {
        super.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) {
        super.write(b, off, len);
    }

    @Override
    public void write(final int b) {
        super.write(b);
    }
}
