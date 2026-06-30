#!/usr/bin/env python3
"""Generate offline birthplace presets from a local GeoNames cities15000.txt file.

This tool intentionally performs no network access. Download GeoNames data manually,
then pass the local cities15000.txt path to this script.
"""

from __future__ import annotations

import argparse
import csv
import re
import unicodedata
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path

DEFAULT_CSV_OUTPUT = Path("composeApp/src/commonMain/composeResources/files/birthplaces.csv")
CSV_HEADER = (
    "geonameId",
    "cityName",
    "countryName",
    "countryCode",
    "latitudeDegrees",
    "longitudeDegrees",
    "timezoneId",
    "population",
    "featureCode",
    "searchNames",
)
PACKAGE_NAME = "com.agc.bwitch.ui.astrology.birthplace"
DEFAULT_KOTLIN_MAX_PRESETS = 200
DEFAULT_SEARCH_NAME_LANGUAGES = ("es", "en", "fr", "it", "pt", "ru", "de")

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
# `centro` is intentionally narrower than the terms above: many real places are
# named Centro, so only common big-city centre patterns found during catalogue
# audits are excluded by default. Add explicit `!` exclusions for any future
# ambiguous centre entries that should be removed even if allowlisted.
URBAN_CENTER_PARENT_TERMS = ("habana", "havana", "madrid")


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
    feature_code: str


@dataclass(frozen=True)
class MatchList:
    geoname_ids: set[str]
    city_country_pairs: set[tuple[str, str]]
    forced_geoname_ids: set[str]
    forced_city_country_pairs: set[tuple[str, str]]
    requested_count: int


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


def parse_cities(path: Path, countries: dict[str, str]) -> list[City]:
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
            feature_code = columns[7].strip()
            population = parse_population(columns[14], line_number)
            timezone_id = columns[17].strip()
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
                    feature_code=feature_code,
                )
            )
    return final_sort(cities)


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


def parse_search_name_languages(value: str) -> set[str]:
    languages = {language.strip().lower() for language in value.split(",") if language.strip()}
    if not languages:
        raise argparse.ArgumentTypeError("provide at least one language code")
    return languages


def normalize_search_name(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value.strip().casefold())
    without_marks = "".join(char for char in normalized if not unicodedata.combining(char))
    return re.sub(r"\s+", " ", re.sub(r"[^\w]+", " ", without_marks, flags=re.UNICODE)).strip()


def collect_search_names(path: Path, cities: list[City], languages: set[str]) -> dict[str, list[str]]:
    selected_by_id = {city.geoname_id: city for city in cities}
    aliases_by_city: dict[str, list[str]] = {city.geoname_id: [] for city in cities}
    seen_by_city: dict[str, set[str]] = {city.geoname_id: set() for city in cities}
    city_names = {city.geoname_id: normalize_search_name(city.name) for city in cities}
    with path.open("r", encoding="utf-8") as handle:
        for line_number, raw_line in enumerate(handle, start=1):
            line = raw_line.rstrip("\n")
            if not line:
                continue
            columns = line.split("\t")
            if len(columns) < 4:
                raise ValueError(
                    f"Invalid alternateNamesV2 row at line {line_number}: expected at least 4 columns"
                )
            geoname_id = columns[1].strip()
            if geoname_id not in selected_by_id:
                continue
            language = columns[2].strip().lower()
            if language not in languages:
                continue
            alias = columns[3].strip()
            normalized_alias = normalize_search_name(alias)
            if not alias or not normalized_alias or normalized_alias == city_names[geoname_id]:
                continue
            if normalized_alias in seen_by_city[geoname_id]:
                continue
            seen_by_city[geoname_id].add(normalized_alias)
            aliases_by_city[geoname_id].append(alias)
    return aliases_by_city


def parse_country_codes(value: str) -> list[str]:
    codes = [code.strip().upper() for code in value.split(",") if code.strip()]
    if not codes:
        raise argparse.ArgumentTypeError("provide at least one ISO country code")
    invalid = [code for code in codes if not re.fullmatch(r"[A-Z]{2}", code)]
    if invalid:
        raise argparse.ArgumentTypeError(f"invalid ISO country code(s): {', '.join(invalid)}")
    return codes


