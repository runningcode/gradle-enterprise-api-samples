package com.gradle.enterprise.api

import com.gradle.enterprise.api.model.BuildsQuery
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.openapitools.client.infrastructure.ApiClient
import java.util.concurrent.TimeUnit

class BuildProcessor(private val url: String, private val authToken: String) {

    fun process(hours: Int) = runBlocking {
        val timeAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours.toLong())
        println(timeAgo)

        val apiClient = ApiClient(baseUrl = url,
            okHttpClientBuilder = OkHttpClient.Builder()
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .connectTimeout(1, TimeUnit.MINUTES),
            authName = "GradleEnterpriseAccessKey",
            bearerToken = authToken)
        val buildApi = apiClient.createService(ModifiedBuildsApi::class.java)

        var query = BuildsQuery(since = timeAgo, maxBuilds = 200)

        val buildAnswer = buildApi.getBuilds(query.since!!)
        val builds = buildAnswer.body()!!.toMutableList()

        // Keep querying while there are still builds to fetch
        while (true) {
            val result = buildApi.getBuilds(query.since, query.sinceBuild, maxBuilds = 200)
            if (!result.isSuccessful || result.body() == null || result.body()!!.isEmpty()) {
                break
            }
            query = BuildsQuery(sinceBuild = result.body()!!.last().id)
            builds.addAll(result.body()!!)
        }

        println("Processing number of builds ${builds.size}")

        // Find projects with longest average build time
        builds.filter {
            it.buildToolType == "gradle"
        }.map { build ->
            buildApi.getGradleAttributes(build.id, null).body()!!
        }.groupBy { it.rootProjectName }
            .map { (projectName, projectBuilds) ->
                projectName to projectBuilds.map { it.buildDuration }.average() }
            .sortedByDescending { pair -> pair.second }
            .forEach { (username, millis) ->
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millis.toLong())
                println("Project ${username} has average build time of ${seconds}s")
            }

        // Find user with longest average build time
        builds.filter { it.buildToolType == "gradle" }.map { build ->
            buildApi.getGradleAttributes(build.id, null).body()!!
        }
            .groupBy { it.environment.username }
            .map { (username, userBuilds) ->
                username to userBuilds.map { it.buildDuration }.average() }
            .sortedByDescending { pair -> pair.second }
            .forEach { (username, millis) ->
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millis.toLong())
                println("Username: ${username}, average build time is ${seconds}s.")
            }

        // Find project with lowest avoidance savings ratio
        builds.filter { it.buildToolType == "gradle" }.map { build ->
            buildApi.getGradleAttributes(build.id, null).body()!! to buildApi.getGradleBuildCachePerformance(build.id, null).body()!!
        }
            .groupBy { it.first.rootProjectName }
            .map { (projectName, projectBuilds) -> projectName to projectBuilds.map { it.second.avoidanceSavingsSummary.ratio }.average() }
            .sortedBy { pair -> pair.second }
            .forEach { (project, avoidanceRatio) ->
                println("Project ${project} has average avoidance savings ratio of ${avoidanceRatio}")
            }

        // Find who has the most build cache errors
        builds.filter { it.buildToolType == "gradle" }.map { build ->
            buildApi.getGradleAttributes(build.id, null).body()!! to buildApi.getGradleBuildCachePerformance(build.id, null).body()!!
        }
            .groupBy { it.first.environment.username }
            .map { (username, buildPairs) ->
                username to buildPairs.count { it.second.buildCaches?.remote?.isDisabledDueToError ?: false }
            }
            .sortedByDescending { pair -> pair.second }
            .forEach { (username, buildErrors) ->
                println("Username: ${username}, total builds disabled due to errors: ${buildErrors}")
            }

        // Find tasks with largest negative avoidance savings
        builds.filter { it.buildToolType == "gradle" }.map { build ->
            buildApi.getGradleBuildCachePerformance(build.id, null).body()!!
        }
            .flatMap { it.taskExecution.filter { (it.avoidanceSavings ?: 0) < 0 } }
            .groupBy { it.taskType }
            .map { (taskType, taskExecutions) -> taskType to taskExecutions.map { it.avoidanceSavings!! }.average() }
            .sortedBy { pair -> pair.second }
            .forEach {  (taskPath, averageAvoidanceSavings) ->
                println("Task $taskPath has average avoidance savings $averageAvoidanceSavings")
            }

        // Find builds with certain tags
        builds.filter { it.buildToolType == "gradle" }.map { build ->
            buildApi.getGradleAttributes(build.id, null).body()!!
        }.filter { it.tags.contains("doctor-negative-avoidance-savings") }
            .forEach { println("Build with id ${it.id} had negative avoidance savings") }
    }
}