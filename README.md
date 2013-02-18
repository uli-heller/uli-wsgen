ULI-WSGEN
=========

Our goal is to implement a replacement for `wsgen -wsdl` which is able to create wsdl files from interfaces instead of concrete implementations. At the moment, this is work in progress. There is now a working prototype, but it is quite ugly at the moment.

For third-party products we use, please see the corresponding section towards the end of this document!

Cloning The GitHub Repo
-----------------------

To clone the GitHub repository of this project, it is probably best to run `git clone https://github.com/uli-heller/uli-wsgen.git --single-branch`. This prevents you from downloading the "downloads" branch which contains all the binaries created by this project.

Requirements
------------

To compile and use this project, you'll need

* Internet access to download various 3rd-party tools
** Groovy
** SoaModelCore
** JaxWsRI
* Either JDK7 (preferred) or JDK6 installed and available via PATH

Project Structure
-----------------

* . ... the root folder
* bin ... various shell scripts
    * ant.sh ... wrapper for ant
    * groovy.sh ... wrapper for groovy
    * myjar.sh ... wrapper for JDK jar
    * prepare.sh ... downloads and unpacks the project dependencies
    * wsdldiff.sh ... wrapper for groovy-scripts/wsdlDiff.groovy
    * wsgen.sh ... wrapper for JDK wsgen
    * uli-wsgen.sh ... wrapper for groovy-scripts/uliWsGen.groovy used for rapid testing
* build ... files generated when doing the compilation
    * classes ... compiled Java class files
* groovy-scripts ... various groovy scripts
    * examineInterface.groovy ... a test script which prints some attributes of a Java interface
    * uliWsGen.groovy ... the main script which is able to create wsdl files from Java interfaces
    * wsdlDiff.groovy ... script for comparing two wsdl files - copied from <http://www.membrane-soa.org/>, CompareWSDL.java
* src ... various source files
* test-data ... some wsdl files to validate our tests

Compile Project
---------------

### Compile Using A Preinstalled ANT

```sh
ant dist
```

### Compile Without A Preinstalled ANT

```sh
bin/ant.sh dist
```

Some Tests
----------

Please remember: Before doing the tests, you have to compile the project by executing `bin/ant.sh`!

Below, you'll find the commands to execute and the expected outcomes.

### SampleWebService

```sh
$ ./uli-wsgen*.sh -c lib/examples.jar com.daemonspoint.webservice.SampleWebService
$ ls *.wsdl
SampleWebServiceImplService.wsdl
```

### SampleWebServiceImpl

```sh
$ ./uli-wsgen*.sh -c lib/examples.jar com.daemonspoint.webservice.SampleWebServiceImpl
Class 'com.daemonspoint.webservice.SampleWebServiceImpl' is not an interface - please use 'wsgen' directly
```

### ComplexWebService

```sh
$ ./uli-wsgen*.sh -c lib/examples.jar com.daemonspoint.webservice.ComplexWebService
$ ls *.wsdl
ComplexWebServiceImplService.wsdl
```

### ComplexWebServiceImpl

```sh
$ ./uli-wsgen*.sh -c lib/examples.jar com.daemonspoint.webservice.ComplexWebServiceImpl
Caught: java.lang.ClassNotFoundException: com.daemonspoint.webservice.ComplexWebServiceImpl
java.lang.ClassNotFoundException: com.daemonspoint.webservice.ComplexWebServiceImpl
	at SourceInterface.loadClass(uliWsGen.groovy:148)
	at SourceInterface.setClassName(uliWsGen.groovy:144)
	at uliWsGen.run(uliWsGen.groovy:61)
```

### CalculatorWsImpl

```sh
$ ./uli-wsgen*.sh -c lib/examples.jar -t my.target.namespace.de com.daemonspoint.webservice.CalculatorWs
$ ls *.wsdl
CalculatorWsImplService.wsdl
```

### InvalidAnnotation

```sh
$ ./uli-wsgen*.sh -c lib/examples.jar com.daemonspoint.webservice.InvalidAnnotation
...
Problem encountered during annotation processing; 
see stacktrace below for more information.
com.sun.tools.internal.ws.processor.modeler.ModelerException: modeler error: The @javax.jws.WebService.serviceName element cannot be specified on a service endpoint interface. Class: com.daemonspoint.webservice.InvalidAnnotation
	at com.sun.tools.internal.ws.processor.modeler.annotation.WebServiceAP.onError(WebServiceAP.java:229)
	at com.sun.tools.internal.ws.processor.modeler.annotation.WebServiceVisitor.verifySEIAnnotations(WebServiceVisitor.java:146)
	at com.sun.tools.internal.ws.processor.modeler.annotation.WebServiceVisitor.visitInterfaceDeclaration(WebServiceVisitor.java:103)
...
```

