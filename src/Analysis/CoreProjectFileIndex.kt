package org.jetbrains.dokka

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.OrderEntry
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jps.model.module.JpsModuleSourceRootType

/**
 * Workaround for the lack of ability to create a ProjectFileIndex implementation using only
 * classes from projectModel-{api,impl}.
 */
class CoreProjectFileIndex(): ProjectFileIndex {
    override fun getContentRootForFile(p0: VirtualFile): VirtualFile? {
        throw UnsupportedOperationException()
    }

    override fun getContentRootForFile(p0: VirtualFile, p1: Boolean): VirtualFile? {
        throw UnsupportedOperationException()
    }

    override fun getPackageNameByDirectory(p0: VirtualFile): String? {
        throw UnsupportedOperationException()
    }

    override fun isInLibrarySource(p0: VirtualFile): Boolean {
        throw UnsupportedOperationException()
    }

    override fun getClassRootForFile(p0: VirtualFile): VirtualFile? {
        throw UnsupportedOperationException()
    }

    override fun getOrderEntriesForFile(p0: VirtualFile): List<OrderEntry> = emptyList()

    override fun isInLibraryClasses(p0: VirtualFile): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isExcluded(p0: VirtualFile): Boolean {
        throw UnsupportedOperationException()
    }

    override fun getSourceRootForFile(p0: VirtualFile): VirtualFile? {
        throw UnsupportedOperationException()
    }

    override fun isUnderIgnored(p0: VirtualFile): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isLibraryClassFile(p0: VirtualFile): Boolean {
        throw UnsupportedOperationException()
    }

    override fun getModuleForFile(p0: VirtualFile): Module? = null

    override fun getModuleForFile(p0: VirtualFile, p1: Boolean): Module? {
        throw UnsupportedOperationException()
    }

    override fun isInSource(p0: VirtualFile): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isIgnored(p0: VirtualFile): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isContentSourceFile(p0: VirtualFile): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isInSourceContent(p0: VirtualFile): Boolean = false

    override fun iterateContent(p0: ContentIterator): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isInContent(p0: VirtualFile): Boolean {
        throw UnsupportedOperationException()
    }

    override fun iterateContentUnderDirectory(p0: VirtualFile, p1: ContentIterator): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isInTestSourceContent(p0: VirtualFile): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isUnderSourceRootOfType(p0: VirtualFile, p1: MutableSet<out JpsModuleSourceRootType<*>>): Boolean {
        throw UnsupportedOperationException()
    }
}

