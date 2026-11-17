#!/usr/bin/env python3
"""Regression tests for viewport-independent emulator navigation."""
import importlib.util
from pathlib import Path
import unittest

SCRIPT = Path(__file__).with_name("tap-emulator-navigation.py")
spec = importlib.util.spec_from_file_location("emulator_navigation", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class EmulatorNavigationTests(unittest.TestCase):
    def test_label_center_uses_actual_bounds(self):
        xml = '<hierarchy><node text="Verbindungen" bounds="[100,200][300,260]"/></hierarchy>'
        self.assertEqual((200, 230), module.center_for_label(xml, "Verbindungen"))

    def test_supports_content_description(self):
        xml = '<hierarchy><node content-desc="Meine Fahrt" bounds="[300,200][500,280]"/></hierarchy>'
        self.assertEqual((400, 240), module.center_for_label(xml, "Meine Fahrt"))

    def test_missing_label_fails_instead_of_tapping_randomly(self):
        xml = '<hierarchy><node text="Start" bounds="[0,0][100,100]"/></hierarchy>'
        with self.assertRaises(ValueError):
            module.center_for_label(xml, "Verbindungen")

    def test_skips_hidden_duplicate_label(self):
        xml = ('<hierarchy>'
               '<node text="Verbindungen" displayed="false" bounds="[0,0][100,100]"/>'
               '<node text="Verbindungen" displayed="true" bounds="[200,300][400,360]"/>'
               '</hierarchy>')
        self.assertEqual((300, 330), module.center_for_label(xml, "Verbindungen"))

    def test_skips_disabled_duplicate_label(self):
        xml = ('<hierarchy>'
               '<node text="Start" enabled="false" bounds="[0,0][100,100]"/>'
               '<node text="Start" enabled="true" bounds="[100,200][300,260]"/>'
               '</hierarchy>')
        self.assertEqual((200, 230), module.center_for_label(xml, "Start")

    def test_rejects_empty_bounds(self):
        xml = '<hierarchy><node text="Verbindungen" bounds="[100,200][100,260]"/></hierarchy>'
        with self.assertRaises(ValueError):
            module.center_for_label(xml, "Verbindungen")


if __name__ == "__main__":
    unittest.main()
