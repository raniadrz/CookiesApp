package com.crumbcodex.crk.domain.usecase

import com.crumbcodex.crk.domain.model.Build
import com.crumbcodex.crk.domain.model.CandyKind
import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.OwnedCookie
import com.crumbcodex.crk.domain.model.Priority
import com.crumbcodex.crk.domain.model.formatStat

enum class CheckStatus { MET, PARTIAL, MISSING }

data class ComparisonItem(
    val label: String,
    val current: String,
    val target: String,
    val status: CheckStatus,
    val progress: Float,
)

data class BuildComparison(val items: List<ComparisonItem>) {
    val met: Int get() = items.count { it.status == CheckStatus.MET }
    val total: Int get() = items.size
    val overall: Float get() = if (items.isEmpty()) 0f else items.map { it.progress }.average().toFloat()
}

/** Compares a user's saved in-game setup against the recommended build benchmarks. */
class CompareBuildUseCase {

    operator fun invoke(cookie: Cookie, build: Build, owned: OwnedCookie): BuildComparison {
        val items = mutableListOf<ComparisonItem>()

        // Toppings: count how many of the recommended pieces are equipped.
        val wanted = build.toppings.sumOf { it.count }
        if (wanted > 0) {
            val matched = build.toppings.sumOf { rec ->
                minOf(rec.count, owned.toppings.filter { it.type == rec.type }.sumOf { it.count })
            }
            val target = build.toppings.joinToString(" + ") { "${it.count}× ${it.type.label}" }
            val current = if (owned.toppings.isEmpty()) "None saved"
            else owned.toppings.joinToString(" + ") { "${it.count}× ${it.type.label}" }
            items += ComparisonItem("Toppings", current, target, statusOf(matched.toFloat() / wanted), matched.toFloat() / wanted)
        }

        // Substats vs thresholds.
        build.substats.sortedBy { it.priority }.forEach { sub ->
            val value = owned.currentStats[sub.stat]
            val unit = if (sub.stat.isPercent) "%" else ""
            val progress = when {
                value == null -> 0f
                sub.min <= 0.0 -> 1f
                else -> (value / sub.min).toFloat().coerceIn(0f, 1f)
            }
            items += ComparisonItem(
                label = sub.stat.label,
                current = value?.let { it.formatStat() + unit } ?: "—",
                target = sub.targetLabel,
                status = statusOf(progress),
                progress = progress,
            )
        }

        // Beascuit: element and grade.
        val rec = build.beascuit
        var beascuitScore = 0f
        var parts = 0
        parts++; if (owned.beascuitGrade != null && owned.beascuitGrade.sortOrder <= rec.grade.sortOrder) beascuitScore += 1f
        if (rec.element != null) { parts++; if (owned.beascuitElement == rec.element) beascuitScore += 1f }
        val beascuitProgress = beascuitScore / parts
        items += ComparisonItem(
            label = "Beascuit",
            current = listOfNotNull(owned.beascuitGrade?.label, owned.beascuitElement?.label).joinToString(" · ").ifEmpty { "None saved" },
            target = listOfNotNull(rec.grade.label, rec.element?.label).joinToString(" · "),
            status = statusOf(beascuitProgress),
            progress = beascuitProgress,
        )

        // Magic Candy / Crystal Jam.
        if (cookie.magicCandy.kind != CandyKind.NONE && cookie.magicCandy.priority != Priority.SKIP) {
            val targetLevel = when (cookie.magicCandy.priority) {
                Priority.HIGH -> OwnedCookie.MAX_CANDY_LEVEL
                Priority.MEDIUM -> 20
                else -> 10
            }
            val p = (owned.candyLevel.toFloat() / targetLevel).coerceIn(0f, 1f)
            items += ComparisonItem(cookie.magicCandy.kind.label, "Lv.${owned.candyLevel}", "Lv.$targetLevel+", statusOf(p), p)
        }

        // User-defined checklist entries.
        owned.checklist.forEach { (label, done) ->
            items += ComparisonItem(label, if (done) "Done" else "To do", "Done", if (done) CheckStatus.MET else CheckStatus.MISSING, if (done) 1f else 0f)
        }
        return BuildComparison(items)
    }

    private fun statusOf(progress: Float) = when {
        progress >= 0.999f -> CheckStatus.MET
        progress > 0f -> CheckStatus.PARTIAL
        else -> CheckStatus.MISSING
    }
}
