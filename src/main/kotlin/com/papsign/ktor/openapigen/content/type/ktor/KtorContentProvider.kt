package com.papsign.ktor.openapigen.content.type.ktor

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.OpenAPIGenModuleExtension
import com.papsign.ktor.openapigen.annotations.encodings.APIRequestFormat
import com.papsign.ktor.openapigen.annotations.encodings.APIResponseFormat
import com.papsign.ktor.openapigen.content.type.BodyParser
import com.papsign.ktor.openapigen.content.type.ContentTypeProvider
import com.papsign.ktor.openapigen.content.type.ResponseSerializer
import com.papsign.ktor.openapigen.model.operation.MediaTypeModel
import com.papsign.ktor.openapigen.model.schema.SchemaModel
import com.papsign.ktor.openapigen.modules.ModuleProvider
import com.papsign.ktor.openapigen.modules.ofType
import com.papsign.ktor.openapigen.schema.builder.provider.FinalSchemaBuilderProviderModule
import com.papsign.ktor.openapigen.unitKType
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.PluginBuilder
import io.ktor.server.application.PluginInstance
import io.ktor.server.application.call
import io.ktor.server.application.pluginOrNull
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiationConfig
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.platformType
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

/**
 * default content provider using the ktor pipeline to handle the serialization and deserialization
 */
object KtorContentProvider : ContentTypeProvider, BodyParser, ResponseSerializer, OpenAPIGenModuleExtension {

    //    private var contentNegotiation: ContentNegotiation? = null
    private var contentTypes: Set<ContentType>? = null

    private fun initContentTypes(apiGen: OpenAPIGen): Set<ContentType>? {
        val reflectedContentTypes = apiGen.pipeline.pluginOrNull(ContentNegotiation)
            ?.getPrivateProperty<PluginInstance, PluginBuilder<ContentNegotiationConfig>>("builder")
            ?.pluginConfig
            ?.getPrivateProperty<ContentNegotiationConfig, ArrayList<*>>("registrations")
            ?.mapNotNull { it }
            ?.mapNotNull { record ->
                Class.forName("io.ktor.server.plugins.contentnegotiation.ConverterRegistration")
                    .kotlin
                    .memberProperties
                    .firstOrNull { it.name == "contentType" }
                    ?.apply { isAccessible = true }
                    ?.let {
                        @Suppress("UNCHECKED_CAST")
                        it as KProperty1<Any, ContentType>
                    }
                    ?.get(record)
            }
            ?.toSet()
            ?: emptySet()

        contentTypes = reflectedContentTypes
        return contentTypes
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any, R> T.getPrivateProperty(name: String): R? =
        T::class.memberProperties
            .firstOrNull { it.name == name }
            ?.apply { isAccessible = true }
            ?.get(this) as? R

    override fun <T> getMediaType(
        type: KType,
        apiGen: OpenAPIGen,
        provider: ModuleProvider<*>,
        example: T?,
        usage: ContentTypeProvider.Usage
    ): Map<ContentType, MediaTypeModel<T>>? {
        if (type == unitKType) return null
        val clazz = type.jvmErasure
        when (usage) { // check if it is explicitly declared or none is present
            ContentTypeProvider.Usage.PARSE -> when {
                clazz.findAnnotation<KtorRequest>() != null -> {
                }
                clazz.annotations.none { it.annotationClass.findAnnotation<APIRequestFormat>() != null } -> {
                }
                else -> return null
            }
            ContentTypeProvider.Usage.SERIALIZE -> when {
                clazz.findAnnotation<KtorResponse>() != null -> {
                }
                clazz.annotations.none { it.annotationClass.findAnnotation<APIResponseFormat>() != null } -> {
                }
                else -> return null
            }
        }
        val contentTypes = initContentTypes(apiGen) ?: return null
        val schemaBuilder = provider.ofType<FinalSchemaBuilderProviderModule>().last().provide(apiGen, provider)

        @Suppress("UNCHECKED_CAST")
        val media = MediaTypeModel(schemaBuilder.build(type) as SchemaModel<T>, example)
        return contentTypes.associateWith { media.copy() }
    }

    override fun <T : Any> getParseableContentTypes(type: KType): List<ContentType> {
        return contentTypes!!.toList()
    }

    override suspend fun <T : Any> parseBody(clazz: KType, request: PipelineContext<Unit, ApplicationCall>): T {
        val info = TypeInfo(
            type = clazz.jvmErasure,
            reifiedType = clazz.platformType,
            kotlinType = clazz
        )
        return request.call.receive(info)
    }

    override fun <T : Any> getSerializableContentTypes(type: KType): List<ContentType> {
        return contentTypes!!.toList()
    }

    override suspend fun <T : Any> respond(
        response: T,
        request: PipelineContext<Unit, ApplicationCall>,
        contentType: ContentType
    ) {
        request.call.respond(response as Any)
    }

    override suspend fun <T : Any> respond(
        statusCode: HttpStatusCode,
        response: T,
        request: PipelineContext<Unit, ApplicationCall>,
        contentType: ContentType
    ) {
        request.call.respond(statusCode, response as Any)
    }
}
