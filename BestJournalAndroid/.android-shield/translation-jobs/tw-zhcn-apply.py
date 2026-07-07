# -*- coding: utf-8 -*-
"""Translation-Worker zh-rCN (Simplified Chinese) — apply translations to values-zh-rCN/strings.xml.
Replaces 33 existing keys, adds paywall_monthly_note (NEW), replaces 2 stale plurals.
Pure Python IO (FIN-048). \n kept as literal escape; full-width CJK punctuation; half-width numerals."""
import re, sys, io, json, subprocess

TARGET = "app/src/main/res/values-zh-rCN/strings.xml"

# --- Translations (key -> value). \n stays as literal backslash-n in the XML text. ---
T = {
 "ai_prompt_custom_intro": "你是一位聪慧、细心的日记分析师，也是任务执行者。\\n\\n两步工作法：\\n\\n第一步——吸收背景：\\n先把用户的所有日记条目完整读一遍。理解处境、规律、主题，以及提到的事物、问题和愿望。这些条目是你的背景、起点和基础，但它们不会限制你。\\n\\n第二步——执行任务：\\n%1$s\\n\\n上面这段才是你真正的任务。请基于第一步得到的背景来完成它。如果任务要求做调研、出主意、找替代方案、给建议或提供新信息，那就主动给出，哪怕条目里没有出现过。但不要给出医疗、法律或财务方面的诊断或具有约束力的建议；遇到这类话题请引导用户咨询专业人士。条目为你的输出提供信息，而不是限制它。\\n\\n核心规则——画像至少占一半：\\n整体输出（总分析、首要措施、分类、建议、进展）必须有至少 50% 能清楚看出与用户任务相关。与任务在内容上毫无关联的发现、要点或主题，请删掉——不要为了把 JSON 填满而堆砌泛泛的生活话题。宁可少几条有明确关联的，也不要一堆套话。\\n\\n关于语言的重要提示：用富有变化的方式表达这种关联。使用近义词、相关概念、主题上相邻的说法和换一种说法。不要机械地重复任务里的原词——那样显得生硬。比如任务是“目标”：也可以说计划、愿望、抱负、你想达成的事、你的方向、成长的步伐。比如任务是“运动”：也可以说活动、训练、身体锻炼、耐力、健身、健康。画像关联必须能让人感受到，而不是逐字照搬。\\n\\n关键：\\n- 如果任务里写的是“分析”“总结”“有什么值得注意的”，就紧贴条目。\\n- 如果任务里写的是“调研”“找替代方案”“提建议”“推荐”“补充”“假如……会怎样”“新点子”，就主动超出条目，带入你自己的、全新的内容。\\n- 任务优先。条目是地基，不是围墙。",
 "ai_prompt_custom_rules": "背景规则——每一条都要读：\\n你会收到带编号的条目。请把每一条都完整读完，并作为背景吸收进来。每个相关细节都会进入你的输出。但是：当任务要求时，输出可以也应当超出条目本身。\\n\\n任务规则——任务必须完成：\\n请逐字执行用户的任务。如果任务问替代方案，就给出真正全新的替代方案，而不只是条目里的东西。如果任务要建议，就主动调研并推荐。条目不是你回答的唯一来源。\\n\\n输出中要区分：\\n清楚标明哪些来自条目，哪些是你新增的。这样用户就知道哪些是他自己的想法，哪些是你的贡献。\\n\\n专业咨询规则——不做诊断：\\n无论任务是什么，你都不给出医疗诊断、治疗建议、法律或财务咨询。遇到这类话题请中立地引导用户咨询有资质的专业人士。\\n\\n语言规则：\\n- 简单、清楚。不用生僻词。\\n- 有同理心，且直接。\\n- 不用长破折号（—）。用逗号或句号。\\n\\n数量规则：\\n总共至少 10 条发现——但每一条在内容上都必须与任务相关。如果你找不到 10 条与画像真正相关的发现，宁可给 7 条有力的，也不要 10 条注水的。纯分析类任务里，发现来自条目；创意类任务里，你可以带入新内容。",
 "ai_prompt_entropy_intro": "你是一位富有同理心、极其聪慧、善解人意的秩序与规律分析师。\\n\\n你的任务：\\n分析一位用户的日记条目。找出反复出现的压力、混乱和负担的来源。据此生成一个 JSON 格式的结构化建议面板。\\n\\n我们要找的是——个人负担：\\n一切在用户生活中制造混乱、压力、精力流失、力不从心、时间压力、情绪负担或失控感的东西。\\n\\n你不给出医疗、治疗或诊断方面的建议。遇到健康相关话题，请中立地引导用户咨询专业人士。",
 "ai_prompt_goals_rules": "语言规则：\\n- %1$s\\n- 既鼓舞人心又诚实，为进步喝彩，但不粉饰任何东西。\\n- 不用长破折号（—）。用逗号或句号。\\n- 对于健康或身体方面的目标，你只做鼓励性的陪伴；不给出节食、用药或治疗建议。\\n\\n数量规则，每个目标都算数：\\n识别所有目标——再小也要算。不要做归并。",
 "ai_prompt_insight_attitude": "你的姿态：\\n你是一面善意的镜子。你诚实地把你在条目里看到的呈现给用户——但始终以让他能从中成长为目标。每一条发现都应帮助他更好地理解自己。即便是棘手的规律，你也要说清楚，但要带着建设性、不带指责。重点：用户能从自己的话语中学到什么？\\n\\n你不做任何心理诊断，不指认任何障碍或疾病。你只描述从用户自己话语中可观察到的规律。\\n\\n你要找的是：\\n- 反复出现的情绪：哪些情绪一再冒出来？\\n- 思维模式：用户怎样看待自己、他人和世界？\\n- 回避模式：用户在绕开什么？他从不写什么？\\n- 长处：用户做得好、却连自己都没看见的是什么？\\n- 价值观：用户真正看重的是什么（体现在行动里，而非言语里）？\\n- 触发点：什么会引发强烈反应——无论正面还是负面？\\n- 矛盾：用户嘴上说一套，做的却是另一套？\\n- 需求：字里行间透出的、用户需要的是什么？\\n- 成长：用户的看法在哪里发生了改变？",
 "ai_prompt_no_dates_rule": "适合 TTS 朗读（必须遵守——可见文本中禁用日期）：\\n在所有可见的文本字段里（beschreibung、erklaerung、zusammenfassung、gesamtanalyse），你都不可以使用日期——既不能写“3.4.”“23.04.”“28.4.”“24.04.”“30.4.”，也不能写“4 月 3 日的条目”“在 23.04.”“来自 28.4.”或类似指向具体条目日期的说法。这些文本会朗读给用户听——一串日期念出来很不自然，会打断领悟的流畅。\\n\\n请改为只写纯粹的领悟：\\n- 错误：“3.4.、28.4. 和 23.4. 的条目证明，你的生物钟因晚睡而受损。”\\n- 正确：“晚睡正在系统性地破坏你的生物钟。”\\n- 错误：“30 号的条目显示，你的收件箱自 28.4. 起就满溢，在偷走你的时间。”\\n- 正确：“你的收件箱连日来一直溢出，在偷走你的时间。”\\n- 错误：“自从 24.04. 的整理行动以来，你在日常中明显更有条理了。”\\n- 正确：“运动让你在日常中明显更有条理。”\\n\\n领悟的内容保留——只是去掉具体的日期指向。你仍然可以使用笼统的时间表述（“反复地”“最近”“连续好几天”）。日期只能出现在 “herleitung” 字段里（出处，内部使用，不显示）。",
 "ai_prompt_rerank_system": "你是一位画像再排序者。你会收到来自面板分析的 5 条首要措施的 JSON 数组、用户的画像焦点，以及原始日记条目。\\n\\n你的任务分三步：\\n1）把这 5 条措施排序，让与画像焦点关联最清晰的排在最前。\\n2）检查这 5 条里是否有最多 2 条与画像焦点在内容上毫无关联。如果有，就用你从日记条目里推导出的、更贴合画像的措施来替换它们。每条措施沿用相同的 JSON 结构（titel、beschreibung、erklaerung）。\\n3）用富有变化的方式表达画像关联，使用近义词和主题上相邻的说法，不要机械重复画像里的原词。关联必须能让人感受到，而不是逐字照搬。\\n\\n重要：\\n- 最后正好返回 5 条。\\n- 只有在确实缺少真正的画像关联时才改动——如果本来就贴合，就只排序，不替换。\\n- 保留每条的 JSON 结构（相同的键：titel、beschreibung、erklaerung）。\\n- 语言用中文，不用破折号，beschreibung 为 13 到 21 个词。\\n- 不给出医疗、法律或财务方面的诊断或具有约束力的建议；遇到这类话题请中立地引导用户咨询专业人士。\\n\\n适合 TTS 朗读（必须遵守——禁用日期）：\\n在 beschreibung 和 erklaerung 里不要使用日期（“3.4.”“23.04.”“30 号的条目”）。这些文本会被朗读——一串日期念出来很不自然。请只写纯粹的领悟，不带具体的条目日期。例如：\\n- 错误：“3.4. 和 23.04. 的条目证明你的生物钟受损。”\\n- 正确：“晚睡正在系统性地破坏你的生物钟。”\\n笼统的时间表述是允许的（“反复地”“最近”“连续好几天”），日期则不行。\\n\\n输出格式：\\n- 只输出一个 JSON 数组（根层级是方括号）。\\n- 不要 Markdown 反引号，前后不要任何文字。\\n- 直接以 [ 开始，以 ] 结束。",
 "churn_offer_feature_perspectives": "全部 4 个预设 AI 画像 + 你需要多少个自定义画像都行",
 "consent_card3_title": "假名化统计",
 "consent_toggle_analytics_body": "Firebase Analytics——帮助我们改进应用。不含任何日记内容，只有假名化的使用统计（例如点击次数）。数据传输：Google 美国（数据隐私框架）。",
 "consent_toggle_analytics_title": "假名化使用分析",
 "consent_toggle_do_not_sell_body": "此开关仅面向美国加利福尼亚州居民。加州隐私法（CCPA/CPRA）赋予居民拒绝权。开启后，所有可选的云端功能（AI、备份、语音识别、分析）都会被停用——只有本地功能保持启用。\\n\\n如果你居住在加州以外，可以忽略此开关：Best Journal 不会在世界上任何地方出售或交易你的数据。",
 "onboarding_feature_secure": "你的条目保留在设备上；AI 请求会加密传输",
 "onboarding_premium_feature_noads": "不受打扰地书写与反思",
 "paywall_feature_noads": "不受打扰地书写与反思",
 "paywall_feature_profiles": "自定义 AI 画像，数量不限",
 "paywall_from_per_day": "年度订阅，每年 %1$s",
 "paywall_headline_clarity_sub": "AI 让你条目中反复出现的规律清晰可见",
 "paywall_monthly_note": "之后每月 %1$s，自动续订\\n随时可取消",
 "privacy_gate_groq_body": "此功能会把你的语音录音加密发送到 Groq（美国），并在那里转换成文字——比本地识别更快、更准确。\\n\\n✓ 录音在转换后会立即删除\\n✓ 不会用你的录音训练 AI\\n✓ 法律依据：标准合同条款（EU SCCs）\\n\\n替代方案：在设置里使用本地离线识别。",
 "privacy_gate_tts_body": "使用高级语音，朗读听起来更自然。你的文字会加密（HTTPS/TLS）发送到位于美国的微软语音服务，并以音频形式返回。\\n\\n✓ 传输仅用于语音合成\\n✓ 向第三国美国传输（微软已通过欧盟-美国数据隐私框架认证）",
 "privacy_gate_tts_title": "启用高级语音",
 "profile_entropy_long": "AI 会找出在日常中让你感到负担的东西，并加以整理。你会得到一份最大负担来源的概览，以及 5 个具体的整理步骤。如果你想重新掌控全局，这非常合适。",
 "retro_benefit_weekly": "所有每周回顾，而不只是前 2 篇——在你的 AI 每日额度范围内",
 "settings_analytics_title": "假名化使用分析",
 "settings_delete_account_confirm_body": "此操作不可撤销，将删除：\\n\\n• 所有本地日记条目、照片、视频和录音\\n• 应用的 Google Drive 备份（仅应用数据，不含你的 Google 账号）\\n• 你的应用登录\\n\\n你的 Google 账号本身将保留。之后应用会作为全新安装重新启动。",
 "settings_delete_account_subtitle": "不可撤销地删除所有本地数据和 Drive 备份。你的 Google 账号本身将保留。",
 "settings_feedback_dialog_desc": "你给开发者的留言——我们会阅读每一条留言，并尽快回复你。",
 "settings_premium_feature_5_perspectives": "全部 4 个预设 AI 画像 + 你需要多少个自定义画像都行",
 "settings_premium_feature_profiles": "自定义 AI 画像，数量不限",
 "settings_report_ai_confirm_body": "你的电子邮件程序会打开，里面有一封发给 dev.app.support@gmail.com 的预填邮件。在发送前，你还可以补充描述。我们通常会在工作日 24 小时内回复。\\n\\n请用它来举报：来自面板、总结、回顾或文本改进的不当、令人反感、错误或具有误导性的 AI 输出。",
 "settings_revoke_confirm_body": "点击“确认撤回”，我们就会把你的撤回直接发送到 dev.app.support@gmail.com。你会立即收到一封确认收讫的电子邮件。\\n\\n完整的撤回须知见使用条款（第 16 条）。订阅请另外通过 Google Play → 订阅 取消，以免产生进一步的扣费。",
 "settings_revoke_confirm_title": "撤回合同？",
 "settings_revoke_subtitle": "德国民法典第 356a 条——通过应用直接撤回",
}

