package com.agc.bwitch.data.astrology.natal

import com.agc.bwitch.domain.astrology.natal.BirthDateTimeUtc
import com.agc.bwitch.domain.astrology.natal.BirthLocation
import java.io.File
import java.util.Locale
import java.util.Random
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Android-only, report-only precision audit between the validated Astronomy Engine runtime
 * and the experimental common natal calculator.
 *
 * This intentionally does not replace runtime behavior. Run with a larger sample when needed:
 * ./gradlew :shared:data:natalEnginePrecisionAudit -DnatalAuditSampleSize=1000
 *
 * The report is always written to build/reports/natal-engine-precision-report.txt.
 */
class CommonNatalEnginePrecisionAuditTest {
    @Test
    fun reportsPrecisionAgainstAstronomyEngineWithoutFailingBuild() {
        val requestedRandomSamples = System.getProperty(SampleSizeProperty)
        val effectiveRandomSamples = requestedRandomSamples
            ?.toIntOrNull()
            ?.coerceAtLeast(0)
            ?: DefaultRandomSampleSize
        val samples = manualBoundarySamples() + randomSamples(effectiveRandomSamples)
        val referenceCalculator = AstronomyEngineNatalChartCalculator()
        val commonCalculator = BasicNatalChartCalculator()

        val results = samples.map { sample ->
            val reference = referenceCalculator.calculate(sample.birthDateTimeUtc, sample.birthLocation)
            val common = commonCalculator.calculate(sample.birthDateTimeUtc, sample.birthLocation)
            AuditResult(
                sample = sample,
                sunErrorDegrees = angularDifferenceDegrees(
                    reference.sunLongitudeDegrees,
                    common.sunLongitudeDegrees,
                ),
                moonErrorDegrees = angularDifferenceDegrees(
                    reference.moonLongitudeDegrees,
                    common.moonLongitudeDegrees,
                ),
                ascendantErrorDegrees = angularDifferenceDegrees(
                    assertNotNull(reference.ascendantLongitudeDegrees),
                    assertNotNull(common.ascendantLongitudeDegrees),
                ),
            )
        }

        writeReport(
            buildReport(
                requestedRandomSamples = requestedRandomSamples,
                effectiveRandomSamples = effectiveRandomSamples,
                samples = samples,
                results = results,
            ),
        )
    }

    private fun randomSamples(count: Int): List<AuditSample> {
        val random = Random(DeterministicSeed)
        return List(count) { index ->
            val year = random.nextInt(MaxYear - MinYear + 1) + MinYear
            val month = random.nextInt(MonthsPerYear) + 1
            val day = random.nextInt(daysInMonth(year, month)) + 1
            AuditSample(
                label = "seed-$DeterministicSeed-random-${index + 1}",
                birthDateTimeUtc = BirthDateTimeUtc(
                    year = year,
                    month = month,
                    day = day,
                    hour = random.nextInt(HoursPerDay),
                    minute = random.nextInt(MinutesPerHour),
                    second = random.nextInt(SecondsPerMinute).toDouble(),
                ),
                birthLocation = BirthLocation(
                    latitudeDegrees = MinLatitudeDegrees + random.nextDouble() * (MaxLatitudeDegrees - MinLatitudeDegrees),
                    longitudeDegrees = MinLongitudeDegrees + random.nextDouble() * (MaxLongitudeDegrees - MinLongitudeDegrees),
                ),
            )
        }
    }

    private fun manualBoundarySamples(): List<AuditSample> = listOf(
        AuditSample("new-year-west-midnight", BirthDateTimeUtc(1900, 1, 1, 0, 1, 5.0), BirthLocation(0.0, -179.9)),
        AuditSample("new-year-east-midnight", BirthDateTimeUtc(2099, 12, 31, 23, 59, 55.0), BirthLocation(0.0, 179.9)),
        AuditSample("march-equinox", BirthDateTimeUtc(2000, 3, 20, 7, 35, 0.0), BirthLocation(40.4167, -3.7)),
        AuditSample("june-solstice", BirthDateTimeUtc(2024, 6, 20, 20, 51, 0.0), BirthLocation(51.5072, -0.1276)),
        AuditSample("september-equinox", BirthDateTimeUtc(2026, 9, 22, 18, 5, 30.0), BirthLocation(-33.8688, 151.2093)),
        AuditSample("december-solstice", BirthDateTimeUtc(1980, 12, 21, 10, 56, 0.0), BirthLocation(-34.6037, -58.3816)),
        AuditSample("near-north-limit", BirthDateTimeUtc(1969, 7, 20, 20, 17, 40.0), BirthLocation(65.9, -179.5)),
        AuditSample("near-south-limit", BirthDateTimeUtc(2038, 1, 19, 3, 14, 7.0), BirthLocation(-65.9, 179.5)),
    )

