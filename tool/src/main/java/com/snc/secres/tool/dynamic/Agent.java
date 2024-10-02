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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Map;

import com.snc.secres.tool.common.io.FileHelpers;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.InitializationStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.matcher.ElementMatchers;

public class Agent extends Thread {

    private static Agent activeAgent = null;

    private final Config config;
    private final Path jsFile;
    private final String jsFullClassName;
    private final Path outDir;
    private final String timestamp;
    private final Path compiledJsDir;
    private final String instanceURL;
    private final int connectTries;
    private final int connectTimeout;
    private final int timeBetweenConnectAttempts;

    private volatile ClassLoader orgClassLoader;

    public Agent(Config config, Path jsFile, String jsFullClassName, Path outDir, String timestamp,
            Path compiledJsDir, String instanceURL, int connectTries, int connectTimeout, int timeBetweenConnectAttempts) {
        this.config = config;
        this.jsFile = jsFile;
        this.jsFullClassName = jsFullClassName;
        this.outDir = outDir;
        this.timestamp = timestamp;
        this.instanceURL = instanceURL;
        this.connectTries = connectTries;
        this.connectTimeout = connectTimeout;
        this.timeBetweenConnectAttempts = timeBetweenConnectAttempts;
        this.compiledJsDir = compiledJsDir;
        this.orgClassLoader = null;
    }

    public ClassLoader getOrgClassLoader() {
        return orgClassLoader;
    }

    public Path getOutDirPath() {
        return outDir;
    }

    public String getTimeStamp() {
        return timestamp;
    }

    public Config getConfig() {
        return config;
    }

