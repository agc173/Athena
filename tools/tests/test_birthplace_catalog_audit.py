import tempfile
import unittest
from pathlib import Path

from tools.audit_birthplace_catalog import BirthplaceRow, build_report, find_key_city


class BirthplaceCatalogAuditTest(unittest.TestCase):
    def test_new_york_city_counts_as_new_york_us_key_city_alternative(self):
        rows = [
            BirthplaceRow(
                geoname_id="5128581",
                city_name="New York City",
                country_name="United States",
                country_code="US",
                latitude_degrees="40.7128",
                longitude_degrees="-74.0060",
                timezone_id="America/New_York",
                population=8_804_190,
                feature_code="PPL",
            )
        ]

        self.assertEqual(rows, find_key_city(rows, [("New York", "US"), ("New York City", "US")]))

    def test_report_does_not_mark_new_york_us_missing_when_new_york_city_exists(self):
        csv_text = "\n".join(
            [
                "geonameId,cityName,countryName,countryCode,latitudeDegrees,longitudeDegrees,timezoneId,population,featureCode",
                "5128581,New York City,United States,US,40.7128,-74.0060,America/New_York,8804190,PPL",
            ]
        )
        with tempfile.TemporaryDirectory() as temp_dir:
            csv_path = Path(temp_dir) / "birthplaces.csv"
            csv_path.write_text(csv_text, encoding="utf-8")
            rows = [
                BirthplaceRow(
                    geoname_id="5128581",
                    city_name="New York City",
                    country_name="United States",
                    country_code="US",
                    latitude_degrees="40.7128",
                    longitude_degrees="-74.0060",
                    timezone_id="America/New_York",
                    population=8_804_190,
                    feature_code="PPL",
                )
            ]

            report = build_report(csv_path, rows)

        self.assertIn("OK New York/US or New York City/US", report)
        self.assertNotIn("MISSING New York/US", report)


if __name__ == "__main__":
    unittest.main()
