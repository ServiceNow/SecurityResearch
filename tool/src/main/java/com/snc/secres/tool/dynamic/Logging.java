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

package com.snc.secres.tool.dynamic;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.snc.secres.tool.common.io.FileHelpers;

public class Logging {

    private static Logger logger = null;

    public static PrintStream getLoggingPrintStream() {
        Logger logger = getLogger();
        PrintStream ret = System.out;
        if(logger == null) {
            return ret;
        } else {
            try {
                return new LoggerPrintStream(logger);
            } catch (UnsupportedEncodingException e) {
                // Uses default system char set so no exception
                return ret;
            } 
        }
    }

    public static PrintStream getLoggingPrintStream(Level level) {
        Logger logger = getLogger();
        PrintStream ret = level.equals(Level.SEVERE) || level.equals(Level.WARNING) ? System.err : System.out;
        if(logger == null) {
            return ret;
        } else {
            try {
                return new LoggerPrintStream(logger, level);
            } catch (UnsupportedEncodingException e) {
                // Uses default system char set so no exception
                return ret;
            } 
        }
    }

    public static final void info(String msg) {
        log(Level.INFO, msg);
    }

    public static final void info(String msg, Throwable e) {
        log(Level.INFO, msg, e);
    }

    public static final void warn(String msg) {
        log(Level.WARNING, msg);
    }

    public static final void warn(String msg, Throwable e) {
        log(Level.WARNING, msg, e);
    }

    public static final void error(String msg) {
        log(Level.SEVERE, msg);
    }

    public static final void error(String msg, Throwable e) {
        log(Level.SEVERE, msg, e);
    }

    public static final void log(Level level, String msg) {
        log(level, msg, null);
    }

    public static final void log(Level level, String msg, Throwable e) {
        Logger logger = getLogger();
        if(logger == null) {
            PrintStream ret = level.equals(Level.SEVERE) || level.equals(Level.WARNING) ? System.err : System.out;
            ret.println(msg);
            if(e != null) {
                e.printStackTrace(ret);
            }
        } else {
            if(e != null) {
                logger.log(level, msg, e);
            } else {
                logger.log(level, msg);
            }
        }
    }

    public static final Logger getLogger() {
        return Logging.logger;
    }

    public static void closeLogging() {
        Logger logger = getLogger();
        if(logger != null) {
            for (Handler h : logger.getHandlers()) {
                h.close();
                logger.removeHandler(h);
            }
            Logging.logger = null;
        }
    }
    
    public static Logger setupLogging(Path logDir, String name, String timestamp) throws SecurityException, IOException {
        Logger logger = Logger.getLogger(name);
        logger.setLevel(Level.INFO);
        logger.setUseParentHandlers(false);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        FileHandler fileHandler = new FileHandler(FileHelpers.getPath(logDir, name + "_" + timestamp + ".log").toString());
        consoleHandler.setLevel(Level.INFO);
        fileHandler.setLevel(Level.INFO);

        Formatter formatter = new Formatter() {
            private String format = "%1$tF %1$tT [%4$s] %3$s(%2$s): %5$s%6$s%n";

            @Override
            public String format(LogRecord record) {
                ZonedDateTime zdt = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());
                String source;
                if (record.getSourceClassName() != null) {
                    source = record.getSourceClassName();
                    if (record.getSourceMethodName() != null) {
                        source += " " + record.getSourceMethodName();
                    }
                } else {
                    source = record.getLoggerName();
                }
                String message = formatMessage(record);
                String throwable = "";
                if (record.getThrown() != null) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    pw.println();
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    throwable = sw.toString();
                }
                return String.format(format, zdt, source, record.getLoggerName(), 
                                    record.getLevel().getLocalizedName(), message, throwable);
            }
        };

        consoleHandler.setFormatter(formatter);
        fileHandler.setFormatter(formatter);

        logger.addHandler(fileHandler);
        logger.addHandler(consoleHandler);
        
        Logging.logger = logger;
        return logger;
    }

}
