# Rhino Tracker - Java to JavaScript to Java

Rhino Tracker is a proof of concept of how one might construct a call graph of call flows that go from Java code through the JavaScript interpreter (Rhino[^1]) back to Java code. The poc uses instrumentation (via Byte Buddy[^3]) to capture calls that flow from a given JavaScript at runtime to Java code. It then uses SootUp[^2] to construct a call graph that models the call flow from Java code into the Rhino interpreter that runs the JavaScript. Lastly, SootUp is used to substitute all outgoing call flows of the Rhino interpreter with the JavaScript to Java calls captured at runtime. The final output is a Java to JavaScript to Java call graph. More information about how Rhino Tracker was developed can be found on the *A Rhino of a Problem* blog post[^4].

## Build

First install some flavor of JDK11. This may work with newer JDK versions but it has not been tested for them. Then build all dependencies for Rhino Tracker by running:

```
gradle build
```

## Running Rhino Tracker

To construct a call graph of the Java to JavaScript to Java call flows of a servlet containing Rhino[^1], Rhino Tracker is divided into two phases. The first is a dynamic analysis phase that relies on instrumentation using Byte Buddy[^3] to capture the Java calls being made from a given JavaScript. The second is a static analysis phase that uses SootUp[^2] to construct a call graph of the Java servlet. The edges to the Rhino method performing the JavaScript interpretation are then substituted for edges to those methods captured during the first phase. More information about this process can be found on the *A Rhino of a Problem* blog post[^4]. Details on how to configure and run these phases are found below along with a description on how to run the sample Rhino servlet.

### Instrumentation of Sample Servlet (Dynamic Analysis)

The instrumentation and runtime capture runs without user input. However, the script being run and other options (e.g., output directory locations) can be configured through the config yaml file. The config yaml file can be provided through the `-c` option or directly through jvmargs. To run the instrumentation of the sample rhino servlet do:

```bash
gradle runDynamic
```
or
```bash
gradle runDynamic --args='-c sample/dynamic_config.yaml'
```
or
```bash
gradle runDynamic -Djvmargs='-javaagent:tool/build/libs/tool.jar=sample/dynamic_config.yaml'
```
or
```bash
java -javaagent:tool/build/libs/tool.jar=sample/dynamic_config.yaml -jar sample/build/libs/sample.jar
```

A default/example config yaml file that runs the sample JavaScript can be found [here](sample/dynamic_config.yaml). The available options for the config yaml file are outlined below.

```yaml
# The directory where log files are written
log_dir_path: 'work/dynamic_logs'
# The directory where the java methods called when sampling 
# the JavaScript at `js_file_path` are written. The java 
# methods sampled are written to a file named `timestamp +
# "__" + js_full_class_name + "__.txt"`.
out_dir_path: 'work/java_call_traces'
# File path to the JavaScript code to be sampled.
js_file_path: 'sample/sample.test.js'
# The full class name of the JavaScript code being sampled.
js_full_class_name: 'sample.test'
# The url of the Rhino servlet. By default Rhino Tracker is
# designed for the sample Rhino servlet but any Rhino servlet
# of similar structure can be used.
instance_url: 'http://127.0.0.1:8080'
# The number of times Rhino tracker attempts to connect to the
# Rhino servlet before exiting with an error.
connect_tries: 30
# How long Rhino Tracker waits for a connection response before
# timing out. This value is in milliseconds.
connect_timeout: 120000
# How long Rhino Tracker sleeps between connection attempts.
# This value is in milliseconds.
time_between_connect_attempts: 10000
# The directory where Rhino compiled JavaScript class files are
# written.
compiled_js_dir_path: 'work/compiled_javascript'
```

### Construct Call Graph of Sample Servlet (Static Analysis)

The call graph construction is configured with a yaml config file which can be passed in using the
`-c` argument. SootUp also may require more memory than
normal java applications when construction the call graph. This can be
adjusted using the jvmargs. See below for examples on how to pass args
and jvmargs. To run the call graph construction do:

```bash
gradle runStatic
```
or
```bash
gradle runStatic --args='-c sample/static_config.yaml'
```
or
```bash
gradle runStatic -Djvmargs='-Xms2g -Xmx4g'
```
or
```bash
java -jar tool/build/libs/tool.jar -c sample/static_config.yaml
```

A default/example yaml config file can be found [here](sample/static_config.yaml). The available options for the config yaml file are outlined below.

```yaml
# The call graph algorithm to use when constructing
# the call graph. The options are cha or rta.
call_graph_algo: rta
# The path of the code being analyzed. This is specified
# in the same way one would specify a class path when
# launching a Java application.
class_path: sample/build/libs/sample.jar
# The signature of the entry point method for the call 
# graph. This should be the method where the call graph 
# should start.
entry_point_method_sig: '<com.snc.secres.sample.RhinoServlet: void doPost(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)>'
# The signature of the main method for the Java 
# application. This does not have to be the actual main
# method. It just needs to be a method that initializes
# all classes important to the analysis before calling
# the entry point method. This method is not required
# for call graphs constructed using cha.
main_method_sig: '<com.snc.secres.sample.DummyMain: void main(java.lang.String[])>'
# The output directory where the call graph dot files
# are written. The file name of the dot file is dependent
# on the file name of `runtime_trace_file_path`. It will
# have the format `js_full_class_name + timestamp + ".dot"`.
output_dir_path: work/cg
# A path to a file created by sampling the provided
# JavaScript during the instrumentation phase. The 
# name of the file should be the same as it was when
# it was created as it is used to name the output file
# for this phase.
runtime_trace_file_path: work/java_call_traces/2024-07-17_13-24-53__sample.test__.txt
# The signature of the method used to evaluate the 
# the sample JavaScript file at runtime. This method 
# will be replaced in the call graph with the methods
# from the runtime trace.
sink_method_sig: '<org.mozilla.javascript.Context: java.lang.Object evaluateString(org.mozilla.javascript.Scriptable, java.lang.String, java.lang.String, int, java.lang.Object)>'
# Call graph filter configuration. This filter removes 
# all edges from methods not defined within the package 
# com\.snc\.secres\.sample.*. More information on 
# the call graph filter is provided below.
filter_default_policy: deny
filter:
  - type: class_path
    pattern: 'com\.snc\.secres\.sample.*'
    policy: allow
