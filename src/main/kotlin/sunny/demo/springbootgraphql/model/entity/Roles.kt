package sunny.demo.springbootgraphql.model.entity

import org.springframework.data.neo4j.core.schema.RelationshipId
import org.springframework.data.neo4j.core.schema.RelationshipProperties
import org.springframework.data.neo4j.core.schema.TargetNode

@RelationshipProperties
data class Roles(
    @RelationshipId
    var id: Long?,

    var roles: MutableList<String>,

    @TargetNode
    var actor: Person
)
