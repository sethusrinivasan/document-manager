package com.app.traveldocs.presentation.onboarding

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * End User License Agreement screen.
 *
 * Shown on first launch BEFORE anything else. User must accept to proceed.
 * If declined, the app closes. Acceptance is persisted with timestamp and GPS location.
 * Accessible later via Settings menu to review the signed agreement.
 */
@Composable
fun EulaScreen(onAccepted: () -> Unit, onDeclined: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            "End User License Agreement",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Please read carefully before using this application",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
            ) {
                EulaText()
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onDeclined,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
            ) {
                Text("I Decline")
            }
            Button(
                onClick = onAccepted,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Text("I Accept")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "By tapping \"I Accept\" you acknowledge that you have read, understood, and agree to be bound by this agreement.",
            fontSize = 10.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Shows the accepted EULA with acceptance metadata (date, time, location).
 * Accessible from the menu for user reference.
 */
@Composable
fun EulaViewScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("eula_prefs", Context.MODE_PRIVATE)
    val acceptedTimestamp = prefs.getLong("accepted_timestamp", 0L)
    val acceptedLocation = prefs.getString("accepted_location", "Not available") ?: "Not available"

    val dateStr = if (acceptedTimestamp > 0) {
        java.text.SimpleDateFormat("EEEE, dd MMMM yyyy 'at' HH:mm:ss z", java.util.Locale.getDefault())
            .format(java.util.Date(acceptedTimestamp))
    } else "Not accepted"

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("End User License Agreement", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Agreement Status: ACCEPTED", fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32), fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("Date & Time: $dateStr", fontSize = 12.sp)
                Text("Location: $acceptedLocation", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
        ) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                EulaText()
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun EulaText() {
    val sections = listOf(
        "1. ACCEPTANCE OF TERMS" to """
By downloading, installing, accessing, or using the Document Manager application ("the App"), you ("User") acknowledge that you have read, understood, and agree to be legally bound by this End User License Agreement ("Agreement"). If you do not agree to these terms, you must immediately uninstall the App and discontinue all use.

This Agreement constitutes a legally binding contract between you and the developer(s) of the App ("Developer"). Your continued use of the App following any modifications to this Agreement constitutes acceptance of those modifications.
        """.trimIndent(),

        "2. LICENSE GRANT" to """
Subject to your compliance with this Agreement, Developer grants you a limited, non-exclusive, non-transferable, revocable license to use the App on a single device owned or controlled by you, solely for your personal, non-commercial purposes.

This license does not grant you any rights to the source code, except as separately provided under the Apache 2.0 open source license governing the source repository.
        """.trimIndent(),

        "3. DISCLAIMER OF WARRANTIES" to """
THE APP IS PROVIDED "AS IS" AND "AS AVAILABLE" WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE. DEVELOPER EXPRESSLY DISCLAIMS ALL WARRANTIES, INCLUDING BUT NOT LIMITED TO:

• IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT
• ANY WARRANTY THAT THE APP WILL MEET YOUR REQUIREMENTS
• ANY WARRANTY THAT THE APP WILL BE UNINTERRUPTED, TIMELY, SECURE, OR ERROR-FREE
• ANY WARRANTY REGARDING THE ACCURACY OR RELIABILITY OF ANY INFORMATION OBTAINED THROUGH THE APP
• ANY WARRANTY THAT DEFECTS WILL BE CORRECTED

YOU ACKNOWLEDGE THAT THE ENTIRE RISK ARISING OUT OF THE USE OR PERFORMANCE OF THE APP REMAINS WITH YOU.
        """.trimIndent(),

        "4. LIMITATION OF LIABILITY" to """
TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, IN NO EVENT SHALL DEVELOPER BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, PUNITIVE, OR EXEMPLARY DAMAGES, INCLUDING BUT NOT LIMITED TO:

• LOSS OF DATA, DOCUMENTS, OR INFORMATION
• LOSS OF REVENUE, PROFITS, OR BUSINESS
• BUSINESS INTERRUPTION
• PERSONAL INJURY OR PROPERTY DAMAGE
• LOSS OF PRIVACY
• FAILURE TO MEET ANY DUTY INCLUDING GOOD FAITH OR REASONABLE CARE
• NEGLIGENCE
• ANY OTHER PECUNIARY OR OTHER LOSS WHATSOEVER

ARISING OUT OF OR IN CONNECTION WITH THE USE OR INABILITY TO USE THE APP, EVEN IF DEVELOPER HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

DEVELOPER'S TOTAL CUMULATIVE LIABILITY SHALL NOT EXCEED ZERO DOLLARS ($0.00 USD).
        """.trimIndent(),

        "5. USER RESPONSIBILITY AND COMPLIANCE" to """
You are solely and exclusively responsible for:

• All content you store, import, share, backup, or process using the App
• Ensuring your use of the App complies with all applicable local, state, national, and international laws, regulations, and government requirements
• Maintaining the security of your device, authentication credentials, and encryption keys
• Verifying the accuracy and completeness of any information processed by the App's automated features (OCR, classification, search)
• Creating and maintaining adequate backups of your data through independent means
• Any consequences arising from sharing, exporting, or transmitting documents through the App's sharing or backup features

You acknowledge that the App's automated document processing (OCR, classification, tagging) is provided for convenience only and may produce inaccurate results. You must independently verify all automatically generated information before relying upon it for any purpose.

You further acknowledge that this App was built and tested with the assistance of artificial intelligence. All document processing occurs locally on your device using third-party libraries whose accuracy and reliability are outside Developer's control. Classification errors, incorrect metadata extraction, and other inaccuracies may occur due to limitations inherent in these libraries. Developer assumes no liability for errors in automated processing.
        """.trimIndent(),

        "6. DATA AND PRIVACY" to """
The App stores all user data locally on your device. Developer does not operate servers, collect user data, or have access to your documents, metadata, or personal information.

If you enable optional features (cloud backup, sharing), you acknowledge that data will leave your device at your explicit direction. Developer bears no responsibility for data once it leaves the App's local encrypted storage.

If you enable anonymous telemetry (opt-in only), only non-identifying usage statistics are collected. No document content, personal information, or location data is transmitted.

GPS logging, if enabled by you, is stored exclusively on your device. Developer has no access to your location history.
        """.trimIndent(),

        "7. ENCRYPTION AND SECURITY" to """
The App uses encryption (AES-256-GCM) to protect stored documents. You acknowledge and accept that:

• If you lose access to your device's biometric authentication or the device's hardware security module is compromised, your encrypted data may become permanently inaccessible
• Developer cannot recover, decrypt, or access your encrypted documents under any circumstances
• No encryption system is absolutely secure; Developer makes no guarantee against all possible attack vectors
• You are responsible for maintaining physical security of your device
        """.trimIndent(),

        "7B. PER-DOCUMENT PIN PROTECTION" to """
The App offers an optional per-document PIN lock feature ("Secure Docs"). You acknowledge and accept that:

• Per-document PINs are derived into encryption keys using PBKDF2-SHA256. The PIN itself is never stored.
• IF YOU FORGET A PER-DOCUMENT PIN, THE PROTECTED DOCUMENT IS PERMANENTLY AND IRREVERSIBLY INACCESSIBLE. There is no recovery mechanism, no master key, and no backdoor.
• Developer has no ability whatsoever to recover, decrypt, or access PIN-protected documents. This is by design.
• This feature is a personal privacy tool. It does not exempt you from legal obligations.
• If compelled by lawful court order, government authority, or law enforcement to disclose document contents, you bear sole responsibility for compliance. Developer cannot assist in decryption.
• You must not use this feature to conceal evidence, obstruct justice, or violate any applicable law.
• Developer bears zero liability for data loss resulting from forgotten PINs.
        """.trimIndent(),

        "8. INDEMNIFICATION" to """
You agree to indemnify, defend, and hold harmless Developer, its affiliates, officers, agents, and licensors from and against any and all claims, damages, obligations, losses, liabilities, costs, or debt, and expenses (including reasonable attorneys' fees) arising from:

• Your use of and access to the App
• Your violation of any term of this Agreement
• Your violation of any third-party right, including any intellectual property, privacy, or property right
• Any claim that your use of the App caused damage to a third party
• Your violation of any applicable law or regulation
        """.trimIndent(),

        "9. PROHIBITED USES" to """
You agree not to use the App to:

• Store, process, or transmit any content that violates applicable law
• Circumvent, disable, or interfere with security features of the App
• Reverse engineer the encryption mechanisms for unlawful purposes
• Use the App in connection with any illegal activity
• Store content that infringes upon the intellectual property rights of others

You bear sole legal responsibility for all content stored within the App.
        """.trimIndent(),

        "10. TERMINATION" to """
This Agreement is effective until terminated. Developer may terminate this Agreement at any time without notice. Upon termination, you must cease all use of the App and destroy all copies.

Sections 3, 4, 5, 8, and 11 shall survive termination of this Agreement.
        """.trimIndent(),

        "11. MODIFICATIONS TO AGREEMENT" to """
DEVELOPER RESERVES THE RIGHT TO MODIFY THIS AGREEMENT AT ANY TIME, WITHOUT PRIOR NOTICE, AT DEVELOPER'S SOLE DISCRETION.

Continued use of the App following any such modification constitutes your acceptance of the modified Agreement. It is your responsibility to review this Agreement periodically.

The most current version of this Agreement supersedes all previous versions.
        """.trimIndent(),

        "11B. PRICING AND MONETIZATION" to """
Developer reserves the right, at its sole discretion, to:

• Introduce paid features, subscriptions, or in-app purchases at any time
• Change pricing for any existing or planned features without prior notice
• Offer different pricing tiers, promotional rates, or regional pricing
• Convert previously free features to paid features
• Discontinue free access to any feature

You acknowledge that continued use of the App after any pricing change constitutes acceptance of the new pricing terms. If you do not agree with a pricing change, your sole remedy is to discontinue use of the App.

No refunds will be issued for features that are subsequently modified, reduced in scope, or discontinued.
        """.trimIndent(),

        "12. GOVERNING LAW AND DISPUTE RESOLUTION" to """
This Agreement shall be governed by and construed in accordance with applicable law, without regard to conflict of law principles.

Any disputes arising under or in connection with this Agreement shall be resolved through binding arbitration, with each party bearing their own costs.

You waive any right to participate in a class action lawsuit or class-wide arbitration against Developer.
        """.trimIndent(),

        "13. SEVERABILITY" to """
If any provision of this Agreement is held to be unenforceable or invalid, such provision shall be changed and interpreted to accomplish the objectives of such provision to the greatest extent possible under applicable law, and the remaining provisions shall continue in full force and effect.
        """.trimIndent(),

        "14. ENTIRE AGREEMENT" to """
This Agreement constitutes the entire agreement between you and Developer regarding the use of the App and supersedes all prior and contemporaneous understandings, agreements, representations, and warranties.

No failure or delay by Developer in exercising any right under this Agreement shall operate as a waiver of that right.
        """.trimIndent()
    )

    for ((title, body) in sections) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(body, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(16.dp))
    }

    Text(
        "Last updated: August 2026\n© 2026 Document Manager Developer. All rights reserved.",
        fontSize = 11.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
