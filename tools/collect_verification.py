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
            variant = "with-jade" if with_jade else "without-jade"
            report = {"module": module, "jade": with_jade, "passed": True, "count": len(passed), "checks": passed}
            (RESULTS / f"{module}-{variant}.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
            summary[module][variant] = len(passed)
        ui = json.loads((ROOT / module / "build/ui-client/ui-results.json").read_text(encoding="utf-8-sig"))
        if not ui["passed"]:
            raise RuntimeError(f"UI checks did not pass: {module}")
        (RESULTS / f"{module}-ui.json").write_text(json.dumps(ui, indent=2) + "\n", encoding="utf-8")
        for source, label in (("jgl-gui-scale-3.png", "gui"), ("jgl-world-hud.png", "jade")):
            shutil.copyfile(ROOT / module / "build/ui-client/screenshots" / source, SCREENSHOTS / f"{module}-{label}.png")
        summary[module]["ui"] = True
    core = json.loads((ROOT / "verification/runtime/core/results.json").read_text(encoding="utf-8-sig"))
    if not core["passed"]:
        raise RuntimeError("Core checks did not pass")
    summary["core"] = core
    (RESULTS / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
