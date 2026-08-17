package com.example.filemanager.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.filemanager.download.DownloadDatabase
import com.example.filemanager.download.DownloadItem
import com.example.filemanager.download.DownloadWorker
import com.example.filemanager.security.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

@Composable
fun UnlockVaultScreen(navController: NavController? = null) {
    val context = LocalContext.current
    val activity = LocalContext.current as Activity
    var status by remember { mutableStateOf("Locked") }
    val executor: Executor = ContextCompat.getMainExecutor(context)

    val biometricPrompt = remember {
        BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                status = "Unlocked"
                // On success, retry paused downloads
                retryPausedDownloads(context)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                status = "Error: $errString"
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                status = "Failed"
            }
        })
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock credentials")
            .setSubtitle("Authenticate to allow downloads that require saved credentials")
            .setNegativeButtonText("Cancel")
            .build()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Vault status: $status")
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            val can = BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            if (can == BiometricManager.BIOMETRIC_SUCCESS) {
                biometricPrompt.authenticate(promptInfo)
            } else {
                Toast.makeText(context, "Biometric not available. Please enable in settings.", Toast.LENGTH_LONG).show()
            }
        }) {
            Text("Unlock")
        }
    }
}

suspend fun retryPausedDownloads(context: Context) = withContext(Dispatchers.IO) {
    val db = DownloadDatabase.get(context)
    val dao = db.downloadDao()
    val paused = dao.getByStatus(DownloadItem.Status.PAUSED)
    for (p in paused) {
        // re-enqueue worker for these items
        // Build input data
        val input = androidx.work.Data.Builder()
            .putString(DownloadWorker.EXTRA_ID, p.id)
            .putString(DownloadWorker.EXTRA_CREDENTIAL_PREFIX, p.credentialPrefix)
            .putString(DownloadWorker.EXTRA_PRIVATE_KEY_NAME, p.privateKeyName)
            .build()
        val req = androidx.work.OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(input).build()
        androidx.work.WorkManager.getInstance(context).enqueue(req)
        dao.update(p.copy(status = DownloadItem.Status.QUEUED))
    }
}
