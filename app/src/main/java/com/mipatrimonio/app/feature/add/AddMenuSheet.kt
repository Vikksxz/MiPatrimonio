package com.mipatrimonio.app.feature.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.ui.components.ScreenTitle
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme

/**
 * Hoja del botón + (T1.7 la completa con tipo, importe, comercio, fecha…).
 * En T1.3 debe abrirse vacía, solo con su título.
 */
@Composable
fun AddMenuSheet(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
    ) {
        ScreenTitle(text = stringResource(R.string.add_sheet_title))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1424)
@Composable
private fun AddMenuSheetPreview() {
    MiPatrimonioTheme {
        AddMenuSheet()
    }
}
