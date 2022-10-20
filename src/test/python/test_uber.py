import json
import subprocess
import unittest
from pathlib import Path

deployable_project = Path("src/test/sample/deployable")
assert deployable_project.exists()


class Test(unittest.TestCase):
    def test_build_local(self):
        # check that our program provides clean stdout with JSON only
        stdout = subprocess.check_output(
            ["java", "-jar", Path("build/libs/mavence.uber.jar").absolute(),
             "local", "io.github.rtmigo:libr"],
            cwd=deployable_project
        )
        print("stdout:", stdout)

        js = json.loads(stdout)
        self.assertEqual(js["group"], 'io.github.rtmigo')
        self.assertEqual(js["artifact"], 'libr')
        self.assertTrue(js["mavenRepo"].startswith('file://'))
        self.assertTrue(js["mavenRepo"].endswith('/.m2'))
        self.assertEqual(js["notation"], 'io.github.rtmigo:libr:1.2.3-rc2')


if __name__ == "__main__":
    unittest.main()
