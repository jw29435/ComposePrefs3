package com.jamal.composeprefs3.ui.prefs

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jamal.composeprefs3.ui.LocalPrefsDataStore
import com.jamal.composeprefs3.ui.ifNotNullThen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Preference which shows a TextField in a Dialog
 *
 * @param key Key used to identify this Pref in the DataStore
 * @param title Main text which describes the Pref
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param dialogTitle Title shown in the dialog. No title if null.
 * @param dialogMessage Summary shown underneath [dialogTitle]. No summary if null.
 * @param defaultValue Default value that will be set in the TextField when the dialog is shown for the first time.
 * @param onValueSaved Will be called with new TextField value when the confirm button is clicked. It is NOT called every time the value changes. Use [onValueChange] for that.
 * @param onValueChange Will be called every time the TextField value is changed.
 * @param dialogBackgroundColor Color of the dropdown menu
 * @param textColor Text colour of the [title] and [summary]
 * @param enabled If false, this Pref cannot be clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalComposeUiApi
@Composable
fun EditTextPref(
    key: String,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    dialogComposable: Composable? = null,
    dialogTitle: String? = null,
    dialogMessage: String? = null,
    defaultValue: String = "",
    onValueSaved: ((String) -> Unit) = {},
    onValueChange: ((String) -> Unit) = {},
    dialogBackgroundColor: Color = MaterialTheme.colorScheme.background,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    enabled: Boolean = true,
    darkenOnDisable: Boolean = false
) {

    var showDialog by rememberSaveable { mutableStateOf(false) }
    val selectionKey = stringPreferencesKey(key)
    val scope = rememberCoroutineScope()

    val datastore = LocalPrefsDataStore.current
    val prefs by remember { datastore.data }.collectAsState(initial = null)

    //value should only change when save button is clicked
    var value by remember { mutableStateOf(defaultValue) }
    //value of the TextField which changes every time the text is modified
    var textVal by remember { mutableStateOf(value) }

    var dialogSize by remember { mutableStateOf(Size.Zero) }

    // Set value initially if it exists in datastore
    LaunchedEffect(Unit) {
        prefs?.get(selectionKey)?.also {
            value = it
        }
    }

    LaunchedEffect(datastore.data) {
        datastore.data.collectLatest { pref ->
            pref[selectionKey]?.also {
                value = it
            }
        }
    }

    fun edit() = run {
        scope.launch {
            try {
                datastore.edit { preferences ->
                    preferences[selectionKey] = textVal
                }
                onValueSaved(textVal)
            } catch (e: Exception) {
                Log.e(
                    "EditTextPref",
                    "Could not write pref $key to database. ${e.printStackTrace()}"
                )
            }
        }
    }

    TextPref(
        title = title,
        modifier = modifier,
        summary = summary,
        darkenOnDisable = darkenOnDisable,
        textColor = textColor,
        enabled = enabled,
        onClick = { if (enabled) showDialog = !showDialog },
    )

    if (showDialog) {
        //reset
        LaunchedEffect(null) {
            textVal = value
        }
        dialogComposable ?:
        AlertDialog(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .onGloballyPositioned {
                    dialogSize = it.size.toSize()
                },
            onDismissRequest = { showDialog = false },
            title = { DialogHeader(dialogTitle, dialogMessage) },
            text = {
                OutlinedTextField(
                    value = textVal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onValueChange = {
                        textVal = it
                        onValueChange(it)
                    }
                )
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.padding(end = 16.dp),
                    onClick = {
                        edit()
                        showDialog = false
                    }
                ) {
                    Text("Save", style = MaterialTheme.typography.bodyLarge)
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.padding(end = 16.dp),
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel", style = MaterialTheme.typography.bodyLarge)
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            containerColor = dialogBackgroundColor,
        )
    }
}

@Composable
fun DialogHeader(dialogTitle: String?, dialogMessage: String?) {

    Column(modifier = Modifier.padding(16.dp)) {
        dialogTitle.ifNotNullThen {
            Text(
                text = dialogTitle!!,
                style = MaterialTheme.typography.titleLarge
            )
        }?.invoke()

        dialogMessage.ifNotNullThen {
            Text(
                text = dialogMessage!!,
                style = MaterialTheme.typography.bodyLarge
            )
        }?.invoke()
    }
}