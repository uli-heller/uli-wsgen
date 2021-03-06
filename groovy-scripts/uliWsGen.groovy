import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.CompilationUnit;

import com.sun.tools.ws.WsGen;

import java.lang.reflect.Method;
import java.security.Permission;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.ToolProvider;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

def cli = new CliBuilder(usage: "uli-wsgen.sh [-h] [-d] [-j] [-c classpath] [-f wsdlfile] [-l url] [-p name] [-s name] [-t tns] interfaceName [--wsgen wsgen-opts ...]", posix: true);
cli.with {
    h longOpt: 'help',                                              'Show usage information'
    d longOpt: 'debug',                                             'Print debug information'
    j longOpt: 'javac',                                             'Use java compiler instead of groovy compiler'
    c longOpt: 'classpath',       args: 1, argName: 'classpath',    'Location of the input files'
    f longOpt: 'filename',        args: 1, argName: 'filename',     'Name of the WSDL file to be created'
    i longOpt: 'implClassName',   args: 1, argName: 'package.name', 'Name of the implementation class'
    l longOpt: 'location',        args: 1, argName: 'url',          'URL of the web service'
    n longOpt: 'name',            args: 1, argName: 'name',         'Name annotation attribute used in implementation class'
    p longOpt: 'portName',        args: 1, argName: 'name',         'Port name annotation attribute used in implementation class'
    s longOpt: 'serviceName',     args: 1, argName: 'name',         'Service name annotation attribute used in implementation class [names the wsdl file]'
    t longOpt: 'targetNamespace', args: 1, argName: 'tns',          'Target namespace annotation attribute used in implementation class [no effect]'
}

def printHelp = {
  cli.usage();
  println """
Examples:
  uli-wsgen.sh -c build/classes com.daemonspoint.webservice.SampleWebService
  uli-wsgen.sh -c build/classes -t http://example.de CalculatorWs --wsgen -wsdl""";
  System.exit(0);
}

// We have to do this separately, since otherwise you cannot get help unless you specify a valid
// set of command line parameters. This means for example you have to specify all
// required parameters.
['-h', '--help'].each {
  if (args.contains(it)) {
    printHelp();
  }
}

// There is a mismatch between the parameter handling of 'wsgen' and CliBuilder.
// For arguments after the interface name, this is true for CliBuilder:
//    -- ... is filtered by CliBuilder
//    -X ... is handled fine by CliBuilder
//    -wsdl ... is translated to 'wsdl' by CliBuilder
// In the end this means we have to access the raw parameters.
List<String> argsList = args;
int dashDashIndex = argsList.indexOf('--wsgen' as String);
def wsgenArgs = dashDashIndex < 0 ? [] : argsList[(dashDashIndex+1)..-1];

def options = cli.parse(args);

if (!options) {
  println "Unable to parse command line options -> EXIT";
  System.exit(1);
}

if (options.h) {
  printHelp();
}

boolean fDebug = options.d;
boolean fJavac = options.j;

def log = { String msg ->
  if (fDebug) {
    println msg;
  }
}

String[] parsedArgs = options.arguments();
if (parsedArgs.length < 1) {
  cli.usage();
  System.err.println("Please specify an interface name!");
  System.exit(1);
}
if (parsedArgs.length > 1) {
  if (!parsedArgs[1].startsWith('--wsgen')) {
    cli.usage();
    System.err.println("Please specify only one interface name!");
    System.exit(1);
  }
}

def additionalClasspath = [];
ClassLoader cl = this.getClass().getClassLoader();
if (options.c) {
  cl = new URLClassLoader(new URL[0], cl);
  additionalClasspath = options.c.tokenize(File.pathSeparator);
  additionalClasspath.each {
    cl.addURL(new File(it).toURI().toURL());
  }
}

SourceInterface sourceInterface = new SourceInterface(className: parsedArgs[0], classLoader: cl);
sourceInterface.check();

