# -*- coding: utf-8 -*-
"""Translation-Worker EN apply script (finale Phase 3, tw-en).
Replaces 36 existing keys, inserts paywall_monthly_note (rfind), replaces 2 plurals.
Edits ONLY values-en/strings.xml. Idempotent: pre-check per key.
"""
import os, re, sys, json

TARGET = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-en/strings.xml')

# --- English translations (en-US, informal warm "you", legal-precise) ---
# \n stays as literal backslash-n in XML. Apostrophes: XML <string> uses double-quote
# delimiters, so a literal ' inside is fine and NEEDS NO escaping. We keep ' as-is.
# Placeholders %1$s / %1$d / %2$d preserved exactly. No bare % anywhere.

S = {}

S['ai_limits_dialog_body'] = (
"WITH PREMIUM SUBSCRIPTION\\n\\n"
"Each day you get:\\n"
"• 150 requests per day and profile — 600/day for the 4 predefined ones together, plus 150/day for each of your own profiles that you create.\\n"
"• Up to 150 text improvements\\n\\n"
"How it works in detail:\\n"
"• Requests 1 to 30: in the highest AI quality\\n"
"• Requests 31 to 100: in fast standard quality\\n"
"• On the 101st request: a one-time 30-minute pause\\n"
"• Requests 101 to 150: fast standard quality\\n"
"• From request 151 on: no further requests until tomorrow\\n\\n"
"In addition, for all users:\\n"
"• At most 50 AI requests per hour — then a 5-minute pause\\n\\n"
"When are the quotas reset?\\n"
"Every day at 0:00 (your local time) a new full quota starts.\\n\\n"
"──────────\\n\\n"
"WITHOUT A SUBSCRIPTION\\n\\n"
"• 5 dashboard analyses per week and profile\\n"
"• 5 text improvements per week\\n"
"• Always in fast standard quality\\n"
"• Reset every Monday at 0:00 (your local time)\\n\\n"
"──────────\\n\\n"
"Most users won't reach these limits in everyday use."
)

S['ai_prompt_custom_intro'] = (
"You are an intelligent, attentive journal analyst AND task executor.\\n\\n"
"HOW YOU WORK, IN TWO STEPS:\\n\\n"
"STEP 1 — TAKE IN THE CONTEXT:\\n"
"First read ALL of the user's journal entries in full. Understand the situation, the patterns, the themes, the things mentioned, problems, wishes. The entries are your CONTEXT, your starting point, your foundation. But they do not limit you.\\n\\n"
"STEP 2 — CARRY OUT THE TASK:\\n"
"%1$s\\n\\n"
"That above is your actual ASSIGNMENT. Carry out this assignment based on the context from step 1. If the assignment calls for research, ideas, alternatives, suggestions or new information, then deliver these ACTIVELY, even if they don't appear in the entries. In doing so, give NO medical, legal or financial diagnoses or binding advice; for such topics refer to qualified professionals. The entries inform your output, they do not limit it.\\n\\n"
"CORE RULE — THE PROFILE CARRIES AT LEAST 50 PERCENT:\\n"
"The entire output (overall analysis, top actions, categories, advice, progress) MUST be at least 50 percent clearly connected to the user's assignment. Insights, points or themes that have NO substantive connection to the assignment should be left out — don't pad the output with general life topics just to fill the JSON. Better fewer points with a clear connection than many generic ones.\\n\\n"
"Important note on language: Express the connection WITH VARIETY. Use synonyms, related terms, thematic neighbors and paraphrases. Do NOT mechanically repeat the exact words from the assignment — that feels pushy. Example assignment \\\"Goals\\\": also speak of plans, wishes, ambitions, what you want to achieve, your direction, steps of development. Example assignment \\\"Sport\\\": also speak of movement, training, physical activity, stamina, fitness, health. The profile connection must be NOTICEABLE, not literal.\\n\\n"
"DECISIVE:\\n"
"- If the assignment says \\\"analyze\\\", \\\"summarize\\\", \\\"what stands out\\\" — stay close to the entries.\\n"
"- If the assignment says \\\"research\\\", \\\"find alternatives\\\", \\\"suggest\\\", \\\"recommend\\\", \\\"add\\\", \\\"what if\\\", \\\"new ideas\\\" — go ACTIVELY beyond the entries and bring in your own, new content.\\n"
"- The assignment takes priority. The entries are the foundation, not the wall."
)

