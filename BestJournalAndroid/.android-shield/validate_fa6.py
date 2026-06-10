import xml.etree.ElementTree as ET
import sys

ANDROID = "http://schemas.android.com/apk/res/android"
path = r"C:/Users/barwa/proggs/BestJournalAndroid/app/src/main/AndroidManifest.xml"

try:
    tree = ET.parse(path)
except ET.ParseError as e:
    print(f"XML FAIL: {e}")
    sys.exit(1)

root = tree.getroot()
app = root.find("application")
if app is None:
    print("FAIL: no <application> element")
    sys.exit(1)

# direct children meta-data of <application>
found = False
for md in app.findall("meta-data"):
    name = md.get(f"{{{ANDROID}}}name")
    val = md.get(f"{{{ANDROID}}}value")
    if name == "firebase_analytics_collection_enabled":
        found = True
        if val != "false":
            print(f"FAIL: value is '{val}', expected 'false'")
            sys.exit(1)

if not found:
    print("FAIL: meta-data firebase_analytics_collection_enabled not a direct child of <application>")
    sys.exit(1)

print("XML OK; meta-data firebase_analytics_collection_enabled=false present as direct child of <application>")
