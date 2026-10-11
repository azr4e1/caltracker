# R8 rules for release builds.

# Navigation finds enum route arguments (e.g. EditorSection) by class name at runtime.
-keep enum com.lorenzo.caltracker.ui.navigation.** { *; }

# ML Kit (barcode scanner) creates its components by reflection.
-keep class com.google.mlkit.** { *; }
