package io.github.vibhor1102.macrion.feature.backup.data.base

import java.io.File
import java.io.IOException

/** Promote validated staged files without leaving a partial result if promotion fails. */
internal fun commitStagedFiles(
    stagingDir: File,
    destinationDir: File,
    referencedPaths: Collection<String>,
) {
    val createdFiles = mutableListOf<File>()

    try {
        referencedPaths.forEach { path ->
            val stagedFile = resolveContainedFile(stagingDir, path)
            val destinationFile = resolveContainedFile(destinationDir, path)
            if (!stagedFile.isFile) {
                throw IOException("A validated imported asset is missing: $path")
            }
            if (destinationFile.exists()) {
                if (!stagedFile.readBytes().contentEquals(destinationFile.readBytes())) {
                    throw IOException("An imported asset conflicts with an existing local file: $path")
                }
            } else {
                stagedFile.copyTo(destinationFile)
                createdFiles.add(destinationFile)
            }
        }
    } catch (error: Exception) {
        createdFiles.forEach(File::delete)
        throw error
    }
}

internal fun resolveContainedFile(directory: File, relativePath: String): File {
    val canonicalDirectory = directory.canonicalFile
    val candidatePath = File(canonicalDirectory, relativePath).canonicalPath
    val directoryPrefix = canonicalDirectory.path.trimEnd(File.separatorChar) + File.separator
    if (candidatePath.startsWith(directoryPrefix)) {
        val candidate = File(candidatePath)
        if (candidate.parentFile == canonicalDirectory) return candidate
    }
    throw IOException("An imported asset path is not a safe local filename: $relativePath")
}
