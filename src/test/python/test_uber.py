import json
import subprocess
import unittest
from pathlib import Path

deployable_project = Path("src/test/sample/deployable")
assert deployable_project.exists()


class Test(unittest.TestCase):
    def test_build_local(self):
        stdout = subprocess.check_output(
            ["java", "-jar", Path("build/libs/mavence.uber.jar").absolute(),
             "local", "io.github.rtmigo:libr"],
            cwd=deployable_project
        )
        js = json.loads(stdout)
        self.assertEqual(js["group"], 'io.github.rtmigo')
        self.assertEqual(js["artifact"], 'libr')
        self.assertEqual(js["mavenUrl"], 'file:///home/rtmigo/.m2')
        self.assertEqual(js["notation"], 'io.github.rtmigo:libr:1.2.3-rc2')

        print(js)


if __name__ == "__main__":
    unittest.main()
