package com.daemonspoint.webservice;

// http://openejb.apache.org/examples-trunk/simple-webservice/
import javax.jws.WebService;

@WebService(targetNamespace = "http://superbiz.org/wsdl")
public interface CalculatorWs {

    public int sum(int add1, int add2);

    public int multiply(int mul1, int mul2);
}
