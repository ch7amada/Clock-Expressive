# EventBus
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Fossify Commons bundles Reprint, which references the legacy fingerprint API
# omitted from the API 37 SDK stubs. Clock does not use fingerprint authentication.
-dontwarn android.hardware.fingerprint.FingerprintManager
-dontwarn android.hardware.fingerprint.FingerprintManager$AuthenticationCallback
-dontwarn android.hardware.fingerprint.FingerprintManager$AuthenticationResult
-dontwarn android.hardware.fingerprint.FingerprintManager$CryptoObject
