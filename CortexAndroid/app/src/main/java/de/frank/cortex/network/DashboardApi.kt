package de.frank.cortex.network

import de.frank.cortex.data.model.OverviewResponse
import retrofit2.http.GET

interface DashboardApi {

    @GET("/api/overview")
    suspend fun overview(): OverviewResponse
}
