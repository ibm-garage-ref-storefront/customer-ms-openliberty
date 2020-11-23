package dev.appsody.customer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/resource")
public class CustomerResource {

    @GET
    public String getRequest() {
        return "CustomerResource response";
    }
}
