# -*- coding: utf-8 -*-
"""finale Translation-Worker apply script for ja (Japanese).
Replaces 34 string keys (paywall_monthly_note NEW) + 2 plurals in values-ja/strings.xml.
Read/write target ONLY via Python file-IO (FIN-048). rfind-insert for the new key (FIN-040).
All teineigo (です/ます) consistent with bestand. Widerruf=契約解除, Auto-Renewal=自動更新.
"""
import re, os, json

APP = os.path.expanduser('~/proggs/BestJournalAndroid')
F = os.path.join(APP, 'app/src/main/res/values-ja/strings.xml')
CKPT = os.path.join(APP, '.android-shield/translation-jobs/worker-checkpoints/tw-ja.jsonl')

# ---- TRANSLATIONS (literal XML body; \n and %1$s etc. kept EXACT from DE) ----
# \\n in a Python triple-string = backslash-n = the literal two chars android needs.
STRINGS = {
 "ai_prompt_custom_intro":
  "あなたは知的で注意深い日記アナリストであり、タスク実行者でもあります。"
  "\\n\\n2つのステップで作業します:"
  "\\n\\nステップ1 — コンテキストを把握する:"
  "\\nまずユーザーのすべての日記エントリーを最後まで読んでください。状況、パターン、テーマ、書かれている事柄、問題、願いを理解します。エントリーはあなたのコンテキスト、出発点、土台です。ただしあなたを制限するものではありません。"
  "\\n\\nステップ2 — タスクを実行する:"
  "\\n%1$s"
  "\\n\\n上記があなたの本来の任務です。ステップ1のコンテキストをもとにこの任務を実行してください。任務が調査、アイデア、代替案、提案、新しい情報を求める場合は、エントリーに出てこなくてもそれらを積極的に提供してください。その際、医学的、法律的、財務的な診断や確定的な助言は行わず、そうしたテーマでは専門家を案内してください。エントリーはあなたのアウトプットに情報を与えますが、それを制限するものではありません。"
  "\\n\\n基本ルール — プロフィールが少なくとも50パーセントを占める:"
  "\\nアウトプット全体(総合分析、最重要の対策、カテゴリー、助言、進捗)は、少なくとも50パーセントがユーザーの任務とはっきり結びついていなければなりません。任務と内容的なつながりがない発見、項目、テーマは省いてください。JSONを埋めるためだけに一般的な人生のテーマでアウトプットを水増ししないこと。つながりの薄い項目を多く出すより、明確なつながりのある項目を少なく出す方がよいです。"
  "\\n\\n言葉づかいについて重要なこと: つながりを多彩に表現してください。同義語、関連する言葉、テーマ的に近い概念、言い換えを使ってください。任務の言葉をそのまま機械的に繰り返さないこと。押しつけがましく見えます。任務が「目標」の例: 計画、願い、抱負、達成したいこと、進む方向、成長のステップとしても表現します。任務が「運動」の例: 体を動かすこと、トレーニング、身体活動、持久力、フィットネス、健康としても表現します。プロフィールとのつながりは字句どおりではなく、感じ取れるものでなければなりません。"
  "\\n\\n決定的なこと:"
  "\\n- 任務に「分析して」「まとめて」「何が目立つか」とある場合は、エントリーに寄り添ってください。"
  "\\n- 任務に「調査して」「代替案を見つけて」「提案して」「勧めて」「補って」「もし〜ならどうか」「新しいアイデア」とある場合は、エントリーを超えて積極的に独自の新しい内容を取り入れてください。"
  "\\n- 任務が優先されます。エントリーは土台であって、壁ではありません。",

 "ai_prompt_custom_rules":
  "コンテキストのルール — すべてのエントリーを読む:"
  "\\n番号付きのエントリーを受け取ります。一つひとつを最後まで読み、コンテキストとして把握してください。関連するすべての細部がアウトプットに反映されます。ただし任務が求める場合、アウトプットはエントリーを超えてよく、また超えるべきです。"
  "\\n\\n任務のルール — 任務を遂行する:"
  "\\nユーザーの任務を文字どおり遂行してください。任務が代替案を求めるなら、エントリーにあるものだけでなく、本当に新しい代替案を提供します。任務が推薦を求めるなら、積極的に調査して推薦します。エントリーはあなたの回答の唯一の情報源ではありません。"
  "\\n\\nアウトプットでの区別:"
  "\\nエントリーから来たものと、あなたが新たに加えたものをはっきり示してください。そうすればユーザーは、どれが自分自身の考えで、どれがあなたの貢献かが分かります。"
  "\\n\\n専門相談のルール — 診断は行わない:"
  "\\n任務にかかわらず、医学的診断、治療の推奨、法律・財務の助言は行いません。そうしたテーマでは中立的に有資格の専門家を案内してください。"
  "\\n\\n言葉のルール:"
  "\\n- 簡潔で明確に。難しい外来語は使わない。"
  "\\n- 共感的かつ率直に。"
  "\\n- 長いダッシュ(—)は使わない。読点や句点を使う。"
  "\\n\\n分量のルール:"
  "\\n全体で少なくとも10個の発見を。ただし一つひとつが内容的に任務に合っていなければなりません。プロフィールと本当につながる発見が10個見つからない場合は、水増しした10個より、しっかりした7個を出す方がよいです。純粋な分析の任務では発見はエントリーから生まれ、創造的な任務では新しい内容を取り入れてかまいません。",

 "ai_prompt_entropy_intro":
  "あなたは共感的で、きわめて知的で、思いやりのある秩序とパターンのアナリストです。"
  "\\n\\nあなたの任務:"
  "\\nユーザーの日記エントリーを分析してください。ストレス、混乱、負担の繰り返される原因を見つけます。それをもとに、構造化された助言ダッシュボードをJSON形式で作成してください。"
  "\\n\\n私たちが探すもの — 個人的な負担:"
  "\\nユーザーの生活の中で、混乱、ストレス、エネルギーの消耗、過負荷、時間的プレッシャー、感情的な負担、コントロールの喪失を生むものすべて。"
  "\\n\\nあなたは医学的、治療的、診断的な助言は行いません。健康に関するテーマでは中立的に専門家を案内してください。",

 "ai_prompt_goals_rules":
  "言葉のルール:"
  "\\n- %1$s"
  "\\n- やる気を引き出し、率直に。進歩を称え、何もごまかさない。"
  "\\n- 長いダッシュ(—)は使わない。読点や句点を使う。"
  "\\n- 健康や身体に関する目標では、やる気を支えるだけにとどめ、食事・薬・治療の推奨は行わない。"
  "\\n\\n分量のルール、すべての目標が大切:"
  "\\nすべての目標を認識してください。小さなものも含めて。まとめないこと。",

 "ai_prompt_insight_attitude":
  "あなたの姿勢:"
  "\\nあなたは好意的な鏡です。エントリーから読み取ったことをユーザーに率直に示します。ただし常に、ユーザーがそこから成長できることを目的とします。どの発見も、ユーザーが自分自身をよりよく理解する助けになるべきです。難しいパターンもはっきり名指ししますが、建設的に、非難なしで伝えます。焦点: ユーザーは自分自身の言葉から、自分について何を学べるか。"
  "\\n\\nあなたは心理学的な診断を行わず、障害や病気を名指ししません。ユーザー自身の言葉から観察できるパターンだけを記述します。"
  "\\n\\nあなたが探すもの:"
  "\\n- 繰り返される感情: どの感情が何度も現れるか。"
  "\\n- 思考パターン: ユーザーは自分、他者、世界についてどう考えているか。"
  "\\n- 回避のパターン: ユーザーは何を避けているか。何について決して書かないか。"
  "\\n- 強み: ユーザーが自分では気づいていなくても、うまくできていることは何か。"
  "\\n- 価値観: ユーザーにとって本当に大切なものは何か(言葉ではなく行動に表れる)。"
  "\\n- きっかけ: 何が強い反応を引き起こすか。良い面でも悪い面でも。"
  "\\n- 矛盾: ユーザーが何かを言いながら、別の行動をしていないか。"
  "\\n- ニーズ: 行間からにじみ出る、ユーザーが必要としているものは何か。"
  "\\n- 成長: ユーザーの見方はどこで変わったか。",

 "ai_prompt_no_dates_rule":
  "TTSで読み上げやすく(必須 — 表示テキストでの日付禁止):"
  "\\nすべての表示テキスト欄(説明、解説、要約、総合分析)で、日付を使ってはいけません。「3.4.」「23.04.」「28.4.」「24.04.」「30.4.」も、「4月3日のエントリー」「23.04.に」「28.4.の」のような具体的なエントリー日付への言及もです。これらのテキストはユーザーに読み上げられます。日付の連なりは読み上げると不自然に聞こえ、発見の流れを断ち切ります。"
  "\\n\\n代わりに純粋な発見を表現してください:"
  "\\n- 誤: 「3.4.、28.4.、23.4.のエントリーが、遅い就寝時間で体内リズムが乱れていることを示している。」"
  "\\n- 正: 「遅い就寝時間が体内リズムを体系的に壊している。」"
  "\\n- 誤: 「30日のエントリーが、28.4.以降あなたの受信箱があふれ、時間を奪っていることを示している。」"
  "\\n- 正: 「あなたの受信箱は何日もあふれ続け、時間を奪っている。」"
  "\\n- 誤: 「24.04.の片づけ以降、日常でより見通しが立っていると感じる。」"
  "\\n- 正: 「体を動かすことで、日常に明らかに見通しが生まれている。」"
  "\\n\\n内容としての発見は残ります。具体的な日付への言及だけが省かれます。一般的な時間的表現(「繰り返し」「最近」「数日にわたって」)は引き続き使えます。日付は「herleitung」欄にのみ入れてください(出所であり、内部で使われ、表示されません)。",

 "ai_prompt_rerank_system":
  "あなたはプロフィール・リランカーです。ダッシュボード分析からの最重要対策5件のJSON配列、ユーザーのプロフィールの焦点、そして元の日記エントリーを受け取ります。"
  "\\n\\nあなたの任務は3つのステップです:"
  "\\n1) プロフィールの焦点と最も明確につながる対策が先に来るよう、5件の対策を並べ替えてください。"
  "\\n2) 5件のうち最大2件が、プロフィールの焦点と内容的なつながりを持たないかどうか確認してください。そうである場合は、日記エントリーから導き出した、よりプロフィールに合う対策に置き換えます。各対策の同じJSON構造(titel、beschreibung、erklaerung)を守ってください。"
  "\\n3) プロフィールとのつながりは、プロフィールの言葉を機械的に繰り返すのではなく、同義語やテーマ的に近い概念を使って多彩に表現してください。つながりは字句どおりではなく、感じ取れるものでなければなりません。"
  "\\n\\n重要:"
  "\\n- 最後にちょうど5件を返してください。"
  "\\n- 変更は本当にプロフィールとのつながりが欠けている場合のみ。すでに適合が良い場合は配列を並べ替えるだけで、何も置き換えないこと。"
  "\\n- 各項目のJSON構造(同じキー: titel、beschreibung、erklaerung)を保ってください。"
  "\\n- 言語は日本語、ダッシュは使わない、beschreibungは13〜21語。"
  "\\n- 医学的、法律的、財務的な診断や確定的な助言は行わないこと。そうしたテーマでは中立的に専門家を案内してください。"
  "\\n\\nTTSで読み上げやすく(必須 — 日付禁止):"
  "\\nbeschreibungとerklaerungでは日付を使わないこと(「3.4.」「23.04.」「30日のエントリー」)。これらのテキストは読み上げられます。日付の連なりは不自然に聞こえます。具体的なエントリー日付なしで、純粋な発見を表現してください。例:"
  "\\n- 誤: 「3.4.と23.04.のエントリーが、あなたの体内リズムが乱れていることを示している。」"
  "\\n- 正: 「遅い就寝時間が体内リズムを体系的に壊している。」"
  "\\n一般的な時間的表現(「繰り返し」「最近」「数日にわたって」)は許されますが、日付は許されません。"
  "\\n\\n出力形式:"
  "\\n- JSON配列のみ(ルートレベルは角かっこ)。"
  "\\n- Markdownのバッククォートは使わず、前後にテキストを付けない。"
  "\\n- [ で始め、] で終えてください。",

 "churn_offer_feature_perspectives":
  "あらかじめ用意された4つのAIプロフィール + 必要なだけのオリジナル",

 "consent_card3_title":
  "仮名化された統計",

 "consent_toggle_analytics_body":
  "Firebase Analytics — アプリの改善に役立ちます。日記の内容は含まれず、仮名化された利用統計(例: クリック数)のみです。データ送信先: Googleアメリカ(Data Privacy Framework)。",

 "consent_toggle_analytics_title":
  "仮名化された利用分析",

 "consent_toggle_do_not_sell_body":
  "このスイッチは、アメリカ・カリフォルニア州の住民のためだけのものです。カリフォルニア州のプライバシー法(CCPA/CPRA)は住民に拒否権を認めています。有効にすると、すべての任意のクラウド機能(AI、バックアップ、音声認識、分析)が無効になり、ローカル機能のみが有効なまま残ります。"
  "\\n\\nカリフォルニア州の外にお住まいなら、このスイッチは無視してかまいません。Best Journalは、世界のどこでもあなたのデータを販売したり売り込んだりしません。",

 "onboarding_feature_secure":
  "あなたのエントリーは端末に残り、AIへのリクエストは暗号化して送信されます",

 "onboarding_premium_feature_noads":
  "邪魔されずに書いて振り返る",

 "paywall_feature_noads":
  "邪魔されずに書いて振り返る",

 "paywall_feature_profiles":
  "件数制限のないオリジナルAIプロフィール",

 "paywall_from_per_day":
  "年間プラン、年額%1$s",

 "paywall_headline_clarity_sub":
  "AIが、あなたのエントリーの中の繰り返されるパターンを見えるようにします",

 # NEW key (count 0)
 "paywall_monthly_note":
  "その後は月額%1$s、自動更新されます\\nいつでも解約可能",

 "privacy_gate_groq_body":
  "この機能は、あなたの音声録音を暗号化してGroq(アメリカ)に送り、そこでテキストに変換します。ローカルの認識より速く、正確です。"
  "\\n\\n✓ 録音は変換後すぐに削除されます"
  "\\n✓ あなたの録音でAIの学習は行いません"
  "\\n✓ 法的根拠: 標準契約条項(EU SCCs)"
  "\\n\\n代替手段: 設定でローカルのオフライン認識を選べます。",

 "privacy_gate_tts_body":
  "プレミアム音声を使うと、読み上げがより自然に聞こえます。あなたのテキストは暗号化(HTTPS/TLS)してアメリカのMicrosoft音声サービスに送られ、音声として返されます。"
  "\\n\\n✓ 送信は音声合成のためだけに行われます"
  "\\n✓ 第三国アメリカへの移転(MicrosoftはEU-US Data Privacy Frameworkの認証を取得しています)",

 "privacy_gate_tts_title":
  "プレミアム音声を有効にする",

 "profile_entropy_long":
  "AIが、あなたを日常で悩ませているものを見つけ、整理します。最も大きな負担の原因の一覧と、5つの具体的な片づけステップが手に入ります。見通しを取り戻したいときに最適です。",

 "retro_benefit_weekly":
  "最初の2件だけでなく、すべての週間振り返り（AIの1日あたりの利用枠の範囲内で）",

 "settings_analytics_title":
  "仮名化された利用分析",

 "settings_delete_account_confirm_body":
  "この操作は元に戻せず、以下を削除します:"
  "\\n\\n• すべてのローカル日記エントリー、写真、動画、音声録音"
  "\\n• アプリのGoogle Driveバックアップ(アプリのデータのみで、あなたのGoogleアカウントは対象外)"
  "\\n• アプリへのログイン"
  "\\n\\nGoogleアカウント自体は残ります。アプリはその後、新規インストールとして再起動します。",

 "settings_delete_account_subtitle":
  "すべてのローカルデータとDriveバックアップを元に戻せない形で削除します。Googleアカウント自体は残ります。",

 "settings_feedback_dialog_desc":
  "開発者へのあなたのメッセージです。いただいたメッセージはすべて読み、できるだけ早くお返事します。",

 "settings_premium_feature_5_perspectives":
  "あらかじめ用意された4つのAIプロフィール + 必要なだけのオリジナル",

 "settings_premium_feature_profiles":
  "件数制限のないオリジナルAIプロフィール",

 "settings_report_ai_confirm_body":
  "あなたのメールソフトが開き、dev.app.support@gmail.com 宛ての下書きメッセージが用意されます。送信する前に説明を補うことができます。通常、営業日の24時間以内に返信します。"
  "\\n\\nダッシュボード、要約、振り返り、文章改善から生じた、不適切、不快、誤った、または誤解を招くAIの出力を、これでご報告ください。",

 "settings_revoke_confirm_body":
  "「契約解除を確定」をクリックすると、あなたの契約解除を dev.app.support@gmail.com に直接送信します。すぐにメールで受領確認が届きます。"
  "\\n\\n契約解除に関する完全な説明は、利用規約(§ 16)に記載されています。サブスクリプションはさらにGoogle Play → 定期購入から解約し、これ以上の請求が発生しないようにしてください。",

 "settings_revoke_confirm_title":
  "契約を解除しますか？",

 "settings_revoke_subtitle":
  "ドイツ民法 § 356a — アプリから直接、契約解除",
}

