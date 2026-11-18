"""Offline regression tests for emulator navigation hit target selection.

Run: python3 -m unittest discover -s scripts -p 'test_tap_emulator_navigation.py'
"""
import importlib.util
from pathlib import Path
import unittest


SCRIPT = Path(__file__).with_name("tap-emulator-navigation.py")
spec = importlib.util.spec_from_file_location("tap_emulator_navigation", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def xml(node):
    return '<hierarchy>' + node + '</hierarchy>'


class NavigationTargetTests(unittest.TestCase):
    def test_visible_label_center(self):
        self.assertEqual(
            module.center_for_label(xml('<node text="Verbindungen" bounds="[10,20][110,60]"/>'), "Verbindungen"),
            (60, 40),
        )

    def test_hidden_parent_rejects_visible_child(self):
        markup = '<node visible-to-user=" FALSE "><node text="Start" bounds="[0,0][100,100]"/></node>'
        with self.assertRaises(ValueError):
            module.center_for_label(xml(markup), "Start")

    def test_disabled_parent_rejects_child(self):
        markup = '<node enabled="false"><node content-desc="Meine Fahrt" bounds="[0,0][100,100]"/></node>'
        with self.assertRaises(ValueError):
            module.center_for_label(xml(markup), "Meine Fahrt")

    def test_skips_hidden_match_and_uses_visible_match(self):
        markup = ('<node displayed="false" text="Start" bounds="[0,0][100,100]"/>'
                  '<node text="Start" bounds="[200,100][300,200]"/>')
        self.assertEqual(module.center_for_label(xml(markup), "Start"), (250, 150))

    def test_rejects_degenerate_bounds(self):
        with self.assertRaises(ValueError):
            module.center_for_label(xml('<node text="Start" bounds="[1,1][1,5]"/>'), "Start")

    def test_rejects_malformed_xml(self):
        with self.assertRaises(Exception):
            module.center_for_label('<hierarchy><node>', "Start")


if __name__ == "__main__":
    unittest.main()
