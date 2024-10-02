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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

import com.snc.secres.tool.common.io.FileHelpers;

public class MTDeleteDirectoryTask extends MTDirectoryWalkTask {

	private static final long serialVersionUID = 4807539011833596678L;

	public MTDeleteDirectoryTask(Path dir) {
		super(dir);
	}
	
	public MTDeleteDirectoryTask(Path dir, MTDeleteDirectoryTask org){
		super(dir, org);
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Objects.requireNonNull(file);
		Objects.requireNonNull(attrs);
		MTDeleteFileVisitTask a = new MTDeleteFileVisitTask(file, attrs, errs);
		a.fork();
		actions.add(a);
		return FileVisitResult.CONTINUE;
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
		}else{
			errs.offer(new Exception("Error: Could not delete non-empty directory '" + dir + "'."));
		}
	}
	
	@Override
	public MTDeleteDirectoryTask getNewThreadedDirectoryWalk(Path dir){
		Objects.requireNonNull(dir);
		return new MTDeleteDirectoryTask(dir,this);
	}
	
	public static class MTDeleteFileVisitTask extends MTFileVisitTask {
		
		private static final long serialVersionUID = -2116280147815325621L;

		public MTDeleteFileVisitTask(Path file, BasicFileAttributes attrs, BlockingQueue<Throwable> errs) {
			super(file, attrs, errs);
		}

		@Override
		protected void computeInner() throws Exception {
			FileHelpers.verifyRWFileExists(file);
			Files.delete(file);
		}
		
	}

}
