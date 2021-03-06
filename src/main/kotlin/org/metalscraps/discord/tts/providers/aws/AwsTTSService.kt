package org.metalscraps.discord.tts.providers.aws

import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import org.metalscraps.discord.tts.core.AudioFormat
import org.metalscraps.discord.tts.core.Response
import org.metalscraps.discord.tts.core.TTSProvider
import org.metalscraps.discord.tts.core.Voice
import org.metalscraps.discord.tts.providers.createResponseError
import org.metalscraps.discord.tts.providers.getProperty
import org.slf4j.LoggerFactory

class AwsTTSService(
    accessKeyId: String? = getProperty(AwsConst.KEY_PROPERTY_NAME),
    secretAccessKey: String? = getProperty(AwsConst.SECRET_PROPERTY_NAME)
) : TTSProvider {
    companion object {
        internal const val ID: String = "aws"
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    private val client: AwsClient
    private val accessKeyId: String
    private val secretAccessKey: String

    init {
        require(!accessKeyId.isNullOrBlank())
        require(!secretAccessKey.isNullOrBlank())
        this.accessKeyId = accessKeyId
        this.secretAccessKey = secretAccessKey

        this.client = Feign.builder()
            .logger(Slf4jLogger())
            .logLevel(org.metalscraps.discord.tts.providers.Const.LOG_LEVEL)
            .requestInterceptor(AwsRequestSigner(accessKeyId = accessKeyId, secretAccessKey = secretAccessKey))
            .encoder(JacksonEncoder())
            .decoder(JacksonDecoder())
            .target(AwsClient::class.java, AwsConst.HOST)
    }

    override fun getId(): String {
        return ID
    }

    override fun getFriendlyName(): String {
        return "아마존"
    }

    override fun synthesize(voice: String, text: String): Response {
        val response = client.synthesize(AwsRequest(voice, text))
        val status = response.status()

        if (status != 200) {
            return createResponseError(response)
        }

        val responseBody: ByteArray = try {
            response.body().asInputStream().readBytes()
        } catch (e: Exception) {
            return createResponseError(response, e.message.toString(), e.stackTraceToString())
        }

        return Response.data(AudioFormat.OGG, responseBody)
    }

    override fun getVoices(): List<Voice> {
        return AwsVoice.values().asList()
    }
}