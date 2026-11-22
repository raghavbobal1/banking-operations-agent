#!/usr/bin/env python3
"""Fail CI when the public demo deployment contract drifts.

Uses the Python standard library only. This does not claim that a remote
Render service is deployed or that paid model calls have been tested.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]


class DeploymentContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.blueprint = (ROOT / "render.yaml").read_text()
        cls.dockerfile = (ROOT / "Dockerfile").read_text()
        cls.readme = (ROOT / "README.md").read_text()
        cls.workflow = (ROOT / ".github/workflows/verify-public-demo.yml").read_text()
        cls.verifier = (ROOT / "scripts/verify_demo.py").read_text()

    def test_render_runs_backend_and_checks_health(self):
        self.assertRegex(self.blueprint, r"(?m)^\s*runtime:\s*docker\s*$")
        self.assertRegex(self.blueprint, r"(?m)^\s*dockerfilePath:\s*\./Dockerfile\s*$")
        self.assertRegex(self.blueprint, r"(?m)^\s*healthCheckPath:\s*/api/health\s*$")
        self.assertRegex(self.blueprint, r"(?m)^\s*plan:\s*free\s*$")

    def test_ai_secret_must_be_provided_privately(self):
        self.assertRegex(
            self.blueprint,
            r"(?m)^\s*- key:\s*OPENAI_API_KEY\s*\n\s*sync:\s*false\s*$",
        )
        self.assertRegex(
            self.blueprint,
            r'(?m)^\s*- key:\s*AI_ENABLED\s*\n\s*value:\s*["\x27]?true["\x27]?\s*$',
        )
        self.assertNotRegex(self.blueprint, r"(?m)^\s*(value|default):\s*sk-[^\s]+")
        self.assertNotRegex(self.dockerfile, r"(?im)^\s*(ENV|ARG)\s+OPENAI_API_KEY\s*=")

    def test_java_21_runtime_and_non_root_user(self):
        self.assertIn("eclipse-temurin-21", self.dockerfile)
        self.assertIn("USER appuser", self.dockerfile)
        self.assertIn("-Dserver.port=$PORT", self.dockerfile)

    def test_public_verification_requires_real_http_check(self):
        self.assertIn("workflow_dispatch:", self.workflow)
        self.assertIn("scripts/verify_demo.py", self.workflow)
        self.assertIn("--mode", self.workflow)
        self.assertIn("spring-ai-tool-calling", self.verifier)
        self.assertIn("https", self.workflow)
        self.assertIn("deterministic", self.readme.lower())
        self.assertIn("not proof of a deployed service", self.readme.lower())


if __name__ == "__main__":
    unittest.main()
