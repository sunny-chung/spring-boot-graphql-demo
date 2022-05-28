package sunny.demo.springbootgraphql.config

import graphql.ErrorClassification
import graphql.ErrorType
import graphql.GraphQLError
import graphql.language.SourceLocation
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.stereotype.Component

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