package sunny.demo.springbootgraphql.repository

import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import sunny.demo.springbootgraphql.model.entity.Movie

interface MovieRepository : Neo4jRepository<Movie, Long> {
    @Query("MATCH (n: Movie) RETURN n")
    override fun findAll(): MutableList<Movie>

    @Query("MATCH (n: Movie {title: \$title}) RETURN n")
    fun findByTitle(title: String): Movie?

    fun findMovieByTitle(title: String): Movie?

    @Query("MATCH (m:Movie)<-[r:REVIEWED]-(p:Person) WHERE ID(m)=\$movieId RETURN m, collect(r), collect(p)")
    fun findReviewByMovieId(movieId: Long): Movie?

    @Query("MATCH (m:Movie)<-[r:REVIEWED]-(p:Person) WHERE ID(m) IN \$movieIds RETURN m, collect(r), collect(p)")
    fun findReviewByMovieIds(movieIds: List<Long>): List<Movie>
}
