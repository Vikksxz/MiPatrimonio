package com.mipatrimonio.app.core.ocr

import java.time.LocalDate

/** Confianza de un campo extraído por OCR (§9.1). */
enum class FieldConfidence { HIGH, MEDIUM, LOW }

/**
 * Resultado del análisis de un ticket (§9.1).
 * Importes en céntimos (Long). Nunca se guarda sin confirmación del usuario.
 */
data class ParsedReceipt(
    val amountCents: Long? = null,
    val amountConfidence: FieldConfidence = FieldConfidence.LOW,
    val date: LocalDate? = null,
    val dateConfidence: FieldConfidence = FieldConfidence.LOW,
    val merchant: String? = null,
    val merchantConfidence: FieldConfidence = FieldConfidence.LOW,
)