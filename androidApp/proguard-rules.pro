# Project-specific R8 rules, on top of getDefaultProguardFile("proguard-android-optimize.txt").
#
# Deliberately empty of app keep rules: Notion's JSON is navigated as a plain
# JsonElement rather than modelled with @Serializable data classes (see
# docs/architecture.md), so there is nothing reflection-shaped in commonMain
# for R8 to strip. Compose, Ktor/OkHttp, kotlinx.serialization and ML Kit
# GenAI all ship their own consumer-rules.pro inside their AARs, applied
# automatically — duplicating them here would just be a second copy to drift.
#
# If a real ClassNotFoundException/NoSuchMethodError shows up in a release
# build that a debug build doesn't have, that is R8 stripping something used
# only via reflection — add the specific `-keep` here, not a broad wildcard.

# ML Kit's components are discovered, not called. Its AAR manifest declares
# `MlKitComponentDiscoveryService` with one `<meta-data>` entry per registrar,
# and `ComponentDiscovery` turns each of those names into an instance with
# `Class.forName(name).getDeclaredConstructor().newInstance()`. The manifest
# entry keeps the *class* — which is why `CommonComponentRegistrar` is still
# unobfuscated in mapping.txt — but nothing in any code path calls the
# constructor, so R8 stripped it, discovery registered no components, and
# `Summarization.getClient` handed back a null that surfaced on the phone as a
# NullPointerException from a release build alone.
#
# Written against the interface rather than today's one registrar on purpose:
# the invariant is "reflectively instantiated from manifest meta-data", and a
# rule naming `CommonComponentRegistrar` would go quietly out of date the next
# time an ML Kit or Firebase artifact brings its own.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
  <init>();
}

# Stack traces, kept readable. `proguard-android-optimize.txt` keeps neither
# SourceFile nor LineNumberTable, so a release build's traces name no file and
# no line, and mapping.txt has nothing to put back — which is exactly the
# position a summary failure that reached the phone and not a debugger left us
# in. Costs a few KB and gives up no obfuscation: the file name is overwritten
# with a constant, and the line numbers are only meaningful through mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
