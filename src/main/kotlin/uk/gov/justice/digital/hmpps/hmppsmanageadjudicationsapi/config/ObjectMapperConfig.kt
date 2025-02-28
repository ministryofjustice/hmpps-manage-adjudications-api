package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class ObjectMapperConfig {
  @Bean
  fun objectMapper(mapperBuilder: Jackson2ObjectMapperBuilder): ObjectMapper? = mapperBuilder.build<ObjectMapper>().setSerializationInclusion(JsonInclude.Include.NON_NULL)
}
