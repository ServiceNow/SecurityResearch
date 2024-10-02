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

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

import com.snc.secres.tool.common.io.PrintStreamUnixEOL;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

public class JavaMethodInterceptor {

    private static void writeToFile(String result, Object cx /* Context cx */) throws IOException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Path _capture_path = null;
        if(cx != null) {
            Field f = cx.getClass().getDeclaredField("_capture_path");
            f.setAccessible(true);
            _capture_path = (Path)f.get(cx);
        }
        if(_capture_path == null) {
            _capture_path = Tools.classNameToCapturePath(Agent.getActiveAgent().getOutDirPath(), Agent.getActiveAgent().getTimeStamp(), "javascript.NoNameGiven");
        }
        try(PrintStreamUnixEOL out = new PrintStreamUnixEOL(Files.newOutputStream(_capture_path,  StandardOpenOption.CREATE,  StandardOpenOption.APPEND))) {
            out.println(result);
        }
    }

    /*private static void logStack(String result) {
        Exception e = new RuntimeException("Foo bar");
        StackTraceElement[] elements = e.getStackTrace();
        String dump = Stream.of(elements).map(elm -> elm.toString()).collect(Collectors.joining("\n  ", result + "\n", "\n"));
        Logging.warn("Stack dump for method call:\n" + dump);
    }*/

    public static Object interceptFunctionObject(@AllArguments Object[] allArguments,
                                    @SuperCall Callable<Object> callableMethod, 
                                    @This Object thiz) throws Exception {
        if(testCaptureEnabled(allArguments[0])) {
            try {
                Field f = thiz.getClass().getDeclaredField("member");
                f.setAccessible(true);
                Object ctor = f.get(thiz); //MemberBox

                String result = Tools.memberBoxToString(ctor);

                JavaMethodInterceptor.writeToFile(result, allArguments[0]);
            } catch(Throwable e) {
                Logging.error("Exception in FunctionObject intercept.", e);
            }

            // Prevent call tracing after a js -> java call is made
            boolean preValue = setCapture(allArguments[0], false);
            Object ret = callableMethod.call();
            setCapture(allArguments[0], preValue);
            return ret;
        } else {
            return callableMethod.call();
        }
    }

    public static Object interceptNativeJavaConstructor(@AllArguments Object[] allArguments,
                                    @SuperCall Callable<Object> callableMethod, 
                                    @This Object thiz) throws Exception {
        if(testCaptureEnabled(allArguments[0])) {
            try {
                Field f = thiz.getClass().getDeclaredField("ctor");
                f.setAccessible(true);
                Object ctor = f.get(thiz); //MemberBox

                String result = Tools.memberBoxToString(ctor);
                JavaMethodInterceptor.writeToFile(result, allArguments[0]);
            } catch(Throwable e) {
                Logging.error("Exception in NativeJavaConstructor intercept.", e);
            }

            // Prevent call tracing after a js -> java call is made
            boolean preValue = setCapture(allArguments[0], false);
            Object ret = callableMethod.call();
            setCapture(allArguments[0], preValue);
            return ret;
        } else {
            return callableMethod.call();
        }
        
    }

    @RuntimeType
    public static Object interceptNativeJavaClass(@AllArguments Object[] allArguments,
                                    @SuperCall Callable<Object> callableMethod) throws Exception {
        if(testCaptureEnabled(allArguments[0])) {
            try {
                Object meth = allArguments[3]; //MemberBox
                String result = Tools.memberBoxToString(meth);
                JavaMethodInterceptor.writeToFile(result, allArguments[0]);
            } catch(Throwable e) {
                Logging.error("Exception in NativeJavaClass intercept.", e);
            }

            // Prevent call tracing after a js -> java call is made
            boolean preValue = setCapture(allArguments[0], false);
            Object ret = callableMethod.call();
            setCapture(allArguments[0], preValue);
            return ret;
        } else {
            return callableMethod.call();
        }
        
    }

    public static Object interceptNativeJavaMethod(@AllArguments Object[] allArguments,
                                    @SuperCall Callable<Object> callableMethod, 
                                    @This Object thiz) throws Exception {
        
        if(testCaptureEnabled(allArguments[0])) {
            try {
                ClassLoader cl = thiz.getClass().getClassLoader();
                Field f = thiz.getClass().getDeclaredField("methods");
                f.setAccessible(true);
                Object methods = f.get(thiz); //MemberBox[]

                Class<?> contextClass = cl.loadClass("org.mozilla.javascript.Context");
                Class<?> nativeJavaMethodClass = cl.loadClass("org.mozilla.javascript.NativeJavaMethod");
                Class<?> memberBoxClass = cl.loadClass("org.mozilla.javascript.MemberBox");

                Method findFunction = nativeJavaMethodClass.getDeclaredMethod("findFunction", contextClass, Array.newInstance(memberBoxClass, 0).getClass(), Object[].class);
                findFunction.setAccessible(true);
                int index = (Integer)findFunction.invoke(thiz, allArguments[0], methods, allArguments[3]);

                if (index >= 0) {
                    Object meth = ((Object[])methods)[index]; //MemberBox
                    String result = Tools.memberBoxToString(meth);

                    JavaMethodInterceptor.writeToFile(result, allArguments[0]);
                }
            } catch(Throwable e) {
                Logging.error("Exception in NativeJavaMethod intercept.", e);
            }

            // Prevent call tracing after a js -> java call is made
            boolean preValue = setCapture(allArguments[0], false);
            Object ret = callableMethod.call();
            setCapture(allArguments[0], preValue);
            return ret;
        } else {
            return callableMethod.call();
        }
    }

    private static boolean setCapture(Object context, boolean value) {
        try {
            Field f = context.getClass().getDeclaredField("_capture");
            f.setAccessible(true);
            boolean prevValue = (Boolean)f.get(context);
            f.set(context, value);
            return prevValue;
        } catch(Exception e) {
            Logging.error("Failed to set _capture to " + value, e);
            throw new RuntimeException(e);
        }
    }

    private static boolean testCaptureEnabled(Object cx /* Context cx */) {
        try{
            Field f = cx.getClass().getDeclaredField("_capture");
            f.setAccessible(true);
            boolean _capture = (Boolean)f.get(cx); 
            return _capture;
        } catch(Exception e) {
            return false;
        }
    }
    
}