    private void sackSecurityManager(Instrumentation instrumentation, PrintStream loggingPrintStream) throws IOException {
        // Ensure our MethodDelegation/Advice class is available to the bootstrap bootloader
        // ClassInjector requires a directory when instrumenting but the code we are using is already available
        // so just use a empty temp directory
        File temp = Files.createTempDirectory("tmp").toFile();
        temp.deleteOnExit();
        ClassInjector.UsingInstrumentation.of(temp, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation).inject(
            Collections.singletonMap(new TypeDescription.ForLoadedType(SecurityManagerInterceptor.class), ClassFileLocator.ForClassLoader.read(SecurityManagerInterceptor.class)));

        // Instrument the setSecurityManager method
        new AgentBuilder.Default()
                // Add loggers so you can see failures with bytebuddy transforming
                .with(new AgentBuilder.Listener.StreamWriting(loggingPrintStream).withTransformationsOnly())
                .with(new AgentBuilder.InstallationListener.StreamWriting(loggingPrintStream))
                // Have to redefine the already loaded std java lib classes
                .with(RedefinitionStrategy.RETRANSFORMATION)
                .with(InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                // Needed to reset the default ignore which includes std java libs
                .ignore(new AgentBuilder.RawMatcher.ForElementMatchers(ElementMatchers.nameStartsWith("net.bytebuddy.")
                        .or(ElementMatchers.isSynthetic()), ElementMatchers.any(), ElementMatchers.any()))
                .type(ElementMatchers.named("java.lang.System"))
                .transform(new AgentBuilder.Transformer() {
                    // This is needed because of a bug in bytebuddy
                    @SuppressWarnings("unused")
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                        return transform(builder, typeDescription, classLoader, module, null);
                    }
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, 
                            JavaModule module, ProtectionDomain protectionDomain) {
                        return builder.method(ElementMatchers.named("setSecurityManager")).intercept(MethodDelegation.to(SecurityManagerInterceptor.class));
                    }
                }).installOn(instrumentation);
    }

    public void instrument(Instrumentation instrumentation) throws IOException {
        PrintStream loggingPrintStream = Logging.getLoggingPrintStream();

        sackSecurityManager(instrumentation, loggingPrintStream);

        AgentBuilder agentBuilder = new AgentBuilder.Default()
            .with(new AgentBuilder.Listener.StreamWriting(loggingPrintStream).withTransformationsOnly())
            .with(new AgentBuilder.InstallationListener.StreamWriting(loggingPrintStream));

        agentBuilder
            .type(ElementMatchers.named("org.mozilla.javascript.Context"))
            .transform(new AgentBuilder.Transformer() {
                // This is needed because of a bug in bytebuddy
                @SuppressWarnings("unused")
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                    return transform(builder, typeDescription, classLoader, module, null);
                }
                @Override
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, 
                        TypeDescription typeDescription, ClassLoader classLoader, 
                        JavaModule module, ProtectionDomain protectionDomain) {
                    // Store the original class loader for use later
                    Agent.getActiveAgent().orgClassLoader = classLoader;
                    // Define the new field but can't user '.value' because field is not a static
                    builder = builder.defineField("_capture", boolean.class, Visibility.PUBLIC);
                    builder = builder.defineField("_capture_path", Path.class, Visibility.PUBLIC);
                    builder = builder.visit(Advice.to(ContextAdvice.class).on(ElementMatchers.isConstructor()));
                    return builder;
                }
            }).installOn(instrumentation);

        // Use to discover who called a method
        /*agentBuilder
            .type(ElementMatchers.named("org.mozilla.javascript.MemberBox"))
            .transform(new AgentBuilder.Transformer() {
                // This is needed because of a bug in bytebuddy
                @SuppressWarnings("unused")
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                    return transform(builder, typeDescription, classLoader, module, null);
                }
                @Override
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, 
                        TypeDescription typeDescription, ClassLoader classLoader, 
                        JavaModule module, ProtectionDomain protectionDomain) {
                    return builder.visit(Advice.to(WhoCalledMeAdvice.class).on(ElementMatchers.named("invoke")));
                }
            }).installOn(instrumentation);*/

        agentBuilder
            .type(ElementMatchers.named("org.mozilla.javascript.NativeJavaClass"))
            .transform(new AgentBuilder.Transformer() {
                // This is needed because of a bug in bytebuddy
                @SuppressWarnings("unused")
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                    return transform(builder, typeDescription, classLoader, module, null);
                }
                @Override
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, 
                        TypeDescription typeDescription, ClassLoader classLoader, 
                        JavaModule module, ProtectionDomain protectionDomain) {
                    return builder.method(ElementMatchers.named("constructSpecific"))
                                .intercept(MethodDelegation.withDefaultConfiguration().filter(ElementMatchers.named("interceptNativeJavaClass")).to(JavaMethodInterceptor.class));
                }
            }).installOn(instrumentation);

        agentBuilder
            .type(ElementMatchers.named("org.mozilla.javascript.NativeJavaConstructor"))
            .transform(new AgentBuilder.Transformer() {
                // This is needed because of a bug in bytebuddy
                @SuppressWarnings("unused")
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                    return transform(builder, typeDescription, classLoader, module, null);
                }
                @Override
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, 
                        TypeDescription typeDescription, ClassLoader classLoader, 
                        JavaModule module, ProtectionDomain protectionDomain) {
                    return builder.method(ElementMatchers.named("call"))
                                .intercept(MethodDelegation.withDefaultConfiguration().filter(ElementMatchers.named("interceptNativeJavaConstructor")).to(JavaMethodInterceptor.class));
                }
            }).installOn(instrumentation);

        agentBuilder
            .type(ElementMatchers.named("org.mozilla.javascript.FunctionObject"))
            .transform(new AgentBuilder.Transformer() {
                // This is needed because of a bug in bytebuddy
                @SuppressWarnings("unused")
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                    return transform(builder, typeDescription, classLoader, module, null);
                }
                @Override
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, 
                        TypeDescription typeDescription, ClassLoader classLoader, 
                        JavaModule module, ProtectionDomain protectionDomain) {
                    return builder.method(ElementMatchers.named("call").and(ElementMatchers.takesArguments(5)))
                                .intercept(MethodDelegation.withDefaultConfiguration().filter(ElementMatchers.named("interceptFunctionObject")).to(JavaMethodInterceptor.class));
                }
            }).installOn(instrumentation);
        
        agentBuilder
            .type(ElementMatchers.named("org.mozilla.javascript.NativeJavaMethod"))
            .transform(new AgentBuilder.Transformer() {
                // This is needed because of a bug in bytebuddy
                @SuppressWarnings("unused")
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                    return transform(builder, typeDescription, classLoader, module, null);
                }
                @Override
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, 
                        TypeDescription typeDescription, ClassLoader classLoader, 
                        JavaModule module, ProtectionDomain protectionDomain) {
                    return builder.method(ElementMatchers.named("call"))
                                .intercept(MethodDelegation.withDefaultConfiguration().filter(ElementMatchers.named("interceptNativeJavaMethod")).to(JavaMethodInterceptor.class));
                }
            }).installOn(instrumentation);
    }

    private boolean connect() {
        try {
            URL url = new URL(instanceURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setConnectTimeout(connectTimeout);

            long start = System.currentTimeMillis();
            int attempts = 0;
            while(attempts < connectTries) {
                try {
                    con.connect();
                    break;
                } catch(ConnectException e) {
                    attempts++;
                    try {
                        if(attempts < connectTries) { // Don't sleep after last attempt
                            Thread.sleep(timeBetweenConnectAttempts);
                        }
                    } catch(InterruptedException ie){}
                } 
            }
            long end = System.currentTimeMillis();
            long connectionAttemptTime = Math.round(((double)(end-start))/(double)1000);

            if(attempts >= connectTries) {
                Logging.error("Failed to connect to " + instanceURL + " after " + connectionAttemptTime + " seconds.");
                return false;
            }
            
            int status = con.getResponseCode();
            if (status == 200) {
                Logging.info("Connected to " + instanceURL + " after " + connectionAttemptTime + " seconds.");
                return true;
            } else {
                Logging.error("Connected to server " + instanceURL + " after " + connectionAttemptTime + " seconds, but got error code " + status + ".");
                return false;
            }
        } catch(Throwable t) {
            Logging.error("Unexpected exception while connecting to server " + instanceURL + ".", t);
            return false;
        }
    }

    private boolean runAndRecordJavaMethodOfJS() {
        try {
            ClassLoader cl = orgClassLoader;
            // Compile js file into java classes using rhino
            Map<String, byte[]> classNameToBytes = Tools.stringToClassFile(jsFullClassName, Files.readString(jsFile), cl);
            // Inject compiled classes into class loader so we can run them
            Map<String, Class<?>> classNameToClass = Collections.unmodifiableMap(new ClassInjector.UsingReflection.UsingReflection(cl).injectRaw(classNameToBytes));

            Tools.writeClassFile(compiledJsDir, timestamp, classNameToBytes);

            boolean atLeastOne = false;
            for(String name : classNameToClass.keySet()) {
                Class<?> clazz = classNameToClass.get(name);
                Method exec = null;
                // Any class that is invokable will have a main and exec method
                // Exec method is really what we want as we need to feed it the custom environment
                for(Method m : clazz.getMethods()) {
                    if(m.getName().equals("exec")) {
                        exec = m;
                        break;
                    }
                }

                if(exec != null) {
                    atLeastOne = true;
                    // Context ctx = Context.enter();
                    Class<?> contextClass = cl.loadClass("org.mozilla.javascript.Context");
                    Method enter = contextClass.getMethod("enter");
                    enter.setAccessible(true);
                    Object context = enter.invoke(null);

                    // ScriptableObject scope = ctx.initStandardObjects();
                    Method initStandardObjects = contextClass.getMethod("initStandardObjects");
                    initStandardObjects.setAccessible(true);
                    Object scope = initStandardObjects.invoke(context);

                    // Setup print and println functions since they are defined in the RhinoServlet
                    //scope.defineFunctionProperties(names, getClass(), ScriptableObject.DONTENUM);
                    StringWriter writer = new StringWriter();
                    PrintWriter out = new PrintWriter(writer);
                    String[] names = {"println", "print"};
                    Class<?> rhinoServletClass = cl.loadClass("com.snc.secres.sample.RhinoServlet");
                    Field outField = rhinoServletClass.getDeclaredField("out");
                    outField.setAccessible(true);
                    outField.set(null, out);
                    Method defineFunctionProperties = scope.getClass().getMethod("defineFunctionProperties", new String[0].getClass(), Class.class, int.class);
                    defineFunctionProperties.setAccessible(true);
                    defineFunctionProperties.invoke(scope, names, rhinoServletClass, 0x02);
                    
                    Field f = context.getClass().getDeclaredField("_capture");
                    f.setAccessible(true);
                    f.set(context, true);
                    Field _capture_path = context.getClass().getDeclaredField("_capture_path");
                    _capture_path.setAccessible(true);
                    _capture_path.set(context, Tools.classNameToCapturePath(outDir, timestamp, name));

                    Constructor<?> clazzConstructor = clazz.getConstructor();
                    Object clazzObject = clazzConstructor.newInstance();
                    exec.setAccessible(true);
                    exec.invoke(clazzObject, context, scope);
                } else {
                    Logging.info("No exec method found in compiled js class " + name + ". Skipping...");
                }
            }

            if(!atLeastOne) {
                Logging.error("Failed to recorded js to java methods. No class contained an exec method for " + jsFullClassName + " - " + jsFile);
                return false;
            }
            Logging.info("Successfully recorded js to java methods for " + jsFullClassName + " - " + jsFile);
            return true;
        } catch(Throwable t) {
            Logging.error("Unexpected exception while recording js to java methods for " + jsFullClassName + " - " + jsFile, t);
            return false;
        }
    }

    public void run() {
        boolean success = true;
        try {
            success = connect();
            if(success) {
                success = runAndRecordJavaMethodOfJS();
            }
        } catch(Throwable t) {
            Logging.error("Unexpected exception", t);
            success = false;
        } finally {
            Logging.closeLogging();
            System.exit(success ? 0 : -1);
        }
    }

    // No op since we are not supporting attaching yet
    public static void agentmain(String agentArgs, Instrumentation inst) {}

    public static void premain(String agentArgs, Instrumentation inst) {
        boolean exit = false;
        Agent agent = null;
        try {
            agent = createAndSetAgent(agentArgs);
            if(agent != null) {
                agent.instrument(inst);
                agent.start();
            } else {
                exit = true;
            }
        } catch(Throwable t) {
            Logging.error("Unexpected exception.", t);
            exit = true;
        } finally {
            if(exit) {
                Logging.closeLogging();
                System.exit(1);
            }
        }
    }

    public static Agent createAndSetAgent(String arguments) {
        try {
            String[] args = arguments.split(",");

            // Load the config file
            Config config;
            if (args.length > 0) {
                config = Config.readFromFile(FileHelpers.getPath(args[0].trim()));
            } else {
                Logging.error("Failed to create Agent. No config file provided.");
                return null;
            }

            // Setup logging
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(System.currentTimeMillis());
            Path logDir = config.getLogDirPath();
            FileHelpers.processDirectory(logDir, true, false);
            Logging.setupLogging(logDir, "log", timestamp);

            // Setup out dir
            Path outDir = config.getOutputDirPath();
            FileHelpers.processDirectory(outDir, true, false);

            // Validate js file path
            Path jsFilePath = config.getJSFilePath();
            if(jsFilePath == null || !FileHelpers.checkRWFileExists(jsFilePath)) {
                Logging.error("Failed to create Agent. No valid JS file was provided.");
                return null;
            }

            // Validate js full class name
            String jsFullClassName = config.getJSFullClassName();
            if(jsFullClassName == null) {
                Logging.error("Failed to create Agent. No valid JS full class name was provided.");
                return null;
            }

            // Make a dir to dump compiled js files
            Path compiledJSDir = config.getCompiledJSDirPath();
            FileHelpers.processDirectory(compiledJSDir, true, false);

            // Read other options
            String instanceURL = config.getInstanceURL();
            int connectTries = config.getConnectTries();
            int connectTimeout = config.getConnectTimeout();
            int timeBetweenConnectAttempts = config.getTimeBetweenConnectAttempts();

            Agent agent = new Agent(config, jsFilePath, jsFullClassName, outDir, timestamp, 
                                    compiledJSDir, instanceURL, connectTries, connectTimeout, 
                                    timeBetweenConnectAttempts);
            activeAgent = agent;
            return agent;
        } catch(Throwable t) {
            Logging.error("Failed to create Agent.", t);
            return null;
        }
    }

    public static final Agent getActiveAgent() {
        return activeAgent;
    }

}