### Running on Windows

On Windows, you should be able to run the programm with minor tweaks:

* Replace "./uli-wsgen*.sh" by ".\uli-wsgen-{version}.bat", for example ".\uli-wsgen-0.1pre.bat"
* Replace "/" by "\", so "build/classes" becomes "build\classes"
* Use "dir" instead of "ls"

An example looks like this:

```bat
> .\uli-wsgen-0.1pre.bat -c lib\examples.jar com.daemonspoint.webservice.ComplexWebService 
> dir *.wsdl
...
```

WSGEN - The Original
--------------------

### Running WSGEN Against An Interface

```sh

$ bin/wsgen.sh com.daemonspoint.webservice.SampleWebService
The class "com.daemonspoint.webservice.SampleWebService" is not an endpoint implementation class.


Usage: WSGEN [options] <SEI>
...
Examples:
  wsgen -cp . example.Stock
  wsgen -cp . example.Stock -wsdl -servicename {http://mynamespace}MyService

$ ls *.wsdl
ls: cannot access *.wsdl: No such file or directory
```

### Running WSGEN Against The Implementation Of An Interface

```sh

$ bin/wsgen.sh com.daemonspoint.webservice.SampleWebServiceImpl
warning: The apt tool and its associated API are planned to be
removed in the next major JDK release.  These features have been
superseded by javac and the standardized annotation processing API,
javax.annotation.processing and javax.lang.model.  Users are
recommended to migrate to the annotation processing features of
javac; see the javac man page for more information.

$ ls *.wsdl
SampleWebService.wsdl
```


GitHub
------

### Publish Project On GitHub

```sh
git remote add origin git@github.com:uli-heller/uli-wsgen.git
git push -u origin master
```

### Store Build Artifacts On GitHub

```sh
$ git checkout master
$ git status
# On branch master
nothing to commit, working directory clean
$ git checkout --orphan downloads
$ git rm -rf .
$ jmacs README.txt # Enter description of the branch
$ git add README.txt
$ git commit -m "Created branch: downloads"
$ git push --set-upstream origin downloads
$ cp .../uli-wsgen-0.1.bat .
$ cp .../uli-wsgen-0.1.sh .
$ git add uli-wsgen-0.1.bat uli-wsgen-0.1.sh
$ git commit -m "Added downloads of version 0.1" .
$ git push # might take some time depending on your internet connection bandwidth
$ git checkout master # switch back to master
```

### Tags

#### Create A Tag Locally

```sh
git tag -a -m "Version 0.1" v0.1
```

#### Push To GitHub

```sh
git push --tags
```

#### Fetch From GitHub

```sh
git fetch --tags
```

Third Party Products And Tools
------------------------------

Here is a list of the 3rd party products and tools we are using for this project:

