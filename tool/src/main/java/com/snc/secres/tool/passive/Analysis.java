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

package com.snc.secres.tool.passive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.snc.secres.tool.common.io.FileHelpers;
import com.snc.secres.tool.common.io.PrintStreamUnixEOL;
import com.snc.secres.tool.dynamic.Tools;

import sootup.callgraph.CallGraphWrapper;
import sootup.callgraph.CallGraphAlgorithm;
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm;
import sootup.callgraph.GraphBasedCallGraph;
import sootup.callgraph.RapidTypeAnalysisAlgorithm;
import sootup.callgraph.filter.CallGraphFilter;
import sootup.core.signatures.MethodSignature;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.MutableJavaView;

public class Analysis {

    private static final String CN = Analysis.class.getSimpleName();

    private final MutableJavaView view;
    private final String jsFullClassName;
    private final Set<MethodSignature> runtimeTraceMethodSigs;
    private final MethodSignature sinkMethodSignature;
    @SuppressWarnings("unused")
    private final MethodSignature mainMethodSignature;
    private final MethodSignature entryMethodSignature;
    private final CallGraphWrapper callGraph;
    private final Config config;

    private Analysis(MutableJavaView view, String jsFullClassName, Set<MethodSignature> runtimeTraceMethodSigs, MethodSignature sinkMethodSignature, MethodSignature mainMethodSignature,
            MethodSignature entryMethodSignature, CallGraphWrapper callGraph, Config config) {
        this.view = view;
        this.jsFullClassName =  jsFullClassName;
        this.runtimeTraceMethodSigs = runtimeTraceMethodSigs;
        this.sinkMethodSignature = sinkMethodSignature;
        this.mainMethodSignature = mainMethodSignature;
        this.entryMethodSignature = entryMethodSignature;
        this.callGraph = callGraph;
        this.config = config;
    }

