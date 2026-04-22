#!/usr/bin/env python3
"""Batch 3: ko, zh-rCN, zh-rTW, ar, hi."""
import os, re, tempfile

APP_DIR = os.path.expanduser("~/proggs/BestJournalAndroid/app/src/main/res")
TRANSLATIONS = {}

# ═══════════ KOREAN (ko) — Haeyoche (해요체), Hangul ═══════════
TRANSLATIONS["ko"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">개인정보와 동의</string>
    <string name="consent_intro">일기는 당신의 개인적인 공간이며, 우리는 이를 존중해요. Best Journal이 데이터를 어떻게 처리하는지 여기서 투명하게 볼 수 있어요.</string>
    <string name="consent_card1_title">로컬 저장</string>
    <string name="consent_card1_body">기록은 기기에 남아 있어요.</string>
    <string name="consent_card2_title">AI 기능 (미국)</string>
    <string name="consent_card2_body">선택적으로 텍스트는 Google Gemini로, 음성 녹음은 Groq로, 읽어주기 텍스트는 Microsoft Edge로 미국에 전송돼요 (EU-US Data Privacy Framework + 표준 계약 조항).</string>
    <string name="consent_card3_title">익명 통계</string>
    <string name="consent_card3_body">Firebase Analytics, 선택, 설정에서 언제든 변경할 수 있어요.</string>
    <string name="consent_links_header">법적 문서:</string>
    <string name="consent_accept_all">동의하고 시작하기</string>
    <string name="consent_disable_stats">통계 끄고 계속하기</string>
    <string name="consent_confirmation">「동의하고 시작하기」를 누르면 개인정보 처리방침, 이용약관, 법적 고지를 읽었고 설명된 데이터 처리에 동의함을 확인해요. 설정에서 언제든 변경할 수 있어요.</string>

    <string name="privacy_gate_groq_title">음성 녹음을 Groq에 보낼까요?</string>
    <string name="privacy_gate_groq_body">클라우드 전사를 위해 음성 녹음이 암호화되어 Groq, Inc. (마운틴뷰, 미국)로 전송되어 텍스트로 변환돼요. 오디오 파일은 처리 후 삭제되며 학습에 사용되지 않아요.\n\n대안: 기기 내 로컬 전사(오프라인, 데이터 전송 없음)를 사용하세요. 설정 → AI 에서 전환할 수 있어요.</string>
    <string name="privacy_gate_groq_accept">동의하고 전송</string>
    <string name="privacy_gate_groq_local">로컬로 전사하기</string>

    <string name="privacy_gate_gemini_title">텍스트를 Google Gemini에 보낼까요?</string>
    <string name="privacy_gate_gemini_body">AI 기능(대시보드, 요약, 회고, 텍스트 개선)을 위해 기록의 일부가 암호화되어 Google Gemini(Firebase AI, 미국)로 전송돼요. 법적 근거: EU-US Data Privacy Framework + 표준 계약 조항. 요청은 처리 후 삭제되며 학습에 사용되지 않아요.</string>
    <string name="privacy_gate_gemini_accept">동의하고 전송</string>
    <string name="privacy_gate_gemini_cancel">취소</string>

    <string name="privacy_gate_tts_title">텍스트를 Microsoft에 보낼까요?</string>
    <string name="privacy_gate_tts_body">읽어주기를 위해 텍스트가 암호화되어 Microsoft Bing Speech(미국)로 전송되고 오디오로 반환돼요. 법적 근거: EU-US Data Privacy Framework + 표준 계약 조항.\n\n대안: Android 기본 오프라인 TTS를 사용하세요.</string>
    <string name="privacy_gate_tts_accept">동의하고 읽어주기</string>
    <string name="privacy_gate_tts_cancel">취소</string>

    <string name="settings_privacy_header">개인정보</string>
    <string name="settings_analytics_title">익명 통계</string>
    <string name="settings_analytics_subtitle">오류 분석과 제품 개선을 위한 Firebase Analytics</string>

    <string name="settings_delete_account_title">계정과 데이터 삭제</string>
    <string name="settings_delete_account_subtitle">모든 로컬 데이터, Google 계정, Drive 백업을 되돌릴 수 없이 삭제해요</string>
    <string name="settings_delete_account_confirm_title">계정을 영구 삭제할까요?</string>
    <string name="settings_delete_account_confirm_body">이 작업은 되돌릴 수 없으며 다음을 삭제해요:\n\n• 모든 로컬 일기 기록, 사진, 동영상\n• Firebase 계정\n• 앱의 Google Drive 백업\n\n앱은 새 설치 상태로 다시 시작해요.</string>
    <string name="settings_delete_account_cancel">취소</string>
    <string name="settings_delete_account_confirm">예, 모두 삭제</string>

    <string name="settings_report_ai_title">AI 응답 신고</string>
    <string name="settings_report_ai_subtitle">부적절하거나 잘못된 AI 출력</string>
    <string name="settings_report_ai_confirm_title">지원팀에 이메일을 열까요?</string>
    <string name="settings_report_ai_confirm_body">이메일 앱이 dev.app.support@gmail.com 로 작성된 메시지와 함께 열려요. 보내기 전에 설명을 추가할 수 있어요. 영업일 기준 24시간 내에 답장해요.\n\n다음을 신고해 주세요: 대시보드, 요약, 회고, 텍스트 개선에서 나온 부적절하거나 공격적이거나 잘못된 AI 출력.</string>
    <string name="settings_report_ai_confirm">신고 작성</string>
    <string name="settings_report_ai_cancel">취소</string>
    <string name="settings_report_ai_no_email">이메일 앱을 찾을 수 없어요. dev.app.support@gmail.com 로 신고를 보내주세요.</string>
    <string name="settings_report_ai_subject">Best Journal: 부적절한 AI 응답</string>
    <string name="settings_report_ai_body">안녕하세요,\n\nBest Journal에서 부적절하거나 잘못된 AI 응답을 신고하고자 합니다.\n\n문제 설명:\n[작성해 주세요]\n\n상황 (어떤 기능, 어떤 입력):\n[작성해 주세요]\n\n감사합니다.</string>

    <string name="settings_revoke_title">청약 철회</string>
    <string name="settings_revoke_subtitle">Premium 구매</string>
    <string name="settings_revoke_confirm_title">지원팀에 이메일을 열까요?</string>
    <string name="settings_revoke_confirm_body">이메일 앱이 dev.app.support@gmail.com 로 작성된 메시지와 함께 열려요. 영업일 기준 24시간 내에 답장해요.\n\n청약 철회 권리에 관한 자세한 정보는 이용약관(§ 16)에서 확인할 수 있어요. 구독은 Google Play → 정기 결제 에서도 취소해 주세요.</string>
    <string name="settings_revoke_cancel">취소</string>
    <string name="settings_revoke_confirm">철회 작성</string>
    <string name="settings_revoke_no_email">이메일 앱을 찾을 수 없어요. 철회 요청을 dev.app.support@gmail.com 로 보내주세요.</string>
    <string name="settings_revoke_email_subject">Best Journal Premium 계약 철회</string>
    <string name="settings_revoke_email_body">이로써 Best Journal의 Premium 기능과 관련하여 체결한 계약의 철회를 통지합니다.\n\n발신자 (Google 계정): %1$s\n철회 시각: %2$s\n\n이 철회는 앱 내의 § 356a BGB 준수 2단계 철회 버튼을 통해 시작되었으며 Gmail API로 자동 발송되었습니다.</string>
    <string name="settings_revoke_confirm_subject">수신 확인: Best Journal 철회</string>
    <string name="settings_revoke_confirm_user_body">안녕하세요.\n\n%1$s 철회 요청을 접수했습니다. 이는 § 356a BGB에 따른 수신 확인입니다.\n\n철회 요청은 가능한 한 빨리 처리되며, 문의가 있으면 dev.app.support@gmail.com 으로 연락드리겠습니다.\n\n추가 결제를 막으려면 Google Play 스토어의 "구독"에서도 구독을 취소해 주세요.\n\n감사합니다\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">로그인된 Google 계정 주소를 찾지 못했습니다. 설정에서 Google 계정으로 로그인하거나 철회 요청을 dev.app.support@gmail.com 으로 직접 보내 주세요.</string>
    <string name="settings_revoke_sending">철회 전송 중…</string>
    <string name="settings_revoke_success_title">철회가 접수되었습니다</string>
    <string name="settings_revoke_success_body">철회 요청이 dev.app.support@gmail.com 으로 전송되었습니다. 수신 확인도 받은편지함에 도착해 있습니다.</string>
    <string name="settings_revoke_success_close">닫기</string>
    <string name="settings_revoke_error_title">철회를 보낼 수 없습니다</string>
    <string name="settings_revoke_error_body">자동 전송에 실패했습니다: %1$s\n\n대신 dev.app.support@gmail.com 으로 수동 이메일을 보낼 수 있습니다. 그러려면 "이메일 앱 열기"를 누르세요.</string>
    <string name="settings_revoke_error_email_fallback">이메일 앱 열기</string>
"""

# ═══════════ SIMPLIFIED CHINESE (zh-rCN) — 你, mainland vocab, full-width ═══════════
TRANSLATIONS["zh-rCN"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">隐私与同意</string>
    <string name="consent_intro">你的日记是一个私人空间,我们尊重这一点。在这里你可以透明地看到 Best Journal 如何处理你的数据。</string>
    <string name="consent_card1_title">本地存储</string>
    <string name="consent_card1_body">你的记录保留在你的设备上。</string>
    <string name="consent_card2_title">AI 功能(美国)</string>
    <string name="consent_card2_body">可选地,文本会发送到 Google Gemini,语音录音发送到 Groq,朗读文本发送到 Microsoft Edge 位于美国的服务器(EU-US Data Privacy Framework + 标准合同条款)。</string>
    <string name="consent_card3_title">匿名统计</string>
    <string name="consent_card3_body">Firebase Analytics,可选,可随时在设置中更改。</string>
    <string name="consent_links_header">我们的法律文本:</string>
    <string name="consent_accept_all">同意并开始</string>
    <string name="consent_disable_stats">关闭统计并继续</string>
    <string name="consent_confirmation">点击「同意并开始」表示你已阅读隐私政策、使用条款和法律声明,并同意所述的数据处理。你可以随时在设置中更改决定。</string>

    <string name="privacy_gate_groq_title">将语音录音发送到 Groq?</string>
    <string name="privacy_gate_groq_body">为进行云端转写,你的语音录音会加密发送到 Groq, Inc.(美国山景城),在那里转换为文本。音频文件在处理后删除,不用于训练。\n\n替代方案:使用设备本地转写(离线,无数据传输),可在 设置 → AI 中切换。</string>
    <string name="privacy_gate_groq_accept">同意并发送</string>
    <string name="privacy_gate_groq_local">改为本地转写</string>

    <string name="privacy_gate_gemini_title">将文本发送到 Google Gemini?</string>
    <string name="privacy_gate_gemini_body">为使用 AI 功能(仪表盘、摘要、回顾、文本优化),你的记录片段会加密发送到 Google Gemini(Firebase AI, 美国)。法律依据:EU-US Data Privacy Framework + 标准合同条款。请求在处理后删除,不用于训练。</string>
    <string name="privacy_gate_gemini_accept">同意并发送</string>
    <string name="privacy_gate_gemini_cancel">取消</string>

    <string name="privacy_gate_tts_title">将文本发送到 Microsoft?</string>
    <string name="privacy_gate_tts_body">为进行朗读,文本会加密发送到 Microsoft Bing Speech(美国),并作为音频返回。法律依据:EU-US Data Privacy Framework + 标准合同条款。\n\n替代方案:使用 Android 自带的离线 TTS。</string>
    <string name="privacy_gate_tts_accept">同意并朗读</string>
    <string name="privacy_gate_tts_cancel">取消</string>

    <string name="settings_privacy_header">隐私</string>
    <string name="settings_analytics_title">匿名统计</string>
    <string name="settings_analytics_subtitle">用于错误分析和产品改进的 Firebase Analytics</string>

    <string name="settings_delete_account_title">删除账户和数据</string>
    <string name="settings_delete_account_subtitle">不可撤销地删除所有本地数据、你的 Google 账户和 Drive 备份</string>
    <string name="settings_delete_account_confirm_title">永久删除账户?</string>
    <string name="settings_delete_account_confirm_body">此操作不可撤销,将删除:\n\n• 所有本地日记记录、照片和视频\n• 你的 Firebase 账户\n• 应用的 Google Drive 备份\n\n应用将以全新安装状态重新启动。</string>
    <string name="settings_delete_account_cancel">取消</string>
    <string name="settings_delete_account_confirm">是的,全部删除</string>

    <string name="settings_report_ai_title">举报 AI 回复</string>
    <string name="settings_report_ai_subtitle">不当或错误的 AI 输出</string>
    <string name="settings_report_ai_confirm_title">打开支持邮件?</string>
    <string name="settings_report_ai_confirm_body">你的邮件应用会打开一封发送到 dev.app.support@gmail.com 的预填邮件。你可以在发送前补充说明。我们在工作日 24 小时内回复。\n\n请举报:来自仪表盘、摘要、回顾或文本优化的不当、冒犯、虚假或误导性 AI 输出。</string>
    <string name="settings_report_ai_confirm">创建举报</string>
    <string name="settings_report_ai_cancel">取消</string>
    <string name="settings_report_ai_no_email">未找到邮件应用。请将举报发送到 dev.app.support@gmail.com。</string>
    <string name="settings_report_ai_subject">Best Journal:不当 AI 回复</string>
    <string name="settings_report_ai_body">你好,\n\n我想举报 Best Journal 中的一条不当或错误的 AI 回复。\n\n问题描述:\n[请填写]\n\n情境(哪个功能、哪项输入):\n[请填写]\n\n谢谢。</string>

    <string name="settings_revoke_title">撤销合同</string>
    <string name="settings_revoke_subtitle">Premium 购买</string>
    <string name="settings_revoke_confirm_title">打开支持邮件?</string>
    <string name="settings_revoke_confirm_body">你的邮件应用会打开一封发送到 dev.app.support@gmail.com 的预填邮件。我们在工作日 24 小时内回复。\n\n有关撤销权的完整信息,请参阅使用条款(§ 16)。订阅请另外通过 Google Play → 订阅 取消。</string>
    <string name="settings_revoke_cancel">取消</string>
    <string name="settings_revoke_confirm">创建撤销</string>
    <string name="settings_revoke_no_email">未找到邮件应用。请将撤销通知发送到 dev.app.support@gmail.com。</string>
    <string name="settings_revoke_email_subject">Best Journal Premium 合同撤销</string>
    <string name="settings_revoke_email_body">本人特此撤销我就 Best Journal Premium 功能订立的合同。\n\n发件人（Google 账户）：%1$s\n撤销时间：%2$s\n\n本次撤销通过应用内符合 § 356a BGB 的两步式撤销按钮触发，并经由 Gmail API 自动发送。</string>
    <string name="settings_revoke_confirm_subject">你的回执：Best Journal 撤销</string>
    <string name="settings_revoke_confirm_user_body">你好，\n\n我们已收到你在 %1$s 提交的撤销。这是你根据 § 356a BGB 获得的回执确认。\n\n我们会尽快处理你的撤销；如有疑问，将通过 dev.app.support@gmail.com 与你联系。\n\n为了避免后续继续扣费，请也在 Google Play 商店的“订阅”中取消你的订阅。\n\n谢谢，顺祝安好\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">未找到已登录的 Google 账户地址。请在设置中登录你的 Google 账户，或将撤销邮件手动发送到 dev.app.support@gmail.com。</string>
    <string name="settings_revoke_sending">正在发送撤销…</string>
    <string name="settings_revoke_success_title">已收到撤销</string>
    <string name="settings_revoke_success_body">你的撤销已发送至 dev.app.support@gmail.com。回执确认邮件也已发到你的收件箱。</string>
    <string name="settings_revoke_success_close">关闭</string>
    <string name="settings_revoke_error_title">无法发送撤销</string>
    <string name="settings_revoke_error_body">自动发送失败：%1$s\n\n你也可以手动向 dev.app.support@gmail.com 发送邮件。为此请点击“打开邮件应用”。</string>
    <string name="settings_revoke_error_email_fallback">打开邮件应用</string>
"""

# ═══════════ TRADITIONAL CHINESE (zh-rTW) — 你, Taiwan vocab, full-width ═══════════
TRANSLATIONS["zh-rTW"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">隱私與同意</string>
    <string name="consent_intro">你的日記是一個私人空間,我們尊重這一點。在這裡你可以透明地看到 Best Journal 如何處理你的資料。</string>
    <string name="consent_card1_title">本機儲存</string>
    <string name="consent_card1_body">你的記錄會保留在你的裝置上。</string>
    <string name="consent_card2_title">AI 功能(美國)</string>
    <string name="consent_card2_body">可選擇將文字傳送到 Google Gemini、語音錄音傳送到 Groq、朗讀文字傳送到 Microsoft Edge 位於美國的伺服器(EU-US Data Privacy Framework + 標準契約條款)。</string>
    <string name="consent_card3_title">匿名統計</string>
    <string name="consent_card3_body">Firebase Analytics,選用,可隨時在設定中變更。</string>
    <string name="consent_links_header">我們的法律文件:</string>
    <string name="consent_accept_all">同意並開始</string>
    <string name="consent_disable_stats">關閉統計並繼續</string>
    <string name="consent_confirmation">點選「同意並開始」表示你已閱讀隱私權政策、使用條款和法律聲明,並同意所述的資料處理。你可以隨時在設定中變更決定。</string>

    <string name="privacy_gate_groq_title">將語音錄音傳送到 Groq?</string>
    <string name="privacy_gate_groq_body">為進行雲端轉錄,你的語音錄音會加密傳送到 Groq, Inc.(美國山景城),並在那裡轉換為文字。音訊檔在處理後刪除,不用於訓練。\n\n替代方案:使用裝置本機轉錄(離線,無資料傳輸),可在 設定 → AI 中切換。</string>
    <string name="privacy_gate_groq_accept">同意並傳送</string>
    <string name="privacy_gate_groq_local">改為本機轉錄</string>

    <string name="privacy_gate_gemini_title">將文字傳送到 Google Gemini?</string>
    <string name="privacy_gate_gemini_body">為使用 AI 功能(儀表板、摘要、回顧、文字改善),你的記錄片段會加密傳送到 Google Gemini(Firebase AI, 美國)。法律依據:EU-US Data Privacy Framework + 標準契約條款。請求在處理後刪除,不用於訓練。</string>
    <string name="privacy_gate_gemini_accept">同意並傳送</string>
    <string name="privacy_gate_gemini_cancel">取消</string>

    <string name="privacy_gate_tts_title">將文字傳送到 Microsoft?</string>
    <string name="privacy_gate_tts_body">為進行朗讀,文字會加密傳送到 Microsoft Bing Speech(美國),並以音訊形式回傳。法律依據:EU-US Data Privacy Framework + 標準契約條款。\n\n替代方案:使用 Android 內建的離線 TTS。</string>
    <string name="privacy_gate_tts_accept">同意並朗讀</string>
    <string name="privacy_gate_tts_cancel">取消</string>

    <string name="settings_privacy_header">隱私</string>
    <string name="settings_analytics_title">匿名統計</string>
    <string name="settings_analytics_subtitle">用於錯誤分析與產品改善的 Firebase Analytics</string>

    <string name="settings_delete_account_title">刪除帳戶與資料</string>
    <string name="settings_delete_account_subtitle">不可復原地刪除所有本機資料、你的 Google 帳戶和 Drive 備份</string>
    <string name="settings_delete_account_confirm_title">永久刪除帳戶?</string>
    <string name="settings_delete_account_confirm_body">此操作不可復原,會刪除:\n\n• 所有本機日記記錄、照片和影片\n• 你的 Firebase 帳戶\n• 應用程式的 Google Drive 備份\n\n應用程式將以全新安裝狀態重新啟動。</string>
    <string name="settings_delete_account_cancel">取消</string>
    <string name="settings_delete_account_confirm">是的,全部刪除</string>

    <string name="settings_report_ai_title">檢舉 AI 回覆</string>
    <string name="settings_report_ai_subtitle">不當或錯誤的 AI 輸出</string>
    <string name="settings_report_ai_confirm_title">開啟支援郵件?</string>
    <string name="settings_report_ai_confirm_body">你的郵件應用程式會開啟一封寄給 dev.app.support@gmail.com 的預填郵件。你可以在寄出前補充說明。我們在工作日 24 小時內回覆。\n\n請檢舉:來自儀表板、摘要、回顧或文字改善的不當、冒犯、虛假或誤導性 AI 輸出。</string>
    <string name="settings_report_ai_confirm">建立檢舉</string>
    <string name="settings_report_ai_cancel">取消</string>
    <string name="settings_report_ai_no_email">找不到郵件應用程式。請將檢舉寄到 dev.app.support@gmail.com。</string>
    <string name="settings_report_ai_subject">Best Journal:不當 AI 回覆</string>
    <string name="settings_report_ai_body">你好,\n\n我想檢舉 Best Journal 中的一則不當或錯誤的 AI 回覆。\n\n問題描述:\n[請填寫]\n\n情境(哪個功能、哪項輸入):\n[請填寫]\n\n謝謝。</string>

    <string name="settings_revoke_title">解除契約</string>
    <string name="settings_revoke_subtitle">Premium 購買</string>
    <string name="settings_revoke_confirm_title">開啟支援郵件?</string>
    <string name="settings_revoke_confirm_body">你的郵件應用程式會開啟一封寄給 dev.app.support@gmail.com 的預填郵件。我們在工作日 24 小時內回覆。\n\n有關解除權的完整資訊,請參閱使用條款(§ 16)。訂閱請另外透過 Google Play → 訂閱 取消。</string>
    <string name="settings_revoke_cancel">取消</string>
    <string name="settings_revoke_confirm">建立解除</string>
    <string name="settings_revoke_no_email">找不到郵件應用程式。請將解除通知寄到 dev.app.support@gmail.com。</string>
    <string name="settings_revoke_email_subject">Best Journal Premium 契約解除</string>
    <string name="settings_revoke_email_body">本人特此撤銷我就 Best Journal Premium 功能訂立的契約。\n\n寄件人（Google 帳戶）：%1$s\n撤銷時間：%2$s\n\n本次撤銷透過應用程式內符合 § 356a BGB 的兩步式撤銷按鈕觸發，並經由 Gmail API 自動寄出。</string>
    <string name="settings_revoke_confirm_subject">你的收訖確認：Best Journal 撤銷</string>
    <string name="settings_revoke_confirm_user_body">你好，\n\n我們已收到你在 %1$s 提交的撤銷。這是你依 § 356a BGB 取得的收訖確認。\n\n我們會盡快處理你的撤銷；如有疑問，將透過 dev.app.support@gmail.com 與你聯絡。\n\n為避免後續繼續扣款，請也在 Google Play 商店的「訂閱」中取消你的訂閱。\n\n謝謝，敬祝安好\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">未找到已登入的 Google 帳戶地址。請在設定中登入你的 Google 帳戶，或將撤銷郵件手動寄到 dev.app.support@gmail.com。</string>
    <string name="settings_revoke_sending">正在傳送撤銷…</string>
    <string name="settings_revoke_success_title">已收到撤銷</string>
    <string name="settings_revoke_success_body">你的撤銷已傳送至 dev.app.support@gmail.com。收訖確認郵件也已寄到你的收件匣。</string>
    <string name="settings_revoke_success_close">關閉</string>
    <string name="settings_revoke_error_title">無法傳送撤銷</string>
    <string name="settings_revoke_error_body">自動傳送失敗：%1$s\n\n你也可以手動將郵件寄到 dev.app.support@gmail.com。為此請點選「打開郵件應用」。</string>
    <string name="settings_revoke_error_email_fallback">打開郵件應用</string>
"""

# ═══════════ ARABIC (ar) — MSA, RTL, masculine default ═══════════
TRANSLATIONS["ar"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">الخصوصية والموافقة</string>
    <string name="consent_intro">مذكراتك مساحة شخصية، ونحن نحترم ذلك. هنا ترى بشفافية كيف يتعامل Best Journal مع بياناتك.</string>
    <string name="consent_card1_title">التخزين المحلي</string>
    <string name="consent_card1_body">تبقى إدخالاتك على جهازك.</string>
    <string name="consent_card2_title">ميزات الذكاء الاصطناعي (الولايات المتحدة)</string>
    <string name="consent_card2_body">اختياريًا تُرسل النصوص إلى Google Gemini، والتسجيلات الصوتية إلى Groq، ونصوص القراءة بصوت عالٍ إلى Microsoft Edge في الولايات المتحدة (إطار EU-US Data Privacy Framework + الشروط التعاقدية النموذجية).</string>
    <string name="consent_card3_title">إحصائيات مجهولة</string>
    <string name="consent_card3_body">Firebase Analytics، اختياري، يمكن تغييره في أي وقت من الإعدادات.</string>
    <string name="consent_links_header">نصوصنا القانونية:</string>
    <string name="consent_accept_all">الموافقة والبدء</string>
    <string name="consent_disable_stats">تعطيل الإحصائيات والمتابعة</string>
    <string name="consent_confirmation">بالنقر على «الموافقة والبدء» تؤكد أنك قرأت سياسة الخصوصية وشروط الاستخدام والبيان القانوني، وتوافق على معالجة البيانات الموضحة. يمكنك تغيير قرارك في أي وقت من الإعدادات.</string>

    <string name="privacy_gate_groq_title">إرسال التسجيل الصوتي إلى Groq؟</string>
    <string name="privacy_gate_groq_body">لنسخ النص من الصوت عبر السحابة، يُرسل تسجيلك الصوتي مشفرًا إلى Groq, Inc. (ماونتن فيو، الولايات المتحدة) ويُحوَّل هناك إلى نص. يُحذف الملف الصوتي بعد المعالجة ولا يُستخدم للتدريب.\n\nبديل: استخدم النسخ المحلي على الجهاز (دون اتصال، بدون نقل بيانات)، قابل للتبديل في الإعدادات → الذكاء الاصطناعي.</string>
    <string name="privacy_gate_groq_accept">الموافقة والإرسال</string>
    <string name="privacy_gate_groq_local">النسخ محليًا بدلًا من ذلك</string>

    <string name="privacy_gate_gemini_title">إرسال النص إلى Google Gemini؟</string>
    <string name="privacy_gate_gemini_body">لميزات الذكاء الاصطناعي (لوحة التحكم، الملخصات، الاستعراضات، تحسين النص) تُرسل مقتطفات من إدخالاتك مشفرة إلى Google Gemini (Firebase AI، الولايات المتحدة). الأساس القانوني: إطار EU-US Data Privacy Framework + الشروط التعاقدية النموذجية. تُحذف الطلبات بعد المعالجة ولا تُستخدم للتدريب.</string>
    <string name="privacy_gate_gemini_accept">الموافقة والإرسال</string>
    <string name="privacy_gate_gemini_cancel">إلغاء</string>

    <string name="privacy_gate_tts_title">إرسال النص إلى Microsoft؟</string>
    <string name="privacy_gate_tts_body">للقراءة بصوت عالٍ، يُرسل النص مشفرًا إلى Microsoft Bing Speech (الولايات المتحدة) ويعود كملف صوتي. الأساس القانوني: إطار EU-US Data Privacy Framework + الشروط التعاقدية النموذجية.\n\nبديل: استخدم أداة تحويل النص إلى كلام المحلية المدمجة في Android.</string>
    <string name="privacy_gate_tts_accept">الموافقة والقراءة</string>
    <string name="privacy_gate_tts_cancel">إلغاء</string>

    <string name="settings_privacy_header">الخصوصية</string>
    <string name="settings_analytics_title">إحصائيات مجهولة</string>
    <string name="settings_analytics_subtitle">Firebase Analytics لتحليل الأخطاء وتحسين المنتج</string>

    <string name="settings_delete_account_title">حذف الحساب والبيانات</string>
    <string name="settings_delete_account_subtitle">يحذف بشكل نهائي جميع البيانات المحلية وحسابك في Google والنسخة الاحتياطية في Drive</string>
    <string name="settings_delete_account_confirm_title">هل تريد حذف الحساب نهائيًا؟</string>
    <string name="settings_delete_account_confirm_body">هذا الإجراء لا يمكن التراجع عنه ويحذف:\n\n• جميع إدخالات المذكرات والصور ومقاطع الفيديو المحلية\n• حسابك في Firebase\n• النسخة الاحتياطية للتطبيق في Google Drive\n\nسيعيد التطبيق التشغيل كتثبيت جديد.</string>
    <string name="settings_delete_account_cancel">إلغاء</string>
    <string name="settings_delete_account_confirm">نعم، احذف كل شيء</string>

    <string name="settings_report_ai_title">الإبلاغ عن رد الذكاء الاصطناعي</string>
    <string name="settings_report_ai_subtitle">مخرجات ذكاء اصطناعي غير مناسبة أو خاطئة</string>
    <string name="settings_report_ai_confirm_title">فتح بريد إلى الدعم؟</string>
    <string name="settings_report_ai_confirm_body">سيفتح تطبيق البريد لديك برسالة معدة مسبقًا إلى dev.app.support@gmail.com. يمكنك إكمال الوصف قبل الإرسال. نرد خلال 24 ساعة في أيام العمل.\n\nأبلغ هنا عن: مخرجات ذكاء اصطناعي غير مناسبة أو مسيئة أو خاطئة أو مضللة من لوحة التحكم أو الملخصات أو الاستعراضات أو تحسين النص.</string>
    <string name="settings_report_ai_confirm">إنشاء البلاغ</string>
    <string name="settings_report_ai_cancel">إلغاء</string>
    <string name="settings_report_ai_no_email">لم يتم العثور على تطبيق بريد. يرجى إرسال البلاغ إلى dev.app.support@gmail.com.</string>
    <string name="settings_report_ai_subject">Best Journal: رد ذكاء اصطناعي غير مناسب</string>
    <string name="settings_report_ai_body">مرحبًا،\n\nأرغب في الإبلاغ عن رد ذكاء اصطناعي غير مناسب أو خاطئ في Best Journal.\n\nوصف المشكلة:\n[يُرجى التعبئة]\n\nالسياق (الميزة والإدخال):\n[يُرجى التعبئة]\n\nشكرًا.</string>

    <string name="settings_revoke_title">الرجوع عن العقد</string>
    <string name="settings_revoke_subtitle">شراء Premium</string>
    <string name="settings_revoke_confirm_title">فتح بريد إلى الدعم؟</string>
    <string name="settings_revoke_confirm_body">سيفتح تطبيق البريد لديك برسالة معدة مسبقًا إلى dev.app.support@gmail.com. نرد خلال 24 ساعة في أيام العمل.\n\nتجد المعلومات الكاملة عن حق الرجوع في شروط الاستخدام (§ 16). ألغِ الاشتراكات أيضًا عبر Google Play ← الاشتراكات.</string>
    <string name="settings_revoke_cancel">إلغاء</string>
    <string name="settings_revoke_confirm">إنشاء الرجوع</string>
    <string name="settings_revoke_no_email">لم يتم العثور على تطبيق بريد. يرجى إرسال الرجوع إلى dev.app.support@gmail.com.</string>
    <string name="settings_revoke_email_subject">الرجوع عن عقد Best Journal Premium</string>
    <string name="settings_revoke_email_body">أُعلن بموجب هذا عدولي عن العقد الذي أبرمته بشأن ميزات Premium في Best Journal.\n\nالمرسل (حساب Google): %1$s\nوقت العدول: %2$s\n\nتم إرسال هذا العدول تلقائيًا عبر Gmail API بعد تأكيده من خلال زر العدول ثنائي الخطوة داخل التطبيق والمتوافق مع § 356a BGB.</string>
    <string name="settings_revoke_confirm_subject">تأكيد الاستلام: العدول لدى Best Journal</string>
    <string name="settings_revoke_confirm_user_body">مرحبًا،\n\nلقد تلقّينا عدولك المؤرخ %1$s. وهذه هي رسالة تأكيد الاستلام وفقًا للمادة § 356a BGB.\n\nسنعالج عدولك في أسرع وقت ممكن، وسنتواصل معك عبر dev.app.support@gmail.com إذا كانت هناك أسئلة.\n\nولإيقاف أي رسوم مستقبلية، يُرجى أيضًا إلغاء اشتراكك في Google Play Store ضمن "الاشتراكات".\n\nشكرًا مع أطيب التحيات\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">لم يتم العثور على عنوان حساب Google مسجَّل الدخول. يُرجى تسجيل الدخول بحساب Google من الإعدادات أو إرسال العدول يدويًا إلى dev.app.support@gmail.com.</string>
    <string name="settings_revoke_sending">جارٍ إرسال العدول…</string>
    <string name="settings_revoke_success_title">تم استلام العدول</string>
    <string name="settings_revoke_success_body">تم إرسال عدولك إلى dev.app.support@gmail.com. كما أن رسالة تأكيد الاستلام موجودة في بريدك الوارد.</string>
    <string name="settings_revoke_success_close">إغلاق</string>
    <string name="settings_revoke_error_title">تعذّر إرسال العدول</string>
    <string name="settings_revoke_error_body">فشل الإرسال التلقائي: %1$s\n\nيمكنك بدلاً من ذلك إرسال رسالة بريد إلكتروني يدويًا إلى dev.app.support@gmail.com. اضغط على "فتح تطبيق البريد الإلكتروني" لذلك.</string>
    <string name="settings_revoke_error_email_fallback">فتح تطبيق البريد الإلكتروني</string>
"""

# ═══════════ HINDI (hi) — Polite आप, Devanagari, Arabic numerals ═══════════
TRANSLATIONS["hi"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">गोपनीयता और सहमति</string>
    <string name="consent_intro">आपकी डायरी एक निजी जगह है, और हम इसका सम्मान करते हैं। यहाँ आप पारदर्शी तरीके से देख सकते हैं कि Best Journal आपके डेटा को कैसे संभालता है।</string>
    <string name="consent_card1_title">लोकल स्टोरेज</string>
    <string name="consent_card1_body">आपकी एंट्रीज़ आपके डिवाइस पर रहती हैं।</string>
    <string name="consent_card2_title">AI फ़ीचर्स (USA)</string>
    <string name="consent_card2_body">वैकल्पिक रूप से टेक्स्ट Google Gemini को, वॉयस रिकॉर्डिंग Groq को और टेक्स्ट-टू-स्पीच के टेक्स्ट Microsoft Edge को USA में भेजे जाते हैं (EU-US Data Privacy Framework + Standard Contractual Clauses)।</string>
    <string name="consent_card3_title">अनाम आँकड़े</string>
    <string name="consent_card3_body">Firebase Analytics, वैकल्पिक, Settings में कभी भी बदल सकते हैं।</string>
    <string name="consent_links_header">हमारे कानूनी दस्तावेज़:</string>
    <string name="consent_accept_all">सहमत हूँ और शुरू करें</string>
    <string name="consent_disable_stats">आँकड़े बंद करके जारी रखें</string>
    <string name="consent_confirmation">\"सहमत हूँ और शुरू करें\" पर टैप करके आप पुष्टि करते हैं कि आपने Privacy Policy, Terms of Use और Imprint पढ़े हैं और बताए गए डेटा प्रोसेसिंग से सहमत हैं। आप अपना निर्णय Settings में कभी भी बदल सकते हैं।</string>

    <string name="privacy_gate_groq_title">वॉयस रिकॉर्डिंग Groq को भेजें?</string>
    <string name="privacy_gate_groq_body">क्लाउड ट्रांसक्रिप्शन के लिए आपकी वॉयस रिकॉर्डिंग एन्क्रिप्टेड रूप में Groq, Inc. (Mountain View, USA) को भेजी जाती है और वहाँ टेक्स्ट में बदली जाती है। ऑडियो फ़ाइल प्रोसेसिंग के बाद डिलीट हो जाती है और ट्रेनिंग में उपयोग नहीं होती।\n\nविकल्प: डिवाइस पर लोकल ट्रांसक्रिप्शन उपयोग करें (ऑफ़लाइन, कोई डेटा ट्रांसफ़र नहीं), Settings → AI में बदला जा सकता है।</string>
    <string name="privacy_gate_groq_accept">सहमत हूँ, भेजें</string>
    <string name="privacy_gate_groq_local">इसके बजाय लोकल ट्रांसक्राइब करें</string>

    <string name="privacy_gate_gemini_title">टेक्स्ट Google Gemini को भेजें?</string>
    <string name="privacy_gate_gemini_body">AI फ़ीचर्स (Dashboard, सारांश, Retrospective, Text Improvement) के लिए आपकी एंट्रीज़ के अंश एन्क्रिप्टेड रूप में Google Gemini (Firebase AI, USA) को भेजे जाते हैं। कानूनी आधार: EU-US Data Privacy Framework + Standard Contractual Clauses। अनुरोध प्रोसेसिंग के बाद डिलीट कर दिए जाते हैं और ट्रेनिंग में उपयोग नहीं होते।</string>
    <string name="privacy_gate_gemini_accept">सहमत हूँ, भेजें</string>
    <string name="privacy_gate_gemini_cancel">रद्द करें</string>

    <string name="privacy_gate_tts_title">टेक्स्ट Microsoft को भेजें?</string>
    <string name="privacy_gate_tts_body">पढ़कर सुनाने के लिए टेक्स्ट एन्क्रिप्टेड रूप में Microsoft Bing Speech (USA) को भेजा जाता है और ऑडियो के रूप में वापस आता है। कानूनी आधार: EU-US Data Privacy Framework + Standard Contractual Clauses।\n\nविकल्प: Android का बिल्ट-इन ऑफ़लाइन TTS उपयोग करें।</string>
    <string name="privacy_gate_tts_accept">सहमत हूँ, पढ़कर सुनाएँ</string>
    <string name="privacy_gate_tts_cancel">रद्द करें</string>

    <string name="settings_privacy_header">गोपनीयता</string>
    <string name="settings_analytics_title">अनाम आँकड़े</string>
    <string name="settings_analytics_subtitle">त्रुटि विश्लेषण और उत्पाद सुधार के लिए Firebase Analytics</string>

    <string name="settings_delete_account_title">अकाउंट और डेटा डिलीट करें</string>
    <string name="settings_delete_account_subtitle">सभी लोकल डेटा, आपका Google अकाउंट और Drive बैकअप स्थायी रूप से हटा देता है</string>
    <string name="settings_delete_account_confirm_title">अकाउंट स्थायी रूप से डिलीट करें?</string>
    <string name="settings_delete_account_confirm_body">यह क्रिया अपरिवर्तनीय है और ये डिलीट करती है:\n\n• सभी लोकल डायरी एंट्रीज़, फ़ोटो और वीडियो\n• आपका Firebase अकाउंट\n• ऐप का Google Drive बैकअप\n\nऐप नई इंस्टॉलेशन की तरह फिर से शुरू होगा।</string>
    <string name="settings_delete_account_cancel">रद्द करें</string>
    <string name="settings_delete_account_confirm">हाँ, सब कुछ डिलीट करें</string>

    <string name="settings_report_ai_title">AI जवाब रिपोर्ट करें</string>
    <string name="settings_report_ai_subtitle">अनुचित या गलत AI आउटपुट</string>
    <string name="settings_report_ai_confirm_title">Support को ईमेल खोलें?</string>
    <string name="settings_report_ai_confirm_body">आपका ईमेल ऐप dev.app.support@gmail.com को तैयार संदेश के साथ खुलेगा। भेजने से पहले आप विवरण जोड़ सकते हैं। हम कार्यदिवसों में 24 घंटे के भीतर उत्तर देते हैं।\n\nयहाँ रिपोर्ट करें: Dashboard, सारांश, Retrospectives या Text Improvement से अनुचित, आपत्तिजनक, गलत या भ्रामक AI आउटपुट।</string>
    <string name="settings_report_ai_confirm">रिपोर्ट बनाएँ</string>
    <string name="settings_report_ai_cancel">रद्द करें</string>
    <string name="settings_report_ai_no_email">कोई ईमेल ऐप नहीं मिला। कृपया रिपोर्ट dev.app.support@gmail.com पर भेजें।</string>
    <string name="settings_report_ai_subject">Best Journal: अनुचित AI जवाब</string>
    <string name="settings_report_ai_body">नमस्ते,\n\nमैं Best Journal में एक अनुचित या गलत AI जवाब की रिपोर्ट करना चाहता हूँ।\n\nसमस्या का विवरण:\n[कृपया भरें]\n\nसंदर्भ (कौन-सा फ़ीचर, कौन-सा इनपुट):\n[कृपया भरें]\n\nधन्यवाद।</string>

    <string name="settings_revoke_title">अनुबंध रद्द</string>
    <string name="settings_revoke_subtitle">Premium खरीद</string>
    <string name="settings_revoke_confirm_title">Support को ईमेल खोलें?</string>
    <string name="settings_revoke_confirm_body">आपका ईमेल ऐप dev.app.support@gmail.com को तैयार संदेश के साथ खुलेगा। हम कार्यदिवसों में 24 घंटे के भीतर उत्तर देते हैं।\n\nअनुबंध रद्द के अधिकार की पूरी जानकारी Terms of Use (§ 16) में मिलेगी। Subscriptions को अतिरिक्त रूप से Google Play → Subscriptions में रद्द करें।</string>
    <string name="settings_revoke_cancel">रद्द करें</string>
    <string name="settings_revoke_confirm">रद्द अनुरोध बनाएँ</string>
    <string name="settings_revoke_no_email">कोई ईमेल ऐप नहीं मिला। कृपया रद्द अनुरोध dev.app.support@gmail.com पर भेजें।</string>
    <string name="settings_revoke_email_subject">Best Journal Premium अनुबंध रद्द</string>
    <string name="settings_revoke_email_body">इसके द्वारा मैं Best Journal की Premium सुविधाओं से संबंधित अपने अनुबंध की वापसी की सूचना देता हूँ।\n\nप्रेषक (Google खाता): %1$s\nवापसी का समय: %2$s\n\nयह वापसी ऐप में मौजूद § 356a BGB-अनुरूप दो-चरणीय वापसी बटन के जरिए शुरू की गई और Gmail API के माध्यम से अपने-आप भेजी गई।</string>
    <string name="settings_revoke_confirm_subject">आपकी प्राप्ति-पुष्टि: Best Journal में वापसी</string>
    <string name="settings_revoke_confirm_user_body">नमस्ते,\n\nहमें %1$s की आपकी वापसी मिल गई है। यह § 356a BGB के तहत आपकी प्राप्ति-पुष्टि है।\n\nहम आपकी वापसी को जल्द से जल्द प्रक्रिया में लाएँगे और अगर कोई सवाल हुआ तो dev.app.support@gmail.com पर आपसे संपर्क करेंगे।\n\nआगे कोई नई बिलिंग न हो, इसके लिए कृपया Google Play Store में "Subscriptions" के तहत अपना सब्सक्रिप्शन भी रद्द करें।\n\nधन्यवाद और शुभकामनाएँ\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">किसी साइन-इन किए हुए Google खाते का पता नहीं मिला। कृपया Settings में अपने Google खाते से साइन इन करें या वापसी को मैन्युअल रूप से dev.app.support@gmail.com पर भेजें।</string>
    <string name="settings_revoke_sending">वापसी भेजी जा रही है…</string>
    <string name="settings_revoke_success_title">वापसी प्राप्त हुई</string>
    <string name="settings_revoke_success_body">आपकी वापसी dev.app.support@gmail.com पर भेज दी गई है। प्राप्ति-पुष्टि भी आपके इनबॉक्स में है।</string>
    <string name="settings_revoke_success_close">बंद करें</string>
    <string name="settings_revoke_error_title">वापसी भेजी नहीं जा सकी</string>
    <string name="settings_revoke_error_body">स्वचालित भेजना विफल रहा: %1$s\n\nवैकल्पिक रूप से आप dev.app.support@gmail.com पर मैन्युअल ई-मेल भेज सकते हैं। इसके लिए "ई-मेल ऐप खोलें" पर टैप करें।</string>
    <string name="settings_revoke_error_email_fallback">ई-मेल ऐप खोलें</string>
"""


def insert(path, block):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    if "CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION" in content:
        return f"SKIP: {path}"
    new_content = content.replace("</resources>", block + "\n\n</resources>")
    d = os.path.dirname(os.path.abspath(path))
    with tempfile.NamedTemporaryFile("w", dir=d, suffix=".tmp", delete=False, encoding="utf-8", newline="\n") as tmp:
        tmp.write(new_content); tmp_path = tmp.name
    os.replace(tmp_path, path)
    return f"OK: {path}"


for locale, block in TRANSLATIONS.items():
    target = os.path.join(APP_DIR, f"values-{locale}", "strings.xml")
    print(insert(target, block))
