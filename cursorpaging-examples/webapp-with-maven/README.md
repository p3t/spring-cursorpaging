# Example Spring Boot Web App, using cursor-based pagination

## Overview

This should be a half-way realistic application implementation used to demonstrate cursor-based pagination. The
application is a simple Spring Boot web application that exposes a REST API to manage a list of data-records.

## Build-notice (gradle vs. maven)!

The project build with gradle is integrated in the cursorpaging overall continuous build - and should work all the time.
The maven build is using the latest released version of the libraries and is only updated in case of a new release.
Therefor it is possible, that it will fail in case new features are used which are not yet released!

## Required infrastructure

For running the application, you need a PostgreSQL ( s. the [docker-compose.yaml](docker-compose.yaml) ) and a Java 17 runtime.

## Components

- Controller
  - An API model (DTOs) mapped from/to the domain model
- Persistence Layer (Repository)
  - Note that the service layer is omitted for simplicity (no business logic present), still there is a service
    generating some test data
- Configurations:
  - EntitySerializerConfig: Demonstrates how to configure a secret and domain-model serializer(s)
  - JpaConfig: Activates the paging repository aspect
  - WebConfig: Some utility for the web layer

## Mapping order and filters for a initial page request in the controller

Ideally I would prefer to have a single value-object like a `DataRecordPageRequest` that contains all the necessary
properties:

```java

import java.util.LinkedHashMap;

@Data
@NoArgsConstructor
public class DataRecordPageRequest {

  private Map<DataRecordAttribute, Order> orderBy = new LinkedHashMap<>();

  private MultiValueMap<DataRecordAttribute, String> filterBy = new LinkedMultiValueMap<>();

  @Min( 1 )
  @Max( 100 )
  private int pageSize = 100;

  public static DataRecordPageRequest valueOf( final String value ) {
    return new DataRecordPageRequest();
  }
}
```

Unfortunately I found no way to map this into the URL request parameters of a GET request.
One alternative approach (s. controller) is to use a POST request and just return the cursor which then can be used to
request the first page.

Still I tried some other variants for having the information in the URL encoded, which turned out to be specifically
problematic. Here some tries:

```java
    // @formatter:off
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class AttributeOrder {
    
        private DataRecordAttribute attribute; // enum with allowed attributes
        private Order order; // ASC/DESC enum
    
        AttributeOrder valueOf( String value ) {
            return new AttributeOrder( DataRecordAttribute.ID, Order.ASC );
        }
    }

    @GetMapping( produces = "application/json" )
    @ResponseStatus( HttpStatus.OK )
    public CollectionModel<DtoDataRecord> getDataRecords(
            
        // All of this variants will result in a "no matching editors or conversion strategy found" error
        @Parameter() @RequestParam( required = false ) final List<AttributeOrder> order1,
        @Parameter( style = ParameterStyle.DEEPOBJECT ) @RequestParam( required = false ) final List<AttributeOrder> order2,
        @Parameter( style = ParameterStyle.DEEPOBJECT ) @RequestParam( required = false ) final AttributeOrder[] order3,
        @Parameter( style = ParameterStyle.FORM ) @RequestParam( required = false ) final AttributeOrder order4,

        // order = null, just not mapped
        @Parameter( style = ParameterStyle.DEEPOBJECT ) @RequestParam( required = false ) final AttributeOrder order5,

        // works but the field must be required, to be shown in swagger
        @Parameter( description = "Define the order of the records", example = """
                {
                    "NAME": "DESC",
                    "ID": "ASC"
                }""" ) @RequestParam( defaultValue = "{}" ) final Map<DataRecordAttribute, Order> orderBy,
                
        // still there is the glitch, that when adding multiple parameters, all of them contain all request parameter.
        // We can use this to have a good representation in Swagger, but we need to parse the request parameters by hand
        @Parameter( style = ParameterStyle.DEEPOBJECT, example = """
                {
                  "NAME": "ASC",
                  "ID": "ASC"
                }""" ) @RequestParam final Map<DataRecordAttribute, Order> orderBy,
        @Parameter( style = ParameterStyle.DEEPOBJECT, example = """
                {
                  "NAME": [
                    "Alpha", "Bravo"
                  ]
                }""" ) @RequestParam final MultiValueMap<DataRecordAttribute, String> filterBy,
        @Parameter( style = ParameterStyle.DEEPOBJECT ) @RequestParam @MaxSize( 20 ) final Optional<Integer> pageSize,
        @RequestParam( required = false ) final MultiValueMap<String, String> request
    ) { 
        //...
    }
    // @formatter:on
```

Other considered alternatives:

- Use simple parameters, and name them as the model
  properties: `@RequestParam( required = false ) final Order orderByName, @RequestParam( required = false ) final Order orderByCreatedAt`
- Encode all in the attribute enum: "ORDER_BY_NAME_ASC", "ORDER_BY_CREATED_AT_DESC, etc."
- Just use a string and decode the contained json in the controller method.

As there is a significant difference between creating a cursor for the first page and requesting the data for all
subsequent page requests, personally I liked the POST approach most.
