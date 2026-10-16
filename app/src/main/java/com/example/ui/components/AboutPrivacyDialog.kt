package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PicoCyan
import com.example.ui.theme.PicoNavyBorder
import com.example.ui.theme.PicoNavyCard
import com.example.ui.theme.PicoNavyDarkest
import com.example.ui.theme.PicoNavySurface
import com.example.ui.theme.PicoTextPrimary
import com.example.ui.theme.PicoTextSecondary
import com.example.ui.theme.PicoViolet

@Composable
fun AboutPrivacyDialog(
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PicoNavyDarkest)
                        .border(1.dp, PicoCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = PicoCyan, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Pico AI Architecture", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PicoTextPrimary)
                    Text("100% Private • Free • Local Inference", style = MaterialTheme.typography.labelSmall, color = PicoCyan)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                PrivacyCard(
                    icon = Icons.Default.Lock,
                    title = "Zero Cloud Transmission",
                    description = "All prompts, chat messages, and answers are generated exclusively by neural weights executing in your device's memory. No data is ever sent to OpenAI, Google, Anthropic, or any remote server."
                )

                Spacer(modifier = Modifier.height(10.dp))

                PrivacyCard(
                    icon = Icons.Default.NetworkCheck,
                    title = "Network Permission Disclosure",
                    description = "The INTERNET permission is used strictly for user-initiated downloads of verified models (such as from Hugging Face or direct weight mirrors). Once downloaded, all features function completely offline."
                )

                Spacer(modifier = Modifier.height(10.dp))

                PrivacyCard(
                    icon = Icons.Default.Memory,
                    title = "Hardware & Huawei Y9 2019 Considerations",
                    description = "The Huawei Y9 2019 features an octa-core HiSilicon Kirin 710 SoC with 3GB or 4GB RAM. For optimal speed and battery life on Kirin 710 and similar devices, we recommend the SmolLM 135M or Gemma 3 1B models, which operate comfortably within low-RAM limits."
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PicoNavySurface)
                        .border(1.dp, PicoNavyBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Open Source Licenses", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = PicoCyan)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "• Application source code: Apache-2.0 License\n" +
                                    "• SmolLM model weights: Apache-2.0 License\n" +
                                    "• Gemma model weights: Gemma Open Model Terms of Use\n" +
                                    "• MediaPipe Tasks GenAI: Apache-2.0 License",
                            style = MaterialTheme.typography.bodySmall,
                            color = PicoTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PicoCyan, contentColor = PicoNavyDarkest)
            ) {
                Text("Got It")
            }
        },
        containerColor = PicoNavyCard
    )
}

@Composable
private fun PrivacyCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PicoNavySurface)
            .border(1.dp, PicoNavyBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = PicoCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = PicoTextPrimary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = PicoTextSecondary)
        }
    }
}
