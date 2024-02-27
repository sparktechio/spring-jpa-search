# Spring JPA Search

![Build spring jpa search](https://github.com/sparktechio/spring-jpa-search/actions/workflows/main.yml/badge.svg)

## Introduction:

Our simple search library provides a powerful solution for efficiently searching through resources within your 
application's database. By seamlessly mapping REST parameters to JPA (Java Persistence API), it simplifies the 
process of defining and executing typical queries, automating much of the mapping process for improved productivity 
and ease of use.

## How it Works:

Our library streamlines the mapping of REST parameters to JPA queries, enabling you to effortlessly define and execute 
searches within your application. Here's an overview of how it functions:
- `page` page number starting from `0`, optional parameter, default `0`, 0 or 1 item 
- `limit` max number of items in response, optional parameter, default `12`, 0 or 1 item
- `order` define sorting, optional parameter, default database order, 0, 1 or multiple items
- `filter` define filtering, optional parameter, default without filters, 0, 1 or multiple items
- `allDate` pull all data in case of needs, default disabled, 0, 1 item

## Simple example

Search users by age from 19 to 60 both included.
```http
GET /search?filter=age>:19&filter=age<:60
```

```java
@RestController
@RequestMapping("search")
public class UserController implements SearchService<String, UserEntity> {

    private final EntityManager entityManager;

    @GetMapping()
    public PageData<Response> search(@RequestParam MultiValueMap<String, String> queryParams) {
        return search(queryParams);
    }
    
    private PageData<Response> toPage(Page<UserEntity> users) {
        //...
    }
}
```

## Advanced example

Search users:
- filter by
  - age is 18 or 19 or 20
  - score is from 25 (included) to 60 (excluded)
  - associate role name is `CUSTOMER`
  - associate address country code is `BA`
- sort by
  - age descending order
  - created time ascending order
- fetch second page
- 20 items per page
- show only users with company id `10`

```http
GET /search
    ?page=1                                 fetch second page
    &limit=20                               20 items per page
    &filter=age::18|age::19|age::20         age is 18 or 19 or 20
    &filter=score>:25                       score is from 25 (included)
    &filter=score:<60                       score is to 60 (excluded)
    &filter=roles.name::CUSTOMER            associate role name is `CUSTOMER`
    &filter=addresses.country.code::BA      address country code is `BA`
    &order=age:d                            age descending order
    &order=created                          created time ascending order
```

```java
@RestController
@RequestMapping("search")
public class UserController implements SearchService<String, UserEntity> {

    private final EntityManager entityManager;

    @GetMapping()
    public PageData<Response> search(@RequestParam MultiValueMap<String, String> queryParams) {
        return search(
                queryParams, 
                (root, query, builder) -> builder.equal(root.get("companyId"), 10) // additional filter only users with company id `10`
        );
    }
    
    private PageData<Response> toPage(Page<UserEntity> users) {
        //...
    }
}
```

## Requirements

Implementation of the service require EntityManager instance only.

## Query parameter rules
- `page`
  - type: `int`
  - min value: `0`
  - required: `false`
  - default `0`
  - number of query params: `0,1`
- `limit`
  - type: `int`
  - min value: `1`
  - required: `false`
  - default `12`
  - number of query params: `0,1`
- `order`
  - type: `String`
  - pattern: `([\w.]*):([a,d])`
  - required: `false`
  - default `default database order`
  - number of query params: `0,1...n`
- `filter`
    - type: `String`
    - pattern: `([\\w.].*)([:</>~!]{2})(.*)"`
    - required: `false`
    - default `fetch whole dataset`
    - number of query params: `0,1...n`
- `allDate`
    - type: `boolean`
    - required: `false`
    - default `false`
    - number of query params: `0,1`
