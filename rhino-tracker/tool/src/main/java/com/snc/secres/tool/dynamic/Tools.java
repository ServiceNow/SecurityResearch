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
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.Files;
import com.snc.secres.tool.common.io.FileHelpers;

public class Tools {

    public static void writeClassFile(Path outDir, String timestamp, Map<String, byte[]> classNameToBytes) throws IOException {
        for(String className : classNameToBytes.keySet()) {
            byte[] bytes = classNameToBytes.get(className);
            Path outFile = Tools.classNameToCapturePath(outDir, timestamp, className, "class");
            try (OutputStream fos = java.nio.file.Files.newOutputStream(outFile)) {
                fos.write(bytes);
            }
        }
    }

    public static Map<String, byte[]> stringToClassFile(String fullClassName, String script, ClassLoader cl) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
        // Context cx = Context.enter();
        Class<?> contextClass = cl.loadClass("org.mozilla.javascript.Context");
        Method enter = contextClass.getDeclaredMethod("enter");
        enter.setAccessible(true);
        Object cx = enter.invoke(null);

        // CompilerEnvirons compilerEnv = new CompilerEnvirons();
        Class<?> compilerEnvironsClass = cl.loadClass("org.mozilla.javascript.CompilerEnvirons");
        Object compilerEnv = compilerEnvironsClass.getConstructor().newInstance();
        
        // compilerEnv.initFromContext(cx);
        Method initFromContext = compilerEnvironsClass.getDeclaredMethod("initFromContext", contextClass);
        initFromContext.setAccessible(true);
        initFromContext.invoke(compilerEnv, cx);
        
        // compilerEnv.setGenerateObserverCount(false);
        Method setGenerateObserverCount = compilerEnvironsClass.getDeclaredMethod("setGenerateObserverCount", boolean.class);
        setGenerateObserverCount.setAccessible(true);
        setGenerateObserverCount.invoke(compilerEnv, false);

        // compilerEnv.setGeneratingSource(false);
        Method setGeneratingSource = compilerEnvironsClass.getDeclaredMethod("setGeneratingSource", boolean.class);
        setGeneratingSource.setAccessible(true);
        setGeneratingSource.invoke(compilerEnv, false);

        // compilerEnv.setGenerateDebugInfo(true);
        Method setGenerateDebugInfo = compilerEnvironsClass.getDeclaredMethod("setGenerateDebugInfo", boolean.class);
        setGenerateDebugInfo.setAccessible(true);
        setGenerateDebugInfo.invoke(compilerEnv, true);

        // ClassCompiler compiler = new ClassCompiler(compilerEnv);
        Class<?> classCompilerClass = cl.loadClass("org.mozilla.javascript.optimizer.ClassCompiler");
        Object compiler = classCompilerClass.getConstructor(compilerEnvironsClass).newInstance(compilerEnv);

        // Object[] compiled = compiler.compileToClassFiles(script, null, 1, fullClassName);
        Method compileToClassFiles = classCompilerClass.getDeclaredMethod("compileToClassFiles", String.class, String.class, int.class, String.class);
        compileToClassFiles.setAccessible(true);
        Object[] compiled = (Object[])compileToClassFiles.invoke(compiler, script, null, 1, fullClassName);

        // Context.exit();
        Method exit = contextClass.getDeclaredMethod("exit");
        enter.setAccessible(true);
        exit.invoke(null);
        
        Map<String, byte[]> ret = new LinkedHashMap<>();
        for(int j = 0; j != compiled.length; j += 2) {
            String className = (String)compiled[j];
            byte[] bytes = (byte[])compiled[(j + 1)];
            ret.put(className, bytes);
        }
        return Collections.unmodifiableMap(ret);
    }

    public static String memberBoxToString(Object memberBox /*MemberBox*/) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Field argTypes = memberBox.getClass().getDeclaredField("argTypes");
        argTypes.setAccessible(true);
        Class<?>[] types = (Class[])argTypes.get(memberBox);

        Method method = memberBox.getClass().getDeclaredMethod("isMethod");
        method.setAccessible(true);
        boolean isMethod = (Boolean)method.invoke(memberBox);

        if(isMethod) {
            method = memberBox.getClass().getDeclaredMethod("method");
            method.setAccessible(true);
            method = (Method)method.invoke(memberBox);
            return methodToSig(method, types);
        } else {
            method = memberBox.getClass().getDeclaredMethod("ctor");
            method.setAccessible(true);
            Constructor<?> ctor = (Constructor<?>)method.invoke(memberBox);
            return constructorToSig(ctor, types);
        }
    }

    public static String methodToSig(Method method, Class<?>[] argTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append(method.getDeclaringClass().getName());
        sb.append(": ");
        sb.append(method.getReturnType().getName());
        sb.append(' ');
        sb.append(method.getName());
        sb.append(liveConnectSignature(argTypes));
        sb.append(">");
        return sb.toString();
    }

    public static String constructorToSig(Constructor<?> ctor, Class<?>[] argTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append(ctor.getDeclaringClass().getName());
        sb.append(": void <init>");
        sb.append(liveConnectSignature(argTypes));
        sb.append(">");
        return sb.toString();
    }

    private static String liveConnectSignature(Class<?>[] argTypes) {
        int N = argTypes.length;
        if (N == 0) { return "()"; }
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i != N; ++i) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(javaSignature(argTypes[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String javaSignature(Class<?> type) {
        if (!type.isArray()) {
            return type.getName();
        }
        int arrayDimension = 0;
        do {
            ++arrayDimension;
            type = type.getComponentType();
        } while (type.isArray());
        String name = type.getName();
        String suffix = "[]";
        if (arrayDimension == 1) {
            return name.concat(suffix);
        }
        int length = name.length() + arrayDimension * suffix.length();
        StringBuilder sb = new StringBuilder(length);
        sb.append(name);
        while (arrayDimension != 0) {
            --arrayDimension;
            sb.append(suffix);
        }
        return sb.toString();
    }

    public static Path classNameToCapturePath(Path outDir, String timestamp, String fullClassName) {
        return classNameToCapturePath(outDir, timestamp, fullClassName, "txt");
    }

    public static Path classNameToCapturePath(Path outDir, String timestamp, String fullClassName, String fileExtension) {
        return FileHelpers.getPath(outDir,timestamp + "__" + fullClassName + "__." + fileExtension);
    }

    public static List<String> capturePathToClassName(Path filePath) throws Exception {
        String fileName = Files.getNameWithoutExtension(filePath.getFileName().toString());
        Pattern pattern = Pattern.compile("^(\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d)__(.+)__$");
        Matcher matcher = pattern.matcher(fileName);
        if(matcher.matches()) {
            String timestamp = matcher.group(1);
            String fullClassName = matcher.group(2);
            return Arrays.asList(filePath.getParent().toString(), timestamp, fullClassName);
        }
        throw new Exception("Improperly formatted file name '" + fileName + "'");
    }
    
}