String wsdlFilename        = options.f ?: "";
String implClassName       = options.i ?: "";
String implName            = Base.getBaseName(implClassName);
String location            = options.l ?: sourceInterface.wsdlLocation();
String targetNamespace     = options.t ?: sourceInterface.targetNamespace();
String name                = options.n ?: ""; // sourceInterface.name() throws an exception!
String portName            = options.p ?: ""; // sourceInterface.portName() not allowed for interfaces
String serviceName         = options.s ?: ""; // sourceInterface.serviceName() not allowed for interfaces

int result=0;
Cleanup cleanup = new Cleanup();
try {
  File temporaryFolder = Base.createTempDirectory();

  String implementationName = implName ?: Implementation.getImplementationName(sourceInterface.getBaseName());
  String implementationClassName = implClassName ?: Base.packageAndClassName(sourceInterface.getPackageName(), implementationName)
  Implementation implementation = new Implementation(className: implementationClassName, topLevel: temporaryFolder, cleanup: cleanup, classPath: options.c, name: name, portName: portName, serviceName: serviceName, targetNamespace: targetNamespace, log: log, useJavac: fJavac);

  String implClassText = implementation.getCode(sourceInterface);
  log(implClassText);
  boolean fSuccess = implementation.compile(implClassText);

  if (!fSuccess) {
    result=1;
    System.err.println "Creation of the class file failed!";
  } else if (! implementation.getClassFile().exists()) {
    result=1;
    System.err.println "Creation of the class file was successful, but the class file doesn't exist!";
  } else {
    String cpForProcessBuilder = temporaryFolder.getAbsolutePath()\
       + File.pathSeparator\
       + options.c\
       + File.pathSeparator\
       + System.getProperty("java.class.path");
    WsGen wsgen = new WsGen();
    def defaultWsGenArgs = [ "-wsdl" ];
    if (WsGen.canInlineSchemas()) {
	defaultWsGenArgs << "-inlineSchemas";
    }
    def additionalArgs = wsgenArgs ?: defaultWsGenArgs;
    wsgen.setArgs([ "-r", temporaryFolder.getAbsolutePath(), "-cp", cpForProcessBuilder, implementationClassName ]);
    wsgen.addArgs(additionalArgs);
    Executor executor = wsgen.getExecutor();
    result = executor.execute(true);
    if (result != 0) {
      System.exit(result);
    }
    Wsdl wsdl = new Wsdl(sourceFolder: temporaryFolder, finalWsdlFilename: wsdlFilename, location: location);
    wsdl.copy();
  }
} finally {
  cleanup.cleanup();
}
return(result);

// http://docs.oracle.com/javase/7/docs/api/javax/tools/JavaCompiler.html
/**
  * A file object used to represent source coming from a string.
  */
