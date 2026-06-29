#!/usr/bin/env python3
"""Audit the offline birthplace CSV catalogue without changing runtime data.

The script is intentionally dependency-free so it can run anywhere the repo can run
Python. It reads the CSV resource, prints a human-readable diagnostic report, and
can optionally write the same report as Markdown.
"""

from __future__ import annotations

import argparse
import csv
import gzip
import re
import sys
import unicodedata
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence

DEFAULT_CSV_PATH = Path("composeApp/src/commonMain/composeResources/files/birthplaces.csv")
DEFAULT_MARKDOWN_PATH = Path("docs/reports/birthplace_catalog_audit.md")
TOP_LIMIT = 30
QUERY_LIMIT = 10
DEFAULT_COUNTRY_PRIORITY = sys.maxsize

URBAN_SUBDIVISION_FEATURE_CODES = {"PPLX"}
URBAN_SUBDIVISION_NAME_TERMS = (
    "administrative district",
    "arrondissement",
    "barrio",
    "borough",
    "district",
    "quarter",
    "ward",
)
URBAN_CENTER_PARENT_TERMS = ("habana", "havana", "madrid")

SEARCH_ALIASES = {
    "roma": ["rome"],
    "moscu": ["moscow"],
    "pekin": ["beijing"],
    "londres": ["london"],
    "nueva york": ["new york", "new york city"],
    "new york": ["new york city"],
}

COUNTRY_PRIORITIES = {
    "madrid": ["ES"],
    "tokyo": ["JP"],
    "moscow": ["RU"],
    "buenos aires": ["AR"],
    "bogota": ["CO"],
    "ciudad de mexico": ["MX"],
    "mexico city": ["MX"],
    "new york": ["US"],
    "los angeles": ["US"],
    "chicago": ["US"],
    "london": ["GB"],
    "paris": ["FR"],
    "berlin": ["DE"],
    "rome": ["IT"],
    "beijing": ["CN"],
    "sao paulo": ["BR"],
    "rio de janeiro": ["BR"],
}

KEY_CITIES = [
    [("Madrid", "ES")],
    [("Barcelona", "ES")],
    [("Valencia", "ES")],
    [("Seville", "ES"), ("Sevilla", "ES")],
    [("New York", "US"), ("New York City", "US")],
    [("Los Angeles", "US")],
    [("Mexico City", "MX")],
    [("Buenos Aires", "AR")],
    [("Bogotá", "CO")],
    [("São Paulo", "BR")],
    [("Rio de Janeiro", "BR")],
    [("London", "GB")],
    [("Paris", "FR")],
    [("Berlin", "DE")],
    [("Rome", "IT")],
    [("Moscow", "RU")],
    [("Tokyo", "JP")],
    [("Beijing", "CN")],
    [("Shanghai", "CN")],
    [("Sydney", "AU")],
]

TEST_QUERIES = [
    "madrid",
    "moscow",
    "tokyo",
    "buenos aires",
    "bogota",
    "new york",
    "roma",
    "moscu",
    "moscú",
    "pekin",
    "pekín",
    "londres",
    "nueva york",
]

SUSPICIOUS_TERMS = [
    "arrondissement",
    "barrio",
    "borough",
    "carabanchel",
    "centro",
    "commune",
    "comuna",
    "district",
    "downtown",
    "municipio",
    "quarter",
    "ward",
]
SUSPICIOUS_RE = re.compile(r"\b(" + "|".join(re.escape(term) for term in SUSPICIOUS_TERMS) + r")\b")


@dataclass(frozen=True)
class BirthplaceRow:
    geoname_id: str
    city_name: str
    country_name: str
    country_code: str
    latitude_degrees: str
    longitude_degrees: str
    timezone_id: str
    population: int
    feature_code: str

    @classmethod
    def from_csv(cls, row: dict[str, str]) -> "BirthplaceRow":
        return cls(
            geoname_id=row.get("geonameId", "").strip(),
            city_name=row.get("cityName", "").strip(),
            country_name=row.get("countryName", "").strip(),
            country_code=row.get("countryCode", "").strip().upper(),
            latitude_degrees=row.get("latitudeDegrees", "").strip(),
            longitude_degrees=row.get("longitudeDegrees", "").strip(),
            timezone_id=row.get("timezoneId", "").strip(),
            population=parse_int(row.get("population", "")),
            feature_code=row.get("featureCode", "").strip(),
        )


def normalize_policy_text(text: str) -> str:
    normalized = unicodedata.normalize("NFKD", text.strip().casefold())
    without_marks = "".join(char for char in normalized if not unicodedata.combining(char))
    return re.sub(r"\s+", " ", re.sub(r"[^a-z0-9]+", " ", without_marks)).strip()


