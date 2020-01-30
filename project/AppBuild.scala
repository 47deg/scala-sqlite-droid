/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2012 47 Degrees, LLC http://47deg.com hello@47deg.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

import android.AndroidLib
import microsites.MicrositesPlugin
import sbt.Keys._
import sbt._
import sbtbuildinfo.BuildInfoPlugin

object AppBuild extends Build with Settings/* with SettingsPublish*/ {
  lazy val mvessel = (project in file("."))
//    .enablePlugins(AndroidLib)
    .settings(basicSettings: _*)
    .aggregate(androidDriver, core)

  lazy val androidDriver = (project in file("android-driver"))
    .enablePlugins(BuildInfoPlugin)
    .enablePlugins(AndroidLib)
    .configs(IntegrationTest)
    .settings(androidDriverSettings: _*)
    .settings(Defaults.itSettings : _*)
    .settings(libraryDependencies ++= androidDriverLibraries)
    .settings(androidSettings)
    .dependsOn(core/*, mockAndroid % "test->test;it->test"*/)

  lazy val core = (project in file("core"))
    .settings(coreSettings: _*)
    .settings(libraryDependencies ++= coreLibraries)

//  lazy val mockAndroid = (project in file("mock-android"))
//    .settings(mockAndroidSettings: _*)
//    .settings(libraryDependencies ++= mockAndroidLibraries)

  lazy val docs = (project in file("docs"))
    .enablePlugins(MicrositesPlugin)
    .settings(docsSettings: _*)
    .settings(moduleName := "docs")
}