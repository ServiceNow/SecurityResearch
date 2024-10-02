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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import com.snc.secres.tool.common.io.FileHelpers;
import com.snc.secres.tool.common.io.PrintStreamUnixEOL;

public class Config {

    private String class_path;
    private String runtime_trace_file_path;
    private String output_dir_path;
    private String call_graph_algo;
    private String main_method_sig;
    private String entry_point_method_sig;
    private String sink_method_sig;
    private String filter_default_policy;
    private List<Map<String,String>> filter;

    public Config() {}

    public Config(String class_path, String runtime_trace_file_path, String output_dir_path, String call_graph_algo, 
            String main_method_sig, String entry_point_method_sig, String sink_method_sig, String filter_default_policy, List<Map<String,String>> filter) {
        this.class_path = class_path;
        this.runtime_trace_file_path = runtime_trace_file_path;
        this.output_dir_path = output_dir_path;
        this.call_graph_algo = call_graph_algo;
        this.main_method_sig = main_method_sig;
        this.entry_point_method_sig = entry_point_method_sig;
        this.sink_method_sig = sink_method_sig;
        this.filter_default_policy = filter_default_policy;
        this.filter = filter;
    }
    
    @Override
    public String toString() {
        return "Config [class_path=" + class_path + ", runtime_trace_file_path=" + runtime_trace_file_path
                + ", output_dir_path=" + output_dir_path + ", call_graph_algo=" + call_graph_algo + ", main_method_sig="
                + main_method_sig + ", entry_point_method_sig=" + entry_point_method_sig + ", sink_method_sig="
                + sink_method_sig + ", filter_default_policy=" + filter_default_policy + ", filter=" + filter + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((class_path == null) ? 0 : class_path.hashCode());
        result = prime * result + ((runtime_trace_file_path == null) ? 0 : runtime_trace_file_path.hashCode());
        result = prime * result + ((output_dir_path == null) ? 0 : output_dir_path.hashCode());
        result = prime * result + ((call_graph_algo == null) ? 0 : call_graph_algo.hashCode());
        result = prime * result + ((main_method_sig == null) ? 0 : main_method_sig.hashCode());
        result = prime * result + ((entry_point_method_sig == null) ? 0 : entry_point_method_sig.hashCode());
        result = prime * result + ((sink_method_sig == null) ? 0 : sink_method_sig.hashCode());
        result = prime * result + ((filter_default_policy == null) ? 0 : filter_default_policy.hashCode());
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
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
        Config other = (Config) obj;
        if (class_path == null) {
            if (other.class_path != null)
                return false;
        } else if (!class_path.equals(other.class_path))
            return false;
        if (runtime_trace_file_path == null) {
            if (other.runtime_trace_file_path != null)
                return false;
        } else if (!runtime_trace_file_path.equals(other.runtime_trace_file_path))
            return false;
        if (output_dir_path == null) {
            if (other.output_dir_path != null)
                return false;
        } else if (!output_dir_path.equals(other.output_dir_path))
            return false;
        if (call_graph_algo == null) {
            if (other.call_graph_algo != null)
                return false;
        } else if (!call_graph_algo.equals(other.call_graph_algo))
            return false;
        if (main_method_sig == null) {
            if (other.main_method_sig != null)
                return false;
        } else if (!main_method_sig.equals(other.main_method_sig))
            return false;
        if (entry_point_method_sig == null) {
            if (other.entry_point_method_sig != null)
                return false;
        } else if (!entry_point_method_sig.equals(other.entry_point_method_sig))
            return false;
        if (sink_method_sig == null) {
            if (other.sink_method_sig != null)
                return false;
        } else if (!sink_method_sig.equals(other.sink_method_sig))
            return false;
        if (filter_default_policy == null) {
            if (other.filter_default_policy != null)
                return false;
        } else if (!filter_default_policy.equals(other.filter_default_policy))
            return false;
        if (filter == null) {
            if (other.filter != null)
                return false;
        } else if (!filter.equals(other.filter))
            return false;
        return true;
    }

