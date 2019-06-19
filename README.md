# GeoGig OpenAPI interface

Geogig OpenAPI is an interface to Geogig data source to allow versioned editing of geospatial data similar to git.

This API is NGA funded as part of the GEOINT Services program


GeoGig's WEB API Swagger definition and implementations for Java Server, Java, JavaScript, and Python clients.

##Getting started

Build with:
```shell
$ mvn clean install
```

The default server implementation uses a directory of JSON files to store the configuration (users, stores, etc).
Have an empty directory to pass as argument: `mkdir -p /data/geogig-server/config`

Run the server:
```
cd modules/server
export GEOGIG_SERVER_CONFIG_DIRECTORY="/data/geogig-server/config"
mvn spring-boot:run
```
Browse to `http://localhost:8181/docs` to access the swagger generated API documentation.

Log in with one the preconfigured admin user: `admin`, password: `g30g1g`.

## Swagger generated sources

The `swagger-codegen-maven-plugin` will create:

* `modules/api-spec/target/generated-sources/swagger/src/main/java/`: request/response model objects for Java client and server
* `modules/server/target/generated-sources/swagger/src/main/java`: the server stub for spring-mvc
* `modules/clients/java/target/generated-sources/swagger/src/main/java/`: Java client using Jersey2 
* `modules/clients/js/target/generated-sources/swagger/`: Javascript client
* `modules/clients/python/target/generated-sources/swagger/`: Python client

## Module layout:

<pre>
modules
 |
 +-- api-spec
 |
 +-- gt-model-bridge
 |
 +-- server
 |
 +-- clients
       |
       +-- java
       |
       +-- geotools-datastore
       |
       +-- js
       |
       +-- python
</pre>

## Module inter-dependencies:

<pre>
   api-spec  <-------- gt-model-bridge 
   ^  ^  ^                ^     ^
   |  |  |                |     |
   |  |  |                |   java-server
   |  | java-client <-- geotools-datastore
   | js-client    
 python-client
</pre>

## Status

* Continued on requirements gathering, API specification, and prototyping for an improved GeoGig WEB API and related server and clients implementations that augment geogig-core's capabilities in order to provide a consistent experience across the different products: BSE, SDK, Mobile, Exchange, and Desktop.

* Prototype Status:
	* Repository Stores: MVP API defined. CRUD operational (File and PostgreSQL backends). 
	* Users: MVP API defined. CRUD operational. 
	* Repository Management: MVP API defined. CRUD operational.
	* Raw Repository Access: MVP API draft. 
	* Transaction Management: MVP API defined. Operational but needs further work.
	* Async Tasks: MVP API defined. Not implemented.
	* Feature Service: MVP API defined.
		* Layers: CRUD Operational.
		* Feature Queries: Return formats GeoJSON and GeoJSON Binary (Smile format). Able to perform queries filtering by bounding box, feature ids, and limit/reorder attributes returned. More advanced query options defined but in progress.
		* Feature Updates and Deletes: working by specifying a predicate as either a bounding box, feature ids, or CQL Filter.
		* GeoTools DataStore: Operational. Able to query and perform transactions. Working as a GeoServer plugin. Ability for GeoServer OWS services to indicate which branch to work against TBD.
	* Events and notifications subsystem: TBD.
	* Collaboration workflows:
		* Forking: MVP API defined. Implementation in progress.
		* Pull requests: MVP API TBD.
	* Containerization/microservices:  TBD.
	 


## Resources

* Swagger: <https://swagger.io/>
* OpenAPI specification: <https://swagger.io/specification/>
* Springfox (Spring MVC/Swagger integration): 
	* home: <http://springfox.github.io/springfox/>
	* repo: <https://github.com/springfox/springfox>
* Swagger maven plugin : <https://github.com/kongchen/swagger-maven-plugin>
* Swagger codegen maven plugin:
	* repo: <https://mvnrepository.com/artifact/io.swagger/swagger-codegen-maven-plugin>
	* source: <https://github.com/swagger-api/swagger-codegen/tree/master/modules/swagger-codegen-maven-plugin>
* JSON Schema: <http://json-schema.org/>
* JSON Patch format: <https://tools.ietf.org/html/rfc6902>
* JSON Merge Patch format: <https://tools.ietf.org/html/rfc7396>
* Swagger-js: <https://github.com/swagger-api/swagger-js>
* HTTP Status Codes: <http://www.iana.org/assignments/http-status-codes/http-status-codes.xhtml>
* GeoJSON: <http://geojson.org/> spec: <https://tools.ietf.org/html/rfc7946>
* WFS 3.0:
	* <https://github.com/opengeospatial/WFS_FES>	
* Jackson binary data formats: <https://github.com/FasterXML/jackson-dataformats-binary>	
^^
