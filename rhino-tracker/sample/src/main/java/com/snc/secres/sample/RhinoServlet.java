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

package com.snc.secres.sample;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RhinoServlet extends HttpServlet {

    private static PrintWriter out = null;

    public RhinoServlet() {
        //init
        Context.enter();
        Context.exit();
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        String script = req.getParameter("code");
        resp.getWriter().println(setupAndDoScriptEval(script));
    }

    private String setupAndDoScriptEval(String script) {
        StringWriter writer = new StringWriter();
        out = new PrintWriter(writer);
        Context ctx = Context.enter();
        try {
            ScriptableObject scope = ctx.initStandardObjects();

            String[] names = {"println", "print"};
            scope.defineFunctionProperties(names, getClass(), ScriptableObject.DONTENUM);

            Object result = doScriptEval(script, scope, ctx);
            String ret = Context.toString(result);
            out.print(ret);

            return writer.toString();
        } catch(RhinoException ex) {
            ex.printStackTrace(out);
            return writer.toString();
        } finally {
            Context.exit();
            out = null;
        }
    }

    private Object doScriptEval(String script, Scriptable scope, Context ctx) {
        return ctx.evaluateString(scope, script, "code", 1, null);
    }

    /**
     * Print the string values of its arguments.
     *
     * <p>This method is defined as a JavaScript function. Note that its arguments are of the
     * "varargs" form, which allows it to handle an arbitrary number of arguments supplied to the
     * JavaScript function.
     */
    public static String println(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if(out != null) {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) out.print(" ");

                // Convert the arbitrary JavaScript value into a string form.
                String s = Context.toString(args[i]);

                out.print(s);
            }
            out.println();
        }
        return "";
    }

    /**
     * Print the string values of its arguments.
     *
     * <p>This method is defined as a JavaScript function. Note that its arguments are of the
     * "varargs" form, which allows it to handle an arbitrary number of arguments supplied to the
     * JavaScript function.
     */
    public static String print(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if(out != null) {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) out.print(" ");

                // Convert the arbitrary JavaScript value into a string form.
                String s = Context.toString(args[i]);

                out.print(s);
            }
        }
        return "";
    }
    
}
