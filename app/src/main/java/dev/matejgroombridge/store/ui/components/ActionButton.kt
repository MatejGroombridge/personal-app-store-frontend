package dev.matejgroombridge.store.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.matejgroombridge.store.data.model.ActionState
import dev.matejgroombridge.store.data.model.InstallState

/**
 * Compact action button that swaps label/style based on combined install + action state.
 */
@Composable
fun ActionButton(
    installState: InstallState,
    actionState: ActionState,
    onClick: () -> Unit,
) {
    AnimatedContent(
        targetState = ButtonModel.from(installState, actionState),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ActionButton",
    ) { model ->
        when (model) {
            ButtonModel.Spinner -> Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            is ButtonModel.Primary -> Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text(model.label) }
            is ButtonModel.Tonal -> FilledTonalButton(onClick = onClick) {
                Text(model.label)
            }
        }
    }
}

private sealed interface ButtonModel {
    data object Spinner : ButtonModel
    data class Primary(val label: String) : ButtonModel
    data class Tonal(val label: String) : ButtonModel

    companion object {
        fun from(install: InstallState, action: ActionState): ButtonModel = when (action) {
            is ActionState.Downloading, ActionState.Verifying, ActionState.AwaitingInstall -> Spinner
            is ActionState.Failed, ActionState.Idle -> when (install) {
                is InstallState.NotInstalled    -> Primary("Install")
                is InstallState.UpdateAvailable -> Primary("Update")
                is InstallState.Installed       -> Tonal("Open")
            }
        }
    }
}
