package com.partyplanner.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.partyplanner.presentation.auth.ForgotPasswordComponent
import com.partyplanner.presentation.auth.ForgotPasswordState
import com.partyplanner.ui.theme.AppShapes
import com.partyplanner.ui.theme.appColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(component: ForgotPasswordComponent) {
    val state by component.state.collectAsState()
    var email by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mot de passe oublié") },
                navigationIcon = {
                    IconButton(onClick = component::onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (val s = state) {
                is ForgotPasswordState.Success -> SuccessContent(onBack = component::onBack)
                else -> {
                    Text(
                        text = "Saisissez votre adresse e-mail et nous vous enverrons un lien pour réinitialiser votre mot de passe.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Adresse e-mail") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = AppShapes.TextField,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            if (email.isNotBlank()) component.onSubmit(email)
                        }),
                        enabled = s !is ForgotPasswordState.Loading
                    )

                    if (s is ForgotPasswordState.Error) {
                        Text(
                            text = s.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    val gradA = MaterialTheme.appColors.gradA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(AppShapes.Pill)
                            .alpha(if (email.isNotBlank() && s !is ForgotPasswordState.Loading) 1f else 0.5f)
                            .background(brush = gradA)
                            .clickable(
                                enabled = email.isNotBlank() && s !is ForgotPasswordState.Loading,
                                onClick = { component.onSubmit(email) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (s is ForgotPasswordState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Envoyer le lien",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "📧", style = MaterialTheme.typography.displayMedium)
        Text(
            text = "E-mail envoyé !",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Si un compte existe avec cette adresse, vous recevrez un e-mail avec un lien de réinitialisation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) {
            Text("Retour à la connexion")
        }
    }
}
