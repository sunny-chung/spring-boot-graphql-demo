package sunny.demo.springbootgraphql.config

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.GraphQLScalarType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import java.time.Instant

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