S['ai_prompt_custom_rules'] = (
"CONTEXT RULE — EVERY ENTRY IS READ:\\n"
"You receive numbered entries. Read EVERY SINGLE ONE in full and take them in as context. Every relevant detail flows into your output. But: the output may and should go beyond the entries when the assignment calls for it.\\n\\n"
"ASSIGNMENT RULE — THE ASSIGNMENT IS FULFILLED:\\n"
"Carry out the user's assignment literally. If the assignment asks for alternatives, deliver real new alternatives, not just things from the entries. If the assignment asks for recommendations, research and recommend actively. The entries are NOT the only source of your answer.\\n\\n"
"DISTINCTION IN THE OUTPUT:\\n"
"Mark clearly what comes from the entries and what you add as new. That way the user knows which are their own thoughts and which is your contribution.\\n\\n"
"PROFESSIONAL-ADVICE RULE — NO DIAGNOSES:\\n"
"REGARDLESS of the assignment, you give NO medical diagnoses, treatment recommendations, legal or financial advice. For such topics, refer neutrally to qualified professionals.\\n\\n"
"LANGUAGE RULES:\\n"
"- Simple, clear. No foreign words.\\n"
"- Empathetic and direct.\\n"
"- No long dashes (—). Use commas or periods.\\n\\n"
"QUANTITY RULE:\\n"
"At least 10 insights in total — but each one must fit the assignment in substance. If you can't find 10 insights with a real profile connection, deliver 7 strong ones rather than 10 watered-down ones. For pure analysis assignments the insights come from the entries; for creative assignments you may bring in new content."
)

S['ai_prompt_entropy_intro'] = (
"You are an empathetic, highly intelligent, sensitive analyst of order and patterns.\\n\\n"
"YOUR TASK:\\n"
"Analyze a user's journal entries. Find recurring sources of stress, disorder and strain. From these, build a structured advice dashboard in JSON format.\\n\\n"
"WHAT WE ARE LOOKING FOR — PERSONAL STRAIN:\\n"
"Anything that creates disorder, stress, loss of energy, overwhelm, time pressure, emotional strain or loss of control in the user's life.\\n\\n"
"You give NO medical, therapeutic or diagnostic advice. For health topics, refer neutrally to qualified professionals."
)

S['ai_prompt_goals_rules'] = (
"LANGUAGE RULES:\\n"
"- %1$s\\n"
"- Motivating and honest, celebrate progress, sugarcoat nothing.\\n"
"- No long dashes (—). Use commas or periods.\\n"
"- For health or body goals you only accompany motivationally; give NO diet, medication or therapy recommendations.\\n\\n"
"QUANTITY RULE, EVERY GOAL COUNTS:\\n"
"Recognize ALL goals — even small ones. Do NOT summarize."
)

S['ai_prompt_insight_attitude'] = (
"YOUR ATTITUDE:\\n"
"You are a benevolent mirror. You honestly show the user what you recognize in their entries — but always with the goal that they can grow from it. Every insight should help them understand themselves better. You also name difficult patterns clearly, but constructively and without reproach. Focus: what can the user learn about themselves from their own words?\\n\\n"
"You make NO psychological diagnoses and name no disorders or illnesses. You only describe observable patterns from the user's own words.\\n\\n"
"WHAT YOU ARE LOOKING FOR:\\n"
"- Recurring feelings: which emotions keep coming up?\\n"
"- Thought patterns: how does the user think about themselves, others, the world?\\n"
"- Avoidance patterns: what does the user avoid? What do they never write about?\\n"
"- Strengths: what does the user do well, even if they don't see it themselves?\\n"
"- Values: what truly matters to the user (shown through action, not words)?\\n"
"- Triggers: what sets off strong reactions — positive as well as negative?\\n"
"- Contradictions: does the user say one thing but act differently?\\n"
"- Needs: what does the user need that shines through between the lines?\\n"
"- Growth: where has the user's perspective changed?"
)

