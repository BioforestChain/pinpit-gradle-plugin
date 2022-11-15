package de.mobanisto.compose.validation.deb

import de.mobanisto.compose.desktop.application.internal.files.isProbablyNotBinary
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.compress.archivers.ar.ArArchiveEntry
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.utils.CountingInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

data class DebContent(val arEntries: Map<String, ArEntry>, val tars: Map<String, Tar>)
data class Tar(val name: String, val entries: List<TarEntry>)
data class ArEntry(val name: String, val size: Long, val user: Long, val group: Long, val hash: String)
data class TarEntry(val name: String, val size: Long, val user: Long, val group: Long, val hash: String)
data class DebAddress(val tar: String, val path: String)

class DebContentBuilder {

    fun buildContent(fis: InputStream): DebContent {
        val arEntries = mutableMapOf<String, ArEntry>()
        val tars = mutableMapOf<String, Tar>()

        val ais = ArArchiveInputStream(fis)
        while (true) {
            val entry1: ArArchiveEntry = ais.nextArEntry ?: break
            val name1 = entry1.name

            if (name1.endsWith("tar.xz")) {
                val tarEntries = mutableListOf<TarEntry>()
                tars[name1] = Tar(name1, tarEntries)
                val xz = XZCompressorInputStream(ais)
                val tis = TarArchiveInputStream(xz)
                while (true) {
                    val entry2 = tis.nextTarEntry ?: break
                    if (entry2.isDirectory) continue
                    val name2 = entry2.name
                    val counter = CountingInputStream(tis)
                    val hash = DigestUtils.sha1Hex(counter)
                    tarEntries.add(TarEntry(name2, counter.bytesRead, entry2.longUserId, entry2.longGroupId, hash))
                }
            } else {
                val counter = CountingInputStream(ais)
                val hash = DigestUtils.sha1Hex(counter)
                arEntries[name1] =
                    ArEntry(name1, counter.bytesRead, entry1.userId.toLong(), entry1.groupId.toLong(), hash)
            }
        }

        return DebContent(arEntries, tars)
    }

    private fun accept(name: String, tar: TarComparisonResult): Boolean {
        for (entry in tar.onlyIn1) {
            if (entry.name == name) {
                return true
            }
        }
        for (entry in tar.onlyIn2) {
            if (entry.name == name) {
                return true
            }
        }
        for (entry in tar.different) {
            if (entry.name == name) {
                return true
            }
        }
        return false
    }

    fun buildContentForComparison(
        fis: InputStream,
        comparison: Map<String, TarComparisonResult>
    ): Map<DebAddress, ByteArray> {
        val ais = ArArchiveInputStream(fis)

        val map = mutableMapOf<DebAddress, ByteArray>()
        while (true) {
            val entry1: ArArchiveEntry = ais.nextArEntry ?: break
            val name1 = entry1.name

            if (name1.endsWith("tar.xz")) {
                val tar = comparison[name1] ?: continue
                val xz = XZCompressorInputStream(ais)
                val tis = TarArchiveInputStream(xz)
                while (true) {
                    val entry2 = tis.nextTarEntry ?: break
                    if (entry2.isDirectory) continue
                    val name2 = entry2.name
                    if (accept(name2, tar)) {
                        val bytes = tis.readBytes()
                        if (bytes.isProbablyNotBinary()) {
                            map[DebAddress(name1, name2)] = bytes
                        } else {
                            println("not collecting data for binary file $name2")
                        }
                    }
                }
            }
        }

        return map
    }

    fun getControl(packageFile: File): ByteArray? {
        return getFile(packageFile, "control.tar.xz", "./control")
    }

    private fun getFile(packageFile: File, tarFile: String, path: String): ByteArray? {
        val fis = FileInputStream(packageFile)
        val ais = ArArchiveInputStream(fis)

        while (true) {
            val entry1: ArArchiveEntry = ais.nextArEntry ?: break
            val name1 = entry1.name

            if (name1 == tarFile) {
                val xz = XZCompressorInputStream(ais)
                val tis = TarArchiveInputStream(xz)
                while (true) {
                    val entry2 = tis.nextTarEntry ?: break
                    if (entry2.isDirectory) continue
                    val name2 = entry2.name
                    if (name2 == path) {
                        return tis.readBytes()
                    }
                }
            }
        }
        return null
    }
}
