# GeekStore

A developer oriented, headless ecommerce framework based on Spring + GraphQL + Angular.

> **Headless** means GeekStore only focus on the backend, it does not care which frontend stack you use.
> GeekStore is built for developers, it only exposes a group of ecommerce admin&shop APIs(in GraphQL), by calling these APIs, developers
> can query data(such as query product data), or mutate data(such as add item with id '123' to the current order).
> So, if you want to develop ecommerce application based on GeekStore, you just need to implement the shopping frontend
> based on your specific business needs. For example, you can leverage Angular/React/Vue, whatever frontend stack you like to build your
> shopping UI. There is no need to re-build the backend, since GeekStore is an ecommerce backend without a 'head'.

> GeekStore is licensed with MIT license, you can change/extend as you need.

**Note, GeekStore is only for POC & learning purpose now.**.

## Features

- Products & Variants
- Stock management
- Product facets & faceted search
- Product categories / collections
- Product Search
- Payment provider integrations
- Shipping provider integrations
- Discounts and promotions
- Multiple administrators with fine-grained permissions
- Built-in admin interface(Angular based)
- Guest checkouts
- Multiple Authentication Methods

## Tech Stack

1. Backend framework：Spring Boot 2.x
2. Frontend(Admin interface) framework：Angular 10.x
3. GraphQL framework：[graphql-java-kickstart](https://github.com/graphql-java-kickstart/graphql-spring-boot)
4. Persistence layer framework：[Mybatis-Plus](https://mybatis.plus/)
5. Async Task：Guava EventBus
6. Security framework：customized，centralized token validation + AOP
7. DB：H2(local test) & MySQL(prod)

## How To Run

### 1. How To Run Test

Run unit + component tests via maven:

```shell
mvn clean test
```

**527** test cases have been written, cover most framework & biz logic, all test cases pass when run locally.
Use embedded H2 DB by default in local test mode.

```shell
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.42 s - in io.geekstore.e2e.StockControlTest
[INFO] Running io.geekstore.common.utils.TimeSpanUtilTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.01 s - in io.geekstore.common.utils.TimeSpanUtilTest
[INFO] Running io.geekstore.data_import.ImportParserTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.024 s - in io.geekstore.data_import.ImportParserTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 527, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:22 min
[INFO] Finished at: 2021-01-11T18:08:32+08:00
[INFO] ------------------------------------------------------------------------
```

### 2. How To Run The Application

Run app via maven:

```shell
mvn spring-boot:run
```

Or run it in Intellij IDE, the main driver class is **GeekStoreApplication**. It will use embedded H2 DB by default.

Then you can play the Store Admin & Shop API via the `GraphQL Playground`：

```shell
http://127.0.0.1:8080/playground
```

Or you can access the GraphQL endpoint via Postman(latest version supporting GraphQL)

```
http://127.0.0.1:8080/graphql
```

To access GraphQL Voyager for interactive graph:

```shell
http://localhost:8080/voyager
```

## TODO List

- [ ] Replace [graphql-java-kickstart](https://github.com/graphql-java-kickstart/graphql-spring-boot) with [Netflix DGS framework](https://netflix.github.io/dgs/)
- [ ] Support admin interface(based on Angular)
- [ ] Support shopping Web App(based on Angular) for store frontend showcase
- [ ] Support MySQL, now only H2 tested
- [ ] Enhance image processing
- [ ] Docker deployment
- [ ] Docs for developer
- [ ] Performance test scripts and tests
- [ ] MicroService version + GraphQL Federation
- [ ] ElasticSearch for product search, now only support simple DB search(use like)
- [ ] Cloud based image storage/processing, now only stores images locally with very simple processing
- [ ] Payment provider integrations(like paypal/alipay etc), now only support mock provider
- [ ] Shipping provider integrations, now only support mock provider

## Source Code Directory

Java Source

```
├── java
│   └── io
│       └── geekstore
│           ├── GeekStoreApplication.java # Spring Boot main entry
│           ├── common # Common classes
│           ├── config # Spring Configuration Beans
│           ├── custom # Security/GraphQL/Mybatis customization
│           ├── data_import # Product data import
│           ├── email # Email handling
│           ├── entity # Entity beans
│           ├── eventbus # Asyn event handling
│           ├── exception # Exception classes
│           ├── mapper # MyBatis-Plus Mapper
│           ├── options # App configs
│           ├── resolver # GraphQL API resolvers(a litter like controllers)
│           ├── service # Service layer(biz logic)
│           └── types # Java types(or DTOs) mapping to GraphQL schemas
```

Resources source

```
└── resources
    ├── application-mysql.yml # Spring configuration file to support MySQL
    ├── application.yml # Default spring configration file，support h2 db by default
    ├── banner.txt # banner
    ├── db
    │   ├── h2 # H2 db schema
    │   └── mysql # MySql db schema
    ├── graphql
    │   ├── admin-api # Admin side GraphQL API schemas
    │   ├── common # Common type schemas
    │   ├── shop-api # Shop side GraphQL API schemas
    │   └── type # Admin&Shop type schemas
    └── templates
        └── email # email templates
```

Java Source for Test：

```
├── java
│   └── io
│       └── geekstore
│           ├── ApiClient.java # GraphQL API client for test, has switch to support admin or shop mode.
│           ├── ApiException.java # API call exception
│           ├── GeekShopGraphQLTest.java # Annotation for GraphQL test(base on SpringBootTest)
│           ├── MockDataService.java # A service for creating mock data via the GraphQL API
│           ├── PopulateOptions.java # Configuration options used to initialize a test server environment
│           ├── common # Tests for common classes
│           ├── config # Spring Configuration Beans for test
│           ├── data_import # Tests for product importing function
│           ├── e2e # Tests for GraphQL resolvers，mosts tests for GeekStore reside in this directory！
│           ├── event # Event handler for test
│           ├── service # A few tests for helpers in service layer
│           └── utils # A few test helpers
```

Resources Source for Test：

```
.
├── application.yml # Spring configuration file for test
├── fixtures # Product mock data for test
├── graphql # GraphQL files(query/mutation/fragments) for test，used by tests in e2e folder.
└── test_fixtures # A few mock data for testing prodct importing function
```

## Attribution

The project is a partial clone of the [vendure](https://github.com/vendure-ecommerce/vendure) headless ecommerce framework.
The original project is based on TypeScript/Nestjs/Angular tech stack. Thanks to the author🙏

## Copyright

#### Copyright © 2021-present GeekStore. All rights reserved.
