package sunny.demo.springbootgraphql.model.entity

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship

@Node
data class Person(
    @Id
    @GeneratedValue
    var id: Long? = null,

    var name: String,

    @Relationship(type = "FOLLOWS", direction = Relationship.Direction.OUTGOING)
    var follows: MutableList<Person> = mutableListOf(),

    @Relationship(type = "FOLLOWS", direction = Relationship.Direction.INCOMING)
    var followers: MutableList<Person> = mutableListOf(),

    @Relationship(type = "REVIEWED", direction = Relationship.Direction.OUTGOING)
    var wroteReviews: MutableList<Review> = mutableListOf()
)
