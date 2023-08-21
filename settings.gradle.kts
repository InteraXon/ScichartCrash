pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        /*maven { url 'https://www.jitpack.io' }*/
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

    }
}

rootProject.name = "MySciChart"
include(":app")
//include(":app", ":proximanova")


//include ':app', ':proximanova', 'muse-cpp'
 