#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Arabic translation injector for BestJournal strings.xml
Atomic write, PYTHONIOENCODING=utf-8, XML validation via xml.etree
"""
import os
import re
import sys
import tempfile
import xml.etree.ElementTree as ET

STRINGS_PATH = os.path.join(
    os.path.dirname(__file__),
    "..", "app", "src", "main", "res", "values-ar", "strings.xml"
)
STRINGS_PATH = os.path.normpath(STRINGS_PATH)

# 22 Arabic translations (MSA, RTL rules applied)
TRANSLATIONS = {
    "dashboard_weekly_review_upsell_body": (
        "لقد مررت بأسبوع مليء بالأحداث. مع Premium، ترى التحليل الكامل:"
        " تعرّف على الأنماط، اكتشف الرؤى، وافهم ما يشغل بالك."
    ),
    "legend_insight_deep": "عميق، أنماط ومعتقدات متكررة",
    "profile_insight_desc": "يكشف الأنماط المتكررة في التفكير والمشاعر",
    "profile_entropy_long": (
        "يبحث الذكاء الاصطناعي بدقة عن التوتر والإجهاد والفوضى في مدخلاتك.\\n\\n"
        "ستحصل على:\\n\\n"
        "\\u2022 تحليل لأكبر مصادر الضغط لديك\\n"
        "\\u2022 5 إجراءات ملموسة للترتيب\\n"
        "\\u2022 نصائح يمكنها مساعدتك في حياتك اليومية\\n\\n"
        "مثالي عندما تشعر أن كل شيء أصبح أكثر من اللازم."
    ),
    "profile_insight_long": (
        "يتعمق الذكاء الاصطناعي أكثر من مجرد الأحداث. يتعرف في مدخلاتك على:\\n\\n"
        "\\u2022 أنماط التفكير والمعتقدات في كلماتك\\n"
        "\\u2022 المشاعر وردود الفعل المتكررة\\n"
        "\\u2022 نقاط القوة الظاهرة في مدخلاتك\\n"
        "\\u2022 القيم التي تحرك أفعالك\\n\\n"
        "لكل من يريد أن يفهم نفسه بشكل أفضل وأن ينمو من الداخل."
    ),
    "settings_premium_cancelled_desc": (
        "ستبقى ميزات Premium متاحة حتى نهاية فترة الفوترة."
        " يمكنك التراجع عن الإلغاء عبر Google Play."
    ),
    "settings_premium_feature_5_perspectives_desc": "من الملخص إلى معرفة الذات",
    "privacy_gate_gemini_body": (
        "يسعدنا أنك تريد استخدام الميزات الذكية لـ ‏Best Journal!"
        " لإنشاء الملخصات والمراجعات ورؤى لوحة التحكم وتحسينات النص،"
        " تُرسَل نصوصك مشفرة إلى Google Gemini.\\n\\n"
        "\\u2713 لا تدريب للذكاء الاصطناعي على بياناتك\\n"
        "\\u2713 لا تُحفظ الطلبات بشكل دائم\\n"
        "\\u2713 مشفرة وفق إطار خصوصية البيانات بين الاتحاد الأوروبي والولايات المتحدة\\n\\n"
        "ملاحظة مهمة: نظرًا لأن مدخلات يومياتك قد تحتوي على بيانات حساسة"
        " (الصحة، الدين، العلاقات الشخصية — المادة 9 من اللائحة العامة لحماية البيانات)،"
        " نطلب موافقتك الصريحة على معالجة الذكاء الاصطناعي."
        " مخرجات الذكاء الاصطناعي هي اقتراحات، وليست استشارة مهنية ولا تغني عن المتخصصين.\\n\\n"
        "يمكنك تغيير ذلك في أي وقت من الإعدادات."
    ),
    "settings_delete_account_force_local": "حذف محلي فقط (يبقى النسخ الاحتياطي على Drive)",
    "retro_monthly_p3": "يعرض التطورات من مدخلاتك",
    "retro_yearly_p2": "يلخص الذكاء الاصطناعي المواضيع الكبرى لعامك",
    "onboarding_subtitle": "يومياتك الشخصية بالذكاء الاصطناعي\\nللمزيد من الوضوح في حياتك اليومية",
    "onboarding_how_step4_desc": "يعرض لوحة التحكم بالذكاء الاصطناعي الروابط من مدخلاتك.",
    "onboarding_premium_feature_patterns": (
        "إظهار الأنماط: يُبيّن الذكاء الاصطناعي أنماط التفكير والمشاعر المتكررة في مدخلاتك"
    ),
    "onboarding_free_trial": "مجاني، قابل للإلغاء في أي وقت",
    "onboarding_try_premium": "8 أيام مجانية، ثم من 3.99 € شهريًا",
    "paywall_yearly_note": (
        "ثم %1$s سنويًا، يتجدد تلقائيًا\\n"
        "قابل للإلغاء في أي وقت خلال فترة التجربة"
    ),
    "paywall_most_popular": "موصى به",
    "paywall_free_note": "يعمل التطبيق بدون اشتراك أيضًا.",
    "paywall_feature_patterns": "تعرّف على الأنماط المتكررة في تفكيرك",
    "churn_exclusive_offer": "عرض خاص لك",
    "paywall_subscribe_button": "الاشتراك بالدفع",
}

def escape_xml_value(value: str) -> str:
    """Escape & and < only (> and ' are safe in attribute values or element content here)."""
    value = value.replace("&", "&amp;")
    value = value.replace("<", "&lt;")
    return value

def build_string_tag(key: str, value: str) -> str:
    return f'    <string name="{key}">{escape_xml_value(value)}</string>\n'

def update_strings_xml(path: str, translations: dict) -> tuple[int, int]:
    """
    Read existing file, update matching keys in-place.
    Keys not found are appended before </resources>.
    Returns (updated_count, appended_count).
    """
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    updated = 0
    appended = 0
    remaining = dict(translations)

    for key, arabic_value in translations.items():
        # Match <string name="KEY">...</string> (single-line and multi-line)
        pattern = re.compile(
            r'(<string\s+name="' + re.escape(key) + r'"[^>]*>)(.*?)(</string>)',
            re.DOTALL
        )
        replacement_value = escape_xml_value(arabic_value)
        # Use a lambda so regex engine never sees backslashes as escape sequences
        def make_repl(rv):
            def repl(m):
                return m.group(1) + rv + m.group(3)
            return repl
        new_content, n = pattern.subn(make_repl(replacement_value), content)
        if n > 0:
            content = new_content
            updated += 1
            del remaining[key]

    # Append missing keys before </resources>
    if remaining:
        insert_lines = ""
        for key, arabic_value in remaining.items():
            insert_lines += build_string_tag(key, arabic_value)
        content = content.replace("</resources>", insert_lines + "</resources>")
        appended = len(remaining)

    # Atomic write
    dir_name = os.path.dirname(path)
    with tempfile.NamedTemporaryFile(
        "w", dir=dir_name, suffix=".tmp", delete=False, encoding="utf-8", newline="\n"
    ) as tmp:
        tmp.write(content)
        tmp_path = tmp.name
    os.replace(tmp_path, path)

    return updated, appended

def validate_xml(path: str) -> bool:
    try:
        ET.parse(path)
        return True
    except ET.ParseError as e:
        print(f"XML validation FAILED: {e}", file=sys.stderr)
        return False

def main():
    print(f"Target: {STRINGS_PATH}")
    if not os.path.exists(STRINGS_PATH):
        print(f"ERROR: File not found: {STRINGS_PATH}", file=sys.stderr)
        sys.exit(1)

    updated, appended = update_strings_xml(STRINGS_PATH, TRANSLATIONS)
    print(f"Keys updated in-place : {updated}")
    print(f"Keys appended          : {appended}")
    print(f"Total                  : {updated + appended}")

    valid = validate_xml(STRINGS_PATH)
    if valid:
        print("XML validation        : OK")
    else:
        sys.exit(1)

if __name__ == "__main__":
    main()
