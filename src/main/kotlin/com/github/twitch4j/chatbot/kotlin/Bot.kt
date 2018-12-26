package com.github.twitch4j.chatbot.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.chatbot.kotlin.features.ChannelNotificationOnDonation
import com.github.twitch4j.chatbot.kotlin.features.ChannelNotificationOnFollow
import com.github.twitch4j.chatbot.kotlin.features.ChannelNotificationOnSubscription
import com.github.twitch4j.chatbot.kotlin.features.WriteChannelChatToConsole

object Bot {

    /** Holds the configuration */
    private val configuration: Configuration =
        loadConfiguration()

    /** Holds the client */
    private val twitchClient: TwitchClient = createClient()

    /** Register all features */
    fun registerFeatures() {
        twitchClient.eventManager.registerListener(WriteChannelChatToConsole)
        twitchClient.eventManager.registerListener(ChannelNotificationOnFollow)
        twitchClient.eventManager.registerListener(ChannelNotificationOnSubscription)
        twitchClient.eventManager.registerListener(ChannelNotificationOnDonation)
    }

    /** Start the bot, connecting it to every channel specified in the configuration */
    fun start() {
        // Connect to all channels
        for (channel in configuration.channels) {
            twitchClient.chat.joinChannel(channel)
        }
    }

    /** Load the configuration from the config.yaml file */
    private fun loadConfiguration(): Configuration {
        val classloader = Thread.currentThread().contextClassLoader
        val inputStream = classloader.getResourceAsStream("config.yaml")

        val mapper = ObjectMapper(YAMLFactory())

        return mapper.readValue(inputStream, Configuration::class.java)

    }

    /** Create the client */
    private fun createClient(): TwitchClient {
        var clientBuilder = TwitchClientBuilder.builder()
        val client: TwitchClient

        //region Chat related configuration
        val credential = OAuth2Credential(
            "twitch",
            configuration.credentials["irc"]
        )

        clientBuilder = clientBuilder
            .withChatAccount(credential)
            .withEnableChat(true)
        //endregion

        //region Api related configuration
        clientBuilder = clientBuilder
            .withClientId(configuration.api["twitch_client_id"])
            .withClientSecret(configuration.api["twitch_client_secret"])
            .withEnableHelix(true)
            /*
                 * GraphQL has a limited support
                 * Don't expect a bunch of features enabling it
                 */
            .withEnableGraphQL(true)
            /*
                 * Kraken is going to be deprecated
                 * see : https://dev.twitch.tv/docs/v5/#which-api-version-can-you-use
                 * It is only here so you can call methods that are not (yet)
                 * implemented in Helix
                 */
            .withEnableKraken(true)
        //endregion

        // Build the client out of the configured builder
        client = clientBuilder.build()

        // Register this class to receive events using the EventSubscriber Annotation
        client.eventManager.registerListener(this)

        return client
    }

}