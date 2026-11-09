package com.mipatrimonio.app.domain.usecase

/**
 * Reparto de un gasto compartido (T4.7, MiPatrimonio.md §7.9/§8.8).
 * Todo en céntimos [Long]. Funciones puras, sin I/O.
 */
object SharedSplit {

    /**
     * Reparte [totalCents] a partes iguales entre [personIds].
     * El resto de céntimos (por división no exacta) se asigna de uno en uno,
     * empezando por [preferPersonId] si está entre los participantes; si no, por el primero.
     * @return mapa personId → parte en céntimos (suma exacta = [totalCents]).
     */
    fun splitEvenly(
        totalCents: Long,
        personIds: List<Long>,
        preferPersonId: Long? = null,
    ): Map<Long, Long> {
        require(totalCents > 0L) { "El total debe ser mayor que 0" }
        require(personIds.isNotEmpty()) { "Debe haber al menos un participante" }
        require(personIds.distinct().size == personIds.size) { "Participantes duplicados" }
        val n = personIds.size.toLong()
        val base = totalCents / n
        var remainder = (totalCents % n).toInt()
        val ordered = orderedForRemainder(personIds, preferPersonId)
        val result = personIds.associateWith { base }.toMutableMap()
        var i = 0
        while (remainder > 0 && i < ordered.size) {
            val pid = ordered[i]
            result[pid] = (result[pid] ?: 0L) + 1L
            remainder--
            i++
        }
        return result
    }

    /**
     * Valida un reparto por importes [shares] (personId → céntimos).
     * - participantes no vacíos, sin duplicados implícitos (mapa)
     * - cada parte > 0
     * - suma exacta = [totalCents]
     */
    fun validateCustomShares(totalCents: Long, shares: Map<Long, Long>) {
        require(totalCents > 0L) { "El total debe ser mayor que 0" }
        require(shares.isNotEmpty()) { "Debe haber al menos un participante" }
        shares.forEach { (personId, cents) ->
            require(personId > 0L) { "Persona no válida" }
            require(cents > 0L) { "Cada parte debe ser mayor que 0" }
        }
        val sum = shares.values.sumOf { it }
        require(sum == totalCents) { "La suma de las partes debe ser igual al total" }
    }

    private fun orderedForRemainder(personIds: List<Long>, preferPersonId: Long?): List<Long> {
        if (preferPersonId == null) return personIds
        val idx = personIds.indexOf(preferPersonId)
        if (idx < 0) return personIds
        return personIds.drop(idx) + personIds.take(idx)
    }
}
