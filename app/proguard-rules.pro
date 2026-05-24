-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int d(...);
    public static int w(...);
}

-keep class com.ezworksafe.widget.** { *; }

# Room database _Impl constructors needed for reflection-based instantiation (R8 full mode)
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
