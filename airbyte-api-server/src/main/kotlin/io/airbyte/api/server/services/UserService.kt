/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.api.server.services

import io.airbyte.api.client2.model.generated.WorkspaceReadList
import io.airbyte.api.server.constants.HTTP_RESPONSE_BODY_DEBUG_MESSAGE
import io.airbyte.api.server.errorHandlers.ConfigClientErrorHandler
import io.airbyte.api.server.forwardingClient.ConfigApiClient
import io.micronaut.context.annotation.Secondary
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.exceptions.HttpClientResponseException
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.UUID

interface UserService {
  fun getAllWorkspaceIdsForUser(
    authorization: String?,
    userInfo: String?,
  ): List<UUID>

  fun getUserIdFromUserInfoString(userInfo: String?): UUID
}

@Singleton
@Secondary
open class UserServiceImpl(private val configApiClient: ConfigApiClient) : UserService {
  companion object {
    private val log = LoggerFactory.getLogger(UserServiceImpl::class.java)
  }

  /**
   * Hits the listAllWorkspaces endpoint since OSS has only one user.
   */
  override fun getAllWorkspaceIdsForUser(
    authorization: String?,
    userInfo: String?,
  ): List<UUID> {
    val response =
      try {
        configApiClient.listAllWorkspaces(authorization, null)
      } catch (e: HttpClientResponseException) {
        log.error("Cloud api response error for listWorkspacesByUser: ", e)
        e.response as HttpResponse<WorkspaceReadList>
      }

    ConfigClientErrorHandler.handleError(response, "airbyte-user")

    val workspaces = response.body()?.workspaces.orEmpty()
    log.debug(HTTP_RESPONSE_BODY_DEBUG_MESSAGE + response.body())

    return workspaces.map { it.workspaceId }
  }

  /**
   * No-op for OSS.
   */
  override fun getUserIdFromUserInfoString(userInfo: String?): UUID {
    return UUID.fromString("00000000-0000-0000-0000-000000000000")
  }
}
