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

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.snc.secres.tool.common.SortingMethods;
import com.snc.secres.tool.common.io.FileHelpers;

public class MTFindTask extends MTDirectoryWalkTask {

	private static final long serialVersionUID = -3492714328167934251L;
	protected final BlockingQueue<Path> results;
	protected final PathMatcher nameMatcher;
	protected final PathMatcher pathMatcher;
	protected final boolean allowDir;
	protected final boolean allowFile;
	protected final PathMatcher pruneNameMatcher;
	protected final PathMatcher prunePathMatcher;
	protected final boolean enablePrune;

	public MTFindTask(Path startDir, String findName, String findPath, String type, String pruneName, String prunePath) {
		super(startDir);
		this.results = new LinkedBlockingQueue<>();
		nameMatcher = (findName == null) ? null : startDir.getFileSystem().getPathMatcher("glob:"+findName);
		pathMatcher = (findPath == null) ? null : startDir.getFileSystem().getPathMatcher("glob:"+findPath);
		allowDir = type == null ? true : type.contains("d");
		allowFile = type == null ? true : type.contains("f");
		pruneNameMatcher = (pruneName == null) ? null : startDir.getFileSystem().getPathMatcher("glob:"+pruneName);
		prunePathMatcher = (prunePath == null) ? null : startDir.getFileSystem().getPathMatcher("glob:"+prunePath);
		enablePrune = pruneName != null || prunePath != null;
	}
	
	public MTFindTask(Path dir, MTFindTask org){
		super(dir, org);
		this.results = org.results;
		this.nameMatcher = org.nameMatcher;
		this.pathMatcher = org.pathMatcher;
		this.allowDir = org.allowDir;
		this.allowFile = org.allowFile;
		this.pruneNameMatcher = org.pruneNameMatcher;
		this.prunePathMatcher = org.prunePathMatcher;
		this.enablePrune = org.enablePrune;
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
		Objects.requireNonNull(file);
		Objects.requireNonNull(attrs);
		if(allowFile && !(enablePrune && find(file,prunePathMatcher,pruneNameMatcher) != null)){
			Path p = find(file,pathMatcher,nameMatcher);
			if(p != null) results.offer(p);
		}
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult preVisitSameDirectory(Path dir, BasicFileAttributes attrs) {
		Objects.requireNonNull(dir);
		Objects.requireNonNull(attrs);
		if(enablePrune && find(dir,prunePathMatcher,pruneNameMatcher) != null)
			return FileVisitResult.SKIP_SUBTREE; 
		if(allowDir){
			Path p = find(dir,pathMatcher,nameMatcher);
			if(p != null) results.add(p);
		}
		return FileVisitResult.CONTINUE;
	}
	
	private static Path find(Path p, PathMatcher pathMatcher, PathMatcher nameMatcher) {
		p = FileHelpers.getNormAndAbsPath(p);
		if(p == null) return null;
		if(pathMatcher != null){
			if(pathMatcher.matches(p)){
				if(nameMatcher == null){
					return p;
				}
			}else{
				return null;
			}
		}
		if(nameMatcher != null){
			Path name = p.getFileName();
			if(name != null && nameMatcher.matches(name))
				return p;
		}
		return null;
	}
	
	@Override
	public MTFindTask getNewThreadedDirectoryWalk(Path dir){
		Objects.requireNonNull(dir);
		return new MTFindTask(dir,this);
	}
	
	public Set<Path> getResults(){
		Set<Path> ret = new HashSet<>();
		for(Path p : results){
			ret.add(FileHelpers.getNormAndAbsPath(p));
		}
		ret = SortingMethods.sortSet(ret);
		return ret;
	}

}
