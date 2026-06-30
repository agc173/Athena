#!/usr/bin/env python3
"""Audit GeoNames alternate names for the offline birthplace catalogue.

Read-only diagnostic tool: it does not modify birthplaces.csv and does not change
runtime search, ranking, loaders, repositories, UI, or generated app code.
"""

from __future__ import annotations

import argparse
import csv
import gzip
import io
import re
import sys
import unicodedata
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from statistics import mean
from typing import Iterable, Sequence

DEFAULT_CSV_PATH = Path("composeApp/src/commonMain/composeResources/files/birthplaces.csv")
DEFAULT_ALTERNATE_NAMES_PATH = Path("tools/geonames/alternateNamesV2.txt")
SUPPORTED_LANGUAGES = ("es", "en", "fr", "it", "pt", "ru", "de")
TOP_LIMIT = 30

DIAGNOSTIC_QUERIES = (
    "Moscú", "Москва", "Moscou", "Moskau",
    "Londres", "Londra", "London",
    "Roma", "Rome",
    "Nueva York", "New York",
    "Pekín", "Pékin", "Pequim", "Peking",
    "Múnich", "Munich", "München",
    "Cologne", "Köln", "Colonia",
    "Vienna", "Wien", "Viena", "Vienne",
    "Lisbon", "Lisboa", "Lisbonne",
    "Brussels", "Bruxelles", "Bruselas",
    "Amsterdam",
)

@dataclass(frozen=True)
class Birthplace:
    geoname_id: str
    city_name: str
    country_code: str

@dataclass(frozen=True)
class AlternateName:
    geoname_id: str
    language: str
    name: str
    normalized: str

@dataclass(frozen=True)
class SizeEstimate:
    search_names_column_bytes: int
    search_names_column_gzip_bytes: int
    separate_csv_bytes: int
    separate_csv_gzip_bytes: int


def configure_stdout_utf8() -> None:
    """Avoid UnicodeEncodeError on Windows PowerShell/cp1252 terminals."""
    if hasattr(sys.stdout, "reconfigure"):
        try:
            sys.stdout.reconfigure(encoding="utf-8")
        except (AttributeError, ValueError):
            pass


def normalize_alias(text: str) -> str:
    normalized = unicodedata.normalize("NFKD", text.strip().casefold())
    without_marks = "".join(char for char in normalized if not unicodedata.combining(char))
    return re.sub(r"\s+", " ", re.sub(r"[^\w]+", " ", without_marks, flags=re.UNICODE)).strip()


def read_birthplaces(path: Path) -> dict[str, Birthplace]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        return {
            row.get("geonameId", "").strip(): Birthplace(
                geoname_id=row.get("geonameId", "").strip(),
                city_name=row.get("cityName", "").strip(),
                country_code=row.get("countryCode", "").strip().upper(),
            )
            for row in reader
            if row.get("geonameId", "").strip()
        }


def iter_alternate_names(path: Path) -> Iterable[tuple[str, str, str]]:
    with path.open("r", encoding="utf-8", errors="replace", newline="") as handle:
        for line in handle:
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 4:
                continue
            _, geoname_id, language, name = parts[:4]
            yield geoname_id.strip(), language.strip().lower(), name.strip()


def collect_supported_aliases(
    alternate_names_path: Path,
    catalogue_geoname_ids: set[str],
    supported_languages: Sequence[str] = SUPPORTED_LANGUAGES,
) -> dict[str, list[AlternateName]]:
    supported = set(supported_languages)
    seen: dict[str, set[str]] = defaultdict(set)
    aliases: dict[str, list[AlternateName]] = defaultdict(list)
    for geoname_id, language, name in iter_alternate_names(alternate_names_path):
        if geoname_id not in catalogue_geoname_ids or language not in supported or not name:
            continue
        normalized = normalize_alias(name)
        if not normalized or normalized in seen[geoname_id]:
            continue
        seen[geoname_id].add(normalized)
        aliases[geoname_id].append(AlternateName(geoname_id, language, name, normalized))
    return dict(aliases)


def percentile(sorted_values: Sequence[int], percentile_value: float) -> float:
    if not sorted_values:
        return 0.0
    index = round((len(sorted_values) - 1) * percentile_value)
    return float(sorted_values[index])


