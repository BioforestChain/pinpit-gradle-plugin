/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("unused")

package de.mobanisto.compose

import de.mobanisto.compose.desktop.DesktopExtension
import de.mobanisto.compose.desktop.application.internal.ComposeProperties
import de.mobanisto.compose.desktop.application.internal.configureDesktop
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.ComponentModuleMetadataHandler
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class ComposePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val composeExtension = project.extensions.create("mocompose", ComposeExtension::class.java)
        val desktopExtension = composeExtension.extensions.create("desktop", DesktopExtension::class.java)

        project.plugins.apply(de.mobanisto.compose.ComposeCompilerKotlinSupportPlugin::class.java)

        project.afterEvaluate {
            configureDesktop(project, desktopExtension)

            fun ComponentModuleMetadataHandler.replaceAndroidx(original: String, replacement: String) {
                module(original) {
                    it.replacedBy(replacement, "org.jetbrains.compose isn't compatible with androidx.compose, because it is the same library published with different maven coordinates")
                }
            }

            val overrideDefaultJvmTarget = ComposeProperties.overrideKotlinJvmTarget(project.providers).get()
            project.tasks.withType(KotlinCompile::class.java) {
                it.kotlinOptions.apply {
                    if (overrideDefaultJvmTarget) {
                        if (jvmTarget.isNullOrBlank() || jvmTarget.toDouble() < 1.8) {
                             jvmTarget = "1.8"
                         }
                    }
                }
            }
        }
    }
}
