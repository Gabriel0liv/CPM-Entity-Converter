import hashlib, json, sys
from pathlib import Path

PIN = "25a41d7375bb7eeda37dadc04b1e03fe486b33e5"
ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / "artifacts" / "results.json"
MANIFEST = ROOT / "fixture-manifest.json"

def fail(message):
    print(json.dumps({"status":"FAIL","error":message}, indent=2))
    raise SystemExit(1)

if not RESULTS.exists(): fail("results.json missing")
data = json.loads(RESULTS.read_text(encoding="utf-8"))
if data.get("checkoutCommit") != PIN: fail("checkout pin mismatch")
if (data.get("assertionsTotal"), data.get("assertionsPassed"), data.get("assertionsFailed")) != (90,90,0): fail("assertion totals mismatch")
fixtures = data.get("fixtures", [])
if len(fixtures) != 37 or any(f.get("status") == "FAIL" for f in fixtures): fail("fixture status/count mismatch")
manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
names = [x["fixture"] for x in manifest["fixtures"]]
observed = [x.get("fixture") for x in fixtures]
if len(set(names)) != 37 or sorted(names) != sorted(observed): fail("fixture manifest mismatch")
for item in manifest["fixtures"]:
    path = ROOT / item["path"]
    if not path.exists(): fail(f"missing fixture {item['fixture']}")
    digest = hashlib.sha256(path.read_bytes().replace(bytes([13, 10]), bytes([10]))).hexdigest()
    result = next(f for f in fixtures if f["fixture"] == item["fixture"])
    if result.get("inputSha256") != digest: fail(f"hash mismatch {item['fixture']}")
print(json.dumps({"status":"PASS","fixtures":37,"assertions":90,"pin":PIN}, indent=2))
