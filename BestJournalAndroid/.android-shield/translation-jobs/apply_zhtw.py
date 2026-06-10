# -*- coding: utf-8 -*-
"""Apply zh-rTW (Traditional Chinese, Taiwan) translations.
Full-replace 34 string keys + new paywall_monthly_note + replace 2 plurals (other only).
Lambda-replacement per key, rfind insert for the new key. FIN-040/048/049 compliant.
"""
import os, re

APP = os.path.expanduser('~/proggs/BestJournalAndroid')
TGT = os.path.join(APP, 'app/src/main/res/values-zh-rTW/strings.xml')

# Traditional Chinese (Taiwan) translations. \\n kept as literal backslash-n (Android escape).
S = {}

S['ai_prompt_custom_intro'] = (
    '你是一位聰明、細心的日記分析師，同時也是任務執行者。\\n\\n'
    '兩步驟工作法：\\n\\n'
    '第一步 — 吸收脈絡：\\n'
    '先把使用者的所有日記條目完整讀過一遍。理解他的處境、模式、主題，以及提到的事物、問題與願望。'
    '這些條目是你的脈絡、你的起點、你的基礎，但它們不會限制你。\\n\\n'
    '第二步 — 執行任務：\\n%1$s\\n\\n'
    '上面才是你真正的任務。請以第一步的脈絡為基礎執行這項任務。如果任務要求查找、構想、替代方案、建議或新資訊，'
    '就主動提供這些內容，即使條目裡沒有出現過。過程中不要給出任何醫療、法律或財務的診斷或具約束力的建議；'
    '遇到這類主題時請轉介給專業人士。條目為你的輸出提供資訊，但不限制它。\\n\\n'
    '核心規則 — 設定檔至少佔一半：\\n'
    '整體輸出（整體分析、首要行動、分類、建議、進展）必須有至少一半清楚可辨地與使用者的任務相連。'
    '與任務沒有任何內容關聯的洞見、要點或主題，請省略 — 不要為了把 JSON 塞滿而用一般生活主題灌水。'
    '寧可少幾條但關聯清楚，也不要許多空泛的。\\n\\n'
    '語言提醒：用多樣的方式表達關聯。善用同義詞、相關詞彙、主題上的鄰近概念與改寫。'
    '不要機械式地重複任務裡的原字 — 那會顯得刻意。例如任務「目標」：也可談及計畫、願望、抱負、想達成的事、你的方向、發展步驟。'
    '例如任務「運動」：也可談及活動、訓練、身體活動、耐力、體能、健康。設定檔的關聯要讓人感受得到，而非字面照搬。\\n\\n'
    '關鍵：\\n'
    '- 任務寫「分析」「總結」「有什麼值得注意」 — 緊貼條目。\\n'
    '- 任務寫「查找」「找替代方案」「提出建議」「推薦」「補充」「如果……會怎樣」「新點子」 — 主動超越條目，帶入你自己的新內容。\\n'
    '- 任務優先。條目是地基，不是牆。'
)

S['ai_prompt_custom_rules'] = (
    '脈絡規則 — 每一條都會被讀：\\n'
    '你會收到編號的條目。請把每一條都完整讀過並當作脈絡吸收。每個相關細節都會納入你的輸出。'
    '但是：當任務有此要求時，輸出可以也應該超越這些條目。\\n\\n'
    '任務規則 — 任務必須被完成：\\n'
    '逐字執行使用者的任務。任務若要求替代方案，就提供真正的新替代方案，而不只是條目裡的東西。'
    '任務若要求推薦，就主動查找並推薦。條目不是你回答的唯一來源。\\n\\n'
    '輸出中的區分：\\n'
    '清楚標示哪些來自條目、哪些是你新加上的。這樣使用者就知道哪些是他自己的想法、哪些是你的貢獻。\\n\\n'
    '專業諮詢規則 — 不下診斷：\\n'
    '無論任務為何，你都不給醫療診斷、治療建議、法律或財務諮詢。遇到這類主題時，請中立地轉介給合格的專業人士。\\n\\n'
    '語言規則：\\n'
    '- 簡單、清楚。不用生僻字。\\n'
    '- 有同理心且直接。\\n'
    '- 不要長破折號（—）。用逗號或句號。\\n\\n'
    '數量規則：\\n'
    '至少 10 條洞見 — 但每一條在內容上都要切合任務。如果你找不到 10 條真正與設定檔相關的洞見，'
    '寧可給 7 條紮實的，也不要 10 條被稀釋的。純分析任務的洞見來自條目；創意任務則可帶入新內容。'
)

