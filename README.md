ULIWSGEN
=======

Our goal is to implement a replacement for `wsgen -wsdl` which is able to create wsdl files from interfaces instead of concrete implementations.

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
* src ... various source files

Compile Project
---------------

```sh
bin/ant.sh
```

WSGEN - The Original
--------------------

```sh

$ bin/wsgen.sh com.daemonspoint.webservice.SampleWebService
The class "com.daemonspoint.webservice.SampleWebService" is not an endpoint implementation class.


Usage: WSGEN [options] <SEI>
...
Examples:
  wsgen -cp . example.Stock
  wsgen -cp . example.Stock -wsdl -servicename {http://mynamespace}MyService
```

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
