# Include java runtime classes
-libraryjars  <java.home>/lib/rt.jar

# Keep filenames and line numbers
-keepattributes SourceFile, LineNumberTable

-target 1.6
-dontoptimize
-dontobfuscate

-ignorewarnings
# -keepdirectories

-dontwarn org.jetbrains.annotations.**
-dontwarn org.apache.commons.httpclient.**
-dontwarn org.apache.tools.ant.**
-dontwarn org.codehaus.plexus.**
-dontwarn hidden.org.codehaus.plexus.**
-dontwarn org.fusesource.**
-dontwarn org.jaxen.jdom.**

-keep class org.jetbrains.dokka.** { *; }
-keep class org.fusesource.** { *; }
-keep class org.jdom.input.JAXPParserFactory { *; }

-keep class org.jetbrains.annotations.** {
    public protected *;
}

-keep class javax.inject.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.compiler.plugin.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.extensions.** {
    public protected *;
}

-keep class org.jetbrains.org.objectweb.asm.Opcodes { *; }

-keep class org.jetbrains.kotlin.codegen.extensions.** {
    public protected *;
}

-keepclassmembers class com.intellij.openapi.vfs.VirtualFile {
    public InputStream getInputStream();
}

-keep class jet.** {
    public protected *;
}

-keep class com.intellij.psi.** {
    public protected *;
}

# for kdoc & dokka
-keep class com.intellij.openapi.util.TextRange { *; }
-keep class com.intellij.lang.impl.PsiBuilderImpl* {
    public protected *;
}
-keep class com.intellij.openapi.util.text.StringHash { *; }

# for gradle plugin and other server tools
-keep class com.intellij.openapi.util.io.ZipFileCache { public *; }

# for j2k
-keep class com.intellij.codeInsight.NullableNotNullManager { public protected *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * {
    ** toString();
    ** hashCode();
    void start();
    void stop();
    void dispose();
}

-keepclassmembers class org.jetbrains.org.objectweb.asm.Opcodes {
    *** ASM5;
}

-keepclassmembers class org.jetbrains.org.objectweb.asm.ClassReader {
    *** SKIP_CODE;
    *** SKIP_DEBUG;
    *** SKIP_FRAMES;
}


