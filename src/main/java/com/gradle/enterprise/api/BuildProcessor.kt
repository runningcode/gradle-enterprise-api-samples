@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalCoroutinesApi::class)

package com.gradle.enterprise.api

import com.gradle.enterprise.api.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import org.openapitools.client.infrastructure.ApiClient
import java.util.concurrent.TimeUnit

class BuildProcessor(private val url: String, private val authToken: String) {

    fun process(minutes: Long) = runBlocking {
        val timeStart = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(minutes)
        println("Starting to fetch builds from $minutes minutes ago, which is $timeStart")
        println("Fetching builds...")

        val apiClient = ApiClient(
            baseUrl = url,
            // add logging interceptor to the client
            okHttpClientBuilder = OkHttpClient.Builder()
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .connectTimeout(1, TimeUnit.MINUTES),
            authName = "GradleEnterpriseAccessKey",
            bearerToken = authToken
        )
        val buildApi = apiClient.createService(BuildsApi::class.java)

        val limited = Dispatchers.IO.limitedParallelism(2)
        val builds = withContext(limited) {
            val builds = loadBuilds(buildApi, minutes)
            val channel = consumeBuilds(buildApi, builds)

            channel.consumeAsFlow()
                .map { it.first.await() to it.second.await() }
                .toList()
        }


        // Find projects with longest average build time
        println("### PROJECTS WITH LONGEST AVERAGE BUILD TIME ###")
        builds.groupBy { it.first.rootProjectName }
            .map { (projectName, projectBuilds) ->
                projectName to projectBuilds.map { it.first.buildDuration }.average()
            }
            .sortedByDescending { pair -> pair.second }
            .forEach { (projectName, millis) ->
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millis.toLong())
                println("Project ${projectName} has average build time of ${seconds}s")
            }
        println()

        // Find usernames with longest average build time
        println("### USERNAMES WITH LONGEST AVERAGE BUILD TIME ###")
        builds
            .groupBy { it.first.environment.username }
            .map { (username, userBuilds) ->
                username to userBuilds.map { it.first.buildDuration }.average()
            }
            .sortedByDescending { pair -> pair.second }
            .forEach { (username, millis) ->
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millis.toLong())
                println("Username: ${username}, average build time is ${seconds}s.")
            }
        println()

        println("### PROJECTS WITH LOWEST AVOIDANCE SAVINGS RATIO ###")
        // Find project with lowest avoidance savings ratio
        builds
            .groupBy { it.first.rootProjectName }
            .map { (projectName, projectBuilds) ->
                projectName to projectBuilds.map { it.second.avoidanceSavingsSummary.ratio }.average()
            }
            .sortedBy { pair -> pair.second }
            .forEach { (project, avoidanceRatio) ->
                println("Project ${project} has average avoidance savings ratio of ${avoidanceRatio}")
            }
        println()

        // Find who has the most build cache errors
        println("### USERS WITH MOST BUILD CACHE ERRORS ###")
        builds
            .groupBy { it.first.environment.username }
            .map { (username, buildPairs) ->
                username to buildPairs.count { it.second.buildCaches?.remote?.isDisabledDueToError ?: false }
            }
            .sortedByDescending { pair -> pair.second }
            .forEach { (username, buildErrors) ->
                println("Username: ${username}, total builds disabled due to errors: ${buildErrors}")
            }
        println()

        // Find tasks with largest negative avoidance savings
        println("### TASKS WITH LARGEST NEGATIVE AVOIDANCE SAVINGS ###")
        builds
            .flatMap { it.second.taskExecution.filter { (it.avoidanceSavings ?: 0) < 0 } }
            .groupBy { it.taskType }
            .map { (taskType, taskExecutions) ->
                taskType to taskExecutions.map { it.avoidanceSavings!! }.average()
            }
            .sortedBy { pair -> pair.second }
            .forEach { (taskPath, averageAvoidanceSavings) ->
                println("Task $taskPath has average avoidance savings $averageAvoidanceSavings")
            }
        println()

        // Print build ids where tasks had negative avoidance savings
        builds.filter { it.second.taskExecution.any { (it.avoidanceSavings ?: 0) < 0 }}
            .map { (attr, cachePerf) ->
                attr.id to cachePerf.taskExecution.filter { (it.avoidanceSavings ?: 0) < 0 }
            }.fold("") { acc, (id : String, tasks: List<GradleBuildCachePerformanceTaskExecutionEntry>) ->
                println("Build $id has tasks with negative avoidance savings:")
                tasks.forEach { println("\t${it.taskType}") }
                acc
            }

        // Find builds with certain tags
        println("### BUILDS WITH CERTAIN TAGS ###")
        builds.filter { it.first.tags.contains("doctor-negative-avoidance-savings") }
            .forEach { println("Build with id ${it.first.id} had negative avoidance savings") }
        println()
    }
}

fun CoroutineScope.loadBuilds(buildApi: BuildsApi, minutes: Long) = produce<List<Build>> {
    val timeStart = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(minutes)
    var query = BuildsQuery(since = timeStart, maxBuilds = 200)

    // Keep querying while there are still builds to fetch
    while (true) {
        val result = buildApi.getBuilds(query.since, query.sinceBuild, maxBuilds = 200)
        if (!result.isSuccessful || result.body() == null || result.body()!!.isEmpty()) {
            break
        }
        query = BuildsQuery(sinceBuild = result.body()!!.last().id)
        println("got ${result.body()!!.size} builds")
        send(result.body()!!)
    }
    println("closing loading")
    close()
}

@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.consumeBuilds(buildApi: BuildsApi, buildsChannel: ReceiveChannel<List<Build>>) = produce<Pair<Deferred<GradleAttributes>, Deferred<GradleBuildCachePerformance>>> {
    for(buildList in buildsChannel) {
        buildList.filter {it.buildToolType == "gradle" }.forEach { build ->
            println("fetching for ${build.id}")
            send(fetchGradleAttributesAndBuildCachePerformance(buildApi, build.id))
        }
    }
    if (buildsChannel.isClosedForReceive) {
        println("closed for receive")
        close()
    }
    println("the end")
}

suspend fun fetchGradleAttributesAndBuildCachePerformance(buildApi: BuildsApi, buildId: String) = coroutineScope {
    val attributes = async { buildApi.getGradleAttributes(buildId, null).body()!! }
    val buildCachePerformance = async { buildApi.getGradleBuildCachePerformance(buildId, null).body()!! }
    attributes to buildCachePerformance
}