    private fun buildReport(
        requestedRandomSamples: String?,
        effectiveRandomSamples: Int,
        samples: List<AuditSample>,
        results: List<AuditResult>,
    ): String = buildString {
        appendLine("Common natal engine precision audit (report-only)")
        appendLine("Requested random samples: ${requestedRandomSamples ?: "<default>"}")
        appendLine("Effective random samples: $effectiveRandomSamples")
        appendLine("Reference: Android test-only AstronomyEngineNatalChartCalculator / Astronomy Engine")
        appendLine("Candidate: common BasicNatalChartCalculator")
        appendLine("Deterministic seed: $DeterministicSeed")
        appendLine("Samples: ${samples.size} (${manualBoundarySamples().size} manual + $effectiveRandomSamples random)")
        appendLine("Random range: years $MinYear-$MaxYear, latitudes $MinLatitudeDegrees..$MaxLatitudeDegrees, longitudes $MinLongitudeDegrees..$MaxLongitudeDegrees")
        appendLine("Guidance: Sun should be very close; Moon is the highest-risk body; Ascendant maxima indicate sidereal-time sensitivity.")
        appendLine(metricReport("Sun", results, AuditResult::sunErrorDegrees))
        appendLine(metricReport("Moon", results, AuditResult::moonErrorDegrees))
        appendLine(metricReport("Ascendant", results, AuditResult::ascendantErrorDegrees))
    }

    private fun metricReport(name: String, results: List<AuditResult>, selector: (AuditResult) -> Double): String {
        val errors = results.map(selector).sorted()
        val worst = results.maxBy(selector)
        return "$name: meanAbs=${errors.average().format()}°, p95=${percentile(errors, 95.0).format()}°, " +
            "p99=${percentile(errors, 99.0).format()}°, max=${selector(worst).format()}°, peor caso=${worst.sample.describe()}"
    }

    private fun writeReport(report: String) {
        val reportFile = File(
            System.getProperty(ReportPathProperty)
                ?: DefaultReportPath,
        )
        reportFile.parentFile?.mkdirs()
        reportFile.writeText(report)
    }

    private fun AuditSample.describe(): String =
        "$label @ ${birthDateTimeUtc.year}-${birthDateTimeUtc.month.pad()}-${birthDateTimeUtc.day.pad()}T" +
            "${birthDateTimeUtc.hour.pad()}:${birthDateTimeUtc.minute.pad()}:${birthDateTimeUtc.second.formatSecond()}Z " +
            "lat=${birthLocation.latitudeDegrees.format()}, lon=${birthLocation.longitudeDegrees.format()}"

    private data class AuditSample(
        val label: String,
        val birthDateTimeUtc: BirthDateTimeUtc,
        val birthLocation: BirthLocation,
    )

    private data class AuditResult(
        val sample: AuditSample,
        val sunErrorDegrees: Double,
        val moonErrorDegrees: Double,
        val ascendantErrorDegrees: Double,
    )

    private companion object {
        const val SampleSizeProperty = "natalAuditSampleSize"
        const val ReportPathProperty = "natalPrecisionAuditReportPath"
        const val DefaultReportPath = "build/reports/natal-engine-precision-report.txt"
        const val DefaultRandomSampleSize = 64
        const val DeterministicSeed = 20260630L
        const val MinYear = 1900
        const val MaxYear = 2100
        const val MinLatitudeDegrees = -66.0
        const val MaxLatitudeDegrees = 66.0
        const val MinLongitudeDegrees = -180.0
        const val MaxLongitudeDegrees = 180.0
        const val MonthsPerYear = 12
        const val HoursPerDay = 24
        const val MinutesPerHour = 60
        const val SecondsPerMinute = 60
    }
}

private fun angularDifferenceDegrees(first: Double, second: Double): Double {
    val raw = abs(first - second) % FullCircleDegrees
    return if (raw > HalfCircleDegrees) FullCircleDegrees - raw else raw
}

private fun percentile(sortedValues: List<Double>, percentile: Double): Double {
    if (sortedValues.isEmpty()) return Double.NaN
    val rank = percentile / 100.0 * (sortedValues.size - 1)
    val lower = floor(rank).toInt()
    val upper = ceil(rank).toInt()
    val fraction = rank - lower
    return sortedValues[lower] + (sortedValues[upper] - sortedValues[lower]) * fraction
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (isLeapYear(year)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private fun isLeapYear(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
private fun Int.pad(): String = toString().padStart(2, '0')
private fun Double.format(): String = "%.6f".format(Locale.US, this)
private fun Double.formatSecond(): String = if (this % 1.0 == 0.0) toInt().pad() else "%05.2f".format(Locale.US, this)

private const val FullCircleDegrees = 360.0
private const val HalfCircleDegrees = 180.0