class JavaSourceFromString extends SimpleJavaFileObject {
  /**
    * The source code of this "file".
    */
  String code;
  /**
   * Constructs a new JavaSourceFromString.
   * @param name the name of the compilation unit represented by this file object
   * @param code the source code for the compilation unit represented by this file object
   */
  JavaSourceFromString(String name, String code) {
    super(URI.create("string:///" + name.replace('.','/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
    this.code = code;
  }

  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
    return code;
  }
}

class SourceInterface extends Base {
  public ClassLoader classLoader;
  Class clazz;
  javax.jws.WebService webService;

  public boolean check() {
    //this.loadClass();
    if (this.clazz == null) {
      System.err.println("Class '${this.className}' could not be loaded - class not found!");
      System.exit(1);
    }
    if (! isWebService()) {
      System.err.println("Class '${this.className}' is not a jaxws webservice - annotation @WebService is missing");
      System.exit(1);
    }
    if (! this.clazz.isInterface()) {
      System.err.println("Class '${this.className}' is not an interface - please use 'wsgen' directly");
      System.exit(1);
    }
  }

  public void setClassName(String className) {
    super.setClassName(className);
    this.loadClass();
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    this.loadClass();
  }

  private void loadClass() {
    ClassLoader cl = this.classLoader ?: this.getClass().getClassLoader();
    try {
      this.clazz = cl.loadClass(this.className);
      this.webService = this.getWebServiceAnnotation(this.clazz);
    } catch (Exception e) {
      // ignore the exception - we *will* detect this later!
      ;
    }
  }

  String wsdlLocation() {
    return this.webService.wsdlLocation();
  }

  String targetNamespace() {
    return this.webService.targetNamespace();
  }

  String name() {
    return this.webService.name();
  }

  String endpointInterface() {
    return this.webService.endpointInterface();
  }

  String portName() {
    return this.webService.portName();
  }

  String serviceName() {
    return this.webService.serviceName();
  }

  javax.jws.WebService getWebServiceAnnotation(Class clazz) {
    return clazz.getAnnotation(javax.jws.WebService.class);
  }

  boolean isWebService() {
    return null != this.webService;
  }

  public Method[] getMethods() {
    return this.clazz.getMethods();
  }

  public String getBaseName() {
    String name = super.getBaseName();
    return name - "Interface";
  }
}

class Implementation extends Base {
  private final static String IMPL="Impl";
  public File topLevel;
  public Cleanup cleanup;
  public String classPath;
  public String name;
  public String portName;
  public String serviceName;
  public String targetNamespace;
  public boolean useJavac = false;
  public def log;

  public void setTopLevel(File topLevel) {
    this.topLevel = topLevel;
  }

  public String getBaseName() {
    String name = super.getBaseName();
    return name - IMPL;
  }

  public String getImplementationName() {
    return Base.getBaseName(this.className);
  }

  public String getImplementationNameDotClass() {
    return this.getImplementationName() + ".class";
  }

  static public String getImplementationName(String baseName) {
    return baseName + IMPL;
  }

  public File getClassFile() {
    File file = this.topLevel;
    this.getPackageNames().each {
      file = new File(file, it);
    }
    return new File(file, this.getImplementationNameDotClass());
  }

  private void createFolders() {
    cleanup.add(topLevel);
    String[] packageNames = this.getPackageNames();
    File currentFolder = topLevel;
    for (p in packageNames) {
      currentFolder = new File(currentFolder, p);
      cleanup.add(currentFolder);
      currentFolder.mkdir();
    }
  }

  public String getCode(SourceInterface sourceInterface) {
    StringBuffer code = new StringBuffer(256);
    String pn = this.getPackageName();
    def annotationArgs = [
      endpointInterface: sourceInterface.className,
      name:            this.name,
      portName:        this.portName,
      serviceName:     this.serviceName,
      targetNamespace: this.targetNamespace,
    ];
    def annotationList = [];
    annotationArgs.keySet().each {
      String value = annotationArgs.get(it);
      if (this.isNotEmpty(value)) {
        annotationList.add("${it}=\"${value}\"");
      }
    }

    if (this.isNotEmpty(pn)) {
      code.append("package ${pn};\n");
    }
    if (this.isNotEmpty(sourceInterface.packageName)) {
      code.append("import ${sourceInterface.className};");
    }
    code.append("""
      @javax.jws.WebService(${annotationList.join(',')})
      public class ${this.getImplementationName()} implements ${sourceInterface.className} {
    """);
    for (def m in sourceInterface.getMethods()) {
      code.append("\tpublic ${toSourceCode(m.returnType)} ${m.name}(");
      int cnt=0;
      code.append(m.parameterTypes.collect{ "${toSourceCode(it)} p${cnt++}" }.join(","));
      code.append(""") {
          throw new RuntimeException();
        }
      """);
    }
    code.append("}\n");
    //println code.toString();
    return code.toString();
  }

  private String toSourceCode(def type) {
    //println "XX-${type.name} ${type.isArray()} ${type.canonicalName}\n${type.dump()}";
    return type.canonicalName;
  }

  public boolean compile(String code) {
    this.createFolders();
    this.cleanup.add(this.getClassFile());
    return useJavac ? this.compileJavac(code) : this.compileGroovyc(code);
  }

  private boolean compileJavac(String code) {
    log("Using the javac compiler");
    // http://www.java2s.com/Code/Java/JDK-6/CompilingfromMemory.htm
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new NullPointerException("Unable to get an instance of the java compiler - please use a JDK (not a JRE)");
    }
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

    JavaSourceFromString jsfs = new JavaSourceFromString(this.className, code);

    Iterable<? extends JavaFileObject> compilationUnits1 = Arrays.asList(jsfs);
    List<String> compilerOptions = new LinkedList<String>();
    compilerOptions.add("-d")
    compilerOptions.add(this.topLevel.getAbsolutePath());
    if (isNotEmpty(this.classPath)) {
      compilerOptions.add("-cp");
      compilerOptions.add(this.classPath);
    }
    CompilationTask task = compiler.getTask(null, fileManager, null, compilerOptions, null, compilationUnits1)
    boolean fSuccess = task.call();
    fileManager.close();
    return fSuccess;
  }

  private boolean compileGroovyc(String code) {
    log("Using the groovyc compiler");
    CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
    if (isNotEmpty(this.classPath)) {
      compilerConfiguration.setClasspath(this.classPath);
    }
    compilerConfiguration.setTargetDirectory(this.topLevel);
    GroovyClassLoader groovyClassLoader = new GroovyClassLoader(this.class.getClassLoader(), compilerConfiguration);
    groovyClassLoader.parseClass(code); // throws an exception
    return true;
  }
}

class Base {
  static final String[] EMPTY=new String[0];
  public String className;
  String[] tokenizedClassName;

