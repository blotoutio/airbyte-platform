package io.airbyte.mappers.transformations

import com.fasterxml.jackson.databind.JsonNode
import io.airbyte.config.ConfiguredMapper
import io.airbyte.config.MapperConfig

interface MapperSpec<T : MapperConfig> {
  fun deserialize(configuredMapper: ConfiguredMapper): T

  fun jsonSchema(): JsonNode

  fun specType(): Class<*>
}