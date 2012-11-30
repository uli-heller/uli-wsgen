package com.daemonspoint.webservice;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

@WebService(name = "invalidAnnotation", serviceName = "notAllowedForInterfaces", targetNamespace = "http:/sample.de/sample-web-service/")
public interface InvalidAnnotation {
   @WebMethod
   public String echo(
            @WebParam(name = "msg", mode = WebParam.Mode.IN) String msg
   );
}
