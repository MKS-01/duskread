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
