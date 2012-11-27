package com.daemonspoint.webservice;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

@WebService(name = "sampleWebService", targetNamespace = "http:/sample.de/sample-web-service/")
public interface SampleWebService {
   @WebMethod
   public String echo(
            @WebParam(name = "msg", mode = WebParam.Mode.IN) String msg
   );
}