  public void setClassName(String className) {
    this.className = className;
    this.tokenizeClassName();
  }

  private void tokenizeClassName() {
    this.tokenizedClassName = this.className.tokenize('.');
  }

  public String[] getPackageNames() {
    String[] packageNames = EMPTY;
    if (this.tokenizedClassName.size() > 1) {
      packageNames = this.tokenizedClassName[0..-2];
    }
    return packageNames;
  }

  public String getBaseName() {
    String name = this.tokenizedClassName[-1];
    return name;
  }

  static public String getBaseName(String packageAndClassName) {
    String[] tokenized = packageAndClassName.tokenize('.') ?: [""];
    return tokenized[-1];
  }

  public String getPackageName() {
    String packageName = "";
    if (tokenizedClassName.length > 1) {
      packageName = this.tokenizedClassName[0..-2].join('.');
    }
    return packageName;
  }

  static public String packageAndClassName(String packageName, String className) {
    String result = className;
    if (this.isNotEmpty(packageName)) {
      result = packageName + "." + className;
    }
    return result;
  }

  static public boolean isEmpty(String s) {
    boolean rc=true;
    if (s != null && s.trim().length() > 0) {
      rc=false;
    }
    return rc;
  }

  static public boolean isNotEmpty(String s) {
    return !(isEmpty(s));
  }

  static public File createTempDirectory() {
    File temporaryFolder = File.createTempFile("uli", "washere");
    boolean fSuccess = temporaryFolder.delete();
    if (!fSuccess) {
      throw new IOException("Unable to delete file '${temporaryFolder.getAbsolutePath()}'");
    }
    fSuccess = temporaryFolder.mkdir();
    if (!fSuccess) {
      throw new IOException("Unable to create folder '${temporaryFolder.getAbsolutePath()}'");
    }
    return temporaryFolder;
  }
}

class Wsdl {
  static public final String LOCATION="REPLACE_WITH_ACTUAL_URL";
  static public final String WSDL_EXT=".wsdl";
  static public final String XSD_EXT=".xsd";
  static public final String LINE_SEPARATOR=System.getProperty("line.separator");
  public File sourceFolder;
  public String finalWsdlFilename;
  public String location;
  String originalXsdName;
  String finalXsdName;
  File finalWsdlFile;
  File finalXsdFile;

