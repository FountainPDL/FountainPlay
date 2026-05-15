#!/bin/bash
# ── PART 1: Gradle + Config ──
# Run from inside ~/FountainPlay

mkdir -p .github/workflows
mkdir -p app/src/main/java/com/fountainpdl/fountainplay/ui/{home,music,video,library,settings}
mkdir -p app/src/main/java/com/fountainpdl/fountainplay/player
mkdir -p app/src/main/java/com/fountainpdl/fountainplay/service
mkdir -p app/src/main/java/com/fountainpdl/fountainplay/model
mkdir -p app/src/main/java/com/fountainpdl/fountainplay/util
mkdir -p app/src/main/res/{layout,menu,navigation,values,values-night,drawable,mipmap-hdpi,xml}
mkdir -p gradle/wrapper
echo "✅ Directories done"

cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

cat > settings.gradle << 'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "FountainPlay"
include ':app'
EOF

cat > build.gradle << 'EOF'
plugins {
    id 'com.android.application' version '8.1.0' apply false
}
EOF

cat > gradle.properties << 'EOF'
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true
EOF

cat > app/build.gradle << 'EOF'
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.fountainpdl.fountainplay'
    compileSdk 34

    defaultConfig {
        applicationId "com.fountainpdl.fountainplay"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    buildFeatures { viewBinding true }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.navigation:navigation-fragment:2.7.6'
    implementation 'androidx.navigation:navigation-ui:2.7.6'
    implementation 'androidx.media3:media3-exoplayer:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-hls:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-dash:1.2.1'
    implementation 'androidx.media3:media3-ui:1.2.1'
    implementation 'androidx.media3:media3-session:1.2.1'
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
    implementation 'androidx.palette:palette:1.0.0'
    implementation 'androidx.viewpager2:viewpager2:1.0.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'
    implementation 'androidx.preference:preference:1.2.1'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
}
EOF

cat > app/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.FountainPlay"
        android:largeHeap="true">

        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:scheme="file" />
                <data android:mimeType="video/*" />
                <data android:mimeType="audio/*" />
            </intent-filter>
        </activity>

        <activity android:name=".player.VideoPlayerActivity"
            android:configChanges="orientation|screenSize|keyboardHidden"
            android:screenOrientation="sensor"
            android:exported="false" />

        <activity android:name=".player.AudioPlayerActivity" android:exported="false" />

        <service android:name=".service.PlaybackService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>

    </application>
</manifest>
EOF

cat > .github/workflows/build.yml << 'EOF'
name: Build Fountain Play APK
on:
  push:
    branches: [ main, dev ]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle
      - run: chmod +x gradlew
      - run: ./gradlew assembleDebug --stacktrace
      - uses: actions/upload-artifact@v4
        with:
          name: FountainPlay-debug-${{ github.sha }}
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 30
EOF

cat > .gitignore << 'EOF'
*.iml
.gradle
/local.properties
/.idea
.DS_Store
/build
/captures
app/build/
*.apk
*.aab
EOF

echo "✅ PART 1 DONE — run fp_part2.sh next"
