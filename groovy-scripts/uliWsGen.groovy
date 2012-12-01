import java.nio.file.Files; // ... available in Java7 only

import java.lang.reflect.Method;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.ToolProvider;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

def cli = new CliBuilder(usage: "uli-wsgen.sh [-h] [-c classpath] interfaceName [--wsgen wsgen-opts ...]");
cli.with {
    h longOpt: 'help',                                              'Show usage information'
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
  uli-wsgen.sh -c build/classes CalculatorWs --wsgen -wsdl""";
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

String wsdlFilename        = options.f ?: "";
String implClassName       = options.i ?: "";
String implName            = Base.getBaseName(implClassName);
String location            = options.l ?: "";
String name                = options.n ?: "";
String portName            = options.p ?: "";
String serviceName         = options.s ?: "";
String targetNamespace     = options.t ?: "";

SourceInterface sourceInterface = new SourceInterface(className: parsedArgs[0], classLoader: cl);
sourceInterface.check();

int result=0;
Cleanup cleanup = new Cleanup();
try {
  File temporaryFolder = Files.createTempDirectory("uliWsGen").toFile();

  String implementationName = implName ?: Implementation.getImplementationName(sourceInterface.getBaseName());
  String implementationClassName = implClassName ?: Base.packageAndClassName(sourceInterface.getPackageName(), implementationName)
  Implementation implementation = new Implementation(className: implementationClassName, topLevel: temporaryFolder, cleanup: cleanup, classPath: options.c, name: name, portName: portName, serviceName: serviceName, targetNamespace: targetNamespace);

  String implClassText = implementation.getCode(sourceInterface);
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
       + options.c;
    def additionalArgs = wsgenArgs ?:  [ "-wsdl", "-inlineSchemas" ];
    def pbArgs = [ "wsgen", "-r", temporaryFolder.getAbsolutePath(), "-cp", cpForProcessBuilder, implementationClassName ];
    pbArgs.addAll(additionalArgs);
    ProcessBuilder processBuilder = new ProcessBuilder(pbArgs as List<String>);
    Executor executor = new Executor(processBuilder: processBuilder, temporaryFolder: temporaryFolder);
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
System.exit(result);

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

  public boolean check() {
    //this.loadClass();
    if (this.clazz == null) {
      System.err.println("Class '${this.className}' could not be loaded - class not found!");
      System.exit(1);
    }
    if (! isWebService(this.clazz)) {
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
    } catch (Exception e) {
      // ignore the exception - we *will* detect this later!
      ;
    }
  }

  private boolean isWebService(Class clazz) {
    javax.jws.WebService a = clazz.getAnnotation(javax.jws.WebService.class);
    return a != null;
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
    code.append("""
      import ${sourceInterface.className};
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
    // http://www.java2s.com/Code/Java/JDK-6/CompilingfromMemory.htm
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
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
}

class Wsdl {
  static public final String LOCATION="REPLACE_WITH_ACTUAL_URL";
  static public final String WSDL_EXT=".wsdl";
  static public final String XSD_EXT=".xsd";
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
        dst.append(System.lineSeparator); // Java7
      }
      //toFile << src.asWritable(); // http://groovyconsole.appspot.com/view.groovy?id=8001
    }
  }

}

class Executor {
  public ProcessBuilder processBuilder;
  public File temporaryFolder;
  private boolean fTemporaryFolderCreated = false;
  public File stdout;
  public File stderr;

  public int execute(boolean fAutoMagic) {
    Process process = processBuilder.start();
    if (temporaryFolder == null) {
      temporaryFolder = File.createTempFile();
      boolean fSuccess = temporaryFolder.delete();
      fSuccess = temporaryFolder.mkdir();
      fTemporaryFolderCreated = true;
    }
    process.consumeProcessOutput(stdout, stderr);
    int result = process.waitFor();
    if (result != 0 && fAutoMagic) {
      System.err.println("Process: ${processBuilder.command().join(' ')}");
      System.err.println("Execution failed, exitCode=${result}");
      System.err.println("\nSTDOUT:\n${stdout.getText()}\n");
      System.err.println("\nSTDERR:\n${stderr.getText()}\n");
    }
    if (fAutoMagic) {
      cleanup();
    }
    return result;
  }

  public void cleanup() {
    Cleanup.delete(stdout);
    Cleanup.delete(stderr);
    stdout = null;
    stdout = null;
    if (fTemporaryFolderCreated) {
      Cleanup.delete(temporaryFolder());
      temporaryFolder = null;
    }
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
