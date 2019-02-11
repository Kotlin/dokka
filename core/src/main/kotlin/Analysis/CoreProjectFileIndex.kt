package org.jetbrains.dokka

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.impl.ProjectOrderEnumerator
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.messages.MessageBus
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRoot
import org.picocontainer.PicoContainer
import java.io.File

/**
 * Workaround for the lack of ability to create a ProjectFileIndex implementation using only
 * classes from projectModel-{api,impl}.
 */
class CoreProjectFileIndex(private val project: Project, contentRoots: List<ContentRoot>) : ProjectFileIndex, ModuleFileIndex {
    override fun iterateContent(p0: ContentIterator, p1: VirtualFileFilter?): Boolean {
        throw UnsupportedOperationException()
    }

    override fun iterateContentUnderDirectory(p0: VirtualFile, p1: ContentIterator, p2: VirtualFileFilter?): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isInLibrary(p0: VirtualFile): Boolean {
        throw UnsupportedOperationException()
    }

    val sourceRoots = contentRoots.filter { it !is JvmClasspathRoot }
    val classpathRoots = contentRoots.filterIsInstance<JvmClasspathRoot>()

    val module: Module = object : UserDataHolderBase(), Module {
        override fun isDisposed(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun getOptionValue(p0: String): String? {
            throw UnsupportedOperationException()
        }

        override fun clearOption(p0: String) {
            throw UnsupportedOperationException()
        }

        override fun getName(): String = "<Dokka module>"

        override fun getModuleWithLibrariesScope(): GlobalSearchScope {
            throw UnsupportedOperationException()
        }

        override fun getModuleWithDependentsScope(): GlobalSearchScope {
            throw UnsupportedOperationException()
        }

        override fun getModuleContentScope(): GlobalSearchScope {
            throw UnsupportedOperationException()
        }

        override fun isLoaded(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun setOption(p0: String, p1: String?) {
            throw UnsupportedOperationException()
        }

        override fun getModuleWithDependenciesScope(): GlobalSearchScope {
            throw UnsupportedOperationException()
        }

        override fun getModuleWithDependenciesAndLibrariesScope(p0: Boolean): GlobalSearchScope {
            throw UnsupportedOperationException()
        }

        override fun getProject(): Project = this@CoreProjectFileIndex.project

        override fun getModuleContentWithDependenciesScope(): GlobalSearchScope {
            throw UnsupportedOperationException()
        }

        override fun getModuleFilePath(): String {
            throw UnsupportedOperationException()
        }

        override fun getModuleTestsWithDependentsScope(): GlobalSearchScope {
            throw UnsupportedOperationException()
        }

        override fun getModuleScope(): GlobalSearchScope {
            throw UnsupportedOperationException()
        }

        override fun getModuleScope(p0: Boolean): GlobalSearchScope {
            throw UnsupportedOperationException()
        }

        override fun getModuleRuntimeScope(p0: Boolean): GlobalSearchScope {
            throw UnsupportedOperationException()
        }

        override fun getModuleFile(): VirtualFile? {
            throw UnsupportedOperationException()
        }

        override fun <T : Any?> getExtensions(p0: ExtensionPointName<T>): Array<out T> {
            throw UnsupportedOperationException()
        }

        override fun getComponent(p0: String): BaseComponent? {
            throw UnsupportedOperationException()
        }

        override fun <T : Any?> getComponent(p0: Class<T>, p1: T): T {
            throw UnsupportedOperationException()
        }

        override fun <T : Any?> getComponent(interfaceClass: Class<T>): T? {
            if (interfaceClass == ModuleRootManager::class.java) {
                return moduleRootManager as T
            }
            throw UnsupportedOperationException()
        }

        override fun getDisposed(): Condition<*> {
            throw UnsupportedOperationException()
        }

        override fun <T : Any?> getComponents(p0: Class<T>): Array<out T> {
            throw UnsupportedOperationException()
        }

        override fun getPicoContainer(): PicoContainer {
            throw UnsupportedOperationException()
        }

        override fun hasComponent(p0: Class<*>): Boolean {
            throw UnsupportedOperationException()
        }

        override fun getMessageBus(): MessageBus {
            throw UnsupportedOperationException()
        }

        override fun dispose() {
            throw UnsupportedOperationException()
        }
    }

    private val sdk: Sdk = object : Sdk, RootProvider {
        override fun getFiles(rootType: OrderRootType): Array<out VirtualFile> = classpathRoots
                .map { StandardFileSystems.local().findFileByPath(it.file.path) }
                .filterNotNull()
                .toTypedArray()

        override fun addRootSetChangedListener(p0: RootProvider.RootSetChangedListener) {
            throw UnsupportedOperationException()
        }

        override fun addRootSetChangedListener(p0: RootProvider.RootSetChangedListener, p1: Disposable) {
            throw UnsupportedOperationException()
        }

        override fun getUrls(p0: OrderRootType): Array<out String> {
            throw UnsupportedOperationException()
        }

        override fun removeRootSetChangedListener(p0: RootProvider.RootSetChangedListener) {
            throw UnsupportedOperationException()
        }

        override fun getSdkModificator(): SdkModificator {
            throw UnsupportedOperationException()
        }

        override fun getName(): String = "<dokka SDK>"

        override fun getRootProvider(): RootProvider = this

        override fun getHomePath(): String? {
            throw UnsupportedOperationException()
        }

        override fun getVersionString(): String? {
            throw UnsupportedOperationException()
        }

        override fun getSdkAdditionalData(): SdkAdditionalData? {
            throw UnsupportedOperationException()
        }

        override fun clone(): Any {
            throw UnsupportedOperationException()
        }

        override fun getSdkType(): SdkTypeId {
            throw UnsupportedOperationException()
        }

        override fun getHomeDirectory(): VirtualFile? {
            throw UnsupportedOperationException()
        }

        override fun <T : Any?> getUserData(p0: Key<T>): T? {
            throw UnsupportedOperationException()
        }

        override fun <T : Any?> putUserData(p0: Key<T>, p1: T?) {
            throw UnsupportedOperationException()
        }
    }

    private val moduleSourceOrderEntry = object : ModuleSourceOrderEntry {
        override fun getFiles(p0: OrderRootType): Array<VirtualFile> {
            throw UnsupportedOperationException()
        }

        override fun getUrls(p0: OrderRootType): Array<String> {
            throw UnsupportedOperationException()
        }

        override fun <R : Any?> accept(p0: RootPolicy<R>, p1: R?): R {
            throw UnsupportedOperationException()
        }


        override fun getPresentableName(): String {
            throw UnsupportedOperationException()
        }

        override fun getOwnerModule(): Module = module


        override fun isValid(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun compareTo(other: OrderEntry?): Int {
            throw UnsupportedOperationException()
        }

        override fun getRootModel(): ModuleRootModel = moduleRootManager

        override fun isSynthetic(): Boolean {
            throw UnsupportedOperationException()
        }
    }

    private val sdkOrderEntry = object : JdkOrderEntry {
        override fun getFiles(p0: OrderRootType): Array<VirtualFile> {
            throw UnsupportedOperationException()
        }

        override fun getUrls(p0: OrderRootType): Array<String> {
            throw UnsupportedOperationException()
        }

        override fun <R : Any?> accept(p0: RootPolicy<R>, p1: R?): R {
            throw UnsupportedOperationException()
        }

        override fun getJdkName(): String? {
            throw UnsupportedOperationException()
        }

        override fun getJdk(): Sdk = sdk

        override fun getPresentableName(): String {
            throw UnsupportedOperationException()
        }

        override fun getOwnerModule(): Module {
            throw UnsupportedOperationException()
        }

        override fun isValid(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun getRootFiles(p0: OrderRootType): Array<out VirtualFile> {
            throw UnsupportedOperationException()
        }

        override fun getRootUrls(p0: OrderRootType): Array<out String> {
            throw UnsupportedOperationException()
        }

        override fun compareTo(other: OrderEntry?): Int {
            throw UnsupportedOperationException()
        }

        override fun isSynthetic(): Boolean {
            throw UnsupportedOperationException()
        }

    }

    inner class MyModuleRootManager : ModuleRootManager() {
        override fun getExternalSource(): ProjectModelExternalSource? {
            throw UnsupportedOperationException()
        }

        override fun getExcludeRoots(): Array<out VirtualFile> {
            throw UnsupportedOperationException()
        }

        override fun getContentEntries(): Array<out ContentEntry> {
            throw UnsupportedOperationException()
        }

        override fun getExcludeRootUrls(): Array<out String> {
            throw UnsupportedOperationException()
        }

        override fun <R : Any?> processOrder(p0: RootPolicy<R>?, p1: R): R {
            throw UnsupportedOperationException()
        }

        override fun getSourceRoots(p0: Boolean): Array<out VirtualFile> {
            throw UnsupportedOperationException()
        }

        override fun getSourceRoots(): Array<out VirtualFile> {
            throw UnsupportedOperationException()
        }

        override fun getSourceRoots(p0: JpsModuleSourceRootType<*>): MutableList<VirtualFile> {
            throw UnsupportedOperationException()
        }

        override fun getSourceRoots(p0: MutableSet<out JpsModuleSourceRootType<*>>): MutableList<VirtualFile> {
            throw UnsupportedOperationException()
        }

        override fun getContentRoots(): Array<out VirtualFile> {
            throw UnsupportedOperationException()
        }

        override fun orderEntries(): OrderEnumerator =
                ProjectOrderEnumerator(project, null).using(object : RootModelProvider {
                    override fun getModules(): Array<out Module> = arrayOf(module)

                    override fun getRootModel(p0: Module): ModuleRootModel = this@MyModuleRootManager
                })

        override fun <T : Any?> getModuleExtension(p0: Class<T>): T {
            throw UnsupportedOperationException()
        }

        override fun getDependencyModuleNames(): Array<out String> {
            throw UnsupportedOperationException()
        }

        override fun getModule(): Module = this@CoreProjectFileIndex.module

        override fun isSdkInherited(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun getOrderEntries(): Array<out OrderEntry> = arrayOf(moduleSourceOrderEntry, sdkOrderEntry)

        override fun getSourceRootUrls(): Array<out String> {
            throw UnsupportedOperationException()
        }

        override fun getSourceRootUrls(p0: Boolean): Array<out String> {
            throw UnsupportedOperationException()
        }

        override fun getSdk(): Sdk? {
            throw UnsupportedOperationException()
        }

        override fun getContentRootUrls(): Array<out String> {
            throw UnsupportedOperationException()
        }

        override fun getModuleDependencies(): Array<out Module> {
            throw UnsupportedOperationException()
        }

        override fun getModuleDependencies(p0: Boolean): Array<out Module> {
            throw UnsupportedOperationException()
        }

        override fun getModifiableModel(): ModifiableRootModel {
            throw UnsupportedOperationException()
        }

        override fun isDependsOn(p0: Module?): Boolean {
            throw UnsupportedOperationException()
        }

        override fun getFileIndex(): ModuleFileIndex {
            return this@CoreProjectFileIndex
        }

        override fun getDependencies(): Array<out Module> {
            throw UnsupportedOperationException()
        }

        override fun getDependencies(p0: Boolean): Array<out Module> {
            throw UnsupportedOperationException()
        }
    }

    val moduleRootManager = MyModuleRootManager()

    override fun getContentRootForFile(p0: VirtualFile): VirtualFile? {
        throw UnsupportedOperationException()
    }

    override fun getContentRootForFile(p0: VirtualFile, p1: Boolean): VirtualFile? {
        throw UnsupportedOperationException()
    }

    override fun getPackageNameByDirectory(p0: VirtualFile): String? {
        throw UnsupportedOperationException()
    }

    override fun isInLibrarySource(file: VirtualFile): Boolean = false

    override fun getClassRootForFile(file: VirtualFile): VirtualFile? =
        classpathRoots.firstOrNull { it.contains(file) }?.let { StandardFileSystems.local().findFileByPath(it.file.path) }

    override fun getOrderEntriesForFile(file: VirtualFile): List<OrderEntry> =
        if (classpathRoots.contains(file)) listOf(sdkOrderEntry) else emptyList()

    override fun isInLibraryClasses(file: VirtualFile): Boolean = classpathRoots.contains(file)

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

    override fun getModuleForFile(file: VirtualFile): Module? =
            if (sourceRoots.contains(file)) module else null

    private fun List<ContentRoot>.contains(file: VirtualFile): Boolean = any { it.contains(file) }

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

    override fun isInSourceContent(file: VirtualFile): Boolean = sourceRoots.contains(file)

    override fun iterateContent(p0: ContentIterator): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isInContent(p0: VirtualFile): Boolean {
        throw UnsupportedOperationException()
    }

    override fun iterateContentUnderDirectory(p0: VirtualFile, p1: ContentIterator): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isInTestSourceContent(file: VirtualFile): Boolean = false

    override fun isUnderSourceRootOfType(p0: VirtualFile, p1: MutableSet<out JpsModuleSourceRootType<*>>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun getOrderEntryForFile(p0: VirtualFile): OrderEntry? {
        throw UnsupportedOperationException()
    }
}

class CoreProjectRootManager(val projectFileIndex: CoreProjectFileIndex) : ProjectRootManager() {
    override fun orderEntries(): OrderEnumerator {
        throw UnsupportedOperationException()
    }

    override fun orderEntries(p0: MutableCollection<out Module>): OrderEnumerator {
        throw UnsupportedOperationException()
    }

    override fun getContentRootsFromAllModules(): Array<out VirtualFile>? {
        throw UnsupportedOperationException()
    }

    override fun setProjectSdk(p0: Sdk?) {
        throw UnsupportedOperationException()
    }

    override fun setProjectSdkName(p0: String?) {
        throw UnsupportedOperationException()
    }

    override fun getModuleSourceRoots(p0: MutableSet<out JpsModuleSourceRootType<*>>): MutableList<VirtualFile> {
        throw UnsupportedOperationException()
    }

    override fun getContentSourceRoots(): Array<out VirtualFile> {
        throw UnsupportedOperationException()
    }

    override fun getFileIndex(): ProjectFileIndex = projectFileIndex

    override fun getProjectSdkName(): String? {
        throw UnsupportedOperationException()
    }

    override fun getProjectSdk(): Sdk? {
        throw UnsupportedOperationException()
    }

    override fun getContentRoots(): Array<out VirtualFile> {
        throw UnsupportedOperationException()
    }

    override fun getContentRootUrls(): MutableList<String> {
        throw UnsupportedOperationException()
    }

}

fun ContentRoot.contains(file: VirtualFile) = when (this) {
    is JvmContentRoot -> {
        val path = if (file.fileSystem.protocol == StandardFileSystems.JAR_PROTOCOL)
            StandardFileSystems.getVirtualFileForJar(file)?.path ?: file.path
        else
            file.path
        File(path).startsWith(this.file.absoluteFile)
    }
    is KotlinSourceRoot -> File(file.path).startsWith(File(this.path).absoluteFile)
    else -> false
}