S['ai_prompt_entropy_intro'] = (
    '你是一位富有同理心、極其聰明、善解人意的秩序與模式分析師。\\n\\n'
    '你的任務：\\n'
    '分析一位使用者的日記條目。找出反覆出現的壓力、混亂與負擔來源。據此製作一份結構化的建議儀表板，採 JSON 格式。\\n\\n'
    '我們在找什麼 — 個人負擔：\\n'
    '任何在使用者生活中造成混亂、壓力、能量流失、不堪負荷、時間壓力、情緒負擔或失控的事物。\\n\\n'
    '你不給任何醫療、治療或診斷上的建議。遇到健康相關主題時，請中立地轉介給專業人士。'
)

S['ai_prompt_goals_rules'] = (
    '語言規則：\\n'
    '- %1$s\\n'
    '- 鼓勵且誠實，慶祝進步，不粉飾任何事。\\n'
    '- 不要長破折號（—）。用逗號或句號。\\n'
    '- 遇到健康或身體目標時，你只做激勵性的陪伴；不給飲食、藥物或治療建議。\\n\\n'
    '數量規則，每個目標都算：\\n'
    '辨識出所有目標 — 連小的也要。不要合併總結。'
)

S['ai_prompt_insight_attitude'] = (
    '你的姿態：\\n'
    '你是一面善意的鏡子。你誠實地讓使用者看見你在他條目裡讀到的東西 — 但始終以幫助他從中成長為目標。'
    '每一條洞見都應幫助他更瞭解自己。即使是困難的模式，你也清楚指出，但要有建設性、不帶責備。'
    '重點：使用者能從自己的文字裡學到關於自己的什麼？\\n\\n'
    '你不下任何心理診斷，也不指稱任何障礙或疾病。你只描述從使用者自己文字中可觀察到的模式。\\n\\n'
    '你要找的是：\\n'
    '- 反覆出現的情緒：哪些情緒一再浮現？\\n'
    '- 思考模式：使用者如何看待自己、他人、世界？\\n'
    '- 迴避模式：使用者迴避什麼？他從不寫到什麼？\\n'
    '- 強項：使用者擅長什麼，即使他自己沒看見？\\n'
    '- 價值觀：什麼對使用者真正重要（從行動而非言語顯現）？\\n'
    '- 觸發點：什麼會引發強烈反應 — 不論正面或負面？\\n'
    '- 矛盾：使用者說一套，做的卻是另一套？\\n'
    '- 需求：在字裡行間透出、使用者所需要的是什麼？\\n'
    '- 成長：使用者的看法在哪裡改變了？'
)

S['ai_prompt_no_dates_rule'] = (
    '朗讀友善（必須 — 可見文字中禁止日期）：\\n'
    '在所有可見的文字欄位（描述、說明、摘要、整體分析）裡，你不准使用任何日期 — 既不能用「3.4.」「23.04.」「28.4.」「24.04.」「30.4.」，'
    '也不能用「4 月 3 日的條目」「在 23.04.」「來自 28.4.」或類似指向具體條目日期的說法。'
    '這些文字會朗讀給使用者聽 — 一串日期念起來不自然，會打斷洞見的流暢感。\\n\\n'
    '請改成只表達純粹的洞見：\\n'
    '- 錯誤：「3.4.、28.4. 和 23.4. 的條目證明，晚睡讓你的生理節律受損。」\\n'
    '- 正確：「晚睡正在系統性地破壞你的生理節律。」\\n'
    '- 錯誤：「30 日的條目顯示，你的信箱自 28.4. 起就爆滿並偷走你的時間。」\\n'
    '- 正確：「你的信箱已連日爆滿，正在偷走你的時間。」\\n'
    '- 錯誤：「自 24.04. 的整理行動以來，你在日常中更有掌握感。」\\n'
    '- 正確：「活動讓你在日常中明顯更有掌握感。」\\n\\n'
    '內容上的洞見保留 — 只是去掉具體的日期指涉。你仍可使用一般性的時間表述（「反覆地」「最近」「連續好幾天」）。'
    '日期只屬於「herleitung」（來源）欄位（內部使用，不會顯示）。'
)