def parse_match_file(path: Path | None, *, allow_forced: bool) -> MatchList:
    if path is None:
        return MatchList(set(), set(), set(), set(), 0)
    geoname_ids: set[str] = set()
    city_country_pairs: set[tuple[str, str]] = set()
    forced_geoname_ids: set[str] = set()
    forced_city_country_pairs: set[tuple[str, str]] = set()
    requested_count = 0
    with path.open("r", encoding="utf-8") as handle:
        for line_number, raw_line in enumerate(handle, start=1):
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            forced = False
            if line.startswith("!"):
                if not allow_forced:
                    raise ValueError(f"Forced syntax is not supported at line {line_number}: {line}")
                forced = True
                line = line[1:].strip()
            requested_count += 1
            if "|" in line:
                city_name, country_code = [part.strip() for part in line.split("|", maxsplit=1)]
                country_code = country_code.upper()
                if not city_name or not re.fullmatch(r"[A-Z]{2}", country_code):
                    raise ValueError(f"Invalid entry at line {line_number}: expected City|CountryCode")
                target = forced_city_country_pairs if forced else city_country_pairs
                target.add((city_name.casefold(), country_code))
            elif line.isdigit():
                target = forced_geoname_ids if forced else geoname_ids
                target.add(line)
            else:
                raise ValueError(f"Invalid entry at line {line_number}: expected GeoName ID or City|CountryCode")
    return MatchList(geoname_ids, city_country_pairs, forced_geoname_ids, forced_city_country_pairs, requested_count)


