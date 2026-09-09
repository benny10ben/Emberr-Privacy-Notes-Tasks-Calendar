package com.emberr.domain.selfhost.crypto

class AndroidKeyDerivationManagerTest : KeyDerivationManagerContract() {
    override fun createKeyDerivationManager(): KeyDerivationManager = Pbkdf2KeyDerivationManager()
}
