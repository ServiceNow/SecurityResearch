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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sootup.core.frontend.OverridingBodySource;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.inputlocation.InMemoryJavaAnalysisInputLocation;
import sootup.core.jimple.basic.NoPositionInformation;
import sootup.core.model.Body;
import sootup.core.model.ClassModifier;
import sootup.core.model.MethodModifier;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.transform.BodyInterceptor;
import sootup.core.types.VoidType;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.OverridingJavaClassSource;
import sootup.java.core.interceptors.Aggregator;
import sootup.java.core.interceptors.CastAndReturnInliner;
import sootup.java.core.interceptors.ConstantPropagatorAndFolder;
import sootup.java.core.interceptors.CopyPropagator;
import sootup.java.core.interceptors.EmptySwitchEliminator;
import sootup.java.core.interceptors.LocalNameStandardizer;
import sootup.java.core.interceptors.LocalPacker;
import sootup.java.core.interceptors.NopEliminator;
import sootup.java.core.interceptors.UnreachableCodeEliminator;
import sootup.java.core.interceptors.UnusedLocalEliminator;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.MutableJavaView;

public class SootTools {

    public static MutableJavaView makeJavaView(String classPath) {
        // This list was constructed and organized by combining the original soot with sootup
        //A default list can be found here but it might not contain everything BytecodeBodyInterceptors.Default.getBodyInterceptors()
        List<BodyInterceptor> bodyInterceptors = Collections.unmodifiableList(Arrays.asList(
                // new TrapTightener(), missing: impl not finished
                // new DuplicateCatchAllTrapRemover(), missing: does not exist in sootup
                new UnreachableCodeEliminator(),
                // new LocalSplitter(), bug: causes infinite loop
                // new SharedInitializationLocalSplitter(), missing: does not exist in sootup
                new Aggregator(),
                new EmptySwitchEliminator(), // new: sootup
                new CastAndReturnInliner(), // new: sootup
                new ConstantPropagatorAndFolder(), // new: sootup
                new UnusedLocalEliminator(),
                // new TypeAssigner(), bug: causes an exception because it leaves intermediary AugmentIntegerTypes
                new LocalNameStandardizer(),
                new CopyPropagator(),
                // new DeadAssignmentEliminator(), bug: creates unconnected exceptional flows - see RuntimeJarConversionTests
                // new ConditionalBranchFolder(), bug: leaves unconnected edges sometimes - see RuntimeJarConversionTests
                new UnusedLocalEliminator(),
                new LocalPacker(),
                new NopEliminator(),
                new UnreachableCodeEliminator(),
                new LocalNameStandardizer())
        );

        List<AnalysisInputLocation> inputLocations = new ArrayList<>();
        inputLocations.add(new JavaClassPathAnalysisInputLocation(classPath, SourceType.Application, bodyInterceptors));
        //inputLocations.add(new DefaultRTJarAnalysisInputLocation());

        return new MutableJavaView(inputLocations);
    }

    public static JavaSootClass makeClassWithEmptyMethod(MutableJavaView view, String fullClassName, String methodName) {
        JavaClassType classType = view.getIdentifierFactory().getClassType(fullClassName);
        JavaSootMethod method = makeEmptyMethod(classType, methodName);
        //It does not seem like we can add this new input location to the view
        //However it does not look like it is needed to be added either since the view only uses it to load things
        //and the classes we will add will already be loaded
        InMemoryJavaAnalysisInputLocation memInputLocation = new InMemoryJavaAnalysisInputLocation(
            classType, 
            view.getIdentifierFactory().getClassType("java.lang.Object"),
            Collections.emptySet(),
            null,
            Collections.emptySet(),
            new HashSet<>(Arrays.asList(method)),
            NoPositionInformation.getInstance(),
            EnumSet.of(ClassModifier.PUBLIC, ClassModifier.FINAL),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            SourceType.Application, 
            Collections.emptyList()
        );
        OverridingJavaClassSource source = memInputLocation.getClassSource();
        JavaSootClass sc =  new JavaSootClass(source, SourceType.Application);
        view.addClass(sc);
        return sc;
    }

    public static JavaSootMethod makeEmptyMethod(JavaClassType classType, String name) {
        MethodSignature methodSig = new MethodSignature(classType, name, Collections.emptyList(), VoidType.getInstance());
        Set<MethodModifier> mods = EnumSet.of(MethodModifier.PUBLIC, MethodModifier.STATIC, MethodModifier.FINAL);
        return (JavaSootMethod)JavaSootMethod.builder()
                .withSource(new OverridingBodySource(methodSig, makeEmptyBody(methodSig, mods)))
                .withSignature(methodSig)
                .withModifier(mods)
                .build();
    }

    public static Body makeEmptyBody(MethodSignature methodSig, Set<MethodModifier> mods) {
        return Body.builder().setMethodSignature(methodSig).setModifiers(mods).build();
    }
    
}
