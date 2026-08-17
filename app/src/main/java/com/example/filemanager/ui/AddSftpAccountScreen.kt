package com.example.filemanager.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.filemanager.security.SecureStorage
import java.io.InputStream

@Composable
fun AddSftpAccountScreen() {
    val context = LocalContext.current
    var host by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var rememberCreds by remember { mutableStateOf(true) }
    val scaffoldState = rememberScaffoldState()
    var refresh by remember { mutableStateOf(0) }

    val prefixes = SecureStorage.listCredentialPrefixes(context)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // read the key bytes and save them encrypted
        val name = "privkey_${host}_${user}.pem"
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                SecureStorage.savePrivateKey(context, name, bytes)
                val prefix = "sftp_${host}_${user}"
                SecureStorage.saveAccountPrivateKeyName(context, prefix, name)
                Toast.makeText(context, "Imported key as $name", Toast.LENGTH_SHORT).show()
                refresh++
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to import key: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(scaffoldState = scaffoldState) { padding ->
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)) {
            Text("Add SFTP Account", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password" ) }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = rememberCreds, onCheckedChange = { rememberCreds = it })
                Text("Remember credentials")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (host.isBlank() || user.isBlank()) {
                    Toast.makeText(context, "Fill host and username", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val prefix = "sftp_${host}_${user}"
                if (rememberCreds && pass.isNotBlank()) {
                    SecureStorage.saveCredentials(context, prefix, user, pass)
                }
                Toast.makeText(context, "Saved account for $host", Toast.LENGTH_SHORT).show()
                host = ""
                user = ""
                pass = ""
            }) {
                Text("Save")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Import private key for account (optional)")
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                OutlinedButton(onClick = {
                    if (host.isBlank() || user.isBlank()) {
                        Toast.makeText(context, "Set Host and Username first (temporary)", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    // Launch SAF to pick PEM file
                    launcher.launch(arrayOf("text/*", "application/octet-stream"))
                }) {
                    Text("Import private key (PEM)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Saved accounts:")
            val saved = SecureStorage.listCredentialPrefixes(context)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(saved) { p ->
                    val priv = SecureStorage.getAccountPrivateKeyName(context, p)
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p)
                            if (priv != null) Text("Key: $priv", style = MaterialTheme.typography.caption)
                        }
                        IconButton(onClick = {
                            SecureStorage.deleteCredentials(context, p)
                            SecureStorage.deleteAccountPrivateKeyName(context, p)
                            Toast.makeText(context, "Deleted $p", Toast.LENGTH_SHORT).show()
                            refresh++
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}