def city_matches(city: City, match_list: MatchList, *, include_forced: bool = True) -> bool:
    return (
        city.geoname_id in match_list.geoname_ids
        or (city.name.casefold(), city.country_code) in match_list.city_country_pairs
        or (
            include_forced
            and (
                city.geoname_id in match_list.forced_geoname_ids
                or (city.name.casefold(), city.country_code) in match_list.forced_city_country_pairs
            )
        )
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


def is_birthplace_catalog_eligible(city: City) -> bool:
    return (
        city.feature_code.upper() not in URBAN_SUBDIVISION_FEATURE_CODES
        and not is_urban_subdivision_name(city.name)
    )


def select_matching_cities(cities: list[City], match_list: MatchList) -> list[City]:
    return [city for city in cities if city_matches(city, match_list)]


def add_city(selected: dict[str, City], city: City, global_limit: int | None = None) -> None:
    if global_limit is None or city.geoname_id in selected or len(selected) < global_limit:
        selected.setdefault(city.geoname_id, city)


def select_tiered_cities(
    all_cities: list[City],
    min_population: int | None,
    tier_a_countries: list[str],
    tier_b_countries: list[str],
    tier_a_limit: int | None,
    tier_b_limit: int | None,
    tier_c_limit: int | None,
    global_limit: int | None,
    allowlist: MatchList,
    exclude_list: MatchList,
) -> tuple[list[City], dict[str, int]]:
    selected: dict[str, City] = {}
    eligible_cities = [city for city in all_cities if is_birthplace_catalog_eligible(city)]
    allowlisted_cities = select_matching_cities(eligible_cities, allowlist)
    for city in allowlisted_cities:
        add_city(selected, city, global_limit)

    selectable = [
        city
        for city in eligible_cities
        if min_population is None or city.population >= min_population or city_matches(city, allowlist)
    ]
    cities_by_country: dict[str, list[City]] = defaultdict(list)
    for city in selectable:
        cities_by_country[city.country_code].append(city)
    for country_code in cities_by_country:
        cities_by_country[country_code] = by_population(cities_by_country[country_code])

    tier_a_set = set(tier_a_countries)
    tier_b_set = set(tier_b_countries)

    def has_capacity() -> bool:
        return global_limit is None or len(selected) < global_limit

    def add_country_slice(country_codes: list[str], limit: int | None) -> None:
        if limit is None:
            return
        for country_code in country_codes:
            if not has_capacity():
                break
            for city in cities_by_country.get(country_code, [])[:limit]:
                add_city(selected, city, global_limit)
                if not has_capacity():
                    break

    add_country_slice(tier_a_countries, tier_a_limit)
    add_country_slice(tier_b_countries, tier_b_limit)

    if tier_c_limit is not None:
        for country_code in sorted(cities_by_country):
            if country_code in tier_a_set or country_code in tier_b_set:
                continue
            if not has_capacity():
                break
            for city in cities_by_country[country_code][:tier_c_limit]:
                add_city(selected, city, global_limit)
                if not has_capacity():
                    break

    if global_limit is None:
        for city in selectable:
            add_city(selected, city)
    elif has_capacity():
        for city in by_population(selectable):
            add_city(selected, city, global_limit)
            if not has_capacity():
                break

    before_exclude = list(selected.values())
    excluded_matches = [city for city in before_exclude if city_matches(city, exclude_list)]
    forced_excluded_ids = {
        city.geoname_id
        for city in before_exclude
        if city.geoname_id in exclude_list.forced_geoname_ids
        or (city.name.casefold(), city.country_code) in exclude_list.forced_city_country_pairs
    }
    allowlisted_ids = {city.geoname_id for city in allowlisted_cities}
    selected = {
        city.geoname_id: city
        for city in before_exclude
        if not city_matches(city, exclude_list)
        or (city.geoname_id in allowlisted_ids and city.geoname_id not in forced_excluded_ids)
    }

    matched_allowlist_keys = {
        city.geoname_id for city in allowlisted_cities if city.geoname_id in allowlist.geoname_ids
    } | {
        (city.name.casefold(), city.country_code)
        for city in allowlisted_cities
        if (city.name.casefold(), city.country_code) in allowlist.city_country_pairs
    }
    requested_allowlist_keys = set(allowlist.geoname_ids) | set(allowlist.city_country_pairs)

    stats = {
        "allowlist_requested": allowlist.requested_count,
        "allowlist_matched": len(matched_allowlist_keys),
        "allowlist_missing": len(requested_allowlist_keys - matched_allowlist_keys),
        "exclude_requested": exclude_list.requested_count,
        "exclude_matched": len({city.geoname_id for city in excluded_matches}),
        "policy_excluded": len(all_cities) - len(eligible_cities),
    }
    return final_sort(list(selected.values())), stats


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


def render_csv(cities: list[City], search_names: dict[str, list[str]] | None = None) -> str:
    from io import StringIO

    buffer = StringIO()
    writer = csv.writer(buffer, lineterminator="\n")
    writer.writerow(CSV_HEADER)
    for city in cities:
        writer.writerow(
            (
                city.geoname_id,
                city.name,
                city.country_name,
                city.country_code,
                city.latitude,
                city.longitude,
                city.timezone_id,
                city.population,
                city.feature_code,
                "|".join((search_names or {}).get(city.geoname_id, [])),
            )
        )
    return buffer.getvalue()


def print_summary(cities: list[City], kotlin_output: Path | None, stats: dict[str, int], csv_output: Path | None) -> None:
    print(f"Selected {len(cities)} birthplace presets")
    if csv_output is not None:
        print(f"Generated CSV birthplace catalog at {csv_output}")
    if kotlin_output is not None:
        print(f"Generated Kotlin fallback presets at {kotlin_output}")
    print(
        "Allowlist requested/matched/missing: "
        f"{stats['allowlist_requested']}/{stats['allowlist_matched']}/{stats['allowlist_missing']}"
    )
    print(f"Excluded requested/matched: {stats['exclude_requested']}/{stats['exclude_matched']}")
    print(f"Policy-excluded urban subdivisions: {stats['policy_excluded']}")
    print("Top country counts:")
    for country_code, count in Counter(city.country_code for city in cities).most_common(20):
        print(f"  {country_code}: {count}")
    if kotlin_output is not None and kotlin_output.exists():
        print(f"Generated Kotlin file size: {kotlin_output.stat().st_size} bytes")
    if csv_output is not None and csv_output.exists():
        print(f"Generated CSV file size: {csv_output.stat().st_size} bytes")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Generate KMP birthplace presets from local GeoNames cities15000.txt."
    )
    parser.add_argument("cities15000", type=Path, help="Path to a local GeoNames cities15000.txt file")
    parser.add_argument("--country-info", type=Path, default=None, help="Optional local GeoNames countryInfo.txt for country names")
    parser.add_argument("--csv-output", type=Path, default=DEFAULT_CSV_OUTPUT, help=f"Output CSV resource file (default: {DEFAULT_CSV_OUTPUT})")
    parser.add_argument("--kotlin-output", type=Path, default=None, help="Optional Kotlin fallback/legacy output file. Disabled by default; use only for small fallback catalogues.")
    parser.add_argument("--output", dest="legacy_output", type=Path, default=None, help="Deprecated alias for --kotlin-output. Disabled by default.")
    parser.add_argument("--kotlin-max-presets", type=positive_int, default=DEFAULT_KOTLIN_MAX_PRESETS, help=f"Maximum number of selected cities allowed when writing Kotlin fallback output (default: {DEFAULT_KOTLIN_MAX_PRESETS})")
    parser.add_argument("--min-population", type=positive_int, default=None, help="Only include non-allowlisted, policy-eligible cities with at least this GeoNames population value")
    parser.add_argument("--limit", type=positive_int, default=None, help="Deprecated alias for --global-limit when --global-limit is omitted")
    parser.add_argument("--priority-country", type=parse_country_codes, default=[], help="Deprecated alias for --tier-a-country")
    parser.add_argument("--per-priority-country-limit", type=positive_int, default=None, help="Deprecated alias for --tier-a-limit")
    parser.add_argument("--per-country-limit", type=positive_int, default=None, help="Deprecated alias for --tier-c-limit")
    parser.add_argument("--tier-a-country", type=parse_country_codes, default=[], help="Comma-separated ISO country codes that receive highest reference catalogue coverage")
    parser.add_argument("--tier-b-country", type=parse_country_codes, default=[], help="Comma-separated ISO country codes that receive medium reference catalogue coverage")
    parser.add_argument("--tier-a-limit", type=positive_int, default=None, help="Include up to N largest cities for each Tier A country")
    parser.add_argument("--tier-b-limit", type=positive_int, default=None, help="Include up to N largest cities for each Tier B country")
    parser.add_argument("--tier-c-limit", type=positive_int, default=None, help="Include up to N largest cities for each non-Tier A/B country")
    parser.add_argument("--global-limit", type=positive_int, default=None, help="Fill with the largest global cities until this total preset count is reached")
    parser.add_argument("--allowlist-file", type=Path, default=None, help='Optional allowlist file with one GeoName ID or "City|CountryCode" per line')
    parser.add_argument("--exclude-file", type=Path, default=None, help='Optional exclude file with one GeoName ID or "City|CountryCode" per line; prefix with ! to override allowlist')
    parser.add_argument(
        "--alternate-names",
        type=Path,
        default=None,
        help="Optional local GeoNames alternateNamesV2.txt used to populate the searchNames CSV column",
    )
    parser.add_argument(
        "--search-name-language",
        type=parse_search_name_languages,
        default=parse_search_name_languages(",".join(DEFAULT_SEARCH_NAME_LANGUAGES)),
        help=(
            "Comma-separated GeoNames alternate-name language codes to include when "
            "--alternate-names is present (default: es,en,fr,it,pt,ru,de)"
        ),
    )
    args = parser.parse_args()

    countries = parse_country_info(args.country_info)
    global_limit = args.global_limit if args.global_limit is not None else args.limit
    tier_a_countries = args.tier_a_country or args.priority_country
    tier_a_limit = args.tier_a_limit if args.tier_a_limit is not None else args.per_priority_country_limit
    tier_c_limit = args.tier_c_limit if args.tier_c_limit is not None else args.per_country_limit
    cities, stats = select_tiered_cities(
        parse_cities(args.cities15000, countries),
        args.min_population,
        tier_a_countries,
        args.tier_b_country,
        tier_a_limit,
        args.tier_b_limit,
        tier_c_limit,
        global_limit,
        parse_match_file(args.allowlist_file, allow_forced=False),
        parse_match_file(args.exclude_file, allow_forced=True),
    )
    kotlin_output = args.kotlin_output or args.legacy_output
    if kotlin_output is not None and len(cities) > args.kotlin_max_presets:
        raise RuntimeError(
            f"Refusing to generate Kotlin fallback with {len(cities)} cities; "
            f"the safe limit is {args.kotlin_max_presets}. Large birthplace catalogues must live in "
            "the CSV resource. Re-run without --kotlin-output/--output for the runtime CSV, "
            "or lower the selection limits for a small legacy fallback."
        )
    search_names = (
        collect_search_names(args.alternate_names, cities, args.search_name_language)
        if args.alternate_names is not None
        else None
    )
    if args.csv_output is not None:
        args.csv_output.parent.mkdir(parents=True, exist_ok=True)
        csv_content = render_csv(cities, search_names)
        csv_row_count = max(0, csv_content.count("\n") - 1)
        if csv_row_count != len(cities):
            raise RuntimeError(
                f"Generated CSV row count ({csv_row_count}) does not match selected city count ({len(cities)})"
            )
        args.csv_output.write_text(csv_content, encoding="utf-8")
    if kotlin_output is not None:
        kotlin_output.parent.mkdir(parents=True, exist_ok=True)
        kotlin_output.write_text(render(cities), encoding="utf-8")
    print_summary(cities, kotlin_output, stats, args.csv_output)


if __name__ == "__main__":
    main()
