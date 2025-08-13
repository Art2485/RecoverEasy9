package com.recovereasy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppScreen() }
    }
}

@Composable
fun AppScreen() {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("RecoverEasy — Build OK", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "สแตกนี้เป็นหน้าทดสอบเพื่อให้บิลด์ผ่านแน่นอน " +
                    "จากนี้ค่อยเติมฟีเจอร์สแกน/พรีวิวได้"
                )
                Button(onClick = { /* TODO: hook ฟีเจอร์จริง */ }) {
                    Text("เริ่มใช้งาน")
                }
            }
        }
    }
}
