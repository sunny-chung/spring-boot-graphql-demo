package sunny.demo.springbootgraphql.model.entity

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import sunny.demo.springbootgraphql.model.payload.Actor

@Node
data class Movie(
    @Id
    @GeneratedValue
    var id: Long? = null,

    var title: String,

    var tagline: String?,

    var released: Int,

    @Relationship(type = "DIRECTED", direction = Relationship.Direction.INCOMING)
    var director: MutableList<Person>? = null,

    @Relationship(type = "ACTED_IN", direction = Relationship.Direction.INCOMING)
    var actorsAndRoles: MutableList<Roles>? = null,

    @Relationship(type = "REVIEWED", direction = Relationship.Direction.INCOMING)
    var reviews: MutableList<Review>? = null

) {
    /**
     * Used in response only.
     */
    @Transient
    var actors: List<Actor> = emptyList()
}
