package com.gradle.enterprise.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response

import com.gradle.enterprise.api.model.Build
import com.gradle.enterprise.api.model.BuildQuery
import com.gradle.enterprise.api.model.GradleAttributes
import com.gradle.enterprise.api.model.GradleBuildCachePerformance
import com.gradle.enterprise.api.model.MavenAttributes
import com.gradle.enterprise.api.model.MavenBuildCachePerformance

interface ModifiedBuildsApi {
    /**
     * Get the common attributes of a Build Scan.
     * The contained attributes are build tool agnostic.
     * Responses:
     *  - 200: The common attributes of a Build Scan.
     *  - 400: The request cannot be fulfilled due to a problem.
     *  - 404: The referenced resource either does not exist or the permissions to know about it are missing.
     *  - 500: The server encountered an unexpected error.
     *  - 503: The server is not ready to handle the request.
     *
     * @param id The Build Scan ID.
     * @param buildQuery  (optional)
     * @return [Build]
     */
    @GET("api/builds/{id}")
    suspend fun getBuild(@Path("id") id: kotlin.String, @Query("BuildQuery") buildQuery: BuildQuery? = null): Response<Build>

    /**
     * Get a list of builds with basic attributes of a Build Scan.
     * The contained attributes are build tool agnostic.
     * Responses:
     *  - 200: A list of builds with basic attributes of a Build Scan.
     *  - 400: The request cannot be fulfilled due to a problem.
     *  - 404: The referenced resource either does not exist or the permissions to know about it are missing.
     *  - 500: The server encountered an unexpected error.
     *  - 503: The server is not ready to handle the request.
     *
     * @param buildsQuery  (optional)
     * @return [kotlin.collections.List<Build>]
     */
    @GET("api/builds")
    suspend fun getBuilds(@Query("since") since: Long? = null, @Query("sinceBuild") sinceBuild: String? = null, @Query("maxBuilds") maxBuilds: Int? = null): Response<kotlin.collections.List<Build>>
    /**
     * Get the attributes of a Gradle Build Scan.
     * This model is Gradle specific and cannot be requested for another build tool.
     * Responses:
     *  - 200: The attributes of a Gradle Build Scan.
     *  - 400: The request cannot be fulfilled due to a problem.
     *  - 404: The referenced resource either does not exist or the permissions to know about it are missing.
     *  - 500: The server encountered an unexpected error.
     *  - 503: The server is not ready to handle the request.
     *
     * @param id The Build Scan ID.
     * @param buildQuery  (optional)
     * @return [GradleAttributes]
     */
    @GET("api/builds/{id}/gradle-attributes")
    suspend fun getGradleAttributes(@Path("id") id: kotlin.String, @Query("BuildQuery") buildQuery: BuildQuery? = null): Response<GradleAttributes>

    /**
     * Get the build cache performance of a Gradle Build Scan.
     * This model is Gradle specific and cannot be requested for another build tool.
     * Responses:
     *  - 200: The build cache performance of a Gradle Build Scan.
     *  - 400: The request cannot be fulfilled due to a problem.
     *  - 404: The referenced resource either does not exist or the permissions to know about it are missing.
     *  - 500: The server encountered an unexpected error.
     *  - 503: The server is not ready to handle the request.
     *
     * @param id The Build Scan ID.
     * @param buildQuery  (optional)
     * @return [GradleBuildCachePerformance]
     */
    @GET("api/builds/{id}/gradle-build-cache-performance")
    suspend fun getGradleBuildCachePerformance(@Path("id") id: kotlin.String, @Query("BuildQuery") buildQuery: BuildQuery? = null): Response<GradleBuildCachePerformance>

    /**
     * Get the attributes of a Maven Build Scan.
     * This model is Maven specific and cannot be requested for another build tool.
     * Responses:
     *  - 200: The attributes of a Maven Build Scan.
     *  - 400: The request cannot be fulfilled due to a problem.
     *  - 404: The referenced resource either does not exist or the permissions to know about it are missing.
     *  - 500: The server encountered an unexpected error.
     *  - 503: The server is not ready to handle the request.
     *
     * @param id The Build Scan ID.
     * @param buildQuery  (optional)
     * @return [MavenAttributes]
     */
    @GET("api/builds/{id}/maven-attributes")
    suspend fun getMavenAttributes(@Path("id") id: kotlin.String, @Query("BuildQuery") buildQuery: BuildQuery? = null): Response<MavenAttributes>

    /**
     * Get the build cache performance of a Maven Build Scan.
     * This model is Maven specific and cannot be requested for another build tool.
     * Responses:
     *  - 200: The build cache performance of a Maven Build Scan.
     *  - 400: The request cannot be fulfilled due to a problem.
     *  - 404: The referenced resource either does not exist or the permissions to know about it are missing.
     *  - 500: The server encountered an unexpected error.
     *  - 503: The server is not ready to handle the request.
     *
     * @param id The Build Scan ID.
     * @param buildQuery  (optional)
     * @return [MavenBuildCachePerformance]
     */
    @GET("api/builds/{id}/maven-build-cache-performance")
    suspend fun getMavenBuildCachePerformance(@Path("id") id: kotlin.String, @Query("BuildQuery") buildQuery: BuildQuery? = null): Response<MavenBuildCachePerformance>

}
