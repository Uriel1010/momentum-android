# WorkManager instantiates workers by class name after process recreation.
-keep class com.momentum.app.** extends androidx.work.ListenableWorker { public <init>(...); }
# Keep Room entities for diagnostic schema stability; generated adapters handle serialization.
-keep class com.momentum.app.Task { *; }
-keep class com.momentum.app.Project { *; }
-keep class com.momentum.app.Event { *; }
-keep class com.momentum.app.DevelopmentIdea { *; }
