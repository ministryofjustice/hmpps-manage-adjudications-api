package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.util.ReflectionUtils
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.spring.web.plugins.WebFluxRequestHandlerProvider
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.DraftAdjudicationController
import java.lang.reflect.Field
import java.util.stream.Collectors

@Configuration
@Import(BeanValidatorPluginsConfiguration::class)
class SpringFoxConfiguration() {

  @Bean
  fun api(): Docket? {
    return Docket(DocumentationType.OAS_30)
      .useDefaultResponseMessages(false)
      .select()
      .apis(RequestHandlerSelectors.basePackage(DraftAdjudicationController::class.java.getPackage().name))
      .paths(PathSelectors.any())
      .build()
  }

  @Bean
  fun springfoxHandlerProviderBeanPostProcessor(): BeanPostProcessor? = object : BeanPostProcessor {
    @Throws(BeansException::class)
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
      if (bean is WebMvcRequestHandlerProvider || bean is WebFluxRequestHandlerProvider) {
        customizeSpringfoxHandlerMappings(getHandlerMappings(bean))
      }
      return bean
    }

    private fun <T : RequestMappingInfoHandlerMapping?> customizeSpringfoxHandlerMappings(mappings: MutableList<T>) {
      val copy = mappings.stream()
        .filter { mapping: T -> mapping?.patternParser == null }
        .collect(Collectors.toList())
      mappings.clear()
      mappings.addAll(copy)
    }

    private fun getHandlerMappings(bean: Any): MutableList<RequestMappingInfoHandlerMapping> = try {
      val field: Field? = ReflectionUtils.findField(bean.javaClass, "handlerMappings")
      field?.isAccessible = true
      @Suppress("UNCHECKED_CAST")
      field?.get(bean) as MutableList<RequestMappingInfoHandlerMapping>
    } catch (e: IllegalArgumentException) {
      throw IllegalStateException(e)
    } catch (e: IllegalAccessException) {
      throw IllegalStateException(e)
    }
  }
}