def estimate_sizes(aliases_by_city: dict[str, list[AlternateName]]) -> SizeEstimate:
    column_cells = []
    separate_rows = [("geonameId", "searchNames")]
    for geoname_id in sorted(aliases_by_city):
        names = [alias.name for alias in aliases_by_city[geoname_id]]
        search_names = "|".join(names)
        column_cells.append([search_names])
        separate_rows.append((geoname_id, search_names))

    column_text = write_csv_text(column_cells)
    separate_text = write_csv_text(separate_rows)
    column_bytes = column_text.encode("utf-8")
    separate_bytes = separate_text.encode("utf-8")
    return SizeEstimate(
        search_names_column_bytes=len(column_bytes),
        search_names_column_gzip_bytes=len(gzip.compress(column_bytes)),
        separate_csv_bytes=len(separate_bytes),
        separate_csv_gzip_bytes=len(gzip.compress(separate_bytes)),
    )


def write_csv_text(rows: Sequence[Sequence[str]]) -> str:
    output = io.StringIO(newline="")
    writer = csv.writer(output)
    writer.writerows(rows)
    return output.getvalue()


def build_report(birthplaces: dict[str, Birthplace], aliases_by_city: dict[str, list[AlternateName]]) -> str:
    counts = [len(aliases_by_city.get(geoname_id, [])) for geoname_id in birthplaces]
    sorted_counts = sorted(counts)
    language_counts = Counter(alias.language for aliases in aliases_by_city.values() for alias in aliases)
    size = estimate_sizes(aliases_by_city)
    lines = [
        "# Birthplace alternate names audit",
        "",
        "Read-only audit for GeoNames alternateNamesV2 aliases limited to ATHENA supported app languages.",
        "",
        f"Catalogue cities: {len(birthplaces):,}",
        f"Cities with >=1 useful alternateName: {sum(1 for count in counts if count > 0):,}",
        f"Total normalized aliases: {sum(counts):,}",
        "",
        "## Aliases by language",
    ]
    for language in SUPPORTED_LANGUAGES:
        lines.append(f"- {language}: {language_counts[language]:,}")
    lines.extend([
        "",
        "## Aliases per city",
        f"- mean: {mean(counts) if counts else 0:.2f}",
        f"- p50: {percentile(sorted_counts, 0.50):.0f}",
        f"- p75: {percentile(sorted_counts, 0.75):.0f}",
        f"- p90: {percentile(sorted_counts, 0.90):.0f}",
        f"- p95: {percentile(sorted_counts, 0.95):.0f}",
        f"- p99: {percentile(sorted_counts, 0.99):.0f}",
        "",
        "## Top cities by alias count",
    ])
    top = sorted(aliases_by_city.items(), key=lambda item: (-len(item[1]), birthplaces[item[0]].city_name, item[0]))[:TOP_LIMIT]
    for geoname_id, aliases in top:
        city = birthplaces[geoname_id]
        sample = ", ".join(alias.name for alias in aliases[:8])
        lines.append(f"- {city.city_name} ({city.country_code}, {geoname_id}): {len(aliases)} aliases; sample: {sample}")
    lines.extend([
        "",
        "## Size estimate",
        f"- Extra CSV column payload bytes: {size.search_names_column_bytes:,}",
        f"- Extra CSV column gzip bytes: {size.search_names_column_gzip_bytes:,}",
        f"- Separate geonameId/searchNames CSV bytes: {size.separate_csv_bytes:,}",
        f"- Separate geonameId/searchNames CSV gzip bytes: {size.separate_csv_gzip_bytes:,}",
        "",
        "## Diagnostic queries",
    ])
    index = build_query_index(birthplaces, aliases_by_city)
    for query in DIAGNOSTIC_QUERIES:
        matches = index.get(normalize_alias(query), [])[:5]
        rendered = "; ".join(f"{city.city_name} ({city.country_code}, {city.geoname_id})" for city in matches) or "NO MATCH"
        lines.append(f"- {query}: {rendered}")
    return "\n".join(lines) + "\n"


def build_query_index(birthplaces: dict[str, Birthplace], aliases_by_city: dict[str, list[AlternateName]]) -> dict[str, list[Birthplace]]:
    index: dict[str, list[Birthplace]] = defaultdict(list)
    for geoname_id, city in birthplaces.items():
        for key in {normalize_alias(city.city_name), *(alias.normalized for alias in aliases_by_city.get(geoname_id, []))}:
            if key:
                index[key].append(city)
    return dict(index)


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Audit supported-language GeoNames alternate names for birthplaces.csv.")
    parser.add_argument("--birthplaces-csv", type=Path, default=DEFAULT_CSV_PATH)
    parser.add_argument("--alternate-names", type=Path, default=DEFAULT_ALTERNATE_NAMES_PATH)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    configure_stdout_utf8()
    args = parse_args(argv)
    birthplaces = read_birthplaces(args.birthplaces_csv)
    aliases = collect_supported_aliases(args.alternate_names, set(birthplaces))
    print(build_report(birthplaces, aliases))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
