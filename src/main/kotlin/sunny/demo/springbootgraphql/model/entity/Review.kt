package sunny.demo.springbootgraphql.model.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.neo4j.core.schema.RelationshipId
import org.springframework.data.neo4j.core.schema.RelationshipProperties
import org.springframework.data.neo4j.core.schema.TargetNode
import java.time.Instant

@RelationshipProperties
data class Review(
    @RelationshipId
    var id: Long? = null,

    var summary: String,

    var rating: Int,

    @TargetNode
    var reviewer: Person,

    @CreatedDate
    var createdWhen: Instant? = null,
)