  public void copy() {
    File wsdlFile = null;
    File xsdFile  = null;
    sourceFolder.eachFileRecurse {
      if (it =~ ~/\.wsdl$/) {
        wsdlFile = it;
      }
      if (it =~ ~/\.xsd$/) {
        xsdFile = it;
        this.originalXsdName = xsdFile.getName();
      }
    }
    if (Base.isEmpty(finalWsdlFilename)) {
      finalWsdlFile = this.copyToDir(wsdlFile, ".", this.getReplaceList());
      finalXsdFile = this.copyToDir(xsdFile, ".", []);
    } else {
      finalWsdlFile = new File(finalWsdlFilename);
      File finalWsdlDir = finalWsdlFile.getParentFile() ?: new File(".");
      finalXsdName = (finalWsdlFile.getName() - WSDL_EXT) + XSD_EXT;
      finalXsdFile = new File(finalWsdlDir, finalXsdName);
      this.copyToFile(wsdlFile, finalWsdlFile, this.getReplaceList());
      this.copyToFile(xsdFile,  finalXsdFile, this.getReplaceList());
    }
  }

  private def getReplaceList() {
    def replaceList = [];
    if (Base.isNotEmpty(this.location)) {
      replaceList.add([ LOCATION, this.location ]);
    }
    if (Base.isNotEmpty(this.originalXsdName) && Base.isNotEmpty(this.finalXsdName)) {
      replaceList.add([ this.originalXsdName, this.finalXsdName ]);
    }
    return replaceList;
  }

  private String doReplace(String line, def replaceList) {
    String result = line;
    replaceList.each {
      result = result.replaceAll(it[0], it[1]);
    }
    return result;
  }

  public File copyToDir(File src, String toDir, def replaceList) {
    File toFile = null;
    if (src != null) {
      String name = src.getName();
      toFile = new File(toDir, name);
      copyToFile(src, toFile, replaceList);
    }
    return toFile;
  }

  public void copyToFile(File src, File dst, def replaceList) {
    if (src != null) {
      dst.setText("");
      src.eachLine {String it ->
        String line = doReplace(it, replaceList);
        dst.append(line);
        dst.append(LINE_SEPARATOR);
      }
      //toFile << src.asWritable(); // http://groovyconsole.appspot.com/view.groovy?id=8001
    }
  }
}


interface Executor {
  public int execute(boolean fAutoMagic);
  public void cleanup();
  public String getStdoutText();
  public String getStderrText();
  public String getDiagnostics(String command, int result);
}

abstract class AbstractExecutor {
  public List<String> args;
  public File temporaryFolder;
  private boolean fTemporaryFolderCreated = false;
  public File stdout;
  public File stderr;

  public void initTemporaryFolder() {
    if (temporaryFolder == null) {
      temporaryFolder = Base.createTempDirectory();
      fTemporaryFolderCreated = true;
    }
    stdout = new File(temporaryFolder, "stdout");
    stderr = new File(temporaryFolder, "stderr");
  }

  public void cleanup() {
    Cleanup.delete(stdout);
    Cleanup.delete(stderr);
    stdout = null;
    stdout = null;
    if (fTemporaryFolderCreated) {
      Cleanup.delete(temporaryFolder);
      temporaryFolder = null;
    }
  }

  public String getStdoutText() {
    return stdout.getText();
  }
  public String getStderrText() {
    return stderr.getText();
  }
  public String getDiagnostics(String command, int result) {
      System.err.println("Command: ${command}, Args: ${this.args.join(' ')}");
      System.err.println("Execution failed, exitCode=${result}");
      System.err.println("\nSTDOUT:\n${this.getStdoutText()}\n");
      System.err.println("\nSTDERR:\n${this.getStderrText()}\n");
  }
}

class ExternalExecutor extends AbstractExecutor implements Executor {
  public int execute(boolean fAutoMagic) {
    List<String> tmpArgs = [ 'wsgen' ];
    tmpArgs.addAll(this.args);
    ProcessBuilder processBuilder = new ProcessBuilder(tmpArgs as List<String>);
    Process process = processBuilder.start();
    this.initTemporaryFolder();
    process.consumeProcessOutput(new FileOutputStream(stdout), new FileOutputStream(stderr));
    int result = process.waitFor();
    if (result != 0 && fAutoMagic) {
      System.err.println(this.getDiagnostics("External WSGEN", result));
    }
    if (fAutoMagic) {
      cleanup();
    }
    return result;
  }

}

class InternalExecutor extends AbstractExecutor implements Executor {
  private class ExitException extends SecurityException {
     public final int status;
     public ExitException(int status) {
         super("There is no escape!");
         this.status = status;
     }
  }

