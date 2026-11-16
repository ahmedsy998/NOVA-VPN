package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NovaApiService {

  @POST("api/v1/auth/login")
  suspend fun login(@Body request: AuthRequestDto): Response<AuthResponseDto>

  @POST("api/v1/auth/register")
  suspend fun register(@Body request: AuthRequestDto): Response<AuthResponseDto>

  @GET("api/v1/locations")
  suspend fun getLocations(): Response<LocationsResponseDto>

  @GET("api/v1/servers")
  suspend fun getServers(
    @Query("country_iso") countryIso: String? = null,
    @Query("status") status: String? = null,
  ): Response<ServersResponseDto>

  @GET("api/v1/servers/{server_id}/health")
  suspend fun getServerHealth(@Path("server_id") serverId: String): Response<ServerHealthDto>

  @POST("api/v1/vpn/sessions")
  suspend fun createVpnSession(@Body request: CreateVpnSessionRequestDto): Response<CreateVpnSessionResponseDto>

  @DELETE("api/v1/vpn/sessions/{session_id}")
  suspend fun revokeVpnSession(@Path("session_id") sessionId: String): Response<Unit>

  @POST("api/v1/diagnostics")
  suspend fun reportDiagnostics(@Body request: DiagnosticsRequestDto): Response<Unit>
}
