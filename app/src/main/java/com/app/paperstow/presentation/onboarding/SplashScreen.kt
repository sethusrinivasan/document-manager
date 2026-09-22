package com.app.paperstow.presentation.onboarding

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.paperstow.R

@Composable
fun SplashScreen(onContinue: (skipInFuture: Boolean) -> Unit) {
    var skipInFuture by remember { mutableStateOf(false) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bodyAlign = if (landscape) TextAlign.Start else TextAlign.Center

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C2041))
            .verticalScroll(rememberScrollState())
            .padding(if (landscape) 20.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (landscape) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "Paperstow",
                    modifier = Modifier.size(88.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Paperstow", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "A pocket for the family’s travel papers.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        } else {
            Spacer(Modifier.height(36.dp))
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "Paperstow",
                modifier = Modifier.size(112.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Paperstow", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "A pocket for the family’s travel papers.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
        }
        Text(
            "Passports, visas, tickets, and insurance stay encrypted on this phone. No account. No Paperstow servers.",
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.92f),
            textAlign = bodyAlign,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(if (landscape) 10.dp else 16.dp))
        Text(
            "Unlock with this phone’s fingerprint, face, or screen lock. Find a page by tag or text — even in airplane mode at the gate.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.82f),
            textAlign = bodyAlign,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(if (landscape) 10.dp else 16.dp))
        Text(
            "Nothing leaves unless you share a file or export an archive. Suggested names and tags are only a helper — always check the original page.",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.72f),
            textAlign = bodyAlign,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(if (landscape) 12.dp else 20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = skipInFuture, onCheckedChange = { skipInFuture = it })
            Text("Don’t show this again", color = Color.White.copy(alpha = 0.95f), fontSize = 14.sp)
        }
        Spacer(Modifier.height(if (landscape) 12.dp else 20.dp))
        Button(
            onClick = { onContinue(skipInFuture) },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Continue", fontSize = 16.sp)
        }
        Spacer(Modifier.height(16.dp))
    }
}
