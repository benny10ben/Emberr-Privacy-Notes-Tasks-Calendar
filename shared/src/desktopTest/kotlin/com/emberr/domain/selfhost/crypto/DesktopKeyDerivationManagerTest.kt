package com.emberr.domain.selfhost.crypto

class DesktopKeyDerivationManagerTest : KeyDerivationManagerContract() {
    override fun createKeyDerivationManager(): KeyDerivationManager = Pbkdf2KeyDerivationManager()
}
