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

import java.util.regex.Pattern;

import sootup.core.signatures.MethodSignature;
import sootup.core.typehierarchy.AuxViewTypeHierarchy;

public class ClassEntry extends Entry {

    private final String name;
    private final Pattern pattern;

    public ClassEntry(boolean isDeny, String pattern, boolean isExact) {
        super(isDeny);
        if(isExact) {
            this.pattern = null;
            this.name = pattern;
        } else {
            this.pattern = Pattern.compile(pattern);
            this.name = null;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
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
        ClassEntry other = (ClassEntry) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (pattern == null) {
            if (other.pattern != null)
                return false;
        } else if (!pattern.equals(other.pattern))
            return false;
        return true;
    }

    public String getName() {
        return name;
    }

    public Pattern getPattern() {
        return pattern;
    }

    @Override
    public boolean matches(MethodSignature sig, AuxViewTypeHierarchy typeHierarchy) {
        String cn = sig.getDeclClassType().getFullyQualifiedName();
        return this.name == null ? pattern.matcher(cn).matches() : this.name.equals(cn);
    }

}
