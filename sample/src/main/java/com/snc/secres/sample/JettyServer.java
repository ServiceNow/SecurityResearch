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

import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
/**
 * A Jetty server for processing js through rhino and displaying
 * the return value.
 */
public class JettyServer {
    
    private Server server;
    
    /**
     * Initialize and start the server.
     */
    public void start() {
        try {
            // Server is available on http://localhost:8080/
            server = new Server();
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(8080);
            server.setConnectors(new Connector[] {connector});
            
            // Grab the path to the index.html in the jar file
            URL url = getClass().getResource("/root/index.html");
            if (url == null) {
                throw new RuntimeException("Unable to find index.html resource.");
            }
            URI uri = url.toURI();
            // Paths needs a file system to be inited before it can process a zip file
            Map<String, String> env = new HashMap<>(); 
            env.put("create", "true");
            FileSystems.newFileSystem(uri, env);
            Path webRootUri = Paths.get(uri).getParent();

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            context.setBaseResource(Resource.newResource(webRootUri));
            context.setWelcomeFiles(new String[]{"index.html"});
            server.setHandler(context);

            // Servlet to process js through rhino and return the return value
            ServletHolder holderHome = new ServletHolder("rhino", new RhinoServlet());
            context.addServlet(holderHome,"/rhino");

            // Servlet to display the index.html page
            ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
            holderPwd.setInitParameter("dirAllowed","true");
            context.addServlet(holderPwd,"/");

            // Start the server, redirect output to err, and join the thread
            server.start();
            server.dump(System.err);
            server.join();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }
}