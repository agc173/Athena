#!/usr/bin/env python3
"""Generate offline birthplace presets from a local GeoNames cities15000.txt file.

This tool intentionally performs no network access. Download GeoNames data manually,
then pass the local cities15000.txt path to this script.
"""

from __future__ import annotations

import argparse
import re
import unicodedata
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

DEFAULT_OUTPUT = Path(
    "composeApp/src/commonMain/kotlin/com/agc/bwitch/ui/astrology/birthplace/BirthplacePresets.generated.kt"
)
PACKAGE_NAME = "com.agc.bwitch.ui.astrology.birthplace"


@dataclass(frozen=True)
class City:
    geoname_id: str
    name: str
    country_code: str
    country_name: str
    latitude: str
    longitude: str
    timezone_id: str
    population: int


def parse_country_info(path: Path | None) -> dict[str, str]:
    if path is None:
        return {}
    countries: dict[str, str] = {}
    with path.open("r", encoding="utf-8") as handle:
        for raw_line in handle:
            line = raw_line.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            columns = line.split("\t")
            if len(columns) > 4 and columns[0]:
                countries[columns[0]] = columns[4]
    return countries


def parse_population(value: str, line_number: int) -> int:
    if not value.strip():
        return 0
    try:
        return int(value)
    except ValueError as exc:
        raise ValueError(f"Invalid population at line {line_number}: {value!r}") from exc


def parse_cities(path: Path, countries: dict[str, str], min_population: int | None) -> list[City]:
    cities: list[City] = []
    with path.open("r", encoding="utf-8") as handle:
        for line_number, raw_line in enumerate(handle, start=1):
            line = raw_line.rstrip("\n")
            if not line:
                continue
            columns = line.split("\t")
            if len(columns) < 19:
                raise ValueError(
                    f"Invalid cities15000 row at line {line_number}: expected at least 19 columns"
                )
            geoname_id = columns[0].strip()
            name = columns[1].strip()
            latitude = columns[4].strip()
            longitude = columns[5].strip()
            country_code = columns[8].strip().upper()
            population = parse_population(columns[14], line_number)
            timezone_id = columns[17].strip()
            if min_population is not None and population < min_population:
                continue
            if not (geoname_id and name and latitude and longitude and country_code and timezone_id):
                continue
            cities.append(
                City(
                    geoname_id=geoname_id,
                    name=name,
                    country_code=country_code,
                    country_name=countries.get(country_code, country_code),
                    latitude=latitude,
                    longitude=longitude,
                    timezone_id=timezone_id,
                    population=population,
                )
            )
    return sorted(
        cities,
        key=lambda city: (city.country_name.casefold(), city.name.casefold(), city.geoname_id),
    )