def is_urban_subdivision_name(name: str) -> bool:
    normalized = normalize_policy_text(name)
    if not normalized:
        return False
    tokens = set(normalized.split())
    if any(
        re.search(rf"\b{re.escape(term)}\b", normalized)
        for term in URBAN_SUBDIVISION_NAME_TERMS
    ):
        return True
    if "centro" in tokens:
        without_centro = " ".join(token for token in normalized.split() if token != "centro")
        if without_centro in URBAN_CENTER_PARENT_TERMS:
            return True
    return False


def matches_generation_exclusion_policy(row: BirthplaceRow) -> bool:
    return (
        row.feature_code.upper() in URBAN_SUBDIVISION_FEATURE_CODES
        or is_urban_subdivision_name(row.city_name)
    )


def parse_int(value: str) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def normalize_search_text(text: str) -> str:
    """Approximate the Kotlin birthplace search normalizer in Python."""
    normalized = unicodedata.normalize("NFKD", text.strip().lower())
    without_marks = "".join(char for char in normalized if not unicodedata.combining(char))
    return without_marks.replace("ß", "ss").replace("þ", "th").replace("ð", "d")


def country_priority_for(normalized_city: str, country_code: str) -> int:
    priorities = COUNTRY_PRIORITIES.get(normalized_city)
    if not priorities:
        return DEFAULT_COUNTRY_PRIORITY
    try:
        return priorities.index(country_code.upper())
    except ValueError:
        return DEFAULT_COUNTRY_PRIORITY


def equivalent_search_queries(normalized_query: str) -> list[tuple[str, int]]:
    queries = [(normalized_query, 0)]
    for alias in SEARCH_ALIASES.get(normalized_query, []):
        normalized_alias = normalize_search_text(alias)
        if normalized_alias and all(query != normalized_alias for query, _ in queries):
            queries.append((normalized_alias, 1))
    return queries


def ranking_score(
    row: BirthplaceRow,
    normalized_query: str,
    original_index: int,
    alias_penalty: int = 0,
) -> tuple[int, int, int, int, int, int] | None:
    normalized_city = normalize_search_text(row.city_name)
    normalized_country = normalize_search_text(row.country_name)
    if normalized_city == normalized_query:
        match_tier = 0
    elif normalized_city.startswith(normalized_query):
        match_tier = 1
    elif normalized_query in normalized_city:
        match_tier = 2
    elif normalized_query in normalized_country:
        match_tier = 3
    else:
        return None

    return (
        match_tier + alias_penalty,
        match_tier,
        country_priority_for(normalized_city, row.country_code),
        len(normalized_city),
        len(normalized_country),
        original_index,
    )


def read_rows(csv_path: Path) -> list[BirthplaceRow]:
    with csv_path.open("r", encoding="utf-8", newline="") as handle:
        return [BirthplaceRow.from_csv(row) for row in csv.DictReader(handle)]


def format_bytes(size: int) -> str:
    return f"{size:,} bytes ({size / 1024:.1f} KiB)"


def find_key_city(rows: Sequence[BirthplaceRow], alternatives: Sequence[tuple[str, str]]) -> list[BirthplaceRow]:
    normalized_alternatives = {(normalize_search_text(city), country.upper()) for city, country in alternatives}
    return [
        row for row in rows
        if (normalize_search_text(row.city_name), row.country_code) in normalized_alternatives
    ]


def format_row(row: BirthplaceRow) -> str:
    return (
        f"{row.city_name}, {row.country_code} "
        f"({row.country_name}; pop {row.population:,}; {row.timezone_id}; id {row.geoname_id})"
    )


