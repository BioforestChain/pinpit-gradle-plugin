/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package de.mobanisto.pinpit.desktop.application.internal.validation

import de.mobanisto.pinpit.desktop.application.dsl.MacOSSigningSettings
import de.mobanisto.pinpit.desktop.application.internal.OS
import de.mobanisto.pinpit.desktop.application.internal.PinpitProperties
import de.mobanisto.pinpit.desktop.application.internal.currentOS
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.File

internal data class ValidatedMacOSSigningSettings(
    val bundleID: String,
    val identity: String,
    val keychain: File?,
    val prefix: String,
    private val appStore: Boolean
) {
    val fullDeveloperID: String
        get() {
            val developerIdPrefix = "Developer ID Application: "
            val thirdPartyMacDeveloperPrefix = "3rd Party Mac Developer Application: "
            return when {
                identity.startsWith(developerIdPrefix) -> identity
                identity.startsWith(thirdPartyMacDeveloperPrefix) -> identity
                else -> (if (!appStore) developerIdPrefix else thirdPartyMacDeveloperPrefix) + identity
            }
        }
}

internal fun MacOSSigningSettings.validate(
    bundleIDProvider: Provider<String?>,
    project: Project,
    appStoreProvider: Provider<Boolean?>
): ValidatedMacOSSigningSettings {
    check(currentOS == OS.MacOS) { ERR_WRONG_OS }

    val bundleID = validateBundleID(bundleIDProvider.orNull)
    val signPrefix = this.prefix.orNull
        ?: (bundleID.substringBeforeLast(".") + ".").takeIf { bundleID.contains('.') }
        ?: error(ERR_UNKNOWN_PREFIX)
    val signIdentity = this.identity.orNull
        ?: error(ERR_UNKNOWN_SIGN_ID)
    val keychainPath = this.keychain.orNull
    val keychainFile = if (keychainPath != null) {
        val keychainFile = listOf(project.file(keychainPath), project.rootProject.file(keychainPath))
            .firstOrNull { it.exists() }
        check(keychainFile != null) {
            "$ERR_PREFIX could not find the specified keychain: $keychainPath"
        }
        keychainFile
    } else null
    val appStore = appStoreProvider.orNull == true

    return ValidatedMacOSSigningSettings(
        bundleID = bundleID,
        identity = signIdentity,
        keychain = keychainFile,
        prefix = signPrefix,
        appStore = appStore
    )
}

private const val ERR_PREFIX = "Signing settings error:"
private val ERR_WRONG_OS =
    "$ERR_PREFIX macOS was expected, actual OS is $currentOS"
private val ERR_UNKNOWN_PREFIX =
    """|$ERR_PREFIX Could not infer signing prefix. To specify:
       |  * Set bundleID to reverse DNS notation (e.g. "com.mycompany.myapp");
       |  * Use '${PinpitProperties.MAC_SIGN_PREFIX}' Gradle property;
       |  * Use 'nativeDistributions.macOS.signing.prefix' DSL property;
    """.trimMargin()
private val ERR_UNKNOWN_SIGN_ID =
    """|$ERR_PREFIX signing identity is null or empty. To specify:
       |  * Use '${PinpitProperties.MAC_SIGN_ID}' Gradle property;
       |  * Use 'nativeDistributions.macOS.signing.identity' DSL property;
    """.trimMargin()
