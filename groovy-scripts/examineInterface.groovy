// This is a groovy script. You pass the name of a Java interface to it and the
// script prints some attributes of the interface

def cli = new CliBuilder(usage: "examineInterface [-h] interfaceName", posix: true);
cli.with {
    h longOpt: 'help',   'Show usage information'
    //f longOpt: 'from',   required: true, args: 1, argName: 'tmpWsdl',   'Name of the temporary wsdl file'
    //t longOpt: 'to',     required: true, args: 1, argName: 'finalWsdl', 'Name of the final wsdl file'
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

// http://stackoverflow.com/questions/3782250/groovy-reflection-on-a-java-class-methods-and-parameters
// For 'com.daemonspoint.webservice.SampleWebService', the output is something like this:
//   boolean equals( java.lang.Object )
//   java.lang.Class getClass(  )
//   int hashCode(  )
//   void notify(  )
//   void notifyAll(  )
//   java.lang.String toString(  )
//   void wait(  )
//   void wait( long )
//   void wait( long, int )
// So unfortunately, the "echo" method is missing!
//def dumpOut( clz ) {
//  clz.metaClass.methods.each { method ->
//    println "${method.returnType.name} ${method.name}( ${method.parameterTypes*.name.join( ', ' )} )"
//  }
//}

def dumpMethods(Class clazz) {
  clazz.methods.each { method ->
    println "dumpMethods: ${method.returnType.name} ${method.name}( ${method.parameterTypes*.name.join( ', ' )} )"
  }
}

def dumpAnnotations(Class clazz) {
  clazz.annotations.each { annotation ->
    println "dumpAnnotation: ${annotation.toString()}";
    println "dumpAnnotation:   ${annotation.annotationType()}";
    if (annotation.equals(javax.jws.WebService)) {
      println "This is never triggered";
    }
    if (annotation.equals(javax.jws.WebService.class)) {
      println "This is never triggered";
    }
  }
}

javax.jws.WebService getWebServiceAnnotation(Class clazz) {
  return clazz.getAnnotation(javax.jws.WebService.class);
}

boolean isWebService(Class clazz) {
  javax.jws.WebService a = getWebServiceAnnotation(clazz);
  return a != null;
}

ClassLoader classLoader = this.getClass().getClassLoader();
for (String className in parsedArgs) {
  Class clazz = classLoader.loadClass(className);
  dumpMethods(clazz);
  dumpAnnotations(clazz);
  if (! isWebService(clazz)) {
    println "This is NOT a web service";
  } else {
    println "This is a web service";
    javax.jws.WebService ws = getWebServiceAnnotation(clazz);
    println "  wsdlLocation=${ws.wsdlLocation()}";
    println "  targetNamespace=${ws.targetNamespace()}";
    println "  name=${ws.name()}";
    println "  endpointInterface=${ws.endpointInterface()}";
    println "  portName=${ws.portName()}";
    println "  serviceName=${ws.serviceName()}";
  }
}

System.exit(0);
