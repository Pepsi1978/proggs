# -*- coding: utf-8 -*-
"""Dump legal-key pairs for one language batch at a time for semantic review."""
import json
import sys

d = json.load(open(r'C:/Users/barwa/proggs/BestJournalAndroid/.android-shield/worker-checkpoints/l6-extracted.json', 'r', encoding='utf-8'))
pairs = d['legal_pairs']

# group by prefix cluster for review
CLUSTERS = {
    'SUBS': ['churn_auto_renew_note', 'churn_cancel_sub', 'churn_discount_badge', 'churn_google_play_redirect',
             'churn_instead_of', 'churn_offer_auto_renew', 'churn_offer_feature_perspectives', 'churn_pause_sub',
             'churn_promo_months_remaining', 'churn_promo_one_month_remaining', 'churn_promo_phase_ends', 'churn_switch_yearly'],
    'PAYWALL': ['paywall_day7', 'paywall_exit_bonus_days', 'paywall_exit_discount', 'paywall_exit_price_comparison',
                'paywall_exit_start_discount', 'paywall_feature_profiles', 'paywall_first_payment', 'paywall_free_note',
                'paywall_from_per_day', 'paywall_headline_clarity_sub', 'paywall_instead_per_month', 'paywall_legal_hint',
                'paywall_monthly_plan', 'paywall_reminder', 'paywall_with_limited', 'paywall_yearly_note'],
    'CONSENT': ['consent_card1_body', 'consent_card3_title', 'consent_confirmation', 'consent_footer_link_agb',
                'consent_footer_link_datenschutz', 'consent_footer_link_impressum', 'consent_toggle_analytics_body',
                'consent_toggle_do_not_sell_body', 'consent_toggle_drive_body'],
    'SETTINGS_LEGAL': ['settings_analytics_title', 'settings_auto_lock', 'settings_crisis_dialog_body',
                       'settings_delete_account_confirm_body', 'settings_delete_account_subtitle', 'settings_entries_auto_loaded',
                       'settings_feedback_dialog_desc', 'settings_legal_online_label', 'settings_legal_section_header',
                       'settings_premium_cancelled_desc', 'settings_premium_cancelled_until', 'settings_premium_desc',
                       'settings_premium_feature_5_perspectives', 'settings_premium_feature_improve_desc',
                       'settings_premium_feature_profiles', 'settings_premium_lifetime', 'settings_premium_lifetime_desc',
                       'settings_revoke_email_body', 'settings_revoke_subtitle', 'settings_unlock_premium'],
    'ONBOARDING': ['onboarding_feature_secure', 'onboarding_feature_secure__privacy', 'onboarding_free_after_trial',
                   'onboarding_free_trial', 'onboarding_premium_feature_noads', 'onboarding_start_free', 'onboarding_try_premium'],
    'PRIVACY_GATE': ['privacy_gate_gemini_body', 'privacy_gate_groq_body', 'privacy_gate_tts_body',
                     'dashboard_gemini_unavailable', 'dashboard_premium_upsell_body', 'permission_rationale_microphone',
                     'transcription_model_groq', 'journal_transcribed_with', 'journal_drive_backup_current',
                     'ai_improved_suffix_marker'],
    'FOLLOWUP_RETRO_MISC': ['follow_up_delete_confirm_text', 'follow_up_premium_body', 'follow_up_premium_hint',
                            'follow_up_premium_later', 'follow_up_premium_title', 'retro_benefit_weekly', 'retro_premium_unlock',
                            'entry_delete_message', 'profile_entropy_long', 'prompt_clear_confirm_text',
                            'ai_limit_yearly', 'ai_prompt_response_language', 'ai_prompt_no_dates_rule', 'ai_prompt_custom_intro',
                            'ai_prompt_entropy_intro', 'ai_prompt_goals_definition', 'ai_prompt_insight_intro'],
}

cl = sys.argv[1] if len(sys.argv) > 1 else 'SUBS'
langs = sys.argv[2].split(',') if len(sys.argv) > 2 else ['en', 'es', 'fr', 'it']
keys = CLUSTERS.get(cl, [])
print(f'########## CLUSTER {cl} ({len(keys)} keys) langs={langs} ##########')
for k in keys:
    if k not in pairs:
        print(f'  [MISSING FROM PAIRS] {k}')
        continue
    v = pairs[k]
    print(f'\n=== {k} ===')
    print(f'  de: {v["de"]!r}')
    for l in langs:
        print(f'  {l}: {v.get(l)!r}')
