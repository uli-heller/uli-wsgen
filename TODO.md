List of Open TODOs
==================

* Cleanup the prototype (work in progress)
* Make the prototype more robust
    * run without '-c'
    * test against various web service interfaces (work in progress)
* Port to Windows (work in progress)
* Use ASM instead of JAVAC
    * What happens when we run uli-wsgen.sh on a JRE instead of a JDK?
        * JAVAC ... an exception
        * GROOVYC ... works
* Use XmlUnit to do the wsdldiff


Closed TODOs
============

* Implement a prototype for proof-of-concept
* Make the prototype more robust
    * test against illegal classes/interfaces
* Package the binaries - `ant dist` creates uli-wsgen*.sh
* Specify a port name via command line parameters
    * for the service implementation class
    * for the final execution of the wsgen command
* Specify a service name via command line parameters
    * for the service implementation class
    * for the final execution of the wsgen command
* Specify a target name space via command line parameters
    * for the service implementation class
    * for the final execution of the wsgen command
* Web service parameters show up as arg0, arg1, ... within the wsdl
* Deadlock when "wsgen" creates lots of output
* Do compilation based on Groovyc - works with JRE