    public String getClassPath() {
        return class_path == null || class_path.isBlank() ? "" : class_path;
    }

    public Path getRuntimeTraceFilePath() {
        return runtime_trace_file_path == null || runtime_trace_file_path.isBlank() ? FileHelpers.getPath("./config.yaml") : FileHelpers.getPath(runtime_trace_file_path);
    }

    public Path getOutputDirPath() {
        return output_dir_path == null || output_dir_path.isBlank() ? FileHelpers.getPath(".") : FileHelpers.getPath(output_dir_path);
    }

    public String getCallGraphAlgo() {
        return call_graph_algo == null || call_graph_algo.isBlank() ? "rta" : call_graph_algo;
    }

    public String getMainMethodSig() {
        return main_method_sig == null || main_method_sig.isBlank() ? "" : main_method_sig;
    }

    public String getEntryPointMethodSig() {
        return entry_point_method_sig == null || entry_point_method_sig.isBlank() ? "" : entry_point_method_sig;
    }

    public String getSinkMethodSig() {
        return sink_method_sig == null || sink_method_sig.isBlank() ? "" : sink_method_sig;
    }

    public String getFilterDefaultPolicy() {
        return filter_default_policy == null ? "allow" : filter_default_policy;
    }

    public List<Map<String,String>> getFilterEntries() {
        return filter == null ? Collections.emptyList() : filter;
    }

    // For yaml

    public String getClass_path() {
        return class_path;
    }

    public String getRuntime_trace_file_path() {
        return runtime_trace_file_path;
    }

    public String getOutput_dir_path() {
        return output_dir_path;
    }

    public String getCall_graph_algo() {
        return call_graph_algo;
    }

    public String getMain_method_sig() {
        return main_method_sig;
    }

    public String getEntry_point_method_sig() {
        return entry_point_method_sig;
    }

    public String getSink_method_sig() {
        return sink_method_sig;
    }

    public String getFilter_default_policy() {
        return filter_default_policy;
    }

    public List<Map<String, String>> getFilter() {
        return filter;
    }

    public void setClass_path(String class_path) {
        this.class_path = class_path;
    }

    public void setRuntime_trace_file_path(String runtime_trace_file_path) {
        this.runtime_trace_file_path = runtime_trace_file_path;
    }

    public void setOutput_dir_path(String output_dir_path) {
        this.output_dir_path = output_dir_path;
    }

    public void setCall_graph_algo(String call_graph_algo) {
        this.call_graph_algo = call_graph_algo;
    }

    public void setMain_method_sig(String main_method_sig) {
        this.main_method_sig = main_method_sig;
    }

    public void setEntry_point_method_sig(String entry_point_method_sig) {
        this.entry_point_method_sig = entry_point_method_sig;
    }

    public void setSink_method_sig(String sink_method_sig) {
        this.sink_method_sig = sink_method_sig;
    }

    public void setFilter_default_policy(String filter_default_policy) {
        this.filter_default_policy = filter_default_policy;
    }

    public void setFilter(List<Map<String, String>> filter) {
        this.filter = filter;
    }
    
    // For yaml

    public static Config readFromFile(Path file) throws IOException {
        LoaderOptions loaderoptions = new LoaderOptions();
        loaderoptions.setAllowDuplicateKeys(false);
        loaderoptions.setAllowRecursiveKeys(false);
        loaderoptions.setWrappedToRootException(true);
        Yaml yaml = new Yaml(new Constructor(Config.class, loaderoptions));

        try(InputStream in = Files.newInputStream(file)) {
            return yaml.load(in);
        }
    }
    
    public void writeToFile(Path outFile) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        // Fix below - additional configuration
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer customRepresenter = new Representer(options);
        customRepresenter.addClassTag(Config.class, Tag.MAP);

        Yaml yaml = new Yaml(new Constructor(Config.class, new LoaderOptions()), customRepresenter);
        try(PrintWriter out = new PrintWriter(new PrintStreamUnixEOL(Files.newOutputStream(outFile)))) {
            yaml.dump(this, out);
        }
    }
    
}
