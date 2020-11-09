###### customer-ms-openliberty

# Microservice Apps Integration with CouchDB Database and enabling OpenID Connect protection for APIs

*This project is part of the 'IBM Cloud Native Reference Architecture' suite, available at
https://cloudnativereference.dev/*

## Table of Contents

* [Introduction](#introduction)
    + [APIs](#apis)
* [Pre-requisites](#pre-requisites)
* [Implementation Details](#implementation-details)
* [Running the application on Docker](#running-the-application-on-docker)
    + [Get the Customer application](#get-the-customer-application)
    + [Run the CouchDB Docker Container](#run-the-couchdb-docker-container)
    + [Run the Auth Docker Container](#run-the-auth-docker-container)
    + [Set up custom keystore](#set-up-custom-keystore)
    + [Run the Customer application](#run-the-customer-application)
    + [Validating the application](#validating-the-application)
    + [Exiting the application](#exiting-the-application)
* [Conclusion](#conclusion)

## Introduction

This project will demonstrate how to deploy an Openliberty Microprofile application with a CouchDB database onto a Kubernetes Cluster. This application provides basic operations of creating and querying customer profiles from [Apache's CouchDB](http://couchdb.apache.org/) NoSQL database as part of the Customer Profile function of Storefront. Additionally, the Auth Microservice calls this microservice to perform Customer username/password authentication.

![Application Architecture](static/customer.png?raw=true)

Here is an overview of the project's features:
- Based on [MicroProfile](https://microprofile.io/).
- Persist Customer data in an [Apache's CouchDB](http://couchdb.apache.org/) NoSQL database using the official [Cloudant Java library](https://github.com/cloudant/java-cloudant).
- Uses [`Docker`](https://docs.docker.com/) to package application binary and its dependencies.
- OAuth protect the microservice REST API using JWT token signed with a RS256 shared secret.

### APIs

The Orders Microservice REST API is OAuth 2.0 protected. These APIs identifies and validates the caller using signed JWT tokens.

* `GET /micro/customer`

  Returns the customer profile information. The caller of this API must pass a valid OAuth token. The OAuth token is a JWT with the customer name of the caller encoded in the `user_name` claim. A JSON object array is returned consisting of customer details.

## Pre-requisites:

* [Appsody](https://appsody.dev/)
    + [Installing on MacOS](https://appsody.dev/docs/installing/macos)
    + [Installing on Windows](https://appsody.dev/docs/installing/windows)
    + [Installing on RHEL](https://appsody.dev/docs/installing/rhel)
    + [Installing on Ubuntu](https://appsody.dev/docs/installing/ubuntu)
For more details on installation, check [this](https://appsody.dev/docs/installing/installing-appsody/) out.

* Docker Desktop
    + [Docker for Mac](https://docs.docker.com/docker-for-mac/)
    + [Docker for Windows](https://docs.docker.com/docker-for-windows/)

## Implementation Details

We created a new openliberty project using appsody as follows.

```
appsody repo add kabanero https://github.com/kabanero-io/kabanero-stack-hub/releases/download/0.6.5/kabanero-stack-hub-index.yaml

appsody init kabanero/java-openliberty
```

## Running the application on Docker

### Get the Orders application

- Clone orders repository:

```bash
git clone https://github.com/ibm-garage-ref-storefront/customer-ms-openliberty.git
cd customer-ms-openliberty
```

### Run the CouchDB Docker Container

Run the below command to get CouchDB running via a Docker container.

```bash
# Start a CouchDB Container with a database user, and a password
docker run -d -e COUCHDB_USER='admin' -e COUCHDB_PASSWORD='password' -p 5984:5984 couchdb
```

If it is successfully deployed, you will see something like below.

```
$ docker ps
CONTAINER ID        IMAGE                           COMMAND                  CREATED             STATUS              PORTS                                            NAMES
a9c442e3ceb1        couchdb                         "tini -- /docker-ent…"   3 hours ago         Up 3 hours          4369/tcp, 9100/tcp, 0.0.0.0:5984->5984/tcp       goofy_noether  
```

- Populate the database with user information.

```
cd populate
python3 populate.py localhost 5984
```

### Run the Auth Docker Container

- Open a new terminal.

- Clone the below repository.

```bash
git clone https://github.com/ibm-garage-ref-storefront/auth-ms-openliberty.git
cd auth-ms-openliberty
```

- Run the below command.

```
appsody build
```

This builds the docker image.

- Now, run the auth service as follows.

```
docker run -it --name auth-ms-openliberty -p 9081:9080 -p 9444:9443 -e jwksIssuer="https://localhost:9444/oidc/endpoint/OP" -d dev.local/auth-ms-openliberty
```

If it is successfully deployed, you will see something like below.

```
$ docker ps
CONTAINER ID        IMAGE                           COMMAND                  CREATED             STATUS              PORTS                                            NAMES
a9c442e3ceb1        couchdb                         "tini -- /docker-ent…"   3 hours ago         Up 3 hours          4369/tcp, 9100/tcp, 0.0.0.0:5984->5984/tcp       goofy_noether
cc037d68430f        dev.local/auth-ms-openliberty   "/opt/ol/helpers/run…"   2 days ago          Up 2 days           0.0.0.0:9081->9080/tcp, 0.0.0.0:9444->9443/tcp   auth-ms-openliberty
```

### Set up custom keystore

- Use the certs generated previously in the Auth service. For details on how to generate them, click [here](https://github.com/ibm-garage-ref-storefront/auth-ms-openliberty#set-up-custom-keystore).

- Grab the `BCKeyStoreFile.p12`, `client.cer`, and `truststore.p12` and place them under `src/main/liberty/config/resources/security`

### Run the Customer application

- Before running the application, make sure you set up the custom keystore properly.

- Set up Jaegar for opentracing. This is optional. If you want to enable distributed tracing your application, run this step.

```
 docker run -d --name jaeger   -e COLLECTOR_ZIPKIN_HTTP_PORT=9411   -p 5775:5775/udp   -p 6831:6831/udp   -p 6832:6832/udp   -p 5778:5778   -p 16686:16686   -p 14268:14268   -p 9411:9411   jaegertracing/all-in-one:1.11
```

- To run the customer application, run the below command.

```
appsody run --docker-options "-e jwksIssuer="https://localhost:9444/oidc/endpoint/OP" -e dev.appsody.customer.client.CouchDBClientService/mp-rest/url="http://<docker_host>:<docker_port>" -e couch_user=<couchdb_user> -e couchdb_password=<couch_password> -e JAEGER_SERVICE_NAME=customer-ms-openliberty -e JAEGER_AGENT_HOST=host.docker.internal -e JAEGER_AGENT_PORT=6831 -e JAEGER_REPORTER_LOG_SPANS=true -e JAEGER_REPORTER_FLUSH_INTERVAL=2000 -e JAEGER_SAMPLER_TYPE=const -e JAEGER_SAMPLER_PARAM=1"
```

For instance <docker_host>, it will be `host.docker.internal`, <docker_port> will be `5984`, <couchdb_user> will be `admin` and <couchdb_password> will be `password`.

If not running Jaegar, run the below command.

```
appsody run --docker-options "-e jwksIssuer="https://localhost:9444/oidc/endpoint/OP" -e dev.appsody.customer.client.CouchDBClientService/mp-rest/url="http://<docker_host>:<docker_port>" -e couch_user=<couchdb_user> -e couchdb_password=<couch_password>"
```

For instance <docker_host>, it will be `host.docker.internal`, <docker_port> will be `5984`, <couchdb_user> will be `admin` and <couchdb_password> will be `password`.

- If it is successfully running, you will see something like below.

```
[Container] [INFO] CWWKM2015I: Match number: 1 is [11/9/20 13:48:03:974 UTC] 00000032 com.ibm.ws.app.manager.AppMessageHelper                      A CWWKZ0001I: Application customer-ms-openliberty started in 8.192 seconds..
[Container] [INFO] Running integration tests...
[Container] [INFO]
[Container] [INFO] -------------------------------------------------------
[Container] [INFO]  T E S T S
[Container] [INFO] -------------------------------------------------------
[Container] [INFO] [AUDIT   ] CWWKT0016I: Web application available (default_host): http://7dc48e165fb3:9080/
[Container] [INFO] [AUDIT   ] CWWKZ0003I: The application customer-ms-openliberty updated in 8.084 seconds.
[Container] [INFO] Running it.dev.appsody.customer.EndpointTest
[Container] [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.173 s - in it.dev.appsody.customer.EndpointTest
[Container] [INFO] Running it.dev.appsody.customer.HealthEndpointTest
[Container] [INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.656 s - in it.dev.appsody.customer.HealthEndpointTest
[Container] [INFO]
[Container] [INFO] Results:
[Container] [INFO]
[Container] [INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[Container] [INFO]
[Container] [INFO] Integration tests finished.
```

- You can also verify it as follows.

```
$ docker ps
CONTAINER ID        IMAGE                           COMMAND                  CREATED             STATUS              PORTS                                                                                                                                                                     NAMES
7dc48e165fb3        kabanero/java-openliberty:0.2   "/.appsody/appsody-c…"   7 minutes ago       Up 7 minutes        0.0.0.0:7777->7777/tcp, 0.0.0.0:9080->9080/tcp, 0.0.0.0:9443->9443/tcp                                                                                                    customer-ms-openliberty
1aeff94d436f        jaegertracing/all-in-one:1.11   "/go/bin/all-in-one-…"   22 minutes ago      Up 22 minutes       0.0.0.0:5775->5775/udp, 0.0.0.0:5778->5778/tcp, 0.0.0.0:9411->9411/tcp, 0.0.0.0:14268->14268/tcp, 0.0.0.0:6831-6832->6831-6832/udp, 0.0.0.0:16686->16686/tcp, 14250/tcp   jaeger
a9c442e3ceb1        couchdb                         "tini -- /docker-ent…"   3 hours ago         Up 3 hours          4369/tcp, 9100/tcp, 0.0.0.0:5984->5984/tcp                                                                                                                                goofy_noether
cc037d68430f        dev.local/auth-ms-openliberty   "/opt/ol/helpers/run…"   2 days ago          Up 2 days           0.0.0.0:9081->9080/tcp, 0.0.0.0:9444->9443/tcp                                                                                                                            auth-ms-openliberty
```

### Validating the application

- Now generate a JWT Token with `openid` scope, which will let you create/get/delete orders.

To do so, run the commands below:

```
curl -k -d "grant_type=password&client_id=bluecomputeweb&client_secret=bluecomputewebs3cret&username=<user>&password=<password>&scope=openid" https://localhost:9444/oidc/endpoint/OP/token
```

If it is successful, you will see something like below.

```
$ curl -k -d "grant_type=password&client_id=bluecomputeweb&client_secret=bluecomputewebs3cret&username=foo&password=bar&scope=openid" https://localhost:9444/oidc/endpoint/OP/token
{"access_token":"eyJraWQiOiJ0aDVYaWg2Z0NQLV83d2pyN2FtOF8yVURnbGxQc28xT1pjNlpMcmhJdmZJIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJmb28iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwic2NvcGUiOlsib3BlbmlkIl0sImp0aSI6Ill1ekEzMkVwcnJKcmcySEMiLCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo5NDQ0L29pZGMvZW5kcG9pbnQvT1AiLCJleHAiOjE2MDMyMTk1ODAsImlhdCI6MTYwMzIxMjM4MCwiZ3JvdXBzIjpbIlVzZXJzIl0sInVwbiI6ImZvbyJ9.ioBKZpIZ6x0c00v0bgwyxRWoSIXf-LXFY0kjQHOT9zrbXzZte3JLpx1J1FkPvYntxZOmxzIGRIiNuMUlRveOD1DIZkjJcsMyijQt2NCx9FWxPN3bUOHhG3F7MCJxCR7tANNMa3DfbQceXmLeP2n4KgLdYoV0SNQ7k5OUZNhuMg5KShledehOeGfrfT8VbXM0gBKC8RyH5wmQLUQj_ykzkKJ4y2czwmUq4NKsVqZoFFdYaFvKFOuxAXSPiIKDWlMViU2L8599wyNi4IHX5LOfLu3vDh-SLVkFebR1SU_82qUwsD6qUcDzJwwrqQE1o5DLc4k7Olo2ZfbdAJGk56TjfA","token_type":"Bearer","expires_in":7200,"scope":"openid","refresh_token":"eN5kHBxwVmZcyGRScE2WLDIwqOUTn50tiwVRkgOwoaBnRcHiSc"}
```      

#### Validation

Now, you can validate the application as follows.

* Get the customer info.

Run the following to retrieve the customer information. Be sure to use the JWT retrieved from the previous step in place of `<jwt token>`.

```
curl -k --request GET --url https://localhost:9443/micro/customer --header 'Authorization: Bearer <jwt token>'   --header 'Content-Type: application/json'
```

If it is running successfully, you will see something like below.

```
$ curl -k --request GET --url https://localhost:9443/micro/customer --header 'Authorization: Bearer eyJraWQiOiJ0aDVYaWg2Z0NQLV83d2pyN2FtOF8yVURnbGxQc28xT1pjNlpMcmhJdmZJIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJmb28iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwic2NvcGUiOlsib3BlbmlkIl0sImp0aSI6IlFqVFZUWE1KNk9USHlqTnIiLCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo5NDQ0L29pZGMvZW5kcG9pbnQvT1AiLCJleHAiOjE2MDMyMDUzMjUsImlhdCI6MTYwMzE5ODEyNSwiZ3JvdXBzIjpbIlVzZXJzIl0sInVwbiI6ImZvbyJ9.k4mCqokWHFQMmyZF7Fu4iXMHdq77k6IDcgwH5Pm1UidIWz_Xi05c-vSgKkmLUR6xkbDnd5-9YslxrRotYNupMiHlc4lG0M3KYwFJrwVjphMQ8bXYmreILjQQqEj4GN-cJzR83N5Kt5y7y3ScBsdrlGeOO2Z-AUcSyzZBHVKNedwJElZo8idzLRaqldCYnSz6tr9krlED70XgRH9f4ea2hjtWh2VRZgcg2H1b1Z35U-LuqnUWO4uTrzOgerz0hBeDibcNOXZVMAH-KOxuyhGwEddUfaWeF3fIwkRW8NJtHiPI4wdL4OjbGwx_D4NtaZwodVtD-4PdC651JDtuwuaa-Q'   --header 'Content-Type: application/json'
{"docs":[
{"_id":"07a4fea9b287c40f102d7a1398000dd1","_rev":"1-83b2a6a2b4670ba7b27f41ba23dabe93","username":"foo","password":"bar","email":"foo@address.com","firstName":"foo","lastName":"fooLast","imageUrl":"image"}
],
"bookmark": "g1AAAABweJzLYWBgYMpgSmHgKy5JLCrJTq2MT8lPzkzJBYorGJgnmqSlJlomGVmYJ5sYpBkaGKWYJxoaW1oYGBikpBiC9HHA9BGlIwsAlxMd8Q",
"warning": "No matching index found, create an index to optimize query time."}
```

If enabled Jaegar, access it at http://localhost:16686/ and point the service to `customer-ms-openliberty` to access the traces.

![Jaegar UI](./static/jaegar_ui_orders.png)

![Jaegar traces](./static/jaegar_traces.png)

Also openapi is enabled.

    - http://localhost:9080/openapi/ui/
    - http://localhost:9080/index.html
    - http://localhost:9080/health
    - http://localhost:9080/openapi

![Openapi UI](./static/openapi-ui.png)

### Exiting the application

To exit the application, just press `Ctrl+C`.

It shows you something like below.

```
[Container] [INFO] ------------------------------------------------------------------------
[Container] [INFO] BUILD SUCCESS
[Container] [INFO] ------------------------------------------------------------------------
[Container] [INFO] Total time:  20:35 min
[Container] [INFO] Finished at: 2020-05-07T09:17:39Z
[Container] [INFO] ------------------------------------------------------------------------
Closing down development environment.
```

## Conclusion

You have successfully deployed and tested the Orders Microservice and a MariaDB database in local Docker Containers using Appsody.

To see the Orders application working in a more complex microservices use case, checkout our Microservice Reference Architecture Application [here](https://cloudnativereference.dev/).
