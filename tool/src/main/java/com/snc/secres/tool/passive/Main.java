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

import java.io.File;
import java.nio.file.Path;

import com.snc.secres.tool.common.io.FileHelpers;

public class Main {

    private static final String HELPMSG = "Usage: Main [-h|--help] [-c <config file path>]\n" +
                                          "  -c <config file path>       The path to the yaml config file used to configure sootup.\n" +
                                          "  -h, --help                  Show this help message and exit.\n";
    private static final String CN = Main.class.getSimpleName();

    private volatile Config config;
    private volatile Analysis analysis;

    public Main() {
        this.config = null;
        this.analysis = null;
    }

    public int parseArgs(String[] args) {
        if(args == null || args.length == 0) {
            System.err.println(CN + ": No arguments.\n\n" + HELPMSG);
            return 0;
        }
        
        for(int i = 0; i < args.length; i++) {
            switch(args[i]) {
                case "-h":
                case "--help":
                    System.out.println(CN + ": Help message requested.\n\n" + HELPMSG);
                    return 2;
                case "-c":
                    String inPath = args[++i];
                    if(inPath.length() > 0 && inPath.charAt(inPath.length()-1) == File.separatorChar)
                        inPath = inPath.substring(0, inPath.length()-1);
                    Path configFile = FileHelpers.getPath(inPath);
                    if(FileHelpers.checkRWFileExists(configFile)) {
                        try {
                            this.config = Config.readFromFile(configFile);
                        } catch(Exception e) {
                            System.err.println(CN + ": Failed to read config file " + configFile + ".\n\n");
                            e.printStackTrace();
                            return 0;
                        }
                    } else {
                        System.err.println(CN + ": Non-readable config file " + configFile + ".");
                        return 0;
                    }
                    break;
                default:
                    System.err.println(CN + ": Unknown argument " + args[i] + ".\n\n" + HELPMSG);
                    return 0;
            }
        }
        return 1;
    }

    public boolean initAnalysis() {
        if(config == null) {
            System.err.println(CN + ": A config file must be provided.");
            return false;
        }

        Path outDir = config.getOutputDirPath();
        try {
            FileHelpers.processDirectory(outDir, true, false);
        } catch(Exception t) {
            System.err.println(CN + ": Failed to verify/create output directory " + outDir + ".\n\n");
            t.printStackTrace();
            return false;
        }

        this.analysis = Analysis.makeAnalysis(config);
        if(this.analysis == null) {
            return false;
        }
        return true;
    }

    

    public static void main(String[] args) {
        int success = 0;
        try {
            Main main = new Main();
            success = main.parseArgs(args);
            if (success == 1) {
                if(main.initAnalysis()) {
                    main.analysis.run();
                } else {
                    success = 0;
                }
            }
        } catch(Throwable t) {
            success = 0;
            System.err.println(CN + ": Unexpected exception.\n\n");
            t.printStackTrace();
        } finally {
            if(success == 1 || success == 2)
                System.exit(0);
            else
                System.exit(1);
        }
    }
}
