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

package sootup.core.inputlocation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import sootup.core.frontend.SootClassSource;
import sootup.core.model.ClassModifier;
import sootup.core.model.Position;
import sootup.core.model.SourceType;
import sootup.core.transform.BodyInterceptor;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.java.core.AnnotationUsage;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.OverridingJavaClassSource;
import sootup.java.core.types.JavaClassType;

public class InMemoryJavaAnalysisInputLocation implements AnalysisInputLocation {

    @Nonnull final Path path = Paths.get("only-in-memory.class");
    @Nonnull final List<BodyInterceptor> bodyInterceptors;
    @Nonnull private final OverridingJavaClassSource classSource;
    @Nonnull final SourceType sourceType;

    public InMemoryJavaAnalysisInputLocation(@Nonnull JavaClassType classType, @Nullable JavaClassType superClass, @Nonnull Set<JavaClassType> interfaces,
            @Nullable JavaClassType outerClass, @Nonnull Set<JavaSootField> sootFields, @Nonnull Set<JavaSootMethod> sootMethods,
            @Nonnull Position position, @Nonnull EnumSet<ClassModifier> modifiers, @Nonnull Iterable<AnnotationUsage> annotations,
            @Nonnull Iterable<AnnotationUsage> methodAnnotations, @Nullable Iterable<AnnotationUsage> fieldAnnotations) {
        this(classType, superClass, interfaces, outerClass, sootFields, sootMethods, position, modifiers, annotations, methodAnnotations, 
                fieldAnnotations, SourceType.Application, Collections.emptyList());
    }

    public InMemoryJavaAnalysisInputLocation(
        @Nonnull JavaClassType classType,
        @Nullable JavaClassType superClass,
        @Nonnull Set<JavaClassType> interfaces,
        @Nullable JavaClassType outerClass,
        @Nonnull Set<JavaSootField> sootFields,
        @Nonnull Set<JavaSootMethod> sootMethods,
        @Nonnull Position position,
        @Nonnull EnumSet<ClassModifier> modifiers,
        @Nonnull Iterable<AnnotationUsage> annotations,
        @Nonnull Iterable<AnnotationUsage> methodAnnotations,
        @Nullable Iterable<AnnotationUsage> fieldAnnotations,
        @Nonnull SourceType sourceType,
        @Nonnull List<BodyInterceptor> bodyInterceptors) {
        this.bodyInterceptors = bodyInterceptors;
        this.sourceType = sourceType;

        this.classSource = new OverridingJavaClassSource(this, path, classType, superClass, interfaces, outerClass, sootFields,
                sootMethods, position, modifiers, annotations, methodAnnotations, fieldAnnotations);
        
    }

    @Nonnull
    @Override
    public Optional<? extends SootClassSource> getClassSource(
        @Nonnull ClassType type, @Nonnull View view) {
        return Optional.of(classSource);
    }

    @Nonnull
    @Override
    public Collection<? extends SootClassSource> getClassSources(@Nonnull View view) {
        return Collections.singletonList(classSource);
    }

    @Nonnull
    @Override
    public SourceType getSourceType() {
        return sourceType;
    }

    @Nonnull
    @Override
    public List<BodyInterceptor> getBodyInterceptors() {
        return bodyInterceptors;
    }

    @Nonnull
    public ClassType getClassType() {
        return classSource.getClassType();
    }

    @Nonnull
    public OverridingJavaClassSource getClassSource() {
        return classSource;
    }
    
}
