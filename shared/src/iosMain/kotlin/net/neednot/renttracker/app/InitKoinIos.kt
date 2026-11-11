package net.neednot.renttracker.app

fun initKoinIos() {
    initKoin(platformModules = listOf(iosPlatformModule))
}
