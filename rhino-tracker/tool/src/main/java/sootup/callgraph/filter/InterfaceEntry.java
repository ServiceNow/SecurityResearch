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

package sootup.callgraph.filter;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sootup.core.signatures.MethodSignature;
import sootup.core.typehierarchy.AuxViewTypeHierarchy;
import sootup.core.types.ClassType;

public class InterfaceEntry extends ClassEntry {

    private final boolean allSubClassMethods;
    private volatile AuxViewTypeHierarchy cachedTypeHierarchy;
    private volatile Set<MethodSignature> cachedMethods;

    public InterfaceEntry(boolean isDeny, String pattern, boolean isExact, boolean allSubClassMethods) {
        super(isDeny, pattern, isExact);
        this.allSubClassMethods = allSubClassMethods;
        this.cachedTypeHierarchy = null;
        this.cachedMethods = null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (allSubClassMethods ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        InterfaceEntry other = (InterfaceEntry) obj;
        if (allSubClassMethods != other.allSubClassMethods)
            return false;
        return true;
    }

    @Override
    public boolean matches(MethodSignature sig, AuxViewTypeHierarchy typeHierarchy) {
        if(this.cachedTypeHierarchy == null || !this.cachedTypeHierarchy.equals(typeHierarchy)) {
            Stream<ClassType> interfaces;
            if(getPattern() == null) {
                interfaces = Stream.of(typeHierarchy.getView().getIdentifierFactory().getClassType(getName()));
            } else {
                interfaces = typeHierarchy.getView().getClasses().stream()
                    .filter(sc -> 
                        sc.isInterface() 
                        && getPattern().matcher(sc.getType().getFullyQualifiedName()).matches())
                    .map(sc -> sc.getType());
            }
            interfaces = interfaces.filter(c -> typeHierarchy.contains(c));

            Stream<MethodSignature> possibleMethods;
            if(!allSubClassMethods) {
                possibleMethods = interfaces.flatMap(i -> typeHierarchy.getAllImplementingMethods(i));
            } else {
                possibleMethods = interfaces.flatMap(i -> typeHierarchy.getAllSubClassMethods(i));
            }

            this.cachedMethods = possibleMethods.collect(Collectors.toSet());
            this.cachedTypeHierarchy = typeHierarchy;
        }

        return this.cachedMethods.contains(sig);
    }

}