# ---- PLURALS (ja: CLDR 'other' only) ----
PLURALS = {
 "datetime_months_relative": {"other": "%1$dか月前"},
 "datetime_years_relative":  {"other": "%1$d年前"},
}

def esc(v):
    """Body is already XML-safe (no raw & < >). Apostrophes/quotes: ja has none needing escape;
    existing bestand uses raw \" inside? -> we keep '' none. Return as-is."""
    return v

def main():
    with open(F, 'r', encoding='utf-8') as fh:
        content = fh.read()
    applied = []
    new_inserted = []

    # 1) Replace existing string keys in-place
    for key, val in STRINGS.items():
        body = esc(val)
        pat = re.compile(r'(<string name="' + re.escape(key) + r'"[^>]*>).*?(</string>)', re.S)
        if pat.search(content):
            content = pat.sub(lambda m: m.group(1) + body + m.group(2), content, count=1)
            applied.append(key)
        else:
            # NEW key -> rfind insert (paywall_monthly_note)
            line = '    <string name="%s">%s</string>' % (key, body)
            idx = content.rfind('</resources>')
            if idx == -1:
                raise RuntimeError("kein </resources> gefunden — Datei korrupt")
            content = content[:idx] + line + '\n' + content[idx:]
            new_inserted.append(key)

    # 2) Replace plurals
    for key, items in PLURALS.items():
        inner = ''.join('\n        <item quantity="%s">%s</item>' % (q, esc(t)) for q, t in items.items())
        block = '<plurals name="%s">%s\n    </plurals>' % (key, inner)
        pat = re.compile(r'<plurals name="' + re.escape(key) + r'">.*?</plurals>', re.S)
        if pat.search(content):
            content = pat.sub(lambda m: block, content, count=1)
            applied.append('PLURAL:' + key)
        else:
            line = '    ' + block
            idx = content.rfind('</resources>')
            content = content[:idx] + line + '\n' + content[idx:]
            new_inserted.append('PLURAL:' + key)

    with open(F, 'w', encoding='utf-8', newline='\n') as fh:
        fh.write(content)

    res = {'applied': applied, 'new_inserted': new_inserted,
           'count_applied': len(applied), 'count_new': len(new_inserted)}
    print(json.dumps(res, ensure_ascii=False))
    with open(CKPT, 'a', encoding='utf-8') as fh:
        fh.write(json.dumps({'phase': 'applied', **res, 'ts': '2026-06-10'}, ensure_ascii=False) + '\n')

if __name__ == '__main__':
    main()
