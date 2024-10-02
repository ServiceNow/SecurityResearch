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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FileHash {

	private String hashName;
	private String hash;
	private String path;
	private byte[] hashBytes;

	private static Map<Path,FileHash> fullPathToFileHash = new HashMap<>();
	
	private FileHash(){
		this(null,null,null,null);
	}
	
	private FileHash(String hashName, String hash, byte[] hashBytes, String path) {
		this.hashName = hashName;
		this.hash = hash;
		this.path = path;
		this.hashBytes = hashBytes;
	}
	
	private Object readResolve(){
		hashBytes = FileHelpers.hexToBytes(hash);
		return this;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode(hash);
		result = prime * result + Objects.hashCode(hashName);
		result = prime * result + Objects.hashCode(path);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !(obj instanceof FileHash))
			return false;
		
		FileHash other = (FileHash) obj;
		return Objects.equals(hashName, other.hashName) 
				&& Objects.equals(hash, other.hash) 
				&& Objects.equals(path, other.path);
	}

	@Override
	public String toString() {
		return "Path=" + path + " --> Type=" + hashName + " Hash=" + hash;
	}
	
	/**
	 * @return the hashName
	 */
	public String getType() {
		return hashName;
	}

	/**
	 * @return the hash
	 */
	public String getHash() {
		return hash;
	}

	/**
	 * @return the path
	 */
	public String getPathString() {
		return path;
	}
	
	public Path getPath() {
		return Paths.get(path);
	}
	
	public Path getFullPath(Path rootPath) {
		return FileHelpers.getPath(rootPath, getPath());
	}

	/**
	 * @return the hashBytes
	 */
	public byte[] getHashBytes() {
		return hashBytes;
	}
	
	public boolean compareHash(FileHash other){
		return hashName != null && other.hashName != null && hash != null && 
				other.hash != null && Objects.equals(hashName,other.hashName) && Objects.equals(hash,other.hash);
	}
	
	public static void resetFileHashRecord(){
		synchronized(fullPathToFileHash) {
			fullPathToFileHash.clear();
		}
	}
	
	public static void removeFileHashRecord(Path fullFilePath) {
		synchronized (fullPathToFileHash) {
			fullPathToFileHash.remove(fullFilePath);
		}
	}
	
	public static FileHash genFileHash(String hashName, Path fullFilePath, Path realtiveFilePath) throws Exception {
		if(realtiveFilePath == null){
			realtiveFilePath = fullFilePath;
		}
		
		synchronized (fullPathToFileHash) {
			FileHash ret = fullPathToFileHash.get(fullFilePath);
			if(ret != null) {
				if(ret.getPathString().equals(realtiveFilePath.toString())) {
					return ret;
				} else {
					return new FileHash(ret.getType(),ret.getHash(),ret.getHashBytes(),realtiveFilePath.toString());
				}
			}
			
			if(Files.exists(fullFilePath) && Files.isRegularFile(fullFilePath) && Files.isReadable(fullFilePath)) {
				
				byte[] hash = FileHelpers.getHashOfFile(hashName, fullFilePath);
				
				ret = new FileHash(hashName,FileHelpers.bytesToHex(hash),hash,realtiveFilePath.toString());
				fullPathToFileHash.put(fullFilePath, ret);
				return ret;
			}else {
				throw new Exception("Error: The file '" + fullFilePath.toString() + 
						"' does not exist, is not a file, or is not readable.");
			}
		}
	}

}
