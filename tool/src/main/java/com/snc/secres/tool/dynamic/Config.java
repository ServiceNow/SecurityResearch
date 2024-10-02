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
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import com.snc.secres.tool.common.io.FileHelpers;
import com.snc.secres.tool.common.io.PrintStreamUnixEOL;

public class Config {

    private String log_dir_path;
    private String out_dir_path;
    private String js_file_path;
    private String js_full_class_name;
    private String instance_url;
    private Integer connect_tries;
    private Integer connect_timeout;
    private Integer time_between_connect_attempts;
    private String compiled_js_dir_path;

    public Config() {}

    public Config(String log_dir_path, String out_dir_path, String js_file_path, String js_full_class_name,
            String instance_url, Integer connect_tries, Integer connect_timeout, Integer time_between_connect_attempts,
            String compiled_js_dir_path) {
        this.log_dir_path = log_dir_path;
        this.out_dir_path = out_dir_path;
        this.js_file_path = js_file_path;
        this.js_full_class_name = js_full_class_name;
        this.instance_url = instance_url;
        this.connect_tries = connect_tries;
        this.connect_timeout = connect_timeout;
        this.time_between_connect_attempts = time_between_connect_attempts;
        this.compiled_js_dir_path = compiled_js_dir_path;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((log_dir_path == null) ? 0 : log_dir_path.hashCode());
        result = prime * result + ((out_dir_path == null) ? 0 : out_dir_path.hashCode());
        result = prime * result + ((js_file_path == null) ? 0 : js_file_path.hashCode());
        result = prime * result + ((js_full_class_name == null) ? 0 : js_full_class_name.hashCode());
        result = prime * result + ((instance_url == null) ? 0 : instance_url.hashCode());
        result = prime * result + ((connect_tries == null) ? 0 : connect_tries.hashCode());
        result = prime * result + ((connect_timeout == null) ? 0 : connect_timeout.hashCode());
        result = prime * result
                + ((time_between_connect_attempts == null) ? 0 : time_between_connect_attempts.hashCode());
        result = prime * result + ((compiled_js_dir_path == null) ? 0 : compiled_js_dir_path.hashCode());
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
        if (log_dir_path == null) {
            if (other.log_dir_path != null)
                return false;
        } else if (!log_dir_path.equals(other.log_dir_path))
            return false;
        if (out_dir_path == null) {
            if (other.out_dir_path != null)
                return false;
        } else if (!out_dir_path.equals(other.out_dir_path))
            return false;
        if (js_file_path == null) {
            if (other.js_file_path != null)
                return false;
        } else if (!js_file_path.equals(other.js_file_path))
            return false;
        if (js_full_class_name == null) {
            if (other.js_full_class_name != null)
                return false;
        } else if (!js_full_class_name.equals(other.js_full_class_name))
            return false;
        if (instance_url == null) {
            if (other.instance_url != null)
                return false;
        } else if (!instance_url.equals(other.instance_url))
            return false;
        if (connect_tries == null) {
            if (other.connect_tries != null)
                return false;
        } else if (!connect_tries.equals(other.connect_tries))
            return false;
        if (connect_timeout == null) {
            if (other.connect_timeout != null)
                return false;
        } else if (!connect_timeout.equals(other.connect_timeout))
            return false;
        if (time_between_connect_attempts == null) {
            if (other.time_between_connect_attempts != null)
                return false;
        } else if (!time_between_connect_attempts.equals(other.time_between_connect_attempts))
            return false;
        if (compiled_js_dir_path == null) {
            if (other.compiled_js_dir_path != null)
                return false;
        } else if (!compiled_js_dir_path.equals(other.compiled_js_dir_path))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Config [log_dir_path=" + log_dir_path + ", out_dir_path=" + out_dir_path + ", js_file_path="
                + js_file_path + ", js_full_class_name=" + js_full_class_name + ", instance_url=" + instance_url
                + ", connect_tries=" + connect_tries + ", connect_timeout=" + connect_timeout
                + ", time_between_connect_attempts=" + time_between_connect_attempts + ", compiled_js_dir_path="
                + compiled_js_dir_path + "]";
    }

    public Path getLogDirPath() {
        return log_dir_path == null || log_dir_path.isBlank() ? FileHelpers.getPath("./work/dynamic_logs") : FileHelpers.getPath(log_dir_path);
    }

    public Path getOutputDirPath() {
        return out_dir_path == null || out_dir_path.isBlank() ? FileHelpers.getPath("./work/java_call_traces") : FileHelpers.getPath(out_dir_path);
    }

    public Path getJSFilePath() {
        return js_file_path == null || js_file_path.isBlank() ? null : FileHelpers.getPath(js_file_path);
    }

    public String getJSFullClassName() {
        if(js_full_class_name == null || js_full_class_name.isBlank()) {
            Path jsFilePath = getJSFilePath();
            return jsFilePath == null ? null : com.google.common.io.Files.getNameWithoutExtension(jsFilePath.getFileName().toString()).replace(" ", "");
        }
        return js_full_class_name;
    }

    public String getInstanceURL() {
        return instance_url == null || instance_url.isBlank() ? "http://127.0.0.1:8080" : instance_url;
    }

    public int getConnectTries() {
        //5 minutes worth of connection attempts + however long it takes to get a valid response back for each attempt
        return connect_tries == null || connect_tries <= 0 ? 30 : connect_tries;
    }

    public int getConnectTimeout() {
        //wait 2 minutes before timeout
        return connect_timeout == null || connect_timeout <= 999 ? 120000 : connect_timeout;
    }

    public int getTimeBetweenConnectAttempts() {
        //10 seconds
        return time_between_connect_attempts == null || time_between_connect_attempts <= 999 ? 10000 : time_between_connect_attempts;
    }

    public Path getCompiledJSDirPath() {
        return compiled_js_dir_path == null || compiled_js_dir_path.isBlank() ? FileHelpers.getPath("./work/compiled_javascript") : FileHelpers.getPath(compiled_js_dir_path);
    } 

    // for yaml

    public String getLog_dir_path() {
        return log_dir_path;
    }

    public void setLog_dir_path(String log_dir_path) {
        this.log_dir_path = log_dir_path;
    }

    public String getOut_dir_path() {
        return out_dir_path;
    }

    public void setOut_dir_path(String out_dir_path) {
        this.out_dir_path = out_dir_path;
    }

    public String getJs_file_path() {
        return js_file_path;
    }

    public void setJs_file_path(String js_file_path) {
        this.js_file_path = js_file_path;
    }

    public String getJs_full_class_name() {
        return js_full_class_name;
    }

    public void setJs_full_class_name(String js_full_class_name) {
        this.js_full_class_name = js_full_class_name;
    }

    public String getInstance_url() {
        return instance_url;
    }

    public void setInstance_url(String instance_url) {
        this.instance_url = instance_url;
    }

    public Integer getConnect_tries() {
        return connect_tries;
    }

    public void setConnect_tries(Integer connect_tries) {
        this.connect_tries = connect_tries;
    }

    public Integer getConnect_timeout() {
        return connect_timeout;
    }

    public void setConnect_timeout(Integer connect_timeout) {
        this.connect_timeout = connect_timeout;
    }

    public Integer getTime_between_connect_attempts() {
        return time_between_connect_attempts;
    }

    public void setTime_between_connect_attempts(Integer time_between_connect_attempts) {
        this.time_between_connect_attempts = time_between_connect_attempts;
    }

    public String getCompiled_js_dir_path() {
        return compiled_js_dir_path;
    }

    public void setCompiled_js_dir_path(String compiled_js_dir_path) {
        this.compiled_js_dir_path = compiled_js_dir_path;
    }

    // for yaml

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
