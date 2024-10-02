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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class FileHashList implements List<FileHash> {

	private List<FileHash> list;
	
	public FileHashList(List<FileHash> list) {
		if(list == null)
			throw new IllegalArgumentException("Error: The list cannot be null.");
		this.list = list;
	}

	@Override public int size() { return list.size(); }
	@Override public boolean isEmpty() { return list.isEmpty(); }
	@Override public boolean contains(Object o) { return list.contains(o); }
	@Override public Iterator<FileHash> iterator() { return list.iterator(); }
	@Override public Object[] toArray() { return list.toArray(); }
	@Override public <T> T[] toArray(T[] a) { return list.toArray(a); }
	@Override public boolean add(FileHash e) { return list.add(e); }
	@Override public boolean remove(Object o) { return list.remove(o); }
	@Override public boolean containsAll(Collection<?> c) { return list.containsAll(c); }
	@Override public boolean addAll(Collection<? extends FileHash> c) { return list.addAll(c); }
	@Override public boolean addAll(int index, Collection<? extends FileHash> c) { return list.addAll(index, c); }
	@Override public boolean removeAll(Collection<?> c) { return list.removeAll(c); }
	@Override public boolean retainAll(Collection<?> c) { return list.retainAll(c); }
	@Override public void clear() { list.clear(); }
	@Override public FileHash get(int index) { return list.get(index); }
	@Override public FileHash set(int index, FileHash element) { return list.set(index, element); }
	@Override public void add(int index, FileHash element) { list.add(index, element); }
	@Override public FileHash remove(int index) { return list.remove(index); }
	@Override public int indexOf(Object o) { return list.indexOf(o); }
	@Override public int lastIndexOf(Object o) { return list.lastIndexOf(o); }
	@Override public ListIterator<FileHash> listIterator() { return list.listIterator(); }
	@Override public ListIterator<FileHash> listIterator(int index) { return list.listIterator(index); }
	@Override public List<FileHash> subList(int fromIndex, int toIndex) { return list.subList(fromIndex, toIndex); }
	@Override public boolean equals(Object o) { return list.equals(o); }
	@Override public int hashCode() { return list.hashCode(); }

}
