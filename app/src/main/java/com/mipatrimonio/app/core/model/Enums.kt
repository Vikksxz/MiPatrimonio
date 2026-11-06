package com.mipatrimonio.app.core.model

/**
 * Enums del modelo de datos (MiPatrimonio.md §6).
 * Los nombres son la fuente de verdad para Room (TypeConverter usa .name).
 * NO renombrar constantes sin migración explícita.
 */
enum class TransactionType {
    CASH, CARD, SUBSCRIPTION, INCOME
}

enum class IncomeKind {
    PAYROLL, OTHER
}

enum class CardKind {
    DEBIT, CREDIT, PREPAID
}

enum class DocumentKind {
    PAYSLIP, INVOICE, WARRANTY, INSURANCE, CONTRACT, OTHER
}

enum class RecurrencePeriod {
    WEEKLY, MONTHLY, QUARTERLY, YEARLY
}