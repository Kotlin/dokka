/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.util.*

// speeds up finding files/classes in classpath/java source roots
// NOT THREADSAFE, needs to be adapted/removed if we want compiler to be multithreaded
// the main idea of this class is for each package to store roots which contains it to avoid excessive file system traversal
class JvmDependenciesIndexImpl(_roots: List<JavaRoot>) : JvmDependenciesIndex {
    //these fields are computed based on _roots passed to constructor which are filled in later
    private val roots: List<JavaRoot> by lazy { _roots.toList() }

    private val maxIndex: Int
        get() = roots.size

    override val indexedRoots by lazy { roots.asSequence() }

    override fun traverseDirectoriesInPackage(
        packageFqName: FqName,
        acceptedRootTypes: Set<JavaRoot.RootType>,
        continueSearch: (VirtualFile, JavaRoot.RootType) -> Boolean
    ) {
        search(TraverseRequest(packageFqName, acceptedRootTypes)) { dir, rootType ->
            if (continueSearch(dir, rootType)) null else Unit
        }
    }

    // findClassGivenDirectory MUST check whether the class with this classId exists in given package
    override fun <T : Any> findClass(
        classId: ClassId,
        acceptedRootTypes: Set<JavaRoot.RootType>,
        findClassGivenDirectory: (VirtualFile, JavaRoot.RootType) -> T?
    ): T? {
        return search(FindClassRequest(classId, acceptedRootTypes), findClassGivenDirectory)
    }

    private fun <T : Any> search(request: SearchRequest, handler: (VirtualFile, JavaRoot.RootType) -> T?): T? {
        // a list of package sub names, ["org", "jb", "kotlin"]
        val packagesPath = request.packageFqName.pathSegments().map { it.identifier }

        for (rootIndex in roots.indices) {
            val directoryInRoot = travelPath(rootIndex, packagesPath) ?: continue
            val root = roots[rootIndex]
            if (root.type in request.acceptedRootTypes) {
                val result = handler(directoryInRoot, root.type)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }

    // try to find a target directory corresponding to package
    // represented by packagesPath in a given root represented by index
    private fun travelPath(rootIndex: Int, packagesPath: List<String>): VirtualFile? {
        if (rootIndex >= maxIndex) {
            return null
        }

        val pathRoot = roots[rootIndex]
        val prefixPathSegments = pathRoot.prefixFqName?.pathSegments()

        var currentFile = pathRoot.file

        for (pathIndex in packagesPath.indices) {
            val subPackageName = packagesPath[pathIndex]
            if (prefixPathSegments != null && pathIndex < prefixPathSegments.size) {
                // Traverse prefix first instead of traversing real directories
                if (prefixPathSegments[pathIndex].identifier != subPackageName) {
                    return null
                }
            } else {
                currentFile = currentFile.findChildPackage(subPackageName, pathRoot.type) ?: return null
            }
        }

        return currentFile
    }

    private fun VirtualFile.findChildPackage(subPackageName: String, rootType: JavaRoot.RootType): VirtualFile? {
        val childDirectory = findChild(subPackageName) ?: return null

        val fileExtension = when (rootType) {
            JavaRoot.RootType.BINARY -> JavaClassFileType.INSTANCE.defaultExtension
            JavaRoot.RootType.SOURCE -> JavaFileType.INSTANCE.defaultExtension
        }

        // If in addition to a directory "foo" there's a class file "foo.class" AND there are no classes anywhere in the directory "foo",
        // then we ignore the directory and let the resolution choose the class "foo" instead.
        if (findChild("$subPackageName.$fileExtension")?.isDirectory == false) {
            if (VfsUtilCore.processFilesRecursively(childDirectory) { file -> file.extension != fileExtension }) {
                return null
            }
        }

        return childDirectory
    }

    private data class FindClassRequest(val classId: ClassId, override val acceptedRootTypes: Set<JavaRoot.RootType>) : SearchRequest {
        override val packageFqName: FqName
            get() = classId.packageFqName
    }

    private data class TraverseRequest(
        override val packageFqName: FqName,
        override val acceptedRootTypes: Set<JavaRoot.RootType>
    ) : SearchRequest

    private interface SearchRequest {
        val packageFqName: FqName
        val acceptedRootTypes: Set<JavaRoot.RootType>
    }

    private sealed class SearchResult {
        class Found(val packageDirectory: VirtualFile, val root: JavaRoot) : SearchResult()

        object NotFound : SearchResult()
    }
}
