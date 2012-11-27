package com.daemonspoint.webservice;

import javax.jws.WebService;

@WebService(endpointInterface = "com.daemonspoint.webservice.SampleWebService", serviceName = "SampleWebService", targetNamespace = "http:/sample.de/sample-web-service/")
public class SampleWebServiceImpl implements SampleWebService {
   public String echo(String msg) {
       return "Echo: "+msg;
   }
}
