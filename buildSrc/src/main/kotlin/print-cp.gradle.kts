val printClasspath by tasks.registering {
    doLast {
        val cp = project(":desktopApp").sourceSets.main.get().runtimeClasspath.map { it.absolutePath }.joinToString(";")
        println(cp)
    }
}
