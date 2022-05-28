package sunny.demo.springbootgraphql.model.payload

data class AddMovieReviewPayload(
    val reviewer: String,
    val summary: String,
    val rating: Int,
    val movie: String,
)
