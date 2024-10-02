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

package com.snc.secres.tool.common.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class PrintStreamUnixEOL extends PrintStream{

	public PrintStreamUnixEOL(String fileName, String csn) throws FileNotFoundException,
			UnsupportedEncodingException {
		super(fileName, csn);
	}

	public PrintStreamUnixEOL(String fileName) throws FileNotFoundException {
		super(fileName);
	}

	public PrintStreamUnixEOL(OutputStream out, boolean autoFlush, String encoding)
			throws UnsupportedEncodingException {
		super(out, autoFlush, encoding);
	}

	public PrintStreamUnixEOL(OutputStream out, boolean autoFlush) {
		super(out, autoFlush);
	}

	public PrintStreamUnixEOL(OutputStream out) {
		super(out);
	}

	public PrintStreamUnixEOL(File file, String csn) throws FileNotFoundException,
			UnsupportedEncodingException {
		super(file, csn);
	}

	public PrintStreamUnixEOL(File file) throws FileNotFoundException {
		super(file);
	}

	private void newLine(){
		write('\n');
	}
	
	@Override
	public void println(){
		newLine();
	}
	
	@Override
	public void println(boolean x) {
        String s = String.valueOf(x) + '\n';
        synchronized (this) {
            print(s);
        }
    }
	
	@Override
	public void println(char x) {
        String s = String.valueOf(x) + '\n';
        synchronized (this) {
            print(s);
        }
    }
	
	@Override
	public void println(int x) {
        String s = String.valueOf(x) + '\n';
        synchronized (this) {
            print(s);
        }
    }
	
	@Override
	public void println(long x) {
        String s = String.valueOf(x) + '\n';
        synchronized (this) {
            print(s);
        }
    }
	
	@Override
	public void println(float x) {
        String s = String.valueOf(x) + '\n';
        synchronized (this) {
            print(s);
        }
    }
	
	@Override
	public void println(double x) {
        String s = String.valueOf(x) + '\n';
        synchronized (this) {
            print(s);
        }
    }
	
	@Override
	public void println(char x[]) {
		String s = String.valueOf(x) + '\n';
        synchronized (this) {
            print(s);
        }
    }
	
	@Override
	public void println(String x) {
        String s = String.valueOf(x) + '\n';
        synchronized (this) {
            print(s);
        }
    }
	
	@Override
	public void println(Object x) {
        String s = String.valueOf(x) + '\n';
        synchronized (this) {
            print(s);
        }
    }
}