* [GitHub](http://github.com) ... for hosting the source code
* [Git](http://git-scm.org) ... for accessing hosted source code
* [Groovy](http://groovy.codehaus.org) ... a scripting language for the Java VM
* [Java](http://java.oracle.com) ... the programming language and execution environment we use
* [JaxWS](http://jax-ws.java.net/) ... the reference implementation of JaxWS
* [Membrane-SOA](http://www.membrane-soa.org/) ... the wsdl diff tool be use for test validation
* Linux, Bash, ...

Links And Notes
---------------

* [StackOverflow: Groovy - Methods and parameters](http://stackoverflow.com/questions/3782250/groovy-reflection-on-a-java-class-methods-and-parameters)
* [BeyondLinux: Dynamically compile a java class](http://www.beyondlinux.com/2011/07/20/3-steps-to-dynamically-compile-instantiate-and-run-a-java-class/)

### ASMified SampleWebService

Genarate with: `bin/asmifier.sh com.daemonspoint.webservice.SampleWebService`

```java
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.attrs.*;
public class SampleWebServiceDump implements Opcodes {

public static byte[] dump () throws Exception {

ClassWriter cw = new ClassWriter(0);
FieldVisitor fv;
MethodVisitor mv;
AnnotationVisitor av0;

cw.visit(V1_7, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "SampleWebService", null,
"java/lang/Object", null);

{
av0 = cw.visitAnnotation("Ljavax/jws/WebService;", true);
av0.visit("name", "sampleWebService");
av0.visit("targetNamespace", "http:/sample.de/sample-web-service/");
av0.visitEnd();
}
cw.visitInnerClass("javax/jws/WebParam$Mode", "javax/jws/WebParam", "Mode",
ACC_PUBLIC + ACC_FINAL + ACC_STATIC + ACC_ENUM);

{
mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "echo",
"(Ljava/lang/String;)Ljava/lang/String;", null, null);
{
av0 = mv.visitAnnotation("Ljavax/jws/WebMethod;", true);
av0.visitEnd();
}
{
av0 = mv.visitParameterAnnotation(0, "Ljavax/jws/WebParam;", true);
av0.visit("name", "msg");
av0.visitEnum("mode", "Ljavax/jws/WebParam$Mode;", "IN");
av0.visitEnd();
}
mv.visitEnd();
}
cw.visitEnd();

return cw.toByteArray();
}
}
```

### ASMified SampleWebServiceImpl

Genarate with: `bin/asmifier.sh com.daemonspoint.webservice.SampleWebServiceImpl`

```java
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.attrs.*;
public class SampleWebServiceImplDump implements Opcodes {

public static byte[] dump () throws Exception {

ClassWriter cw = new ClassWriter(0);
FieldVisitor fv;
MethodVisitor mv;
AnnotationVisitor av0;

cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, "SampleWebServiceImpl", null,
"java/lang/Object", new String[] { "SampleWebService" });

{
av0 = cw.visitAnnotation("Ljavax/jws/WebService;", true);
av0.visit("endpointInterface", "SampleWebService");
av0.visit("serviceName", "SampleWebService");
av0.visit("targetNamespace", "http:/sample.de/sample-web-service/");
av0.visitEnd();
}
{
mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
mv.visitCode();
mv.visitVarInsn(ALOAD, 0);
mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
mv.visitInsn(RETURN);
mv.visitMaxs(1, 1);
mv.visitEnd();
}
{
mv = cw.visitMethod(ACC_PUBLIC, "echo", "(Ljava/lang/String;)Ljava/lang/String;",
null, null);
mv.visitCode();
mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
mv.visitInsn(DUP);
mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V");
mv.visitLdcInsn("Echo: ");
mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
"(Ljava/lang/String;)Ljava/lang/StringBuilder;");
mv.visitVarInsn(ALOAD, 1);
mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
"(Ljava/lang/String;)Ljava/lang/StringBuilder;");
mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
"()Ljava/lang/String;");
mv.visitInsn(ARETURN);
mv.visitMaxs(2, 2);
mv.visitEnd();
}
cw.visitEnd();

return cw.toByteArray();
}
}
```

Problems And Issues
-------------------

### STDERR: error reading ./uli-wsgen-0.1.sh; cannot read zip file entry

This happens typically when you use Java6. It can be fixed by this:

* EITHER: Use Java7
* OR: Execute the jar file instead of the sh file, so use `java -jar uli-wsgen*jar ...` instead of `./uli-wsgen*sh ...`

### Unrecognized parameter -inlineSchemas

This happens typically when you use Java6. It can be fixed by this:

* EITHER: Use Java7
* OR: Append "--wsgen -wsdl" to the command line parameters

### ModelerException: The @javax.jws.WebService.serviceName element cannot be specified on a service endpoint interface

There are chances that you get an error message like this:

```
com.sun.tools.internal.ws.processor.modeler.ModelerException: modeler error: The @javax.jws.WebService.serviceName element cannot be specified on a service endpoint interface. Class: com.daemonspoint.webservice.IllegalAnnotation
	at com.sun.tools.internal.ws.processor.modeler.annotation.WebServiceAP.onError(WebServiceAP.java:229)
	at com.sun.tools.internal.ws.processor.modeler.annotation.WebServiceVisitor.verifySEIAnnotations(WebServiceVisitor.java:146)
	at com.sun.tools.internal.ws.processor.modeler.annotation.WebServiceVisitor.visitInterfaceDeclaration(WebServiceVisitor.java:103)
	at com.sun.tools.apt.mirror.declaration.InterfaceDeclarationImpl.accept(InterfaceDeclarationImpl.java:50)
	at com.sun.tools.internal.ws.processor.modeler.annotation.WebServiceVisitor.inspectEndpointInterface(WebServiceVisitor.java:408)
...
```

This is an issue with the existing interface class of the web service. The interface class must *not* specify a serviceName within the @WebService annotation - see [javaee reference documentation](http://docs.oracle.com/javaee/5/api/javax/jws/WebService.html#serviceName) for details!
