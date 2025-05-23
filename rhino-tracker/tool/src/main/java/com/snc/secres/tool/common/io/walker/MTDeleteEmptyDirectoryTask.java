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

package com.snc.secres.tool.common.io.walker;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.snc.secres.tool.common.io.FileHelpers;

public class MTDeleteEmptyDirectoryTask extends MTDirectoryWalkTask {

	private static final long serialVersionUID = -4505288071221569612L;

	public MTDeleteEmptyDirectoryTask(Path dir) {
		super(dir);
	}
	
	public MTDeleteEmptyDirectoryTask(Path dir, MTDeleteEmptyDirectoryTask org){
		super(dir, org);
	}
	
	@Override
	public void finalAction(Path dir) throws IOException {
		FileHelpers.verifyRWDirectoryExists(dir);
		boolean isEmpty;
		try(DirectoryStream<Path> ds = Files.newDirectoryStream(dir);){
			isEmpty = !ds.iterator().hasNext();
		}
		if(isEmpty){
			Files.delete(dir);
		}
	}
	
	@Override
	public MTDeleteEmptyDirectoryTask getNewThreadedDirectoryWalk(Path dir){
		Objects.requireNonNull(dir);
		return new MTDeleteEmptyDirectoryTask(dir,this);
	}

}