S['ai_prompt_rerank_system'] = (
    '你是一位設定檔重排器。你會收到一個來自儀表板分析的 5 條首要行動 JSON 陣列、使用者的設定檔焦點，以及原始日記條目。\\n\\n'
    '你的任務分為 3 步：\\n'
    '1）將這 5 條行動排序，使與設定檔焦點關聯最清楚的排在最前面。\\n'
    '2）檢查這 5 條中是否有最多 2 條與設定檔焦點在內容上毫無關聯。若有，就用你從日記條目推導出、設定檔關聯更強的行動取代它們。'
    '維持每條行動相同的 JSON 結構（titel、beschreibung、erklaerung）。\\n'
    '3）以多樣的方式表達設定檔關聯，運用同義詞與主題上的鄰近概念，而非機械式重複設定檔的字眼。關聯要讓人感受得到，而非字面照搬。\\n\\n'
    '重要：\\n'
    '- 最後請正好回傳 5 條。\\n'
    '- 只在真正缺乏設定檔關聯時才更動 — 若契合度已佳，就只排序、不取代。\\n'
    '- 保留每條的 JSON 結構（相同鍵：titel、beschreibung、erklaerung）。\\n'
    '- 語言用德文，不用破折號，beschreibung 為 13 至 21 個字。\\n'
    '- 不給任何醫療、法律或財務的診斷或具約束力的建議；遇到這類主題時請中立地轉介給專業人士。\\n\\n'
    '朗讀友善（必須 — 禁止日期）：\\n'
    '在 beschreibung 和 erklaerung 裡不准使用日期（「3.4.」「23.04.」「30 日的條目」）。這些文字會被朗讀 — 一串日期念起來不自然。'
    '請只表達純粹的洞見，不帶具體條目日期。例如：\\n'
    '- 錯誤：「3.4. 和 23.04. 的條目證明你的生理節律受損。」\\n'
    '- 正確：「晚睡正在系統性地破壞你的生理節律。」\\n'
    '一般性的時間表述是允許的（「反覆地」「最近」「連續好幾天」），日期則不行。\\n\\n'
    '輸出格式：\\n'
    '- 只回傳一個 JSON 陣列（根層級為方括號）。\\n'
    '- 不要 Markdown 反引號，前後不要任何文字。\\n'
    '- 直接以 [ 開頭、以 ] 結尾。'
)

S['churn_offer_feature_perspectives'] = '全部 4 個預設 AI 設定檔 + 你需要多少自訂就有多少'

S['consent_card3_title'] = '匿名統計'

S['consent_toggle_analytics_body'] = (
    'Firebase Analytics — 協助我們改善應用程式。不含任何日記內容，僅匿名的使用統計（例如點擊次數）。'
    '資料傳輸：Google 美國（Data Privacy Framework）。'
)

S['consent_toggle_analytics_title'] = '匿名使用分析'

S['consent_toggle_do_not_sell_body'] = (
    '此開關僅供美國加州居民使用。加州隱私法（CCPA/CPRA）賦予居民拒絕權。'
    '啟用時，所有選用的雲端功能（AI、備份、語音辨識、分析）都會停用 — 僅保留本機功能。\\n\\n'
    '若你不住在加州，可以忽略此開關：Best Journal 不會在世界任何地方販售或行銷你的資料。'
)

S['onboarding_feature_secure'] = '你的條目留在裝置上；AI 請求以加密方式傳輸'

S['onboarding_premium_feature_noads'] = '不受打擾地書寫與反思'

S['paywall_feature_noads'] = '不受打擾地書寫與反思'

S['paywall_feature_profiles'] = '自訂 AI 設定檔，沒有數量上限'

S['paywall_from_per_day'] = '年度訂閱，每年 %1$s'

S['paywall_headline_clarity_sub'] = 'AI 讓你條目中反覆出現的模式變得清晰可見'

S['paywall_monthly_note'] = '之後每月 %1$s，自動續訂\\n隨時可取消'

S['privacy_gate_groq_body'] = (
    '此功能會將你的語音錄音加密傳送到 Groq（美國），並在那裡轉換為文字 — 比本機辨識更快、更準確。\\n\\n'
    '✓ 錄音在轉換後立即刪除\\n'
    '✓ 不會用你的錄音訓練 AI\\n'
    '✓ 法律依據：標準契約條款（EU SCCs）\\n\\n'
    '替代方案：在設定中使用本機離線辨識。'
)

S['privacy_gate_tts_body'] = (
    '使用進階版語音會讓朗讀聽起來更自然。你的文字會加密（HTTPS/TLS）傳送到位於美國的 Microsoft 語音服務，並以音訊形式回傳。\\n\\n'
    '✓ 傳輸僅用於語音合成\\n'
    '✓ 第三國傳輸至美國（Microsoft 已通過 EU-US Data Privacy Framework 認證）'
)

S['privacy_gate_tts_title'] = '啟用進階版語音'

S['profile_entropy_long'] = (
    'AI 會找出日常中讓你負擔的事物，並把它們整理好。你會得到一份最大負擔來源的總覽，以及 5 個具體的整理步驟。'
    '當你想重新找回掌握感時，這最理想。'
)

