"""Collect checked, portable test evidence without publishing machine logs or worlds."""
from pathlib import Path
import json
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / "verification/results"
SCREENSHOTS = ROOT / "docs/screenshots"
MODULES = ("forge-1.20.1", "neoforge-1.21.1", "neoforge-26.1.2")


def main():
    RESULTS.mkdir(parents=True, exist_ok=True)
    SCREENSHOTS.mkdir(parents=True, exist_ok=True)
    summary = {}
    for module in MODULES:
        summary[module] = {}
        for with_jade in (False, True):
            filename = "verification-jade-console.log" if with_jade else "verification-console.log"
            log = (ROOT / module / "build" / filename).read_text(encoding="utf-8-sig", errors="replace")
            passed = re.findall(r"JGL_CHECK_PASS (.+)", log)
            result = re.search(r"JGL_VERIFICATION PASS (\d+)", log)
            if not result or int(result[1]) != len(passed):
                raise RuntimeError(f"Verification did not pass: {module}, Jade={with_jade}")
            if not any("Real table generates after four Speed T5 ticks" in check for check in passed):
                raise RuntimeError(f"Report predates the 2.0 generation tests: {module}")
            if with_jade and not any("Installed Lootr block and entity provider families" in check for check in passed):
                raise RuntimeError(f"Lootr integration was not exercised: {module}")
            variant = "with-jade" if with_jade else "without-jade"
            report = {"module": module, "modVersion": "2.0.0", "jade": with_jade, "lootr": with_jade, "passed": True, "count": len(passed), "checks": passed}
            (RESULTS / f"{module}-{variant}.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
            summary[module][variant] = len(passed)
        ui = json.loads((ROOT / module / "build/ui-client/ui-results.json").read_text(encoding="utf-8-sig"))
        if not ui["passed"] or not ui.get("tabletCapturePacket") or not ui.get("serverTabletConfiguration"):
            raise RuntimeError(f"UI checks did not pass: {module}")
        (RESULTS / f"{module}-ui.json").write_text(json.dumps(ui, indent=2) + "\n", encoding="utf-8")
        for source, label in (("jgl-gui-scale-3.png", "gui"), ("jgl-world-hud.png", "jade"), ("jgl-tablet-selected.png", "tablet"), ("jgl-generator-table-hud.png", "jade-context")):
            shutil.copyfile(ROOT / module / "build/ui-client/screenshots" / source, SCREENSHOTS / f"{module}-{label}.png")
        summary[module]["ui"] = True
    supplemental = ROOT / "forge-1.20.1/build/verification-lootr-console.log"
    if supplemental.is_file():
        log = supplemental.read_text(encoding="utf-8-sig", errors="replace")
        checks = re.findall(r"JGL_CHECK_PASS (.+)", log)
        result = re.search(r"JGL_VERIFICATION PASS (\d+)", log)
        if not result or int(result[1]) != len(checks) or any("Jade bulk provider" in check for check in checks):
            raise RuntimeError("Forge Lootr-only verification did not pass independently of Jade")
        report = {"module": "forge-1.20.1", "modVersion": "2.0.0", "jade": False, "lootr": True, "passed": True, "count": len(checks), "checks": checks}
        (RESULTS / "forge-1.20.1-lootr-only.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        summary["forge-1.20.1"]["lootr-without-jade"] = len(checks)
    core = json.loads((ROOT / "verification/runtime/core/results.json").read_text(encoding="utf-8-sig"))
    if not core["passed"]:
        raise RuntimeError("Core checks did not pass")
    summary["core"] = core
    (RESULTS / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
