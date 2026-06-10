task printCp {
    dependsOn(":desktopApp:compileKotlin")
    doLast {
        val cp = project(":desktopApp").sourceSets.main.get().runtimeClasspath
            .map { it.absolutePath }
            .joinToString(";")
        println(cp)
    }
}
