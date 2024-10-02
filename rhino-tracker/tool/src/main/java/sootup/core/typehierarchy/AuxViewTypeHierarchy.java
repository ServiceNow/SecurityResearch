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

package sootup.core.typehierarchy;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.signatures.MethodSubSignature;
import sootup.core.typehierarchy.ViewTypeHierarchy.ScanResult.Vertex;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.core.views.View;

public class AuxViewTypeHierarchy extends ViewTypeHierarchy {

    private final View view;

    public AuxViewTypeHierarchy(View view) {
        super(view);
        this.view = view;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((view == null) ? 0 : view.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AuxViewTypeHierarchy other = (AuxViewTypeHierarchy) obj;
        if (view == null) {
            if (other.view != null)
                return false;
        } else if (!view.equals(other.view))
            return false;
        return true;
    }

    public View getView() {
        return view;
    }

    /** TODO
	 * Returns all the sub-classes of a given class including the given class. These sub-classes are
	 * directly and indirectly related to the given class. An Interface will return an empty set.
	 */
	public Stream<ClassType> getAllSubClasses(ClassType type) {
        if(isInterface(type))
            return Stream.empty();
        return Stream.concat(subtypesOf(type), Stream.of(type));
	}

    /** TODO
	 * This method is an extension of the FastHierarchy's method getAllImplementersOfInterface(SootClass).
	 * Instead of just returning the direct implementers of the parent interface, all the direct 
	 * interfaces of the parent, and all the indirect interfaces of the parent (i.e. all interfaces returned by 
	 * FastHierarchy's method getAllSubinterfaces(SootClass) called on parent), this method will return every
	 * sub class of this parent interface. That is every direct or indirect sub class of the parent interface.
	 * 
	 * This means that just like FastHierarchy's getAllImplementersOfInterface(SootClass) method, no interfaces
	 * are returned (including the parent because that is an interface). Only actual classes are returned.
	 */
	public Stream<ClassType> getAllSubClassesOfInterface(ClassType parent) {
		if (isClass(parent))
			return Stream.empty();

        return implementersOf(parent).filter(e -> isClass(e));
	}

    /*
	 * Produces a set of all the methods that implement all the methods defined in a given interface
	 * or class excluding constructions and static initializers. That is for an interface, it will
	 * produce a set of all methods that implement (have bodies or are native) all methods defined
	 * in the interface. For a class, it will produce a set of all methods that override the methods
	 * in the class including the implemented methods of the given class.
	 */
	public Stream<MethodSignature> getAllImplementingMethods(ClassType org) {
		Set<String> subSigs = view.getClass(org).get().getMethods().stream().map(m -> getSubSignatureWOReturn(m)).collect(Collectors.toSet());

        Stream<SootClass> classes;
		if (isInterface(org)) {
            classes = getAllSubClassesOfInterface(org).map(e -> view.getClass(e).get());
		} else {
            classes = getAllSubClasses(org).map(e -> view.getClass(e).get());
		}

        return classes.flatMap(e -> e.getMethods().stream())
            .filter(m -> 
                !m.isAbstract() && !m.getName().equals("<init>") 
                    && !m.getName().equals("<clinit>") 
                    && subSigs.contains(getSubSignatureWOReturn(m)))
            .map(m -> m.getSignature());
	}

    private String getSubSignatureWOReturn(SootMethod sm) {
        return sm.getName() + "("
            + sm.getParameterTypes().stream()
                .map(Object::toString)
                .collect(Collectors.joining(","))
            + ")";
    }

    public Stream<MethodSignature> getAllImplementingMethods(MethodSignature orgSM) {
		if(orgSM.getName().equals("<init>") || orgSM.getName().equals("<clinit>")) {
            return Stream.of(orgSM);
		}

		ClassType org = orgSM.getDeclClassType();
		MethodSubSignature subSig = orgSM.getSubSignature();

        Stream<SootClass> classes;
		if (isInterface(org)) {
            classes = getAllSubClassesOfInterface(org).map(e -> view.getClass(e).get());
		} else {
            classes = getAllSubClasses(org).map(e -> view.getClass(e).get());
		}

        return classes.flatMap(e -> e.getMethods().stream())
            .filter(m -> !m.isAbstract() && subSig.equals(m.getSignature().getSubSignature()))
            .map(m -> m.getSignature());
	}

    public Stream<MethodSignature> getAllSubClassMethods(ClassType org) {
        Stream<SootClass> classes;
		if (isInterface(org)) {
            classes = getAllSubClassesOfInterface(org).map(e -> view.getClass(e).get());
		} else {
            classes = getAllSubClasses(org).map(e -> view.getClass(e).get());
		}

        return classes.flatMap(e -> e.getMethods().stream()).map(m -> m.getSignature());
    }

    @Override
    public void addType(@Nonnull SootClass sootClass) {
        super.addType(sootClass);
    }

    @Override
    public boolean contains(ClassType type) {
        return super.contains(type);
    }

    @Override
    protected Stream<Vertex> directExtendedInterfacesOf(@Nonnull Vertex interfaceVertex) {
        return super.directExtendedInterfacesOf(interfaceVertex);
    }

    @Override
    protected Stream<Vertex> directImplementedInterfacesOf(@Nonnull Vertex classVertex) {
        return super.directImplementedInterfacesOf(classVertex);
    }

    @Override
    @Nonnull
    public Stream<ClassType> directSubtypesOf(@Nonnull ClassType type) {
        return super.directSubtypesOf(type);
    }

    @Override
    protected Stream<Vertex> directSuperClassOf(@Nonnull Vertex classVertex) {
        return super.directSuperClassOf(classVertex);
    }

    @Override
    @Nonnull
    public Stream<ClassType> directlyExtendedInterfacesOf(@Nonnull ClassType interfaceType) {
        return super.directlyExtendedInterfacesOf(interfaceType);
    }

    @Override
    public Stream<ClassType> directlyImplementedInterfacesOf(@Nonnull ClassType classType) {
        return super.directlyImplementedInterfacesOf(classType);
    }

    @Override
    @Nonnull
    public Stream<ClassType> implementedInterfacesOf(@Nonnull ClassType type) {
        return super.implementedInterfacesOf(type);
    }

    @Override
    @Nonnull
    public Stream<ClassType> implementersOf(@Nonnull ClassType interfaceType) {
        return super.implementersOf(interfaceType);
    }

    @Override
    public boolean isClass(@Nonnull ClassType type) {
        return super.isClass(type);
    }

    @Override
    public boolean isInterface(@Nonnull ClassType type) {
        return super.isInterface(type);
    }

    @Override
    @Nonnull
    public Stream<ClassType> subclassesOf(@Nonnull ClassType classType) {
        return super.subclassesOf(classType);
    }

    @Override
    @Nonnull
    public Stream<ClassType> subtypesOf(@Nonnull ClassType type) {
        return super.subtypesOf(type);
    }

    @Override
    @Nonnull
    public Optional<ClassType> superClassOf(@Nonnull ClassType classType) {
        return super.superClassOf(classType);
    }

    @Override
    @Nonnull
    protected Stream<Vertex> superClassesOf(@Nonnull Vertex classVertex, boolean excludeSelf) {
        return super.superClassesOf(classVertex, excludeSelf);
    }

    @Override
    public boolean isSubtype(@Nonnull Type supertype, @Nonnull Type potentialSubtype) {
        return super.isSubtype(supertype, potentialSubtype);
    }

    @Override
    @Nonnull
    public Stream<ClassType> superClassesOf(@Nonnull ClassType classType) {
        return super.superClassesOf(classType);
    }

    
    
}