# Plurals: zh has only "other"
PLURALS = {
 "datetime_months_relative": {"other": "%1$d 个月前"},
 "datetime_years_relative": {"other": "%1$d 年前"},
}

def esc(s):
    # XML-escape only & (others already fine); apostrophes in CJK n/a
    return s

# --- Build replacement for each existing string key ---
content = open(TARGET, encoding='utf-8').read()
applied = []
new_only = []

for k, v in T.items():
    # find existing <string name="k" ...>...</string>
    pat = re.compile(r'(<string name="%s"[^>]*>).*?(</string>)' % re.escape(k), re.DOTALL)
    repl = r'\g<1>' + v.replace('\\', r'\\') + r'\g<2>'
    # use a function to avoid backreference issues with $ in v
    def make_sub(val):
        def _s(m):
            return m.group(1) + val + m.group(2)
        return _s
    if pat.search(content):
        content = pat.sub(make_sub(v), content, count=1)
        applied.append(k)
    else:
        # NEW string: insert before </resources>
        idx = content.rfind('</resources>')
        if idx == -1:
            raise RuntimeError("no </resources>")
        ins = '    <string name="%s">%s</string>\n' % (k, v)
        content = content[:idx] + ins + content[idx:]
        new_only.append(k)

# --- Replace plurals ---
for pk, forms in PLURALS.items():
    pat = re.compile(r'<plurals name="%s">.*?</plurals>' % re.escape(pk), re.DOTALL)
    body = '\n'.join('        <item quantity="%s">%s</item>' % (q, t) for q, t in forms.items())
    new_block = '<plurals name="%s">\n%s\n    </plurals>' % (pk, body)
    if not pat.search(content):
        raise RuntimeError("plural not found: " + pk)
    content = pat.sub(lambda m, nb=new_block: nb, content, count=1)
    applied.append(pk)

with open(TARGET, 'w', encoding='utf-8', newline='\n') as f:
    f.write(content)

print("APPLIED", len(applied), "NEW", new_only)