S['retro_benefit_weekly'] = '所有每週回顧，而不只是前 2 篇 — 在你的 AI 每日額度範圍內'

S['settings_analytics_title'] = '匿名使用分析'

S['settings_delete_account_confirm_body'] = (
    '此操作不可復原，會刪除：\\n\\n'
    '• 所有本機日記條目、照片、影片與語音錄音\\n'
    '• 應用程式的 Google 雲端硬碟備份（僅限應用程式資料，不含你的 Google 帳號）\\n'
    '• 你的應用程式登入\\n\\n'
    '你的 Google 帳號本身將保留。應用程式之後會以全新安裝的狀態重新開始。'
)

S['settings_delete_account_subtitle'] = (
    '不可復原地移除所有本機資料與雲端硬碟備份。你的 Google 帳號本身將保留。'
)

S['settings_feedback_dialog_desc'] = (
    '你給開發者的留言 — 我們會閱讀每一則訊息，並盡快回覆你。'
)

S['settings_premium_feature_5_perspectives'] = '全部 4 個預設 AI 設定檔 + 你需要多少自訂就有多少'

S['settings_premium_feature_profiles'] = '自訂 AI 設定檔，沒有數量上限'

S['settings_report_ai_confirm_body'] = (
    '你的電子郵件程式會開啟一封寄給 dev.app.support@gmail.com 的預填訊息。你可以在寄出前再補充說明。'
    '我們通常會在工作日的 24 小時內回覆。\\n\\n'
    '請以此回報：來自儀表板、摘要、回顧或文字潤飾的不當、令人反感、錯誤或誤導的 AI 輸出。'
)

S['settings_revoke_confirm_body'] = (
    '只要點選「確認撤回」，我們就會將你的撤回直接寄送到 dev.app.support@gmail.com。你會立即收到一封電子郵件的收件確認。\\n\\n'
    '完整的撤回須知請見使用條款（第 16 條）。訂閱請另外透過 Google Play → 訂閱項目取消，以免再產生任何扣款。'
)

S['settings_revoke_confirm_title'] = '撤回合約？'

S['settings_revoke_subtitle'] = '德國民法典第 356a 條 — 直接透過應用程式撤回'

# Plurals: Chinese has no grammatical plural -> "other" only.
PLURALS = {
    'datetime_months_relative': {'other': '%1$d 個月前'},
    'datetime_years_relative': {'other': '%1$d 年前'},
}

# ---- Validation: format args + naked % ----
fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')
def check_fmt(key, val):
    unguarded = fmt.sub('', val).count('%')
    if unguarded != 0:
        raise AssertionError(f"unguarded literal % in {key}: {val!r}")

for k, v in S.items():
    check_fmt(k, v)

# ---- Read target (Python only, not Read-tool) ----
with open(TGT, 'r', encoding='utf-8') as f:
    content = f.read()

def esc(s):
    return s  # values already pre-escaped (\\n literal, no raw quotes besided handled)

applied = []
# Replace each existing <string name="KEY">...</string> via regex (non-greedy, DOTALL)
for key, val in S.items():
    pat = re.compile(r'(<string name="' + re.escape(key) + r'"[^>]*>).*?(</string>)', re.DOTALL)
    if pat.search(content):
        content, n = pat.subn(lambda m: m.group(1) + val + m.group(2), content, count=1)
        if n == 1:
            applied.append(key)
    else:
        # missing -> insert before </resources>
        idx = content.rfind('</resources>')
        if idx == -1:
            raise RuntimeError("no </resources>")
        ins = f'    <string name="{key}">{val}</string>\n'
        content = content[:idx] + ins + content[idx:]
        applied.append(key + ' (NEW)')

# Replace plurals
applied_plurals = []
for key, items in PLURALS.items():
    inner = ''.join(f'        <item quantity="{q}">{t}</item>\n' for q, t in items.items())
    block = f'    <plurals name="{key}">\n{inner}    </plurals>'
    pat = re.compile(r'<plurals name="' + re.escape(key) + r'">.*?</plurals>', re.DOTALL)
    if pat.search(content):
        content = pat.sub(block, content, count=1)
        applied_plurals.append(key)
    else:
        idx = content.rfind('</resources>')
        content = content[:idx] + block + '\n' + content[idx:]
        applied_plurals.append(key + ' (NEW)')

with open(TGT, 'w', encoding='utf-8', newline='\n') as f:
    f.write(content)

print("APPLIED STRINGS:", len(applied))
for a in applied:
    print("  ", a)
print("APPLIED PLURALS:", applied_plurals)