  private class NoExitSecurityManager extends SecurityManager {
    @Override
    public void checkPermission(Permission perm) {
        // allow anything.
    }
    @Override
    public void checkPermission(Permission perm, Object context) {
        // allow anything.
    }
    @Override
    public void checkExit(int status) {
        super.checkExit(status);
        throw new ExitException(status);
    }
  }

  public int execute(boolean fAutoMagic) {
    initTemporaryFolder();
    PrintStream pStdout = new PrintStream(new FileOutputStream(stdout));
    PrintStream pStderr = new PrintStream(new FileOutputStream(stderr));
    PrintStream oldOut = System.out;
    System.out = pStdout;
    PrintStream oldErr = System.err;
    System.err = pStderr;
    SecurityManager sm = System.getSecurityManager();
    int result = 0;
    try {
      System.setSecurityManager(new NoExitSecurityManager());
      com.sun.tools.ws.WsGen.main(args as String[]);
    } catch (ExitException e) {
      result = e.status;
    } catch (Throwable e) {
      e.printStackTrace(oldErr);
    } finally {
      System.setSecurityManager(sm);
      System.out = oldOut;
      System.err = oldErr;
      pStdout.close();
      pStderr.close();
    }

    if (result != 0 && fAutoMagic) {
      System.err.println(this.getDiagnostics("Internal WSGEN", result));
    }
    if (fAutoMagic) {
      cleanup();
    }
    return result;
  }
}

class WsGen {
  /*
   * For jaxws-ri-2.2.7, we have to specify '-target' '2.1'. Without these options,
   * an error message like this is shown:
   *
   *   You are running on JDK6 which comes with JAX-WS 2.1 API, but this tool
   *   requires JAX-WS 2.2 API. Use the endorsed standards override mechanism
   *   (http://docs.oracle.com/javase/6/docs/technotes/guides/standards/),
   *    or use -Xendorsed option.
   *
   * The option seems to work with the WSGEN provided by JDK6 as well.
   */
  static final def REQUIRED_ARGS = [ '-target', '2.1' ];
  def args = [];

  public WsGen() {
    this.setArgs(['-help']);
  }

  public void setArgs(List args) {
    this.args = [  ];
    this.addArgs(args);
  }

  public void addArgs(List args) {
    this.args.addAll(args);
  }

  public Executor getExecutor() {
    def myArgs = [];
    myArgs += REQUIRED_ARGS;
    myArgs += this.args;
    Executor executor = new InternalExecutor(args: myArgs as List<String>);
    return executor;
  }

  public static boolean canInlineSchemas() {
    WsGen wsgen = new WsGen();
    Executor e = wsgen.getExecutor();
    e.execute(false);
    String output = e.getStdoutText() + e.getStderrText();
    boolean result = output.indexOf('inlineSchemas') >= 0;
    e.cleanup();
    return result;
  }
}

class Cleanup {
  private List<File> filesAndFolders = new LinkedList<File>();

  public void add(File file) {
    this.filesAndFolders.add(file);
  }

  public void cleanup() {
    for (File f in this.filesAndFolders.reverse()) {
      delete(f);
    }
  }

  static public void delete(File f) {
    if (f != null) {
      if (f.isDirectory()) {
        f.deleteDir();
      } else {
        f.delete();
      }
    }
  }
}