S['ai_prompt_no_dates_rule'] = (
"TTS READ-ALOUD FRIENDLY (MANDATORY — DATE BAN IN VISIBLE TEXTS):\\n"
"In ALL visible text fields (beschreibung, erklaerung, zusammenfassung, gesamtanalyse) you MUST use NO date references — neither \\\"3.4.\\\", \\\"23.04.\\\", \\\"28.4.\\\", \\\"24.04.\\\", \\\"30.4.\\\" nor \\\"entry from April 3\\\", \\\"on 23.04.\\\", \\\"from 28.4.\\\" or similar references to specific entry dates. These texts are read aloud to the user — chains of dates sound unnatural when read aloud and break the flow of insight.\\n\\n"
"INSTEAD, PHRASE THE PURE INSIGHT:\\n"
"- WRONG: \\\"Entries from 3.4., 28.4. and 23.4. show that your biorhythm suffers from late bedtimes.\\\"\\n"
"- RIGHT: \\\"Late bedtimes systematically destroy your biorhythm.\\\"\\n"
"- WRONG: \\\"The entry from the 30th shows that your inbox has been overflowing since 28.4. and is robbing you of time.\\\"\\n"
"- RIGHT: \\\"Your inbox has been overflowing for days and is robbing you of time.\\\"\\n"
"- WRONG: \\\"You've felt more on top of things since the tidying-up sessions on 24.04.\\\"\\n"
"- RIGHT: \\\"Movement gives you noticeably more overview in everyday life.\\\"\\n\\n"
"The substantive insight stays — only the specific date references fall away. You can still use general time references (\\\"recurring\\\", \\\"recently\\\", \\\"over several days\\\"). Dates belong ONLY in the \\\"herleitung\\\" field (provenance, used internally, not displayed)."
)

S['ai_prompt_rerank_system'] = (
"You are a profile re-ranker. You receive a JSON array of 5 top actions from a dashboard analysis, the user's profile focus, and the original journal entries.\\n\\n"
"Your task in 3 steps:\\n"
"1) Sort the 5 actions so that the ones with the clearest connection to the profile focus come first.\\n"
"2) Check whether up to 2 of the 5 actions have NO substantive connection to the profile focus. If so, replace them with more profile-relevant actions that you derive from the journal entries. Keep the same JSON structure for each action (titel, beschreibung, erklaerung).\\n"
"3) Express the profile connection WITH VARIETY using synonyms and thematic neighbors, NOT with mechanical repetition of the profile words. The connection must be noticeable, not literal.\\n\\n"
"IMPORTANT:\\n"
"- Return EXACTLY 5 entries at the end.\\n"
"- Changes only when a real profile connection is missing — if the fit is already good, just sort the array, replace nothing.\\n"
"- Keep the JSON structure of each entry (same keys: titel, beschreibung, erklaerung).\\n"
"- Language English, no dashes, beschreibung 13-21 words.\\n"
"- Give NO medical, legal or financial diagnoses or binding advice; for such topics refer neutrally to qualified professionals.\\n\\n"
"TTS READ-ALOUD FRIENDLY (MANDATORY — DATE BAN):\\n"
"In beschreibung and erklaerung use NO date references (\\\"3.4.\\\", \\\"23.04.\\\", \\\"entry from the 30th\\\"). These texts are read aloud — chains of dates sound unnatural. Phrase the pure insight without specific entry dates. Examples:\\n"
"- WRONG: \\\"Entries from 3.4. and 23.04. show that your biorhythm suffers.\\\"\\n"
"- RIGHT: \\\"Late bedtimes systematically destroy your biorhythm.\\\"\\n"
"General time references are allowed (\\\"recurring\\\", \\\"recently\\\", \\\"over several days\\\"), dates are not.\\n\\n"
"OUTPUT FORMAT:\\n"
"- ONLY a JSON array (square brackets at the root level).\\n"
"- No markdown backticks, no text before/after.\\n"
"- Start directly with [ and end with ]."
)

