package com.gradle.enterprise.api

import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(
    name = "gradle-enterprise-kotlin-api-sample",
    description = ["Gradle Enterprise API"],
)
class CommandLineArgumentProcessor : Callable<Int> {

    @Option(
        names = ["--server-url"],
        description = ["The address of the Gradle Enterprise server"],
        required = true,
        order = 0
    )
    lateinit var serverUrl: String

    @Option(
        names = ["--auth-key"],
        description = ["The Gradle Enterprise access token"],
        required = true,
        order = 1
    )
    lateinit var accessKey: String

    @Option(
        names = ["--hours"],
        description = ["The number of hours of build history to fetch."],
        required = false,
        defaultValue = "1",
        order = 2
    )
    var hours: Int = 1

    override fun call(): Int {
        BuildProcessor(serverUrl, accessKey).process(hours)
        return 0
    }
}