```

### Run Sample Servlet

The sample Rhino servlet presents a webpage on `http://127.0.0.1:8080` where one can run any JavaScript through the Rhino interpreter to get an output. This illustrates a simple program flow that a server may have when running JavaScript with rhino. To run the sample Rhino servlet do:

```bash
gradle runSample
```
or
```bash
java -jar sample/build/libs/sample.jar
```

## Call Graph Filter Configuration

A call graph may contain paths to segments of code that are unrelated to the analysis being performed. These paths could increase analysis time, result in noise in the output data, or increase the manual effort involved in the analysis. As such, reducing the scope of the call graph further may be desired. To do this, `CallGraphFilter` was created. The `CallGraphFilter` allows a user to specify specific class methods whose outgoing edges are to be removed from the call graph. This effectively makes these methods end nodes in the graph. The filter itself is a allow/deny list of Java methods. The source code for the call graph filter can be found [here](tool/src/main/java/sootup/callgraph/filter).

For Rhino Tracker, the filter is defined in the yaml config file. Each entry in the filter has a `policy` key that defines the action for the methods that match that entry. The key takes the values `allow` or `deny`. A value of `deny` implies that any matching method will have its outgoing edges removed from the call graph. On the other hand, a value of `allow` implies that any match method will have its outgoing edges included in the call graph. Entries in the call graph filter are applied in a top down order and only the first matching is considered. Additionally, the key `filter_default_policy` defines the default policy for the filter and takes the values `allow` or `deny` as well. The default policy is applied if no other entry in the filter matches a method. Note the default value for `filter_default_policy` is `allow` and the default value for `policy` is `deny`.

There are three different types of entries in the filter. These are specified using the key `type`. The first entry type `class_path` matches methods by class path. In other words, if the full class path of a method matches the entry then the policy for that entry is applied. The `class_path` entry has the additional options of `pattern` and `exact`. The option `pattern` specifies the class path for an entry as either a regular expression or an exact string. The option `exact` takes values `true` or `false` to indicate how to treat the `pattern` when matching. The default value for `exact` is false.

```yaml
filter_default_policy: deny
filter:
  - type: class_path
    pattern: 'com\.snc\.secres\.sample.*'
    policy: allow
```
<sub>This filter limits the call graph to only those classes/methods defined in the sample server code. In other words, all methods not in the package `com\.snc\.secres\.sample.*` are treated as end nodes.Note the `policy` field of the entry denotes wether this entry should allow the classes specified in the call graph or deny them. The default policy of the call graph filter is specified by `filter_default_policy`.</sub>

 The second entry type `interface_class_path` is similar to `class_path` except the classes identified by `pattern` must be interfaces. This is because `interface_class_path` matches all implemented methods of a interface for all of its child classes. The type `interface_class_path` also has an additional option of `all_sub_class_methods` which accepts values of `true` or `false`. If `all_sub_class_methods` is set to true, all methods of all classes implementing the interfaces identified by `pattern` will be considered a match, not just those methods defined in the interfaces. The default value for `all_sub_class_methods` is `false`.

 ```yaml
filter_default_policy: allow
filter:
  - type: interface_class_path
    pattern: 'java.util.Map'
    policy: deny
    exact: true
```
<sub>This filter removes all outgoing edges of the methods defined in the `Map` interface but implemented in its child classes. All other methods are allowed through the filter.</sub>

 The third and final entry type is `super_class_path`. This has the same options as `interface_class_path` and functions similarly except the classes identified by `pattern` must be parent classes. This is because `super_class_path` matches all methods of a parent class that are overridden in a child class. The option `all_sub_class_methods` functions in a similar manner when set to true except it applies to all methods of all classes that extend the classes identified by `pattern`.

 ```yaml
filter_default_policy: allow
filter:
  - type: super_class_path
    pattern: 'java.util.HashMap'
    policy: deny
    exact: true
```
<sub>This filter removes all outgoing edges of the methods defined in the `HashMap` class but implemented in it and its child classes. All other methods are allowed through the filter. Add `all_sub_class_methods: true` to exclude all methods of all child classes of `HashMap`.</sub>

## Updating License For New Files

This project uses [license-eye](https://github.com/apache/skywalking-eyes?tab=readme-ov-file) to manage the license header of files. The config for license-eye is at `.licenserc.yaml`. Note while license-eye can be configured to use GitHub Actions this project does not use them because the repo is shared with other projects. 

```bash
# check for files missing a license and in general see what files will be modified
license-eye header check
# add the license to the files
license-eye header fix  
```

[^1]: <https://github.com/mozilla/rhino>
[^2]: <https://soot-oss.github.io/SootUp/develop/>
[^3]: <https://bytebuddy.net/#/>
[^4]: <https://securitylab.servicenow.com/research/2024-07-29-a-rhino-of-a-problem/>