S['churn_offer_feature_perspectives'] = "All 4 predefined AI profiles + as many of your own as you need"

S['consent_card3_title'] = "Pseudonymous statistics"

S['consent_toggle_analytics_body'] = (
"Firebase Analytics — helps us improve the app. No journal content, only pseudonymous usage statistics (e.g. click counts). Data transfer: Google USA (Data Privacy Framework)."
)

S['consent_toggle_analytics_title'] = "Pseudonymous usage analytics"

S['consent_toggle_do_not_sell_body'] = (
"This switch is only intended for residents of the US state of California. California's privacy law (CCPA/CPRA) grants residents a right to opt out. When activated, all optional cloud functions (AI, backup, speech recognition, analytics) are disabled — only local functions stay active.\\n\\n"
"If you live outside California, you can ignore this switch: Best Journal does not sell or market your data anywhere in the world."
)

S['onboarding_feature_secure'] = "Your entries stay on the device; AI requests are transmitted encrypted"

S['onboarding_premium_feature_noads'] = "Write and reflect without interruptions"

S['paywall_feature_noads'] = "Write and reflect without interruptions"

S['paywall_feature_profiles'] = "Your own AI profiles with no limit on number"

S['paywall_from_per_day'] = "Annual subscription, %1$s per year"

S['paywall_headline_clarity_sub'] = "The AI makes recurring patterns in your entries visible"

# NEW key (rfind insert)
S['paywall_monthly_note'] = "Then %1$s per month, renews automatically\\nCancel anytime"

S['privacy_gate_groq_body'] = (
"This function sends your voice recording encrypted to Groq (USA) and converts it into text there — faster and more accurate than local recognition.\\n\\n"
"✓ The recording is deleted immediately after conversion\\n"
"✓ No AI training with your recordings\\n"
"✓ Legal basis: standard contractual clauses (EU SCCs)\\n\\n"
"Alternative: local offline recognition in the settings."
)

S['privacy_gate_tts_body'] = (
"With the premium voices, reading aloud sounds more natural. Your text is sent encrypted (HTTPS/TLS) to a Microsoft speech service in the USA and returned as audio.\\n\\n"
"✓ Transmission solely for speech synthesis\\n"
"✓ Third-country transfer to the USA (Microsoft is certified under the EU-US Data Privacy Framework)"
)

S['privacy_gate_tts_title'] = "Enable premium voices"

S['profile_entropy_long'] = (
"The AI finds what weighs on you in everyday life and sorts it out. You get an overview of your biggest sources of strain and 5 concrete tidying-up steps. Ideal when you want to regain your overview."
)

S['retro_benefit_weekly'] = "All weekly reviews instead of just the first 2 – within your daily AI quota"

S['settings_analytics_title'] = "Pseudonymous usage analytics"

S['settings_delete_account_confirm_body'] = (
"This action is irreversible and deletes:\\n\\n"
"• All local journal entries, photos, videos and audio recordings\\n"
"• The app's Google Drive backup (only the app data, not your Google account)\\n"
"• Your app sign-in\\n\\n"
"Your Google account itself remains. The app then restarts as a fresh installation."
)

S['settings_delete_account_subtitle'] = (
"Irreversibly removes all local data and the Drive backup. Your Google account itself remains."
)

S['settings_feedback_dialog_desc'] = (
"Your message to the developers — we read every message and get back to you as quickly as possible."
)

