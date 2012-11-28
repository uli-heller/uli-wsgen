import java.nio.file.Files; // ... available in Java7 only

import java.lang.reflect.Method;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.ToolProvider;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

def cli = new CliBuilder(usage: "uliWsGen [-h] [-c classpath] interfaceName");
cli.with {
    h longOpt: 'help',                                        'Show usage information'
    c longOpt: 'classpath',  args: 1, argName: 'classpath',   'Location of the input files'
}

def printHelp = {
  cli.usage();
  println "\nExamples:\n  examineInterface com.daemonspoint.webservice.SampleWebService";
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

def options = cli.parse(args);

if (!options) {
  println "Unable to parse command line options -> EXIT";
  System.exit(1);
}

if (options.h) {
  printHelp();
}

String[] parsedArgs = options.arguments();
if (parsedArgs.length != 1) {
  cli.usage();
  System.err.println("Please specify only one interface name!");
  System.exit(1);
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

SourceInterface sourceInterface = new SourceInterface(className: parsedArgs[0]);
sourceInterface.check();

int result=0;
Cleanup cleanup = new Cleanup();
try {
  File temporaryFolder = Files.createTempDirectory("uliWsGen").toFile();

  String implementationName = Implementation.getImplementationName(sourceInterface.getBaseName());
  String implementationClassName = Base.packageAndClassName(sourceInterface.getPackageName(), implementationName)
  Implementation implementation = new Implementation(className: implementationClassName, topLevel: temporaryFolder, cleanup: cleanup, classPath: options.c);

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
    ProcessBuilder processBuilder = new ProcessBuilder("wsgen", "-cp", cpForProcessBuilder, "-wsdl", "-inlineSchemas", implementationClassName);
    //processBuilder.directory(temporaryFolder);
    Process process = processBuilder.start();
    result = process.waitFor();
    if (result != 0) {
      System.err.println("STDERR:");
      System.err.println(process.getErrorStream().getText());
      System.out.println("STDOUT:");
      System.out.println(process.getInputStream().getText());
    }
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
  Class clazz;

  public boolean check() {
    //this.loadClass();
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

  private void loadClass() {
    this.clazz = this.getClass().getClassLoader().loadClass(this.className);
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

  public void setTopLevel(File topLevel) {
    this.topLevel = topLevel;
  }

  public String getBaseName() {
    String name = super.getBaseName();
    return name - IMPL;
  }

  public String getImplementationName() {
    return Implementation.getImplementationName(this.getBaseName());
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
    code.append("""
      package ${this.getPackageName()};
      @javax.jws.WebService
      public class ${this.getImplementationName()} implements ${sourceInterface.className} {
    """);
    for (def m in sourceInterface.getMethods()) {
      code.append("\tpublic ${m.returnType.name} ${m.name}(");
      int cnt=0; 
      code.append(m.parameterTypes.collect{ "${it.name} p${cnt++}" }.join(","));
      code.append(""") {
          throw new RuntimeException();
        }
      """);
    }
    code.append("}\n");
    return code.toString();
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
    if (this.classPath != null && this.classPath.length() > 0) {
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
    String[] packageNames = this.tokenizedClassName[0..-2];
    return packageNames;
  }

  public String getBaseName() {
    String name = this.tokenizedClassName[-1];
    return name;
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
    if (packageName != null && packageName.length() > 0) {
      result = packageName + "." + className;
    }
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
      f.delete();
    }
  }
}
