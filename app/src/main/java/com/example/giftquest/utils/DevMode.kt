package com.example.giftquest.utils

/**
 * Dev Mode Configuration
 *
 * Controls auto-login behavior for testing devices.
 * In production, set DEV_MODE_ENABLED = false
 */
object DevMode {

    // 🔧 Master switch - set to false for production
    const val DEV_MODE_ENABLED = true

    /**
     * Device whitelist for auto-login
     * Only devices in this list will auto-login
     */
    private val WHITELISTED_DEVICES = mapOf(
        // Pixel Fold
        "Pixel Fold" to DeviceConfig(
            email = "backupphoto308@gmail.com",
            password = "123456789"
        ),

        // Pixel 9 Pro XL
        "sdk_gphone64_x86_64" to DeviceConfig(
            email = "backupphoto308@gmail.com",
            password = "123456789"
        ),

        // Xiaomi Device
        "23117RA68G" to DeviceConfig(
            email = "bphoto043@gmail.com",
            password = "987654321"
        ),

        // Add more test devices here
        // "Device Model" to DeviceConfig(email, password)
    )

    /**
     * Check if current device is whitelisted for auto-login
     */
    fun isDeviceWhitelisted(): Boolean {
        if (!DEV_MODE_ENABLED) return false

        val deviceModel = android.os.Build.MODEL
        android.util.Log.d("DevMode", "Current device: $deviceModel")

        return WHITELISTED_DEVICES.keys.any { whitelistedModel ->
            deviceModel.contains(whitelistedModel, ignoreCase = true)
        }
    }

    /**
     * Get auto-login credentials for current device
     * Returns null if device is not whitelisted
     */
    fun getDeviceConfig(): DeviceConfig? {
        if (!DEV_MODE_ENABLED) return null

        val deviceModel = android.os.Build.MODEL

        return WHITELISTED_DEVICES.entries.firstOrNull { (whitelistedModel, _) ->
            deviceModel.contains(whitelistedModel, ignoreCase = true)
        }?.value
    }

    /**
     * Configuration for a test device
     */
    data class DeviceConfig(
        val email: String,
        val password: String
    )
}