    public void run() throws IOException {

        Deque<MethodSignature> toVisit = new ArrayDeque<>();
        Set<MethodSignature> visited = new HashSet<>();
        toVisit.add(entryMethodSignature);
        Set<MethodSignature> sinkContMethods = new HashSet<>();
        while(!toVisit.isEmpty()) {
            MethodSignature cur = toVisit.poll();
            if(visited.add(cur)) {
                for(MethodSignature dest : callGraph.callsFrom(cur)) {
                    if(dest.equals(sinkMethodSignature)) {
                        sinkContMethods.add(cur);
                    }
                    toVisit.push(dest);
                }
            }
        }

        // Add in our blank class and method to house the runtime simulation
        JavaSootClass runtimeSimClass = SootTools.makeClassWithEmptyMethod(view, jsFullClassName, "runtimeSimulator");
        JavaSootMethod runtimeSimMethod = runtimeSimClass.getMethodsByName("runtimeSimulator").iterator().next();

        // Add the runtime simulation method to the call graph
        callGraph.addMethod(runtimeSimMethod.getSignature());

        // Add nodes and edges to the cg that go from our runtime simulator method to the methods the script calls
        for(MethodSignature methodSig : runtimeTraceMethodSigs) {
            callGraph.addMethod(methodSig);
            callGraph.addCall(runtimeSimMethod.getSignature(), methodSig);
        }

        for(MethodSignature sinkContMethodSignature : sinkContMethods) {
            // Add the call to sink container to runtime sim method edge
            callGraph.addCall(sinkContMethodSignature, runtimeSimMethod.getSignature());
            // Remove the call from sink container to sink edge from the call graph
            callGraph.removeCall(sinkContMethodSignature, sinkMethodSignature);
        }

        try(PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(config.getOutputDirPath(), jsFullClassName + ".dot")))) {
            ps.println(callGraph.exportAsDot());
        }
    }

    private static MethodSignature parseMethodSignature(MutableJavaView view, String sig, String name) {
        if(sig.isEmpty()) {
            System.err.println(CN + ": A '" + name + "' method signature must be supplied.");
            return null;
        }
        try {
            return view.getIdentifierFactory().parseMethodSignature(sig);
        } catch(Exception e) {
            System.err.println(CN + ": SootUp failed to parse '" + name + "' method signature '" + sig + "'.\n\n");
            e.printStackTrace();
            return null;
        }
    }

    public static Analysis makeAnalysis(Config config) {
        Path runtimeTraceFile = config.getRuntimeTraceFilePath();
        if(!FileHelpers.checkRWFileExists(runtimeTraceFile)) {
            System.err.println(CN + ": Failed to verify the existence of the runtime trace file '" + runtimeTraceFile + "'.");
            return null;
        }

        // Get the name of js class and timestamp from the runtime trace file dump and combine them
        String jsFullClassName;
        try {
            List<String> temp = Tools.capturePathToClassName(runtimeTraceFile);
            //String dumpDir = temp.get(0);
            String timestamp = temp.get(1);
            jsFullClassName = temp.get(2);
            jsFullClassName = jsFullClassName + timestamp.replace("-", "").replace("_", "");
        } catch(Exception e) {
            System.err.println(CN + ": Improperly formatted name for runtime trace file '" + runtimeTraceFile + "'.\n\n");
            e.printStackTrace();
            return null;
        }

        if(config.getClassPath().isEmpty()) {
            System.err.println(CN + ": A non-empty class-path must be supplied.");
            return null;
        }

        // Generate view of code to be analyzed
        MutableJavaView view;
        try {
            view = SootTools.makeJavaView(config.getClassPath());
        } catch(Exception e) {
            System.err.println(CN + ": SootUp failed to load classpath '" + config.getClassPath() + "'.\n\n");
            e.printStackTrace();
            return null;
        }

        // Read in and resolve the methods recorded during the runtime activity
        Set<MethodSignature> runtimeTraceMethodSigs;
        try {
            runtimeTraceMethodSigs = new LinkedHashSet<>();
            for(String methodSig : new LinkedHashSet<>(Files.readAllLines(runtimeTraceFile))) {
                methodSig = methodSig.trim();
                if(methodSig.isEmpty())
                    continue;
                runtimeTraceMethodSigs.add(view.getIdentifierFactory().parseMethodSignature(methodSig));
            }
        } catch(Exception e) {
            System.err.println(CN + ": Failed to read in the runtime trace file '" + runtimeTraceFile + "'.\n\n");
            e.printStackTrace();
            return null;
        }

        if(runtimeTraceMethodSigs.isEmpty()) {
            System.err.println(CN + ": No methods in the runtime trace file '" + runtimeTraceFile + "'.\n\n");
            return null;
        }

        CallGraphFilter cgFilter;
        try {
            cgFilter = CallGraphFilter.makeCallGraphFilter(config.getFilterDefaultPolicy(), config.getFilterEntries());
        } catch(Exception e) {
            System.err.println(CN + ": Failed to load the call graph filter.\n\n");
            e.printStackTrace();
            return null;
        }

        // Grab the sink method signature
        MethodSignature sinkMethodSignature = parseMethodSignature(view, config.getSinkMethodSig(), "sink");
        if(sinkMethodSignature == null)
            return null;

        // Resolve the entry point, main method (if needed), and build the call graph based on the given algorithm
        MethodSignature mainMethodSignature;
        MethodSignature entryMethodSignature;
        CallGraphAlgorithm cga;
        CallGraphWrapper callGraph;
        switch(config.getCallGraphAlgo().toLowerCase()) {
            case "cha":
                entryMethodSignature = parseMethodSignature(view, config.getEntryPointMethodSig(), "entry point");
                if(entryMethodSignature == null)
                    return null;
                mainMethodSignature = null;
                cga = new ClassHierarchyAnalysisAlgorithm(view);
                callGraph = new CallGraphWrapper((GraphBasedCallGraph)cga.initialize(Collections.singletonList(entryMethodSignature)));
                callGraph.applyFilter(cgFilter, view);
                break;
            case "rta":
                entryMethodSignature = parseMethodSignature(view, config.getEntryPointMethodSig(), "entry point");
                if(entryMethodSignature == null) {
                    return null;
                }
                mainMethodSignature = parseMethodSignature(view, config.getMainMethodSig(), "main");
                if(mainMethodSignature == null)
                    return null;
                cga = new RapidTypeAnalysisAlgorithm(view);
                callGraph = new CallGraphWrapper((GraphBasedCallGraph)cga.initialize(Collections.singletonList(mainMethodSignature)));
                callGraph.applyFilter(cgFilter, view);
                break;
            default:
                System.err.println(CN + ": Unsupported call graph algorithm given '" + config.getCallGraphAlgo() + "''.");
                return null;
        }

        return new Analysis(view, jsFullClassName, runtimeTraceMethodSigs, sinkMethodSignature, mainMethodSignature, entryMethodSignature, callGraph, config);
    }
    
}