def slug(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    normalized = normalized.lower().replace("&", " and ")
    normalized = re.sub(r"[^a-z0-9]+", "-", normalized)
    return normalized.strip("-") or "city"


def kt_string(value: str) -> str:
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'


def kt_double_literal(value: str) -> str:
    stripped = value.strip()
    if not stripped:
        raise ValueError("Kotlin Double literal cannot be empty")
    return stripped if any(marker in stripped.lower() for marker in (".", "e")) else f"{stripped}.0"


def by_population(cities: list[City]) -> list[City]:
    return sorted(
        cities,
        key=lambda city: (
            -city.population,
            city.country_name.casefold(),
            city.name.casefold(),
            city.geoname_id,
        ),
    )


def final_sort(cities: list[City]) -> list[City]:
    return sorted(
        cities,
        key=lambda city: (city.country_name.casefold(), city.name.casefold(), city.geoname_id),
    )


def parse_country_codes(value: str) -> list[str]:
    codes = [code.strip().upper() for code in value.split(",") if code.strip()]
    if not codes:
        raise argparse.ArgumentTypeError("provide at least one ISO country code")
    invalid = [code for code in codes if not re.fullmatch(r"[A-Z]{2}", code)]
    if invalid:
        raise argparse.ArgumentTypeError(f"invalid ISO country code(s): {', '.join(invalid)}")
    return codes


def parse_allowlist(path: Path | None) -> tuple[set[str], set[tuple[str, str]]]:
    if path is None:
        return set(), set()
    geoname_ids: set[str] = set()
    city_country_pairs: set[tuple[str, str]] = set()
    with path.open("r", encoding="utf-8") as handle:
        for line_number, raw_line in enumerate(handle, start=1):
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            if "|" in line:
                city_name, country_code = [part.strip() for part in line.split("|", maxsplit=1)]
                country_code = country_code.upper()
                if not city_name or not re.fullmatch(r"[A-Z]{2}", country_code):
                    raise ValueError(
                        f"Invalid allowlist entry at line {line_number}: expected City|CountryCode"
                    )
                city_country_pairs.add((city_name.casefold(), country_code))
            elif line.isdigit():
                geoname_ids.add(line)
            else:
                raise ValueError(
                    f"Invalid allowlist entry at line {line_number}: expected GeoName ID or City|CountryCode"
                )
    return geoname_ids, city_country_pairs


def select_allowlisted_cities(cities: list[City], allowlist_file: Path | None) -> list[City]:
    geoname_ids, city_country_pairs = parse_allowlist(allowlist_file)
    if not geoname_ids and not city_country_pairs:
        return []
    return [
        city
        for city in cities
        if city.geoname_id in geoname_ids
        or (city.name.casefold(), city.country_code) in city_country_pairs
    ]


def add_city(selected: dict[str, City], city: City) -> None:
    selected.setdefault(city.geoname_id, city)


def select_weighted_cities(
    cities: list[City],
    priority_countries: list[str],
    per_priority_country_limit: int | None,
    per_country_limit: int | None,
    global_limit: int | None,
    allowlist_file: Path | None,
) -> list[City]:
    selected: dict[str, City] = {}
    for city in select_allowlisted_cities(cities, allowlist_file):
        add_city(selected, city)

    cities_by_country: dict[str, list[City]] = defaultdict(list)
    for city in cities:
        cities_by_country[city.country_code].append(city)
    for country_code in cities_by_country:
        cities_by_country[country_code] = by_population(cities_by_country[country_code])

    def has_capacity() -> bool:
        return global_limit is None or len(selected) < global_limit

    priority_country_set = set(priority_countries)
    if per_priority_country_limit is not None:
        for country_code in priority_countries:
            if not has_capacity():
                break
            for city in cities_by_country.get(country_code, [])[:per_priority_country_limit]:
                if not has_capacity() and city.geoname_id not in selected:
                    break
                add_city(selected, city)

    if per_country_limit is not None:
        for country_code in sorted(cities_by_country):
            if not has_capacity():
                break
            if country_code in priority_country_set:
                continue
            for city in cities_by_country[country_code][:per_country_limit]:
                if not has_capacity() and city.geoname_id not in selected:
                    break
                add_city(selected, city)

    if global_limit is None:
        for city in cities:
            add_city(selected, city)
    elif has_capacity():
        for city in by_population(cities):
            if not has_capacity() and city.geoname_id not in selected:
                break
            add_city(selected, city)

    return final_sort(list(selected.values()))


def positive_int(value: str) -> int:
    parsed = int(value)
    if parsed < 1:
        raise argparse.ArgumentTypeError("value must be greater than 0")
    return parsed


def render(cities: list[City]) -> str:
    lines = [
        "// Generated by tools/generate_birthplace_presets.py. Do not edit manually.",
        f"package {PACKAGE_NAME}",
        "",
        "import com.agc.bwitch.domain.astrology.natal.BirthplacePreset",
        "",
        "internal val GeneratedBirthplacePresets: List<BirthplacePreset> = listOf(",
    ]
    for city in cities:
        preset_id = f"{slug(city.name)}-{city.country_code.lower()}-{city.geoname_id}"
        lines.append(
            "    BirthplacePreset("
            f"id = {kt_string(preset_id)}, "
            f"cityName = {kt_string(city.name)}, "
            f"countryName = {kt_string(city.country_name)}, "
            f"latitudeDegrees = {kt_double_literal(city.latitude)}, "
            f"longitudeDegrees = {kt_double_literal(city.longitude)}, "
            f"timezoneId = {kt_string(city.timezone_id)}, "
            f"countryCode = {kt_string(city.country_code)}"
            "),"
        )
    lines.append(")")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Generate KMP birthplace presets from local GeoNames cities15000.txt."
    )
    parser.add_argument("cities15000", type=Path, help="Path to a local GeoNames cities15000.txt file")
    parser.add_argument(
        "--country-info",
        type=Path,
        default=None,
        help="Optional local GeoNames countryInfo.txt for country names",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT,
        help=f"Output Kotlin file (default: {DEFAULT_OUTPUT})",
    )
    parser.add_argument(
        "--min-population",
        type=positive_int,
        default=None,
        help="Only include cities with at least this GeoNames population value",
    )
    parser.add_argument(
        "--limit",
        type=positive_int,
        default=None,
        help="Deprecated alias for --global-limit when --global-limit is omitted",
    )
    parser.add_argument(
        "--priority-country",
        type=parse_country_codes,
        default=[],
        help="Comma-separated ISO country codes that receive weighted birthplace coverage",
    )
    parser.add_argument(
        "--per-priority-country-limit",
        type=positive_int,
        default=None,
        help="Include up to N largest cities for each --priority-country value",
    )
    parser.add_argument(
        "--per-country-limit",
        type=positive_int,
        default=None,
        help="Include up to N largest cities for each non-priority country",
    )
    parser.add_argument(
        "--global-limit",
        type=positive_int,
        default=None,
        help="Fill with the largest global cities until this total preset count is reached",
    )
    parser.add_argument(
        "--allowlist-file",
        type=Path,
        default=None,
        help='Optional allowlist file with one GeoName ID or "City|CountryCode" per line',
    )
    args = parser.parse_args()

    countries = parse_country_info(args.country_info)
    global_limit = args.global_limit if args.global_limit is not None else args.limit
    cities = select_weighted_cities(
        parse_cities(args.cities15000, countries, args.min_population),
        args.priority_country,
        args.per_priority_country_limit,
        args.per_country_limit,
        global_limit,
        args.allowlist_file,
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(render(cities), encoding="utf-8")
    print(f"Generated {len(cities)} birthplace presets at {args.output}")


if __name__ == "__main__":
    main()
