-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class com.llamatik.** { *; }

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keep class net.zetetic.database.** { *; }
-keepclassmembers class net.zetetic.database.** { *; }

-keepclassmembers enum com.ben.emberr.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

-keepclassmembers class com.ben.emberr.**$$serializer {
    *;
}

-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
