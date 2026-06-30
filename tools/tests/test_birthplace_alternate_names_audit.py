import tempfile
import unittest
from pathlib import Path

from tools.audit_birthplace_alternate_names import (
    collect_supported_aliases,
    estimate_sizes,
    read_birthplaces,
)


class BirthplaceAlternateNamesAuditTest(unittest.TestCase):
    def write_fixture(self, temp_dir: str, csv_text: str, alternate_text: str):
        csv_path = Path(temp_dir) / "birthplaces.csv"
        alt_path = Path(temp_dir) / "alternateNamesV2.txt"
        csv_path.write_text(csv_text, encoding="utf-8")
        alt_path.write_text(alternate_text, encoding="utf-8")
        return csv_path, alt_path

    def test_filters_alternate_names_by_geoname_id_present_in_csv(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            csv_path, alt_path = self.write_fixture(
                temp_dir,
                "geonameId,cityName,countryName,countryCode,latitudeDegrees,longitudeDegrees,timezoneId,population,featureCode\n1,Moscow,Russia,RU,0,0,UTC,1,PPLC\n",
                "10\t1\tes\tMoscú\t\t\t\t\t\t\n11\t2\tes\tLondres\t\t\t\t\t\t\n",
            )
            birthplaces = read_birthplaces(csv_path)
            aliases = collect_supported_aliases(alt_path, set(birthplaces))

        self.assertEqual(["Moscú"], [alias.name for alias in aliases["1"]])
        self.assertNotIn("2", aliases)

    def test_filters_by_supported_languages_and_ignores_unsupported_languages(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            csv_path, alt_path = self.write_fixture(
                temp_dir,
                "geonameId,cityName,countryName,countryCode,latitudeDegrees,longitudeDegrees,timezoneId,population,featureCode\n1,Beijing,China,CN,0,0,UTC,1,PPLC\n",
                "10\t1\tfr\tPékin\t\t\t\t\t\t\n11\t1\tja\t北京\t\t\t\t\t\t\n12\t1\tpt\tPequim\t\t\t\t\t\t\n",
            )
            birthplaces = read_birthplaces(csv_path)
            aliases = collect_supported_aliases(alt_path, set(birthplaces))

        self.assertEqual(["fr", "pt"], [alias.language for alias in aliases["1"]])
        self.assertEqual(["Pékin", "Pequim"], [alias.name for alias in aliases["1"]])

    def test_deduplicates_aliases_by_normalized_value_and_ignores_empty_aliases(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            csv_path, alt_path = self.write_fixture(
                temp_dir,
                "geonameId,cityName,countryName,countryCode,latitudeDegrees,longitudeDegrees,timezoneId,population,featureCode\n1,Munich,Germany,DE,0,0,UTC,1,PPL\n",
                "10\t1\tde\tMünchen\t\t\t\t\t\t\n11\t1\ten\tMunchen\t\t\t\t\t\t\n12\t1\tes\t   \t\t\t\t\t\t\n",
            )
            birthplaces = read_birthplaces(csv_path)
            aliases = collect_supported_aliases(alt_path, set(birthplaces))

        self.assertEqual(1, len(aliases["1"]))
        self.assertEqual("München", aliases["1"][0].name)

    def test_calculates_size_estimate(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            csv_path, alt_path = self.write_fixture(
                temp_dir,
                "geonameId,cityName,countryName,countryCode,latitudeDegrees,longitudeDegrees,timezoneId,population,featureCode\n1,Rome,Italy,IT,0,0,UTC,1,PPLC\n",
                "10\t1\ten\tRome\t\t\t\t\t\t\n11\t1\tit\tRoma\t\t\t\t\t\t\n",
            )
            birthplaces = read_birthplaces(csv_path)
            aliases = collect_supported_aliases(alt_path, set(birthplaces))
            estimate = estimate_sizes(aliases)

        self.assertGreater(estimate.search_names_column_bytes, 0)
        self.assertGreater(estimate.search_names_column_gzip_bytes, 0)
        self.assertGreater(estimate.separate_csv_bytes, estimate.search_names_column_bytes)
        self.assertGreater(estimate.separate_csv_gzip_bytes, 0)


if __name__ == "__main__":
    unittest.main()
