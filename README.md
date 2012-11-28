ULIWSGEN
=======

Our goal is to implement a replacement for `wsgen -wsdl` which is able to create wsdl files from interfaces instead of concrete implementations. At the moment, this is work in progress. There is now a working prototype, but it is very ugly at the moment. (`bin/groovy.sh groovy-scripts/uliWsGen.groovy -c build/classes com.daemonspoint.webservice.SampleWebService` creates "UliWsGenTemporaryClassService.wsdl")

Project Structure
-----------------

* . ... the root folder
* bin ... various shell scripts
    * ant.sh ... wrapper for ant
    * groovy.sh ... wrapper for groovy
    * myjar.sh ... wrapper for JDK jar
    * prepare.sh ... downloads and unpacks the project dependencies
    * wsgen.sh ... wrapper for JDK wsgen
* build ... files generated when doing the compilation
    * classes ... compiled Java class files
* groovy-scripts ... various groovy scripts
* src ... various source files

Compile Project
---------------

```sh
bin/ant.sh
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
git remote add origin git@github.com:uli-heller/uliwsgen.git
git push -u origin master
```

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