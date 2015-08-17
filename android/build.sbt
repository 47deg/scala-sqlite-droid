scalaVersion := "2.11.7"

javacOptions ++= Seq("-target", "1.7", "-source", "1.7")

platformTarget in Android := "android-22"

debugIncludesTests in Android := true

instrumentTestRunner in Android :=
  "android.support.test.runner.AndroidJUnitRunner"

libraryDependencies ++= Seq(
  "com.android.support.test" % "runner" % "0.2",
  "com.android.support.test.espresso" % "espresso-core" % "2.1")

apkbuildExcludes in Android += "LICENSE.txt"

proguardScala in Android := true

proguardOptions in Android ++= Seq(
  "-ignorewarnings",
  "-keep class scala.Dynamic",
  "-keep public class * extends junit.framework.TestCase",
  "-keepclassmembers class * extends junit.framework.TestCase { *; }"
)
