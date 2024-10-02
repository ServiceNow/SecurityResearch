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

import net.bytebuddy.implementation.bind.annotation.AllArguments;

public class SecurityManagerInterceptor {
    
    /*@Advice.OnMethodEnter()
    public static void setSecurityManager(SecurityManager s) {
        System.err.println("Trying to register SM: " + s.getClass().getName());
        // To get the method to no-op using advice you need to throw an exception as a return statement appears to be ignored here
        // Throwing the exception still allows the instance to start
        throw new RuntimeException("No setty the security manager");
    }*/

    public static void interceptSetSecurityManager(@AllArguments Object[] allArguments) {
        //No-op
        //Don't trying to run anything here as it could cause issues
        //If you want to run code use Advice like above
        //If you want to run your custom code you may need to edit the ClassInjector line in the Agent class to
        //add the additional classes
    }
    
}
