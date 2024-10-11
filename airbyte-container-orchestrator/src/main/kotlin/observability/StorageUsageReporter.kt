package io.airbyte.containerOrchestrator.observability

import io.airbyte.featureflag.Connection
import io.airbyte.featureflag.FeatureFlagClient
import io.airbyte.featureflag.ReportConnectorDiskUsage
import io.airbyte.metrics.lib.MetricAttribute
import io.airbyte.metrics.lib.MetricClient
import io.airbyte.metrics.lib.MetricTags
import io.airbyte.metrics.lib.OssMetricsRegistry
import io.airbyte.workers.pod.FileConstants.DEST_DIR
import io.airbyte.workers.pod.FileConstants.SOURCE_DIR
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micronaut.context.annotation.Value
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import java.io.BufferedReader
import java.io.InputStreamReader

private val logger = KotlinLogging.logger {}

/**
 * Periodically measures disk usage for connector dirs and reports a metric to DD.
 * Can be enabled/disabled via FF in case of performance degradation.
 */
@Singleton
class StorageUsageReporter(
  @Value("\${airbyte.connection-id}") private val connectionId: String,
  private val metricClient: MetricClient,
  private val featureFlagClient: FeatureFlagClient,
) {
  @Scheduled(fixedDelay = "60s")
  fun reportConnectorDiskUsage() {
    // NO-OP if FF disabled
    if (!featureFlagClient.boolVariation(ReportConnectorDiskUsage, Connection(connectionId))) {
      return
    }

    val sourceMbUsed = measureDirMbViaProc(SOURCE_DIR)
    logger.debug { "Disk used by source: $sourceMbUsed MB" }

    val destMbUsed = measureDirMbViaProc(DEST_DIR)
    logger.debug { "Disk used by dest: $destMbUsed MB" }

    val connectionIdAttr = MetricAttribute(MetricTags.CONNECTION_ID, connectionId)
    sourceMbUsed?.let {
      val typeAttr = MetricAttribute(MetricTags.CONNECTOR_TYPE, "source")
      metricClient.gauge(OssMetricsRegistry.CONNECTOR_STORAGE_USAGE_MB, it, typeAttr, connectionIdAttr)
    }
    destMbUsed?.let {
      val typeAttr = MetricAttribute(MetricTags.CONNECTOR_TYPE, "destination")
      metricClient.gauge(OssMetricsRegistry.CONNECTOR_STORAGE_USAGE_MB, it, typeAttr, connectionIdAttr)
    }
  }

  private fun measureDirMbViaProc(dir: String): Double? {
    logger.debug { "Checking disk usage for dir: $dir" }

    val command =
      arrayOf(
        "/bin/sh",
        "-c",
        """du -cm $dir | tail -n 1 | grep -o '^[0-9]\+'""",
      )

    val proc = Runtime.getRuntime().exec(command)

    val exitCode = proc.waitFor()
    if (exitCode != 0) {
      return null
    }

    return try {
      val r = BufferedReader(InputStreamReader(proc.inputStream))
      val result = r.readLine()
      result.toDouble()
    } catch (e: Exception) {
      logger.error(e) { "Failure checking disk usage for dir: $dir" }
      null
    }
  }
}