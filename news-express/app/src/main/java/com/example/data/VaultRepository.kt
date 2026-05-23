package com.example.data

import kotlinx.coroutines.flow.Flow

class VaultRepository(private val vaultDao: VaultDao) {
    val hiddenApps: Flow<List<HiddenApp>> = vaultDao.getAllHiddenApps()

    suspend fun addHiddenApp(packageName: String, appName: String) {
        vaultDao.insertHiddenApp(HiddenApp(packageName, appName))
    }

    suspend fun removeHiddenApp(packageName: String) {
        vaultDao.deleteHiddenAppByPackage(packageName)
    }

    suspend fun getPasscode(): String? {
        return vaultDao.getConfigValue("passcode")
    }

    suspend fun savePasscode(passcode: String) {
        vaultDao.insertConfig(VaultConfig("passcode", passcode))
    }
}
