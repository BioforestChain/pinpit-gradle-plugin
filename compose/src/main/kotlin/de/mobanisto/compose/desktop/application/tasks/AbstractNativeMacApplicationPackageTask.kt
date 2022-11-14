/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package de.mobanisto.compose.desktop.application.tasks

import de.mobanisto.compose.desktop.application.internal.ioFile
import de.mobanisto.compose.desktop.application.internal.notNullProperty
import de.mobanisto.compose.desktop.tasks.AbstractComposeDesktopTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class AbstractNativeMacApplicationPackageTask : AbstractComposeDesktopTask() {
    @get:Input
    val packageName: Property<String> = objects.notNullProperty()

    @get:Input
    val packageVersion: Property<String> = objects.notNullProperty("1.0.0")

    @get:Internal
    internal val fullPackageName: Provider<String> =
        project.provider { "${packageName.get()}-${packageVersion.get()}" }

    @get:OutputDirectory
    val destinationDir: DirectoryProperty = objects.directoryProperty()

    @get:LocalState
    val workingDir: Provider<Directory> = project.layout.buildDirectory.dir("mocompose/tmp/$name")

    @TaskAction
    fun run() {
        cleanDirs(destinationDir, workingDir)

        createPackage(
            destinationDir = destinationDir.ioFile,
            workingDir = workingDir.ioFile
        )
    }

    protected abstract fun createPackage(
        destinationDir: File,
        workingDir: File
    )
}
