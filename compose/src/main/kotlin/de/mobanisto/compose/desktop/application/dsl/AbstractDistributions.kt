/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package de.mobanisto.compose.desktop.application.dsl

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import java.util.*
import javax.inject.Inject

abstract class AbstractDistributions {
    @get:Inject
    internal abstract val objects: ObjectFactory

    @get:Inject
    internal abstract val layout: ProjectLayout

    val outputBaseDir: DirectoryProperty = objects.directoryProperty().apply {
        set(layout.buildDirectory.dir("pinpit/binaries"))
    }

    var jvmVendor: String? = null
    var jvmVersion: String? = null

    var packageName: String? = null
    var packageVersion: String? = null
    var copyright: String? = null

    var description: String? = null
    var vendor: String? = null
    val appResourcesRootDir: DirectoryProperty = objects.directoryProperty()
    val licenseFile: RegularFileProperty = objects.fileProperty()
}
