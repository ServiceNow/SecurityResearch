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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import sootup.core.signatures.MethodSignature;
import sootup.core.typehierarchy.AuxViewTypeHierarchy;

public class CallGraphFilter {

    private final boolean defaultDeny;
    private final List<Entry> entries;

    private volatile AuxViewTypeHierarchy cachedTypeHierarchy;
    private volatile Map<MethodSignature,Boolean> cache;

    private CallGraphFilter(boolean defaultDeny, List<Entry> entries) {
        this.defaultDeny = defaultDeny;
        this.entries = entries;

        this.cachedTypeHierarchy = null;
        this.cache = null;
    }

    public boolean deniedEdge(MethodSignature source, MethodSignature destination, AuxViewTypeHierarchy typeHierarchy) {
        if(this.cachedTypeHierarchy == null || !this.cachedTypeHierarchy.equals(typeHierarchy)) {
            this.cache = new HashMap<>();
            this.cachedTypeHierarchy = typeHierarchy;
        }

        Boolean ret = cache.get(source);
        if(ret != null) {
            return ret;
        } else {
            for(Entry e : entries) {
                if(e.matches(source, typeHierarchy)) {
                    cache.put(source, e.denyIfMatch());
                    return e.denyIfMatch();
                }
            }
            cache.put(source, defaultDeny);
            return defaultDeny;
        }
    }

    public static CallGraphFilter makeCallGraphFilter(String defaultPolicy, List<Map<String,String>> entries) {
        defaultPolicy = defaultPolicy == null ? "allow" : defaultPolicy.trim().toLowerCase();
        boolean defaultDeny;
        if(defaultPolicy.equals("deny")) {
            defaultDeny = true;
        } else if(defaultPolicy.equals("allow")) {
            defaultDeny = false;
        } else {
            throw new IllegalArgumentException("Error: 'filter_default_policy' must be either deny or allow.");
        }

        if(entries != null) {
            List<Entry> newEntries = new ArrayList<>();
            for(Map<String,String> entry : entries) {
                Set<String> keysToRemove = new HashSet<>();
                entry.keySet().forEach(o -> {
                    if(!o.equals(o.trim().toLowerCase()))
                        keysToRemove.add(o);
                });
                keysToRemove.forEach(o -> entry.put(o.trim().toLowerCase(), entry.remove(o)));

                String deny = entry.getOrDefault("policy", "deny").trim().toLowerCase();
                boolean isDeny;
                if(deny.equals("deny")) {
                    isDeny = true;
                } else if(deny.equals("allow")) {
                    isDeny = false;
                } else {
                    throw new IllegalArgumentException("Error: 'policy' must be either deny or allow.");
                }

                String type = entry.getOrDefault("type", "noop").trim().toLowerCase();
                if("class_path".equals(type) || "interface_class_path".equals(type) || "super_class_path".equals(type)) {
                    String pattern = entry.getOrDefault("pattern", null);
                    if(pattern == null || pattern.isBlank()) {
                        throw new IllegalArgumentException("Error: A non-empty 'pattern' must be given for a types 'class_path', 'interface_class_path', and 'super_class_path'.");
                    }
                    String exact = entry.getOrDefault("exact", "false").trim().toLowerCase();
                    boolean isExact;
                    if(exact.equals("true")) {
                        isExact = true;
                    } else if(exact.equals("false")) {
                        isExact = false;
                    } else {
                        throw new IllegalArgumentException("Error: 'exact' must be either true or false for types 'class_path', 'interface_class_path', and 'super_class_path'.");
                    }
                    
                    if("class_path".equals(type)) {
                        newEntries.add(new ClassEntry(isDeny, pattern, isExact));
                    } else {
                        String allSubClassMethods = entry.getOrDefault("all_sub_class_methods", "false").trim().toLowerCase();
                        boolean isAllSubClassMethods;
                        if(allSubClassMethods.equals("true")) {
                            isAllSubClassMethods = true;
                        } else if(allSubClassMethods.equals("false")) {
                            isAllSubClassMethods = false;
                        } else {
                            throw new IllegalArgumentException("Error: 'all_sub_class_methods' must be either true or false for types 'interface_class_path' or 'super_class_path'.");
                        }
                        newEntries.add("interface_class_path".equals(type) ? new InterfaceEntry(isDeny, pattern, isExact, isAllSubClassMethods) : new SuperEntry(isDeny, pattern, isExact, isAllSubClassMethods));
                    }
                } else {
                    throw new IllegalArgumentException("Error: Unrecognized type " + Objects.toString(type));
                }
            }
            return new CallGraphFilter(defaultDeny, newEntries);
        } else {
            return new CallGraphFilter(defaultDeny, Collections.emptyList());
        }
    }
    
}
