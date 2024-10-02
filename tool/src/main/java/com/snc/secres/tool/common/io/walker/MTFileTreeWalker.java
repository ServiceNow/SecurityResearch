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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MTFileTreeWalker {
	
	protected MTDirectoryWalkTask startTask;
	protected final BlockingQueue<Throwable> errs;

	public MTFileTreeWalker(MTDirectoryWalkTask startTask) {
		Objects.requireNonNull(startTask);
		this.startTask = startTask;
		this.errs = new LinkedBlockingQueue<>();
		startTask.setErrsList(this.errs);
	}
	
	public void walkFileTree() throws Exception {
		ForkJoinPool pool = null;
		try{
			pool = new ForkJoinPool();
			//Starts thread and joins on thread
			//Will only return when all other threads have exited which is when their child threads exit with an exception or normal
			//I.e. all threads that spawn other threads wait for them to exit by join
			pool.invoke(startTask);
		}catch(Throwable t){
			errs.offer(new Exception("Error: Unexpected exception when walking the file tree starting at '" + startTask.getDirectory() + "'.",t));
		}finally{
			try{
				if(pool != null)
					pool.shutdown();
			} catch(Throwable t){
				errs.offer(new Exception("Error: Unexpected exception when trying to shutdown the thread pool for the file tree walk starting at '" 
						+ startTask.getDirectory() + "'.",t));
			}
			try{
				if(pool != null)
					pool.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);//wait until the end of time for all threads to exit
			} catch(Throwable t){
				errs.offer(new Exception("Error: Unexpected exception when trying to wait for all threads to terminate for the file tree walk "
						+ "starting at '" + startTask.getDirectory() + "'.",t));
			}
		}
		
		if(!errs.isEmpty()){
			String msg = null;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try(PrintStream ps = new PrintStream(baos,true,"utf-8")){
				ps.println("Error: Failed to completly walk the file tree starting at '" + startTask.getDirectory()
						+ "' and/or complete all required tasks on the stroll. The following exceptions were thrown in the process:");
				int i = 0;
				for(Throwable t : errs){
					ps.print("Exception ");
					ps.print(i++);
					ps.print(": ");
					t.printStackTrace(ps);
				}
				msg = new String(baos.toByteArray(), StandardCharsets.UTF_8);
			}catch(Throwable t){
				throw new Exception("Error: Something went wrong when combining all exceptions into one exception.",t);
			}
			throw new Exception(msg);
		}
	}
	
}
