package com.mipatrimonio.app.feature.wallet

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.database.entity.CardEntity
import com.mipatrimonio.app.core.model.CardKind
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Vista previa de una tarjeta (MiPatrimonio.md §5.5).
 * Degradado diagonal, borde de 1 dp del acento y brillo. Altura 180 dp en la lista
 * y 120 dp en el formulario. Nunca muestra números de tarjeta.
 */
@Composable
fun CardPreview(
    card: CardEntity,
    design: CardDesign,
    height: Int = 180,
    modifier: Modifier = Modifier,
    showBank: Boolean = true,
) {
    val accent = Color(design.accentColor)
    val shape = RoundedCornerShape(16.dp)
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = card.photoPath,
    ) {
        value = withContext(Dispatchers.IO) {
            card.photoPath
                ?.takeIf { it.isNotBlank() }
                ?.let { path -> decodeSampled(path)?.asImageBitmap() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.copy(alpha = 0.35f),
                spotColor = accent.copy(alpha = 0.35f),
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(design.startColor), Color(design.endColor)),
                ),
            )
            .border(width = 1.dp, color = accent, shape = shape),
    ) {
        val photo = bitmap
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
            )
        }
        CardPreviewContent(
            card = card,
            accent = accent,
            showBank = showBank,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        )
    }
}

@Composable
private fun CardPreviewContent(
    card: CardEntity,
    accent: Color,
    showBank: Boolean,
    modifier: Modifier = Modifier,
) {
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.8f),
        offset = Offset(0f, 1f),
        blurRadius = 6f,
    )
    val titleStyle = MaterialTheme.typography.headlineMedium.copy(shadow = textShadow)
    val labelStyle = MaterialTheme.typography.labelLarge.copy(shadow = textShadow)
    val bodyStyle = MaterialTheme.typography.bodySmall.copy(shadow = textShadow)
    val nameStyle = MaterialTheme.typography.titleMedium.copy(shadow = textShadow)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = card.variant.ifBlank { card.bank },
                color = accent,
                style = titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (showBank && card.variant.isNotBlank()) {
                Text(
                    text = card.bank,
                    color = accent,
                    style = labelStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(card.kind.descriptionRes()),
                color = NeonColor.ink.copy(alpha = 0.85f),
                style = bodyStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.alias,
                color = NeonColor.ink,
                style = nameStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Texto de la sección 5.5 para el tipo de tarjeta. */
private fun CardKind.descriptionRes(): Int = when (this) {
    CardKind.DEBIT -> R.string.card_preview_debit
    CardKind.CREDIT -> R.string.card_preview_credit
    CardKind.PREPAID -> R.string.card_preview_prepaid
}

private const val PREVIEW_MAX_DIMENSION = 720

/** Decodifica la foto con muestreo para no cargar imágenes completas en memoria. */
private fun decodeSampled(path: String): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= PREVIEW_MAX_DIMENSION ||
        bounds.outHeight / (sampleSize * 2) >= PREVIEW_MAX_DIMENSION
    ) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    )
}

private fun sampleCard(
    bank: String,
    variant: String,
    kind: CardKind,
    alias: String,
    theme: String,
): CardEntity = CardEntity(
    id = 1L,
    bank = bank,
    variant = variant,
    kind = kind,
    alias = alias,
    theme = theme,
    photoPath = null,
    createdAt = 0L,
)

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun CardPreviewListPreview() {
    MiPatrimonioTheme {
        CardPreview(
            card = sampleCard("CaixaBank", "Imagin", CardKind.DEBIT, "Mi tarjeta", "cyan"),
            design = CardDesign.getById("cyan"),
            height = 180,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun CardPreviewFormPreview() {
    MiPatrimonioTheme {
        CardPreview(
            card = sampleCard("BBVA", "", CardKind.CREDIT, "Tarjeta principal", "magenta"),
            design = CardDesign.getById("magenta"),
            height = 120,
            modifier = Modifier.padding(16.dp),
        )
    }
}
