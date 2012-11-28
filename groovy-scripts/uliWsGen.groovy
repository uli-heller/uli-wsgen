// This is a groovy script. You pass the name of a Java interface to it and the
// script prints some attributes of the interface
import java.nio.file.Files;
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

ClassLoader cl = this.getClass().getClassLoader();
if (options.c) {
  URL url = new File(options.c).toURI().toURL();
  URL[] urls = new URL[1];
  urls[0]    = url;
  cl = new URLClassLoader(urls, cl);
}

boolean isWebService(Class clazz) {
  javax.jws.WebService a = clazz.getAnnotation(javax.jws.WebService.class);
  return a != null;
}

String className = parsedArgs[0];
Class clazz = cl.loadClass(className);
if (! isWebService(clazz)) {
  System.err.println("Class '${className}' is not a jaxws webservice - annotation @WebService is missing");
  System.exit(1);
}
if (! clazz.isInterface()) {
  System.err.println("Class '${className}' is not an interface - please use 'wsgen'");
  System.exit(1);
}

def deleteList = [];
File temporaryFolder = Files.createTempDirectory("uliWsGen").toFile();
deleteList.add(temporaryFolder);
String[] packageNames = className.tokenize('.')[0..-2];
File currentFolder = temporaryFolder;
for (p in packageNames) {
  currentFolder = new File(currentFolder, p);
  deleteList.add(currentFolder);
  currentFolder.mkdir();
}

String CLASSNAME="uliWsGenTemporaryClass";
File classFile = new File(currentFolder, "${CLASSNAME}.class");
deleteList.add(classFile);
String implClassText = "package ${packageNames.join('.')};\n" \
  + "@javax.jws.WebService \n" \
  + "public class ${CLASSNAME} implements ${className} {\n";

for (def m in clazz.getMethods()) {
  implClassText += "\tpublic ${m.returnType.name} ${m.name}(";
  int cnt=0; 
  implClassText += m.parameterTypes.collect{ "${it.name} p${cnt++}" }.join(",");
  implClassText += ") {\n" \
    + "\t\t throw new RuntimeException();\n" \
    + "\t}\n";
}
implClassText += "}\n";

// http://www.java2s.com/Code/Java/JDK-6/CompilingfromMemory.htm
JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

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
               super(URI.create("string:///" + name.replace('.','/') + JavaFileObject.Kind.SOURCE.extension),
                     JavaFileObject.Kind.SOURCE);
               this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return code;
    }
}

JavaSourceFromString jsfs = new JavaSourceFromString("${packageNames.join('.')}.${CLASSNAME}", implClassText);

Iterable<? extends JavaFileObject> compilationUnits1 = Arrays.asList(jsfs);
Iterable compilerOptions = Arrays.asList("-d", temporaryFolder.getAbsolutePath(), "-cp", options.c);
CompilationTask task = compiler.getTask(null, fileManager, null, compilerOptions, null, compilationUnits1)
boolean fSuccess = task.call();
fileManager.close();

int result=0;
if (!fSuccess) {
  result=1;
  System.err.println "Creation of the class file failed!";
} else if (! classFile.exists()) {
  result=1;
  System.err.println "Creation of the class file was successful, but the class file doesn't exist!";
} else {
  String cpForProcessBuilder = temporaryFolder.getAbsolutePath()\
     + File.pathSeparator\
     + options.c;
  ProcessBuilder processBuilder = new ProcessBuilder("wsgen", "-cp", cpForProcessBuilder, "-wsdl", "-inlineSchemas", "${packageNames.join('.')}.${CLASSNAME}");
  //processBuilder.directory(temporaryFolder);
  Process process = processBuilder.start();
  result = process.waitFor();
  System.out.println(process.getErrorStream().getText());
  System.out.println(process.getInputStream().getText());
}

for (File f in deleteList.reverse()) {
  f.delete();
}
System.exit(result);
