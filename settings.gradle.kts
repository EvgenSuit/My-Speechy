
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.gradle.enterprise") {
                useModule("com.gradle:gradle-enterprise-gradle-plugin:${requested.version}")
            }
        }
    }
    plugins {
        kotlin("jvm") version "1.9.23"
    }
}

plugins {
    id("com.gradle.develocity") version "3.17.2"
    //id("com.gradle.enterprise") version "3.12.2"
}
develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}
/*gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}*/

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        flatDir {
            dirs("app")
        }
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io" ) }
    }
}

rootProject.name = "My Speechy"
include(":app")
include(":lib")
include(":lib2")
