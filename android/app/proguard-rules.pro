# Keep kotlinx.serialization generated serializers
-keepclassmembers class com.dietagent.android.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.dietagent.android.data.remote.dto.** { *; }
