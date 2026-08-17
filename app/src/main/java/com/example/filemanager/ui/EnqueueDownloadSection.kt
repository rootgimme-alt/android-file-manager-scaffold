package com.example.filemanager.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.filemanager.download.DownloadRepository
import com.example.filemanager.security.SecureStorage
import kotlinx.coroutines.launch

@Composable
fun EnqueueDownloadSection(treeUri: Uri?) {
    val context = LocalContext.current
    val repo = remember { DownloadRepository(context) }
    var url by remember { mutableStateOf("") }
    var filename by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    val accountPrefixes by remember { mutableStateOf(SecureStorage.listCredentialPrefixes(context)) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Download URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = filename, onValueChange = { filename = it }, label = { Text("Filename (optional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        if (url.startsWith("sftp://")) {
            Text("Choose account (saved)")
            Spacer(modifier = Modifier.height(4.dp))
            val accounts = accountPrefixes
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(selectedAccount ?: "Select account (optional)")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(onClick = {
                        selectedAccount = null
                        expanded = false
                    }) { Text("Use inline URL creds / no saved account") }
                    accounts.forEach { a ->
                        DropdownMenuItem(onClick = {
                            selectedAccount = a
                            expanded = false
                        }) { Text(a) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(onClick = {
            if (treeUri == null) {
                Toast.makeText(context, "Pick a destination directory first", Toast.LENGTH_SHORT).show()
                return@Button
            }
            val finalFilename = if (filename.isBlank()) Uri.parse(url).lastPathSegment ?: "downloadfile" else filename
            scope.launch {
                repo.enqueueDownload(url, finalFilename, treeUri, credentialPrefix = selectedAccount)
            }
            url = ""
            filename = ""
            selectedAccount = null
        }) {
            Text("Enqueue download")
        }
    }
}
