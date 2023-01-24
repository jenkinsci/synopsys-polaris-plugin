/**
 * buildSrc
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.polaris.docs

import com.synopsys.integration.detect.docs.content.Terms
import com.synopsys.integration.detect.docs.markdown.MarkdownOutputFormat
import freemarker.template.Configuration
import freemarker.template.Template
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream

open class GenerateDocsTask : DefaultTask() {
    @TaskAction
    fun generateDocs() {
        val outputDir = project.file("docs/generated");

        outputDir.deleteRecursively()
        outputDir.mkdirs()

        val templateProvider = TemplateProvider(project.file("docs/templates"), project.version.toString())
        handleContent(outputDir, templateProvider)
    }

    private fun handleContent(outputDir: File, templateProvider: TemplateProvider) {
        val templatesDir = File(project.projectDir, "docs/templates")
        project.file("docs/templates").walkTopDown().forEach {
            if (it.canonicalPath.endsWith((".ftl"))) {
                createContentMarkdownFromTemplate(templatesDir, it, outputDir, templateProvider)
            }
        }
    }

    private fun createContentMarkdownFromTemplate(templatesDir: File, templateFile: File, baseOutputDir: File, templateProvider: TemplateProvider) {
        val helpContentTemplateRelativePath = templateFile.toRelativeString(templatesDir)
        val outputFile = deriveOutputFileForContentTemplate(templatesDir, templateFile, baseOutputDir)
        println("Generating markdown from template file: ${helpContentTemplateRelativePath} --> ${outputFile.canonicalPath}")
        createFromFreemarker(templateProvider, helpContentTemplateRelativePath, outputFile, HashMap<String, String>())
    }

    private fun deriveOutputFileForContentTemplate(contentDir: File, helpContentTemplateFile: File, baseOutputDir: File): File {
        val templateSubDir = helpContentTemplateFile.parentFile.toRelativeString(contentDir)
        val outputDir = File(baseOutputDir, templateSubDir)
        val outputFile = File(outputDir, "${helpContentTemplateFile.nameWithoutExtension}.md")
        return outputFile
    }

    private fun createFromFreemarker(templateProvider: TemplateProvider, outputDir: File, templateName: String, data: Any) {
        createFromFreemarker(templateProvider, "$templateName.ftl", File(outputDir, "$templateName.md"), data);
    }

    private fun createFromFreemarker(templateProvider: TemplateProvider, templateRelativePath: String, to: File, data: Any) {
        to.parentFile.mkdirs()
        val template = templateProvider.getTemplate(templateRelativePath)
        FileOutputStream(to, true).buffered().writer().use { writer ->
            template.process(data, writer)
        }
    }
}

class TemplateProvider(templateDirectory: File, projectVersion: String) {
    private val configuration: Configuration = Configuration(Configuration.VERSION_2_3_26);

    init {
        configuration.setDirectoryForTemplateLoading(templateDirectory)
        configuration.defaultEncoding = "UTF-8"
        configuration.registeredCustomOutputFormats = listOf(MarkdownOutputFormat.INSTANCE);

        val terms = Terms()
        terms.termMap.put("program_version", projectVersion)
        configuration.setSharedVaribles(terms.termMap)
    }

    fun getTemplate(templateName: String): Template {
        val template =  configuration.getTemplate(templateName)
        return template;
    }
}