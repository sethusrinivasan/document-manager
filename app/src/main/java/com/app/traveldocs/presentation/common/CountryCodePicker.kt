package com.app.traveldocs.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CountryCode(val name: String, val code: String, val dial: String)

val countryCodes = listOf(
    CountryCode("US", "US", "+1"), CountryCode("UK", "GB", "+44"), CountryCode("India", "IN", "+91"),
    CountryCode("Canada", "CA", "+1"), CountryCode("Australia", "AU", "+61"), CountryCode("Germany", "DE", "+49"),
    CountryCode("France", "FR", "+33"), CountryCode("Japan", "JP", "+81"), CountryCode("Singapore", "SG", "+65"),
    CountryCode("Brazil", "BR", "+55"), CountryCode("Mexico", "MX", "+52"), CountryCode("Italy", "IT", "+39"),
    CountryCode("Spain", "ES", "+34"), CountryCode("South Korea", "KR", "+82"), CountryCode("China", "CN", "+86"),
    CountryCode("UAE", "AE", "+971"), CountryCode("Thailand", "TH", "+66"), CountryCode("Malaysia", "MY", "+60"),
    CountryCode("Indonesia", "ID", "+62"), CountryCode("New Zealand", "NZ", "+64"),
    CountryCode("Ireland", "IE", "+353"), CountryCode("Sri Lanka", "LK", "+94"),
    CountryCode("Pakistan", "PK", "+92"), CountryCode("Bangladesh", "BD", "+880"),
    CountryCode("Nigeria", "NG", "+234"), CountryCode("South Africa", "ZA", "+27"),
    CountryCode("Kenya", "KE", "+254"), CountryCode("Turkey", "TR", "+90"),
    CountryCode("Russia", "RU", "+7"), CountryCode("Saudi Arabia", "SA", "+966"),
)

@Composable
fun PhoneInputWithCountryCode(fullPhone: String, onPhoneChange: (String) -> Unit, label: String = "Phone number", modifier: Modifier = Modifier) {
    val initial = countryCodes.find { fullPhone.startsWith(it.dial) }
    var selectedCountry by remember { mutableStateOf(initial ?: countryCodes[2]) }
    var phoneNumber by remember { mutableStateOf(if (initial != null) fullPhone.removePrefix(initial.dial).trim() else fullPhone.replace(Regex("^\\+\\d{1,3}\\s?"), "")) }
    var expanded by remember { mutableStateOf(false) }

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Box {
            TextButton(onClick = { expanded = true }) { Text(selectedCountry.dial, fontSize = 14.sp); Icon(Icons.Filled.ArrowDropDown, "Country") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 300.dp)) {
                countryCodes.forEach { c ->
                    DropdownMenuItem(text = { Text("${c.name} (${c.dial})", fontSize = 13.sp) }, onClick = { selectedCountry = c; expanded = false; onPhoneChange("${c.dial} $phoneNumber") })
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        OutlinedTextField(value = phoneNumber, onValueChange = { phoneNumber = it.filter { c -> c.isDigit() }.take(12); onPhoneChange("${selectedCountry.dial} $phoneNumber") }, label = { Text(label) }, singleLine = true, modifier = Modifier.weight(1f), placeholder = { Text("9876543210") })
    }
}
