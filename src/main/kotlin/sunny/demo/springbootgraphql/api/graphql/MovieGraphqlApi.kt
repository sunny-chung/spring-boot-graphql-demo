package sunny.demo.springbootgraphql.api.graphql

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.*
import org.springframework.stereotype.Controller
import sunny.demo.springbootgraphql.model.entity.Movie
import sunny.demo.springbootgraphql.model.entity.Person
import sunny.demo.springbootgraphql.model.entity.Review
import sunny.demo.springbootgraphql.model.payload.AddMovieReviewPayload
import sunny.demo.springbootgraphql.repository.MovieRepository
import sunny.demo.springbootgraphql.repository.PersonRepository
import java.time.Instant

@Controller
class MovieGraphqlApi {
    @Autowired lateinit var movieRepository: MovieRepository
    @Autowired lateinit var personRepository: PersonRepository

    @QueryMapping("movie")
    fun movie(@Argument name: String): Movie = movieRepository.findByTitle(title = name)!!

    @QueryMapping("movies")
    fun movies(): List<Movie> = movieRepository.findAll()

//    @SchemaMapping(field = "reviews", typeName = GraphqlApi.Type.Movie)
//    fun movieReviews(movie: Movie): List<Review> {
//        return movieRepository.findReviewByMovieId(movie.id!!)?.reviews ?: emptyList()
//    }

    @SchemaMapping(field = "follows", typeName = GraphqlApi.Type.Person)
    fun personFollows(person: Person): List<Person> {
        return personRepository.findFollowsOfPerson(person.id!!)
    }

    @SchemaMapping(field = "followers", typeName = GraphqlApi.Type.Person)
    fun personFollowers(person: Person): List<Person> {
        return personRepository.findFollowersOfPerson(person.id!!)
    }

    @BatchMapping(field = "reviews", typeName = GraphqlApi.Type.Movie)
    fun movieReviews(movies: List<Movie>): Map<Movie, List<Review>> {
        val ids = movies.map { it.id!! }
        val result = movieRepository.findReviewByMovieIds(ids)
            .associateBy { it.id!! }
        return movies.associateWith { result[it.id!!]?.reviews ?: emptyList() }
    }

    @MutationMapping("addMovieReview")
    fun addMovieReview(@Argument input: AddMovieReviewPayload): Review {
        val movie = movieRepository.findMovieByTitle(input.movie) ?: throw IllegalArgumentException("No such movie")
        val reviewer = personRepository.findByName(input.reviewer) ?: throw IllegalArgumentException("No such reviewer")
        movie.reviews!!.add(
            Review(
                summary = input.summary,
                rating = input.rating,
                reviewer = reviewer,
                createdWhen = Instant.now()
            )
        )
        return movieRepository.save(movie).reviews!!.first { it.reviewer == reviewer }
    }

    @MutationMapping("deleteMovieReviews")
    fun deleteMovieReviews(@Argument movieName: String): Boolean {
        val movie = movieRepository.findMovieByTitle(movieName) ?: throw IllegalArgumentException("No such movie")
        movie.reviews?.clear()
        movieRepository.save(movie)
        return true
    }
}
