@REM Maven Wrapper for Windows
@REM ---------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one or more
@REM contributor license agreements.
@REM ---------------------------------------------------------------------------
@echo off

set MAVEN_WRAPPER_JAR=.mvn\wrapper\maven-wrapper.jar
set MAVEN_WRAPPER_PROPERTIES=.mvn\wrapper\maven-wrapper.properties

@REM Ensure JAVA_HOME is set to JDK 17+
if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME is not set. Please install JDK 17+ and set JAVA_HOME.
    exit /b 1
)

"%JAVA_HOME%\bin\java" ^
  -classpath "%MAVEN_WRAPPER_JAR%" ^
  org.apache.maven.wrapper.MavenWrapperMain ^
  %*
