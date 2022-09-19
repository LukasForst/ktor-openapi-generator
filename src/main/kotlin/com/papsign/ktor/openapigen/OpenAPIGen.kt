package com.papsign.ktor.openapigen

import com.papsign.ktor.openapigen.model.base.OpenAPIModel
import com.papsign.ktor.openapigen.model.info.ContactModel
import com.papsign.ktor.openapigen.model.info.ExternalDocumentationModel
import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.model.server.ServerModel
import com.papsign.ktor.openapigen.modules.CachingModuleProvider
import com.papsign.ktor.openapigen.modules.OpenAPIModule
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.util.AttributeKey
import org.reflections.Reflections
import kotlin.reflect.full.starProjectedType

class OpenAPIGen(
    config: Configuration,
    @Deprecated("Will be replaced with less dangerous alternative when the use case has been fleshed out.") val pipeline: ApplicationCallPipeline
) {
    private val log = classLogger()

    val api = config.api

    private val tags = HashMap<String, APITag>()

    val globalModuleProvider = CachingModuleProvider()

    init {
        (config.scanPackagesForModules + javaClass.`package`.name).forEach { packageName ->
            val reflections = Reflections(packageName)
            log.debug("Registering modules in package $packageName")
            val objects = reflections.getSubTypesOf(OpenAPIGenExtension::class.java).mapNotNull { it.kotlin.objectInstance }
            objects.forEach {
                log.trace("Registering global module: ${it::class.simpleName}")
                it.onInit(this)
            }
        }
        config.removeModules.forEach(globalModuleProvider::unRegisterModule)
        config.addModules.forEach { globalModuleProvider.registerModule(it, it::class.starProjectedType) }
    }

    class Configuration(val api: OpenAPIModel) {
        inline fun info(crossinline configure: InfoModel.() -> Unit) {
            api.info = InfoModel().apply(configure)
        }

        inline fun InfoModel.contact(crossinline configure: ContactModel.() -> Unit) {
            contact = ContactModel().apply(configure)
        }

        inline fun server(url: String, crossinline configure: ServerModel.() -> Unit = {}) {
            api.servers.add(ServerModel(url).apply(configure))
        }

        inline fun externalDocs(url: String, crossinline configure: ExternalDocumentationModel.() -> Unit = {}) {
            api.externalDocs = ExternalDocumentationModel(url).apply(configure)
        }

        var openApiJsonPath = "/openapi.json"
        var serveOpenApiJson = true

        var swaggerUiPath = "swagger-ui"
        var serveSwaggerUi = true
        var swaggerUiVersion = "4.14.0"

        var scanPackagesForModules: Array<String> = arrayOf()

        var addModules = mutableListOf<OpenAPIModule>()
        var removeModules = mutableListOf<OpenAPIModule>()

        fun addModules(vararg modules: OpenAPIModule) {
            addModules.addAll(modules)
        }

        fun addModules(modules: Iterable<OpenAPIModule>) {
            addModules.addAll(modules)
        }

        fun removeModules(vararg modules: OpenAPIModule) {
            removeModules.addAll(modules)
        }

        fun removeModules(modules: Iterable<OpenAPIModule>) {
            removeModules.addAll(modules)
        }

        fun replaceModule(delete: OpenAPIModule, add: OpenAPIModule) {
            addModules.add(add)
            removeModules.add(delete)
        }
    }


    fun getOrRegisterTag(tag: APITag): String {
        val other = tags.getOrPut(tag.name) {
            api.tags.add(tag.toTag())
            tag
        }
        if (other != tag) error("TagModule named ${tag.name} is already defined")
        return tag.name
    }

    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, OpenAPIGen> {
        override val key = AttributeKey<OpenAPIGen>("OpenAPI Generator")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): OpenAPIGen {
            val api = OpenAPIModel()
            val cfg = Configuration(api).apply(configure)

            if (cfg.serveOpenApiJson) {
                pipeline.intercept(ApplicationCallPipeline.Call) {
                    if (call.request.path() == cfg.openApiJsonPath) {
                        call.respond(api.serialize())
                    }
                }
            }

            if (cfg.serveSwaggerUi) {
                val ui = SwaggerUi(cfg.swaggerUiPath, cfg.swaggerUiVersion, if (cfg.serveOpenApiJson) cfg.openApiJsonPath else null)
                val swaggerRoot = "/${cfg.swaggerUiPath.removePrefix("/")}"
                val swaggerUiResources = "/${cfg.swaggerUiPath.trim('/')}/"
                pipeline.intercept(ApplicationCallPipeline.Call) {
                    when {
                        call.request.path() == swaggerRoot -> call.respondRedirect("${swaggerRoot}/index.html")
                        call.request.path().startsWith(swaggerUiResources) ->
                            ui.serve(call.request.path().removePrefix(swaggerUiResources), call)
                    }
                }
            }
            return OpenAPIGen(cfg, pipeline)
        }
    }
}