S['settings_premium_feature_5_perspectives'] = "All 4 predefined AI profiles + as many of your own as you need"

S['settings_premium_feature_profiles'] = "Your own AI profiles with no limit on number"

S['settings_report_ai_confirm_body'] = (
"Your email program will open with a prepared message to dev.app.support@gmail.com. You can still add to the description before you send it. We usually reply within 24 hours on business days.\\n\\n"
"Please use this to report: inappropriate, offensive, false or misleading AI outputs from the dashboard, summaries, reviews or the text improvement."
)

S['settings_revoke_confirm_body'] = (
"With one click on „Confirm withdrawal\" we send your withdrawal directly to dev.app.support@gmail.com. You will immediately receive a confirmation of receipt by email.\\n\\n"
"You can find the full withdrawal instructions in the Terms of Use (§ 16). Please also cancel subscriptions via Google Play → Subscriptions so that no further charges occur."
)

S['settings_revoke_confirm_title'] = "Withdraw from contract?"

S['settings_revoke_subtitle'] = "§ 356a BGB — withdraw directly via the app"

# --- Plurals (currently German, replace) ---
PLURALS = {
    'datetime_months_relative': {'one': "%1$d month ago", 'other': "%1$d months ago"},
    'datetime_years_relative':  {'one': "%1$d year ago",  'other': "%1$d years ago"},
}


def xml_escape_value(v):
    """Escape only & and < (XML text). ' and " are fine inside double-quote-delimited
    Android string values; we keep typographic chars literal."""
    # Order matters: & first
    v = v.replace('&', '&amp;')
    v = v.replace('<', '&lt;')
    return v


def main():
    with open(TARGET, 'r', encoding='utf-8') as f:
        content = f.read()

    applied = []
    skipped_already_present = []  # not used as skip here; we REPLACE existing
    replaced = []

    # 1) Replace existing <string name="KEY">...</string> (DOTALL for multiline)
    for key, val in S.items():
        esc = xml_escape_value(val)
        # Does the key already exist as a string?
        pat = re.compile(
            r'<string name="' + re.escape(key) + r'"(\s[^>]*)?>.*?</string>',
            re.DOTALL,
        )
        if pat.search(content):
            # preserve any attributes after the name (e.g. translatable) by capturing group 1
            def _repl(m, _esc=esc):
                attrs = m.group(1) or ''
                return '<string name="' + key + '"' + attrs + '>' + _esc + '</string>'
            content, n = pat.subn(_repl, content, count=1)
            if n == 1:
                replaced.append(key)
            else:
                print('WARN no-sub-existing', key)
        else:
            # NEW key -> rfind insert before </resources>
            idx = content.rfind('</resources>')
            if idx == -1:
                raise RuntimeError('no </resources> in target')
            ins = '    <string name="' + key + '">' + esc + '</string>\n'
            content = content[:idx] + ins + content[idx:]
            applied.append(key)

    # 2) Replace plurals (DOTALL)
    plural_replaced = []
    for pname, forms in PLURALS.items():
        items = ''.join(
            '\n        <item quantity="%s">%s</item>' % (q, xml_escape_value(t))
            for q, t in forms.items()
        )
        block = '<plurals name="' + pname + '">' + items + '\n    </plurals>'
        pat = re.compile(
            r'<plurals name="' + re.escape(pname) + r'">.*?</plurals>',
            re.DOTALL,
        )
        if pat.search(content):
            content, n = pat.subn(lambda m, _b=block: _b, content, count=1)
            if n == 1:
                plural_replaced.append(pname)
            else:
                print('WARN plural no-sub', pname)
        else:
            raise RuntimeError('plural not found: ' + pname)

    with open(TARGET, 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)

    print(json.dumps({
        'replaced_strings': replaced,
        'inserted_strings': applied,
        'replaced_plurals': plural_replaced,
        'count_replaced': len(replaced),
        'count_inserted': len(applied),
    }, ensure_ascii=False))


if __name__ == '__main__':
    main()
