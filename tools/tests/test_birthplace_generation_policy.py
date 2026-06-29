import unittest

from tools.generate_birthplace_presets import (
    City,
    MatchList,
    is_birthplace_catalog_eligible,
    select_tiered_cities,
)


def city(name, country_code, feature_code="PPL", population=100_000, geoname_id=None):
    return City(
        geoname_id=geoname_id or f"id-{name}-{country_code}",
        name=name,
        country_code=country_code,
        country_name=country_code,
        latitude="0",
        longitude="0",
        timezone_id="UTC",
        population=population,
        feature_code=feature_code,
    )


EMPTY_MATCH_LIST = MatchList(set(), set(), set(), set(), 0)


class BirthplaceGenerationPolicyTest(unittest.TestCase):
    def test_city_policy_keeps_major_cities_and_excludes_urban_subdivisions(self):
        cases = [
            ("Madrid", "ES", "PPLC", True),
            ("Paris", "FR", "PPLC", True),
            ("Carabanchel", "ES", "PPLX", False),
            ("Madrid Centro", "ES", "PPLX", False),
            ("Paris 10e Arrondissement", "FR", "PPLX", False),
            ("Tuggeranong Administrative District", "AU", "PPLX", False),
            ("Centro Habana", "CU", "PPLX", False),
        ]

        for name, country_code, feature_code, expected in cases:
            with self.subTest(name=name):
                self.assertEqual(
                    expected,
                    is_birthplace_catalog_eligible(city(name, country_code, feature_code)),
                )

    def test_pplx_feature_code_is_excluded_even_without_name_term(self):
        self.assertFalse(is_birthplace_catalog_eligible(city("Example", "US", feature_code="PPLX")))

    def test_default_mode_selects_all_eligible_cities(self):
        madrid = city("Madrid", "ES", population=3_000_000, geoname_id="1")
        amsterdam = city("Amsterdam", "NL", population=900_000, geoname_id="2")
        tiny = city("Small Eligible City", "ZZ", population=15_000, geoname_id="3")
        district = city("Example District", "US", feature_code="PPL", population=200_000, geoname_id="4")

        selected, stats = select_tiered_cities(
            [district, tiny, amsterdam, madrid],
            min_population=None,
            tier_a_countries=[],
            tier_b_countries=[],
            tier_a_limit=None,
            tier_b_limit=None,
            tier_c_limit=None,
            global_limit=None,
            allowlist=EMPTY_MATCH_LIST,
            exclude_list=EMPTY_MATCH_LIST,
        )

        self.assertEqual([madrid, amsterdam, tiny], selected)
        self.assertEqual(1, stats["policy_excluded"])

    def test_default_mode_keeps_multiple_countries_without_per_country_limit(self):
        cities = [
            city("Spain One", "ES", population=100_000, geoname_id="1"),
            city("Spain Two", "ES", population=90_000, geoname_id="2"),
            city("Us One", "US", population=80_000, geoname_id="3"),
            city("Us Two", "US", population=70_000, geoname_id="4"),
            city("Netherlands One", "NL", population=60_000, geoname_id="5"),
        ]

        selected, _ = select_tiered_cities(
            cities,
            min_population=None,
            tier_a_countries=[],
            tier_b_countries=[],
            tier_a_limit=None,
            tier_b_limit=None,
            tier_c_limit=None,
            global_limit=None,
            allowlist=EMPTY_MATCH_LIST,
            exclude_list=EMPTY_MATCH_LIST,
        )

        self.assertEqual([cities[0], cities[1], cities[4], cities[2], cities[3]], selected)

    def test_legacy_global_limit_still_compacts_when_explicit(self):
        cities = [
            city("Largest", "US", population=3_000_000, geoname_id="1"),
            city("Middle", "ES", population=2_000_000, geoname_id="2"),
            city("Smallest", "NL", population=1_000_000, geoname_id="3"),
        ]

        selected, _ = select_tiered_cities(
            cities,
            min_population=None,
            tier_a_countries=[],
            tier_b_countries=[],
            tier_a_limit=None,
            tier_b_limit=None,
            tier_c_limit=None,
            global_limit=2,
            allowlist=EMPTY_MATCH_LIST,
            exclude_list=EMPTY_MATCH_LIST,
        )

        self.assertEqual([cities[1], cities[0]], selected)

    def test_allowlist_does_not_override_urban_subdivision_policy(self):
        madrid = city("Madrid", "ES", "PPLC", population=3_000_000, geoname_id="1")
        paris = city("Paris", "FR", "PPLC", population=2_000_000, geoname_id="2")
        madrid_centro = city("Madrid Centro", "ES", "PPLX", population=150_000, geoname_id="3")
        carabanchel = city("Carabanchel", "ES", "PPLX", population=250_000, geoname_id="4")
        allowlist = MatchList(
            set(),
            {
                ("madrid", "ES"),
                ("madrid centro", "ES"),
                ("carabanchel", "ES"),
            },
            set(),
            set(),
            3,
        )

        selected, stats = select_tiered_cities(
            [madrid, paris, madrid_centro, carabanchel],
            min_population=None,
            tier_a_countries=[],
            tier_b_countries=[],
            tier_a_limit=None,
            tier_b_limit=None,
            tier_c_limit=None,
            global_limit=None,
            allowlist=allowlist,
            exclude_list=EMPTY_MATCH_LIST,
        )

        self.assertEqual([madrid, paris], selected)
        self.assertEqual(2, stats["policy_excluded"])
        self.assertEqual(1, stats["allowlist_matched"])
        self.assertEqual(2, stats["allowlist_missing"])

    def test_forced_exclude_still_wins_over_allowlist_for_eligible_city(self):
        madrid = city("Madrid", "ES", "PPLC", population=3_000_000, geoname_id="1")
        allowlist = MatchList(set(), {("madrid", "ES")}, set(), set(), 1)
        exclude_list = MatchList(set(), set(), {"1"}, set(), 1)

        selected, _ = select_tiered_cities(
            [madrid],
            min_population=None,
            tier_a_countries=[],
            tier_b_countries=[],
            tier_a_limit=None,
            tier_b_limit=None,
            tier_c_limit=None,
            global_limit=None,
            allowlist=allowlist,
            exclude_list=exclude_list,
        )

        self.assertEqual([], selected)


if __name__ == "__main__":
    unittest.main()
