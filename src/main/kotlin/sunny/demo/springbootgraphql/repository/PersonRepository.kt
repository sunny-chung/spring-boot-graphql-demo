package sunny.demo.springbootgraphql.repository

import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import sunny.demo.springbootgraphql.model.entity.Person

interface PersonRepository : Neo4jRepository<Person, Long> {

    @Query("MATCH (n:Person)-[:FOLLOWS]->(f:Person) WHERE ID(n)=\$personId RETURN f")
    fun findFollowsOfPerson(personId: Long): List<Person>

    @Query("MATCH (n:Person)-[:FOLLOWS]->(f:Person) WHERE ID(n)=\$personId RETURN f")
    fun findFollowersOfPerson(personId: Long): List<Person>

    fun findByName(name: String): Person?
}
