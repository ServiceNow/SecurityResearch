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

package sootup.callgraph;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

import org.jgrapht.graph.DefaultDirectedGraph;

import sootup.callgraph.filter.CallGraphFilter;
import sootup.core.signatures.MethodSignature;
import sootup.core.typehierarchy.AuxViewTypeHierarchy;
import sootup.core.views.View;

public class CallGraphWrapper extends GraphBasedCallGraph {

    private final GraphBasedCallGraph cg;

    public CallGraphWrapper(GraphBasedCallGraph cg) {
        this.cg = cg;
    }
    
    @Override
    public void addMethod(@Nonnull MethodSignature calledMethod) {
        cg.addMethod(calledMethod);
    }

    @Override
    public void addCall(@Nonnull MethodSignature sourceMethod, @Nonnull MethodSignature targetMethod) {
        cg.addCall(sourceMethod, targetMethod);
    }

    @Nonnull
    @Override
    public Set<MethodSignature> getMethodSignatures() {
        return cg.getMethodSignatures();
    }

    @Nonnull
    @Override
    public Set<MethodSignature> callsFrom(@Nonnull MethodSignature sourceMethod) {
        return cg.callsFrom(sourceMethod);
    }

    @Nonnull
    @Override
    public Set<MethodSignature> callsTo(@Nonnull MethodSignature targetMethod) {
        return cg.callsTo(targetMethod);
    }

    @Override
    public boolean containsMethod(@Nonnull MethodSignature method) {
        return cg.containsMethod(method);
    }

    @Override
    public boolean containsCall(@Nonnull MethodSignature sourceMethod, @Nonnull MethodSignature targetMethod) {
        return cg.containsCall(sourceMethod, targetMethod);
    }

    @Override
    public int callCount() {
        return cg.callCount();
    }

    @Override
    public String exportAsDot() {
        return cg.exportAsDot();
    }

    @Nonnull
    @Override
    public MutableCallGraph copy() {
        return new CallGraphWrapper((GraphBasedCallGraph)cg.copy());
    }

    @Nonnull
    @Override
    public Vertex vertexOf(@Nonnull MethodSignature method) {
        return cg.vertexOf(method);
    }

    @Nonnull
    @Override
    public DefaultDirectedGraph<Vertex, Edge> getGraph() {
        return cg.getGraph();
    }

    @Nonnull
    @Override
    public Map<MethodSignature, Vertex> getSignatureToVertex() {
        return cg.getSignatureToVertex();
    }

    @Nonnull
    @Override
    public MethodSignature vertex2MethodSignature(@Nonnull Vertex vertex) {
        return cg.vertex2MethodSignature(vertex);
    }

    @Override
    public String toString() {
        return cg.toString();
    }

    public void removeCall(@Nonnull MethodSignature sourceMethod, @Nonnull MethodSignature targetMethod) {
        getGraph().removeEdge(vertexOf(sourceMethod), vertexOf(targetMethod));
    }

    public void removeMethod(@Nonnull MethodSignature method) {
        if(calls(method).isEmpty()) {
            removeMethodForce(method);
        }
    }

    // will remove all edges associated with vertex as well
    public void removeMethodForce(@Nonnull MethodSignature method) {
        getGraph().removeVertex(vertexOf(method));
        getSignatureToVertex().remove(method);
    }

    @Nonnull
    public Set<MethodSignature> calls(@Nonnull MethodSignature method) {
        Set<MethodSignature> ret = new LinkedHashSet<>();
        ret.addAll(callsFrom(method));
        ret.addAll(callsTo(method));
        return ret;
    }

    public void applyFilter(CallGraphFilter cgFilter, View view) {
        AuxViewTypeHierarchy typeHierarchy = new AuxViewTypeHierarchy(view);
        for(MethodSignature source : getMethodSignatures()) {
            for(MethodSignature dest : callsFrom(source)) {
                if(cgFilter.deniedEdge(source, dest, typeHierarchy)) {
                    removeCall(source, dest);
                }
            }
        }
    }

}