def build_report(csv_path: Path, rows: Sequence[BirthplaceRow]) -> str:
    raw_bytes = csv_path.read_bytes()
    gzip_bytes = gzip.compress(raw_bytes, compresslevel=9)
    country_counts = Counter(row.country_code or "<missing>" for row in rows)

    city_countries: dict[str, set[str]] = defaultdict(set)
    city_counts: Counter[str] = Counter()
    display_city_name: dict[str, str] = {}
    for row in rows:
        normalized_city = normalize_search_text(row.city_name)
        city_countries[normalized_city].add(row.country_code or "<missing>")
        city_counts[normalized_city] += 1
        display_city_name.setdefault(normalized_city, row.city_name)

    repeated_across_countries = {
        city: countries for city, countries in city_countries.items() if len(countries) > 1
    }
    suspicious_rows = [row for row in rows if matches_generation_exclusion_policy(row)]

    lines: list[str] = []
    lines.append("# Birthplace catalog audit")
    lines.append("")
    lines.append(f"CSV: `{csv_path}`")
    lines.append("")
    lines.append("## 1. Size")
    lines.append(f"- CSV bytes: {format_bytes(len(raw_bytes))}")
    lines.append(f"- Approx. gzip size: {format_bytes(len(gzip_bytes))}")
    lines.append(f"- Total rows: {len(rows):,}")
    lines.append("")

    lines.append("## 2. Coverage by countryCode")
    lines.append(f"- Distinct country codes: {len(country_counts):,}")
    lines.append("- Top 30 countries by entries:")
    for code, count in country_counts.most_common(TOP_LIMIT):
        lines.append(f"  - {code}: {count:,}")
    lines.append("")

    lines.append("## 3. Duplicates / homonyms")
    lines.append(f"- City names repeated in multiple countries: {len(repeated_across_countries):,}")
    lines.append("- Top 30 cityName values by repetition:")
    for city, count in city_counts.most_common(TOP_LIMIT):
        countries = ", ".join(sorted(city_countries[city]))
        lines.append(f"  - {display_city_name[city]}: {count:,} entries across {countries}")
    lines.append("")

    lines.append("## 4. Suspicious entries")
    lines.append(
        "- Mirrors the generator-only urban subdivision exclusion policy; "
        "this audit remains read-only and changes no runtime data."
    )
    lines.append(f"- Suspicious rows matched: {len(suspicious_rows):,}")
    for row in suspicious_rows[:TOP_LIMIT]:
        reasons = []
        if row.feature_code.upper() in URBAN_SUBDIVISION_FEATURE_CODES:
            reasons.append(f"featureCode={row.feature_code}")
        if is_urban_subdivision_name(row.city_name):
            reasons.append("name-policy")
        lines.append(f"  - {format_row(row)} — reasons: {', '.join(reasons)}")
    if len(suspicious_rows) > TOP_LIMIT:
        lines.append(f"  - … {len(suspicious_rows) - TOP_LIMIT:,} more")
    lines.append("")

    lines.append("## 5. Key cities")
    for alternatives in KEY_CITIES:
        label = " or ".join(f"{city}/{country}" for city, country in alternatives)
        matches = find_key_city(rows, alternatives)
        if matches:
            lines.append(f"- OK {label}: {format_row(matches[0])}")
        else:
            lines.append(f"- MISSING {label}")
    lines.append("")

    lines.append("## 6. Test queries")
    lines.append("Python approximation of the runtime normalization/ranking; report-only.")
    for query in TEST_QUERIES:
        normalized_query = normalize_search_text(query)
        ranked = sorted(
            (
                (
                    min(
                        (
                            score
                            for equivalent_query, alias_penalty in equivalent_search_queries(normalized_query)
                            if (score := ranking_score(row, equivalent_query, index, alias_penalty)) is not None
                        ),
                        default=None,
                    ),
                    row,
                )
                for index, row in enumerate(rows)
            ),
            key=lambda item: item[0] if item[0] is not None else (sys.maxsize,),
        )
        matches = [row for score, row in ranked if score is not None][:QUERY_LIMIT]
        lines.append(f"- `{query}` → {len(matches)} shown")
        for row in matches:
            lines.append(f"  - {format_row(row)}")
    lines.append("")
    lines.append("## Notes")
    lines.append("- This script does not modify the catalogue or runtime code.")
    lines.append("- Suspicious-entry detection is intentionally broad and diagnostic only.")
    return "\n".join(lines)


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Audit the birthplace CSV catalogue.")
    parser.add_argument("--csv", type=Path, default=DEFAULT_CSV_PATH, help=f"CSV path (default: {DEFAULT_CSV_PATH})")
    parser.add_argument(
        "--markdown-output",
        type=Path,
        help=f"Optional Markdown report path, for example {DEFAULT_MARKDOWN_PATH}",
    )
    return parser.parse_args(argv)


def main(argv: Sequence[str]) -> int:
    args = parse_args(argv)
    csv_path = args.csv
    if not csv_path.exists():
        print(f"CSV not found: {csv_path}", file=sys.stderr)
        return 1

    rows = read_rows(csv_path)
    report = build_report(csv_path, rows)
    print(report)

    if args.markdown_output:
        args.markdown_output.parent.mkdir(parents=True, exist_ok=True)
        args.markdown_output.write_text(report + "\n", encoding="utf-8")
        print(f"\nWrote Markdown report: {args.markdown_output}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
