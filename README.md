# Demo of Using GraphQL with Spring Boot

GraphQL is a relatively new query language to be used in API communications. In some situations, it outperforms RESTful APIs, for example, allowing multiple queries in a single request, querying nested resources. Spring Boot v2.7.0, a recently released version, starts to officially support using GraphQL easily without much configuration.

This article walks through how to implement GraphQL endpoints in Spring Boot. Readers are assumed to have the following knowledge:

- Basic understanding of Kotlin and Java
- Development using Spring Boot
- Basic understanding of any variant of Spring Data

## Sample Project

In this demonstration, Neo4j database with Spring Data Neo4j are chosen to be data sources of GraphQL endpoints, but you are free to use any data sources with GraphQL, e.g. MySQL databases, in-memory caches, remote HTTPS data sources, etc. Please also note that Kotlin and Java are interchangeable.

Readers are welcomed to look at source code of the sample project and this article concurrently.

Sample Project: [https://github.com/sunny-chung/spring-boot-graphql-demo](https://github.com/sunny-chung/spring-boot-graphql-demo)

## Setup

This demonstration uses Gradle to manage dependencies. Below dependencies are required. Remember only Spring Boot 2.7.0 or upper can be used.

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-data-neo4j")
}
```

It is required to define GraphQL schemas for use in API communication. It is defined as `graphql/*.graphqls` files under the resources directory.

We use data from the Movie database that comes with Neo4j installation as an example. Therefore, the GraphQL schemas can be like this:

```graphql
# Root Queries
type Query {
    movie(name: String!): Movie
    movies: [Movie!]!
}

type Movie {
    id: Int!
    title: String!
    tagline: String
    released: Int!

    director: [Person!]
    actors: [Actor!]
    reviews: [Review!]
}

type Actor {
    name: String!
    roles: [String!]
}

type Person {
    name: String!
    follows: [Person!]
    followers: [Person!]
    wroteReviews: [Review!]
}

type Review {
    summary: String!
    rating: Int!
    reviewer: Person
}
```

We defined two root queries to query a movie and all movies respectively. A `[]` represents an array, `()` allows parameter inputs, and `!` represents non-nullable. This schema will be publicly accessible.

We also need to define neo4j connection config and define entity classes.

## Root Queries

Then, we can start implementing root queries in a controller. You can see we do not have to configure anything about GraphQL before implementation.

```kotlin
@Controller
class MovieGraphqlApi {
    @Autowired lateinit var movieRepository: MovieRepository

    @QueryMapping("movie")
    fun movie(@Argument name: String): Movie =
        movieRepository.findByTitle(title = name)!!

    @QueryMapping("movies")
    fun movies(): List<Movie> = movieRepository.findAll()
}
```

To prevent loading an unnecessarily large graph and cyclic relations from the database, we specify all the Neo4j cypher queries ourselves not to load any relationships.
```kotlin
interface MovieRepository : Neo4jRepository<Movie, Long> {
    @Query("MATCH (n: Movie) RETURN n")
    override fun findAll(): MutableList<Movie>

    @Query("MATCH (n: Movie {title: \$title}) RETURN n")
    fun findByTitle(title: String): Movie?
}
```

Now, run the Spring Boot application and try to send some queries using your favourite HTTP client, for example, [Insomnia](https://insomnia.rest/).

Sample Request
```graphql
fragment Person on Person {
    name
}

fragment FullPerson on Person {
    name
    follows {
        ...Person
    }
    followers {
        ...Person
    }
    wroteReviews {
        ...Review
    }
}

fragment Review on Review {
    summary
    rating
    reviewer {
        name
        follows {
            ...Person
        }
        followers {
            ...Person
        }
    }
}

fragment Movie on Movie {
    id
    title
    tagline
    released
    director {
        ...FullPerson
    }
    actors {
        name
        roles
    }
    reviews {
        ...Review
    }
}

query {
    movie(name: "The Matrix") {
        ...Movie
    }
    movies {
        ...Movie
    }
}
```

Sample Response
```json
{
  "data": {
    "movie": {
      "id": 0,
      "title": "The Matrix",
      "tagline": "Welcome to the Real World",
      "released": 1999,
      "director": [],
      "actors": [],
      "reviews": []
    },
    "movies": [
      {
        "id": 0,
        "title": "The Matrix",
        "tagline": "Welcome to the Real World",
        "released": 1999,
        "director": [],
        "actors": [],
        "reviews": []
      },
      {
        "id": 9,
        "title": "The Matrix Reloaded",
        "tagline": "Free your mind",
        "released": 2003,
        "director": [],
        "actors": [],
        "reviews": []
      },
      // ...
```

## Nested Queries

It can be observed that nested relationships are not fetched. This is expected, because we explicitly ignored them in the `@Query` expression.

We will retrieve them using `@SchemaMapping` in this section. Let's add below methods to the controller.

```kotlin
    @SchemaMapping(field = "reviews", typeName = GraphqlApi.Type.Movie)
    fun movieReviews(movie: Movie): List<Review> {
        return movieRepository.findReviewByMovieId(movie.id!!)?.reviews ?: emptyList()
    }

    @SchemaMapping(field = "follows", typeName = GraphqlApi.Type.Person)
    fun personFollows(person: Person): List<Person> {
        return personRepository.findFollowsOfPerson(person.id!!)
    }

    @SchemaMapping(field = "followers", typeName = GraphqlApi.Type.Person)
    fun personFollowers(person: Person): List<Person> {
        return personRepository.findFollowersOfPerson(person.id!!)
    }
```

and the constants, for better mobility to rename types in the future.

```kotlin
object GraphqlApi {
    object Type {
        const val Movie = "Movie"
        const val Person = "Person"
    }
}
```

Let's execute the queries again. All the reviewers, the people they follow and their followers can be all fetched.

```json
{
  "data": {
    "movies": [
      // ...
      {
        "id": 109,
        "title": "The Da Vinci Code",
        "tagline": "Break The Codes",
        "released": 2006,
        "director": [],
        "actors": [],
        "reviews": [
          {
            "summary": "Fun, but a little far fetched",
            "rating": 65,
            "reviewer": {
              "name": "James Thompson",
              "follows": [
                {
                  "name": "Jessica Thompson"
                }
              ],
              "followers": [
                {
                  "name": "Jessica Thompson"
                }
              ]
            }
          },
          {
            "summary": "A solid romp",
            "rating": 68,
            "reviewer": {
              "name": "Jessica Thompson",
              "follows": [],
              "followers": []
            }
          }
        ]
      },
      // ...
```

But, do you notice the query is a bit slow? Let's turn on cypher logging in `application.yml` to see what happened.
```yaml
logging:
  level:
    org.springframework.data.neo4j: DEBUG
```

When we are querying for a list of movies, say there are `N` movies, we further queried movie's reviewers for `N` times. Neglecting the further person queries, we sent `N+1` queries here, which is known as the N + 1 select problem. The slowness would drastically increase when there are many users using your service.

## Batch Queries

To improve the performance, we can group all the queries of movie's reviewers, and send only one batch query to the database. Spring Boot provides `@BatchMapping` to help us group all the requests of the same field of a type.

Let's change the `movieReviews` handler in the controller by using `@BatchMapping`.

```kotlin
    @BatchMapping(field = "reviews", typeName = GraphqlApi.Type.Movie)
    fun movieReviews(movies: List<Movie>): Map<Movie, List<Review>> {
        val ids = movies.map { it.id!! }
        val result = movieRepository.findReviewByMovieIds(ids)
            .associateBy { it.id!! }
        return movies.associateWith { result[it.id!!]?.reviews ?: emptyList() }
    }
```

The queries that fetch follows and followers of people can also be batched. It is left as an exercise for readers.

## Mutations

GraphQL can also be used to handle requests that modify data. It is accomplished using the `Mutation` type. `input` in GraphQL schema can be used to structure multiple-layer data.

For example, let's define a mutation in the GraphQL schema to add a review to a movie:

```graphql
input AddMovieReviewInput {
    reviewer: String!
    summary: String!
    rating: Int!
    movie: String!
}

type Mutation {
    addMovieReview(input: AddMovieReviewInput): Review
}
```

Implement in controller using the `@MutationMapping` annotation.

```kotlin
    @MutationMapping("addMovieReview")
    fun addMovieReview(@Argument input: AddMovieReviewPayload): Review {
        val movie = movieRepository.findMovieByTitle(input.movie) ?: throw IllegalArgumentException("No such movie")
        val reviewer = personRepository.findByName(input.reviewer) ?: throw IllegalArgumentException("No such reviewer")
        movie.reviews!!.add(
            Review(
                summary = input.summary,
                rating = input.rating,
                reviewer = reviewer
            )
        )
        return movieRepository.save(movie).reviews!!.first { it.reviewer == reviewer }
    }
```

Afterwards, send a request to try it out.

```graphql
mutation {
  addMovieReview(input: {
    reviewer: "Kelly McGillis",
    summary: "This is a good movie!",
    rating: 99,
    movie: "The Matrix"
  }) {
    reviewer {
      name
      follows {
        name
      }
      followers {
        name
      }
    }
    summary
  }
}
```

Finally, verify the new review is added by querying the movie again.

Similar to queries, multiple mutations can be sent within one request.

## Custom Scalar Types

GraphQL supports a few scalar types, but some common types, for example, timestamps, are not supported. Here, we will define a scalar type `Instant` to map to the Java class `Instant`.

First, add an `Instant` field to the `Review` class.
```kotlin
    var createdWhen: Instant? = null
```

Don't forget to modify `addMovieReview()` to store current timestamp with new Review objects.

Also, add the new scalar type and new field to the GraphQL schema.

```graphql
scalar Instant

type Review {
    summary: String!
    rating: Int!
    reviewer: Person
    createdWhen: Instant
}
```

The core part comes. We need to define how to translate the new scalar type by configuration.

```kotlin
@Configuration
class GraphQLConfig {
    @Bean
    fun registerScalarTypes(): RuntimeWiringConfigurer {
        return RuntimeWiringConfigurer { wiring ->
            wiring
                .scalar(
                    GraphQLScalarType.newScalar()
                        .name("Instant")
                        .coercing(object : Coercing<Instant, String> {
                            override fun serialize(dataFetcherResult: Any): String {
                                val value = dataFetcherResult as Instant?
                                return value.toString()
                            }

                            override fun parseValue(input: Any): Instant {
                                if (input is String) {
                                    return Instant.parse(input)
                                } else {
                                    throw CoercingParseValueException("unsupported value $input")
                                }
                            }

                            override fun parseLiteral(input: Any): Instant {
                                if (input is StringValue) {
                                    return Instant.parse(input.value)
                                } else {
                                    throw CoercingParseLiteralException("unsupported value $input")
                                }
                            }
                        })
                        .build()
                )
        }
    }
}
```

After adding this boilerplate, adding new records, and adding the new field to the query, the new field should be outputted in the query response as expected.

```graphql
{
  "data": {
    "movie": {
      "id": 0,
      "title": "The Matrix",
      "tagline": "Welcome to the Real World",
      "released": 1999,
      "director": [],
      "actors": [],
      "reviews": [
        {
          "summary": "This is a good movie!",
          "rating": 99,
          "reviewer": {
            "name": "Kelly McGillis",
            "follows": [],
            "followers": []
          },
          "createdWhen": "2022-05-28T10:23:25.237169Z"
        }
      ]
    },
    // ...
```

## Customizing Error Response

It is observed that error messages are always "INTERNAL_ERROR for {executionId}", which is not informational. It can be a time killer during development.

We can output something informational, for example, showing the original exception message, by implementing `DataFetcherExceptionResolver`.

In this minimal example, we show error messages only if the exception is a `IllegalArgumentException`.

```kotlin
@Component
class GraphqlErrorHandler : DataFetcherExceptionResolverAdapter() {
    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? {
        return when (ex) {
            is IllegalArgumentException -> GraphQLErrorImpl(ErrorType.ValidationError, ex.message ?: ex::class.simpleName!!)
            else -> super.resolveToSingleError(ex, env)
        }
    }

    class GraphQLErrorImpl(val type: ErrorType, val errorMessage: String) : GraphQLError {
        override fun getMessage(): String = errorMessage

        override fun getLocations(): MutableList<SourceLocation> {
            return mutableListOf()
        }

        override fun getErrorType(): ErrorClassification {
            return type
        }
    }
}
```

Now, let's try to input a non-existing movie name in the mutation `addMovieReview` to trigger an error, and check if error message becomes meaningful.

## Conclusion

In this article, we learnt how to implement GraphQL APIs by going through various common tasks that we would meet during API implementation. Actually, Spring for GraphQL also provides other less common features like interceptors and subscriptions. Please have a visit on their documentation when necessary: [https://docs.spring.io/spring-graphql/docs/current/reference/html/](https://docs.spring.io/spring-graphql/docs/current/reference/html/)

Thank you for reading. Hope this article helps you!

