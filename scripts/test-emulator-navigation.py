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

    def test_skips_invalid_duplicate_bounds_before_valid_label(self):
        xml = ('<hierarchy>'
               '<node text="Verbindungen" bounds="[100,200][100,260]"/>'
               '<node text="Verbindungen" bounds="[300,400][500,480]"/>'
               '</hierarchy>')
        self.assertEqual((400, 440), module.center_for_label(xml, "Verbindungen"))

    def test_all_hidden_or_disabled_duplicates_fail_closed(self):
        xml = ('<hierarchy>'
               '<node text="Meine Fahrt" displayed="false" bounds="[0,0][100,100]"/>'
               '<node text="Meine Fahrt" enabled="false" bounds="[100,0][200,100]"/>'
               '</hierarchy>')
        with self.assertRaises(ValueError):
            module.center_for_label(xml, "Meine Fahrt")

    def test_skips_label_inside_hidden_parent(self):
        xml = ('<hierarchy>'
               '<node displayed="false" bounds="[0,0][500,500]">'
               '<node text="Verbindungen" displayed="true" bounds="[0,0][100,100]"/>'
               '</node>'
               '<node text="Verbindungen" bounds="[200,300][400,360]"/>'
               '</hierarchy>')
        self.assertEqual((300, 330), module.center_for_label(xml, "Verbindungen"))

    def test_skips_label_inside_disabled_parent(self):
        xml = ('<hierarchy>'
               '<node enabled="false" bounds="[0,0][500,500]">'
               '<node text="Meine Fahrt" bounds="[0,0][100,100]"/>'
               '</node>'
               '<node text="Meine Fahrt" bounds="[300,400][500,480]"/>'
               '</hierarchy>')
        self.assertEqual((400, 440), module.center_for_label(xml, "Meine Fahrt"))

    def test_fails_when_only_matching_label_has_hidden_parent(self):
        xml = ('<hierarchy><node displayed="false">'
               '<node text="Start" bounds="[0,0][100,100]"/>'
               '</node></hierarchy>')
        with self.assertRaises(ValueError):
            module.center_for_label(xml, "Start")

    def test_hidden_parent_blocks_content_description_target(self):
        xml = ('<hierarchy><node displayed="false">'
               '<node content-desc="Meine Fahrt" bounds="[0,0][100,100]"/>'
               '</node>'
               '<node content-desc="Meine Fahrt" bounds="[200,300][400,360]"/>'
               '</hierarchy>')
        self.assertEqual((300, 330), module.center_for_label(xml, "Meine Fahrt"))

    def test_disabled_grandparent_blocks_nested_target(self):
        xml = ('<hierarchy><node enabled="false"><node enabled="true">'
               '<node text="Start" bounds="[0,0][100,100]"/>'
               '</node></node>'
               '<node text="Start" bounds="[100,200][300,260]"/>'
               '</hierarchy>')
        self.assertEqual((200, 230), module.center_for_label(xml, "Start"))

    def test_malformed_xml_fails_without_tap_target(self):
        with self.assertRaises(Exception):
            module.center_for_label('<hierarchy><node text="Start"', "Start")

    def test_skips_android_visible_to_user_false_parent(self):
        xml = ('<hierarchy><node visible-to-user="false">'
               '<node text="Verbindungen" bounds="[0,0][100,100]"/>'
               '</node><node text="Verbindungen" bounds="[200,300][400,360]"/>'
               '</hierarchy>')
        self.assertEqual((300, 330), module.center_for_label(xml, "Verbindungen"))

    def test_rejects_uppercase_false_visibility_and_enabled(self):
        xml = ('<hierarchy><node displayed="False">'
               '<node text="Start" bounds="[0,0][100,100]"/>'
               '</node><node text="Start" enabled="FALSE" bounds="[100,0][200,100]"/>'
               '</hierarchy>')
        with self.assertRaises(ValueError):
            module.center_for_label(xml, "Start")

    def test_rejects_whitespace_padded_false_visibility(self):
        xml = ('<hierarchy><node visible-to-user=" FALSE ">'
               '<node text="Start" bounds="[0,0][100,100]"/>'
               '</node><node text="Start" displayed=" false " bounds="[100,0][200,100]"/>'
               '<node text="Start" bounds="[200,300][400,360]"/>'
               '</hierarchy>')
        self.assertEqual((300, 330), module.center_for_label(xml, "Start"))

    def test_whitespace_padded_disabled_parent_fails_closed(self):
        xml = ('<hierarchy><node enabled=" false ">'
               '<node content-desc="Meine Fahrt" bounds="[0,0][100,100]"/>'
               '</node></hierarchy>')
        with self.assertRaises(ValueError):
            module.center_for_label(xml, "Meine Fahrt")

    def test_skips_negative_or_malformed_bounds_before_valid_duplicate(self):
        # Android hierarchy dumps can contain off-screen or malformed duplicates.
        # Never tap those coordinates when a usable navigation label exists.
        xml = ('<hierarchy>'
               '<node text="Verbindungen" bounds="[-20,0][80,100]"/>'
               '<node text="Verbindungen" bounds="garbled"/>'
               '<node text="Verbindungen" bounds="[240,360][440,440]"/>'
               '</hierarchy>')
        self.assertEqual((340, 400), module.center_for_label(xml, "Verbindungen"))

    def test_negative_bounds_without_valid_duplicate_fail_closed(self):
        xml = '<hierarchy><node text="Meine Fahrt" bounds="[-10,0][100,100]"/></hierarchy>'
        with self.assertRaises(ValueError):
            module.center_for_label(xml, "Meine Fahrt")

    def test_rejects_empty_bounds(self):
        xml = '<hierarchy><node text="Verbindungen" bounds="[100,200][100,260]"/></hierarchy>'
        with self.assertRaises(ValueError):
            module.center_for_label(xml, "Verbindungen")


    def test_malformed_dump_never_sends_adb_tap(self):
        # A corrupt UI dump must fail before issuing any input event.
        from unittest.mock import patch
        import sys
        commands = []
        def fake_run(args, **kwargs):
            commands.append(args)
        with patch.object(sys, "argv", ["tap-emulator-navigation.py", "Start"]), \
             patch.object(module.subprocess, "run", side_effect=fake_run), \
             patch.object(module.subprocess, "check_output",
                          return_value='<hierarchy><node text="Start"'):
            with self.assertRaises(Exception):
                module.main()
        self.assertEqual([["adb", "shell", "uiautomator", "dump", "/sdcard/window.xml"]], commands)

    def test_missing_target_never_sends_adb_tap(self):
        from unittest.mock import patch
        import sys
        commands = []
        with patch.object(sys, "argv", ["tap-emulator-navigation.py", "Meine Fahrt"]), \
             patch.object(module.subprocess, "run", side_effect=lambda args, **kwargs: commands.append(args)), \
             patch.object(module.subprocess, "check_output",
                          return_value='<hierarchy><node text="Start" bounds="[0,0][100,100]"/></hierarchy>'):
            with self.assertRaises(ValueError):
                module.main()
        self.assertEqual(1, len(commands))
        self.assertEqual("uiautomator", commands[0][2])

    def test_successful_navigation_taps_exactly_once_at_detected_center(self):
        from unittest.mock import patch
        import sys
        commands = []
        xml = '<hierarchy><node text="Verbindungen" bounds="[120,240][320,300]"/></hierarchy>'
        with patch.object(sys, "argv", ["tap-emulator-navigation.py", "Verbindungen"]), \
             patch.object(module.subprocess, "run",
                          side_effect=lambda args, **kwargs: commands.append(args)), \
             patch.object(module.subprocess, "check_output", return_value=xml):
            module.main()
        self.assertEqual([
            ["adb", "shell", "uiautomator", "dump", "/sdcard/window.xml"],
            ["adb", "shell", "input", "tap", "220", "270"],
        ], commands)

    def test_failed_uiautomator_dump_never_taps(self):
        from unittest.mock import patch
        import subprocess
        import sys
        commands = []
        def failing_run(args, **kwargs):
            commands.append(args)
            raise subprocess.CalledProcessError(1, args)
        with patch.object(sys, "argv", ["tap-emulator-navigation.py", "Start"]), \
             patch.object(module.subprocess, "run", side_effect=failing_run), \
             patch.object(module.subprocess, "check_output") as read_dump:
            with self.assertRaises(subprocess.CalledProcessError):
                module.main()
            read_dump.assert_not_called()
        self.assertEqual(
            [["adb", "shell", "uiautomator", "dump", "/sdcard/window.xml"]],
            commands,
        )


    def test_failed_hierarchy_read_never_taps(self):
        # A successful dump command is not enough: reading the XML can fail.
        from unittest.mock import patch
        import subprocess
        import sys
        commands = []
        def fake_run(args, **kwargs):
            commands.append(args)
        with patch.object(sys, "argv", ["tap-emulator-navigation.py", "Start"]), \
             patch.object(module.subprocess, "run", side_effect=fake_run), \
             patch.object(module.subprocess, "check_output",
                          side_effect=subprocess.CalledProcessError(
                              1, ["adb", "exec-out", "cat", "/sdcard/window.xml"])):
            with self.assertRaises(subprocess.CalledProcessError):
                module.main()
        self.assertEqual(
            [["adb", "shell", "uiautomator", "dump", "/sdcard/window.xml"]],
            commands,
        )


if __name__ == "__main__":
    unittest.main()
