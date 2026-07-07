# -*- coding: utf-8 -*-
"""finale Phase 3 Translation-Worker apply: lang 'in' (Indonesian, ISO id).
Replaces 36 existing keys in place (regex), inserts paywall_monthly_note (rfind),
replaces 2 plurals. NEVER loads file into LLM context — pure Python IO."""
import re, sys, io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
TARGET = "app/src/main/res/values-in/strings.xml"

# Indonesian translations (kamu form, informal). \n preserved as literal backslash-n.
# Placeholders %1$s/%1$d kept exact. Legal statements 1:1.
T = {
"ai_prompt_custom_intro": "Kamu adalah analis buku harian yang cerdas dan jeli SEKALIGUS pelaksana tugas.\\n\\nCARA KERJA DALAM DUA LANGKAH:\\n\\nLANGKAH 1 — MENYERAP KONTEKS:\\nBaca dulu SEMUA entri buku harian pengguna secara lengkap. Pahami situasinya, polanya, temanya, hal-hal yang disebut, masalah, keinginan. Entri adalah KONTEKSmu, titik awalmu, dasarmu. Tapi entri tidak membatasimu.\\n\\nLANGKAH 2 — MENJALANKAN TUGAS:\\n%1$s\\n\\nYang di atas adalah TUGASmu yang sebenarnya. Jalankan tugas ini berdasarkan konteks dari Langkah 1. Jika tugas meminta riset, ide, alternatif, saran, atau informasi baru, maka sajikan itu secara AKTIF, meski tidak muncul dalam entri. Jangan memberikan diagnosis atau saran mengikat soal medis, hukum, atau keuangan; untuk topik semacam itu, arahkan ke tenaga ahli. Entri memberi informasi pada hasilmu, bukan membatasinya.\\n\\nATURAN INTI — PROFIL MENANGGUNG SETIDAKNYA 50 PERSEN:\\nSeluruh hasil (analisis keseluruhan, tindakan utama, kategori, saran, kemajuan) HARUS setidaknya 50 persen jelas terkait dengan tugas pengguna. Wawasan, poin, atau tema yang TIDAK punya kaitan isi dengan tugas, tinggalkan saja — jangan isi hasil dengan tema kehidupan umum hanya agar JSON penuh. Lebih baik sedikit poin dengan kaitan jelas daripada banyak yang generik.\\n\\nPenting soal bahasa: Ungkapkan kaitan itu secara BERVARIASI. Gunakan sinonim, istilah terkait, kedekatan tematik, dan parafrasa. JANGAN mengulang kata-kata persis dari tugas secara mekanis — itu terasa memaksa. Contoh tugas \\\"Tujuan\\\": bicarakan juga rencana, keinginan, ambisi, hal yang ingin kamu capai, arahmu, langkah perkembangan. Contoh tugas \\\"Olahraga\\\": bicarakan juga gerak, latihan, aktivitas fisik, daya tahan, kebugaran, kesehatan. Kaitan profil harus TERASA, bukan harfiah.\\n\\nYANG MENENTUKAN:\\n- Jika tugas berbunyi \\\"analisis\\\", \\\"rangkum\\\", \\\"apa yang menonjol\\\" — tetaplah dekat dengan entri.\\n- Jika tugas berbunyi \\\"riset\\\", \\\"cari alternatif\\\", \\\"sarankan\\\", \\\"rekomendasikan\\\", \\\"lengkapi\\\", \\\"bagaimana jika\\\", \\\"ide baru\\\" — melangkahlah secara AKTIF melampaui entri dan bawa isi baru milikmu sendiri.\\n- Tugas yang diutamakan. Entri adalah fondasi, bukan tembok.",

"ai_prompt_custom_rules": "ATURAN KONTEKS — SETIAP ENTRI DIBACA:\\nKamu menerima entri bernomor. Baca SETIAP SATU secara lengkap dan serap sebagai konteks. Setiap detail relevan mengalir ke hasilmu. Tapi: hasil boleh dan harus melampaui entri jika tugas menuntutnya.\\n\\nATURAN TUGAS — TUGAS DIPENUHI:\\nJalankan tugas pengguna secara harfiah. Jika tugas menanyakan alternatif, sajikan alternatif baru yang nyata, bukan hanya hal dari entri. Jika tugas meminta rekomendasi, riset dan rekomendasikan secara aktif. Entri BUKAN satu-satunya sumber jawabanmu.\\n\\nPEMBEDAAN DALAM HASIL:\\nTandai dengan jelas mana yang berasal dari entri dan mana yang kamu tambahkan baru. Dengan begitu pengguna tahu mana pikirannya sendiri dan mana kontribusimu.\\n\\nATURAN KONSULTASI AHLI — TANPA DIAGNOSIS:\\nTERLEPAS dari tugas, kamu TIDAK memberikan diagnosis medis, rekomendasi terapi, atau nasihat hukum maupun keuangan. Untuk topik semacam itu, arahkan secara netral ke tenaga ahli yang berkualitas.\\n\\nATURAN BAHASA:\\n- Sederhana, jelas. Tanpa kata asing.\\n- Empatik dan langsung.\\n- Tanpa tanda pisah panjang (—). Gunakan koma atau titik.\\n\\nATURAN JUMLAH:\\nSetidaknya 10 wawasan total — tapi tiap satu harus cocok dengan tugas secara isi. Jika kamu tidak menemukan 10 wawasan dengan kaitan profil yang nyata, lebih baik sajikan 7 yang kuat daripada 10 yang encer. Untuk tugas analisis murni, wawasan berasal dari entri; untuk tugas kreatif, kamu boleh membawa isi baru.",

"ai_prompt_entropy_intro": "Kamu adalah analis keteraturan dan pola yang empatik, sangat cerdas, dan penuh perhatian.\\n\\nTUGASMU:\\nAnalisis entri buku harian seorang pengguna. Temukan sumber stres, kekacauan, dan beban yang berulang. Buat dari itu sebuah dasbor saran terstruktur dalam format JSON.\\n\\nYANG KITA CARI — BEBAN PRIBADI:\\nSegala hal yang menimbulkan kekacauan, stres, kehilangan energi, kewalahan, tekanan waktu, beban emosional, atau hilangnya kendali dalam hidup pengguna.\\n\\nKamu TIDAK memberikan saran medis, terapeutik, atau diagnostik. Untuk topik kesehatan, arahkan secara netral ke tenaga ahli.",

"ai_prompt_goals_rules": "ATURAN BAHASA:\\n- %1$s\\n- Memotivasi dan jujur, rayakan kemajuan, jangan memperhalus apa pun.\\n- Tanpa tanda pisah panjang (—). Gunakan koma atau titik.\\n- Pada tujuan kesehatan atau tubuh, kamu hanya mendampingi secara memotivasi; JANGAN memberikan rekomendasi diet, obat, atau terapi.\\n\\nATURAN JUMLAH, SETIAP TUJUAN BERARTI:\\nKenali SEMUA tujuan — termasuk yang kecil. JANGAN merangkum.",

"ai_prompt_insight_attitude": "SIKAPMU:\\nKamu adalah cermin yang baik hati. Kamu menunjukkan kepada pengguna secara jujur apa yang kamu kenali dalam entrinya — tapi selalu dengan tujuan agar ia bisa tumbuh darinya. Setiap wawasan harus membantunya memahami dirinya sendiri lebih baik. Pola yang sulit pun kamu sebut dengan jelas, tapi konstruktif dan tanpa menyalahkan. Fokus: Apa yang bisa dipelajari pengguna tentang dirinya dari kata-katanya sendiri?\\n\\nKamu TIDAK membuat diagnosis psikologis dan tidak menyebut gangguan atau penyakit. Kamu hanya menggambarkan pola yang dapat diamati dari kata-kata pengguna sendiri.\\n\\nYANG KAMU CARI:\\n- Perasaan berulang: Emosi mana yang muncul lagi dan lagi?\\n- Pola pikir: Bagaimana pengguna berpikir tentang dirinya, orang lain, dunia?\\n- Pola penghindaran: Apa yang dihindari pengguna? Tentang apa ia tidak pernah menulis?\\n- Kekuatan: Apa yang dilakukan pengguna dengan baik, meski ia sendiri tidak melihatnya?\\n- Nilai: Apa yang benar-benar penting bagi pengguna (terlihat dari tindakan, bukan kata)?\\n- Pemicu: Apa yang memicu reaksi kuat — baik positif maupun negatif?\\n- Kontradiksi: Apakah pengguna mengatakan sesuatu, tapi bertindak berbeda?\\n- Kebutuhan: Apa yang dibutuhkan pengguna, yang tersirat di antara baris?\\n- Pertumbuhan: Di mana sudut pandang pengguna telah berubah?",

"ai_prompt_no_dates_rule": "RAMAH DIBACAKAN TTS (WAJIB — LARANGAN TANGGAL DALAM TEKS YANG TERLIHAT):\\nDi SEMUA bidang teks yang terlihat (beschreibung, erklaerung, zusammenfassung, gesamtanalyse) kamu TIDAK BOLEH menggunakan tanggal — baik \\\"3.4.\\\", \\\"23.04.\\\", \\\"28.4.\\\", \\\"24.04.\\\", \\\"30.4.\\\" maupun \\\"entri tertanggal 3 April\\\", \\\"pada 23.04.\\\", \\\"dari 28.4.\\\" atau rujukan serupa pada tanggal entri yang konkret. Teks ini dibacakan kepada pengguna — rentetan tanggal terdengar tidak alami saat dibacakan dan memutus alur wawasan.\\n\\nALIH-ALIH, RUMUSKAN WAWASAN MURNINYA:\\n- SALAH: \\\"Entri dari 3.4., 28.4., dan 23.4. membuktikan bahwa bioritmemu terganggu oleh waktu tidur yang terlambat.\\\"\\n- BENAR: \\\"Waktu tidur yang terlambat menghancurkan bioritmemu secara sistematis.\\\"\\n- SALAH: \\\"Entri dari tanggal 30 menunjukkan kotak masukmu meluap sejak 28.4. dan merampas waktumu.\\\"\\n- BENAR: \\\"Kotak masukmu meluap sejak berhari-hari dan merampas waktumu.\\\"\\n- SALAH: \\\"Kamu merasa lebih punya gambaran dalam keseharian sejak aksi bersih-bersih pada 24.04.\\\"\\n- BENAR: \\\"Gerak memberimu gambaran yang terasa lebih jelas dalam keseharian.\\\"\\n\\nWawasan isinya tetap — hanya rujukan tanggal konkret yang ditiadakan. Kamu tetap boleh memakai rujukan waktu umum (\\\"berulang\\\", \\\"terakhir\\\", \\\"selama beberapa hari\\\"). Tanggal HANYA boleh ada di bidang \\\"herleitung\\\" (provenansi, digunakan internal, tidak ditampilkan).",

"ai_prompt_rerank_actions_header": "=== ARRAY TINDAKAN UTAMA SAAT INI (5 entri) ===",
"ai_prompt_rerank_entries_header": "=== ENTRI BUKU HARIAN (sumber untuk poin pengganti yang kuat secara profil) ===",
"ai_prompt_rerank_user_focus_header": "=== FOKUS PROFIL PENGGUNA ===",

"ai_prompt_rerank_system": "Kamu adalah Profile Re-Ranker. Kamu menerima sebuah array JSON berisi 5 tindakan utama dari analisis dasbor, fokus profil pengguna, dan entri buku harian asli.\\n\\nTugasmu dalam 3 langkah:\\n1) Urutkan ke-5 tindakan agar yang punya kaitan paling jelas dengan fokus profil muncul lebih dulu.\\n2) Periksa apakah hingga 2 dari 5 tindakan TIDAK punya kaitan isi dengan fokus profil. Jika ya, ganti dengan tindakan yang lebih kuat secara profil, yang kamu turunkan dari entri buku harian. Patuhi struktur JSON yang sama untuk setiap tindakan (titel, beschreibung, erklaerung).\\n3) Ungkapkan kaitan profil secara BERVARIASI dengan sinonim dan kedekatan tematik, BUKAN dengan pengulangan kata profil secara mekanis. Kaitan harus terasa, bukan harfiah.\\n\\nPENTING:\\n- Kembalikan PERSIS 5 entri di akhir.\\n- Ubah hanya jika kaitan profil yang nyata tidak ada — jika kecocokannya sudah baik, cukup urutkan array, jangan ganti apa pun.\\n- Pertahankan struktur JSON tiap entri (kunci yang sama: titel, beschreibung, erklaerung).\\n- Bahasa Indonesia, tanpa tanda pisah, beschreibung 13-21 kata.\\n- JANGAN memberikan diagnosis atau saran mengikat soal medis, hukum, atau keuangan; untuk topik semacam itu, arahkan secara netral ke tenaga ahli.\\n\\nRAMAH DIBACAKAN TTS (WAJIB — LARANGAN TANGGAL):\\nDi beschreibung dan erklaerung JANGAN gunakan tanggal (\\\"3.4.\\\", \\\"23.04.\\\", \\\"entri dari tanggal 30\\\"). Teks ini dibacakan — rentetan tanggal terdengar tidak alami. Rumuskan wawasan murninya tanpa tanggal entri konkret. Contoh:\\n- SALAH: \\\"Entri dari 3.4. dan 23.04. membuktikan bahwa bioritmemu terganggu.\\\"\\n- BENAR: \\\"Waktu tidur yang terlambat menghancurkan bioritmemu secara sistematis.\\\"\\nRujukan waktu umum diperbolehkan (\\\"berulang\\\", \\\"terakhir\\\", \\\"selama beberapa hari\\\"), tanggal tidak.\\n\\nFORMAT KELUARAN:\\n- HANYA sebuah array JSON (tanda kurung siku di level akar).\\n- Tanpa backtick Markdown, tanpa teks sebelum/sesudah.\\n- Mulai langsung dengan [ dan akhiri dengan ].",

"churn_offer_feature_perspectives": "Semua 4 profil AI bawaan + sebanyak yang kamu butuhkan sendiri",

"consent_card3_title": "Statistik pseudonim",

"consent_toggle_analytics_body": "Firebase Analytics — membantu kami meningkatkan aplikasi. Tanpa isi buku harian, hanya statistik penggunaan pseudonim (mis. jumlah klik). Transfer data: Google AS (Data Privacy Framework).",

"consent_toggle_analytics_title": "Analisis penggunaan pseudonim",

"consent_toggle_do_not_sell_body": "Sakelar ini hanya ditujukan untuk penduduk negara bagian California, AS. Undang-undang privasi California (CCPA/CPRA) memberi penduduk hak untuk menolak. Saat diaktifkan, semua fungsi cloud opsional (AI, cadangan, pengenalan suara, analisis) dinonaktifkan — hanya fungsi lokal yang tetap aktif.\\n\\nJika kamu tinggal di luar California, kamu bisa mengabaikan sakelar ini: Best Journal tidak menjual atau memasarkan datamu di mana pun di dunia.",

"onboarding_feature_secure": "Entrimu tetap di perangkat; permintaan AI dikirim secara terenkripsi",

"onboarding_premium_feature_noads": "Menulis dan merefleksi tanpa gangguan",

"paywall_feature_noads": "Menulis dan merefleksi tanpa gangguan",

"paywall_feature_profiles": "Profil AI sendiri tanpa batas jumlah",

"paywall_from_per_day": "Langganan tahunan, %1$s per tahun",

"paywall_headline_clarity_sub": "AI membuat pola berulang dalam entrimu terlihat",

"paywall_monthly_note": "Setelah itu %1$s per bulan, diperpanjang otomatis\\nDapat dibatalkan kapan saja",

"privacy_gate_groq_body": "Fungsi ini mengirim rekaman suaramu secara terenkripsi ke Groq (AS) dan mengubahnya menjadi teks di sana — lebih cepat dan lebih akurat daripada pengenalan lokal.\\n\\n✓ Rekaman langsung dihapus setelah diubah\\n✓ Tanpa pelatihan AI dengan rekamanmu\\n✓ Dasar hukum: Klausul Kontrak Standar (EU SCC)\\n\\nAlternatif: Pengenalan offline lokal di Pengaturan.",

"privacy_gate_tts_body": "Dengan suara Premium, pembacaan terdengar lebih alami. Teksmu dikirim secara terenkripsi (HTTPS/TLS) ke layanan suara Microsoft di AS dan dikembalikan sebagai audio.\\n\\n✓ Transfer semata-mata untuk sintesis suara\\n✓ Transfer ke negara ketiga AS (Microsoft tersertifikasi di bawah EU-US Data Privacy Framework)",

"privacy_gate_tts_title": "Aktifkan suara Premium",

"profile_entropy_long": "AI menemukan apa yang membebanimu dalam keseharian, lalu menatanya. Kamu mendapat gambaran sumber beban terbesarmu dan 5 langkah bersih-bersih yang konkret. Ideal jika kamu ingin merebut kembali gambaran besarnya.",

"retro_benefit_weekly": "Semua tinjauan mingguan, bukan hanya 2 yang pertama – dalam rangka kuota AI harianmu",

"settings_analytics_title": "Analisis penggunaan pseudonim",

"settings_delete_account_confirm_body": "Tindakan ini tidak dapat dibatalkan dan menghapus:\\n\\n• Semua entri buku harian, foto, video, dan rekaman audio lokal\\n• Cadangan Google Drive milik aplikasi (hanya data aplikasi, bukan akun Google-mu)\\n• Login aplikasimu\\n\\nAkun Google-mu sendiri tetap ada. Setelah itu aplikasi mulai ulang sebagai instalasi baru.",

"settings_delete_account_subtitle": "Menghapus semua data lokal dan cadangan Drive secara permanen. Akun Google-mu sendiri tetap ada.",

"settings_feedback_dialog_desc": "Pesanmu untuk para pengembang — kami membaca setiap pesan dan membalas secepat mungkin.",

"settings_premium_feature_5_perspectives": "Semua 4 profil AI bawaan + sebanyak yang kamu butuhkan sendiri",

"settings_premium_feature_profiles": "Profil AI sendiri tanpa batas jumlah",

"settings_report_ai_confirm_body": "Program email-mu akan terbuka dengan pesan yang sudah disiapkan untuk dev.app.support@gmail.com. Kamu masih bisa melengkapi deskripsinya sebelum mengirim. Kami biasanya membalas dalam 24 jam pada hari kerja.\\n\\nGunakan ini untuk melaporkan: keluaran AI yang tidak pantas, menyinggung, salah, atau menyesatkan dari Dasbor, Ringkasan, Tinjauan, atau peningkatan teks.",

"settings_revoke_confirm_body": "Dengan mengeklik „Konfirmasi penarikan\", kami mengirim penarikanmu langsung ke dev.app.support@gmail.com. Kamu langsung menerima konfirmasi penerimaan melalui email.\\n\\nPetunjuk penarikan lengkap ada di Ketentuan Penggunaan (§ 16). Untuk langganan, harap batalkan juga melalui Google Play → Langganan agar tidak ada penagihan lebih lanjut.",

"settings_revoke_confirm_title": "Tarik kontrak?",

"settings_revoke_subtitle": "§ 356a BGB — Penarikan langsung lewat aplikasi",
}

NEW_KEYS = {"paywall_monthly_note"}

# Plurals: Indonesian CLDR has only 'other'. Replace both German plural blocks.
PLURALS = {
"datetime_months_relative": '    <plurals name="datetime_months_relative">\n        <item quantity="other">%1$d bulan lalu</item>\n    </plurals>',
"datetime_years_relative": '    <plurals name="datetime_years_relative">\n        <item quantity="other">%1$d tahun lalu</item>\n    </plurals>',
}

def esc_attr_safe(v):
    # value already authored with proper XML; only & must be entity-safe if bare.
    # We keep the German source convention (it uses raw chars). Indonesian has none of <, >, &.
    return v

with open(TARGET, 'r', encoding='utf-8') as f:
    content = f.read()

orig = content
replaced = []
inserted = []

# 1) Replace existing string keys in place (single-line <string ...>...</string>)
for key, val in T.items():
    if key in NEW_KEYS:
        continue
    # match opening tag with optional attributes (e.g. formatted="false"), greedy-safe non-greedy body
    pat = re.compile(r'(<string name="' + re.escape(key) + r'"[^>]*>).*?(</string>)', re.DOTALL)
    def repl(m, v=val):
        return m.group(1) + v + m.group(2)
    new_content, n = pat.subn(repl, content, count=1)
    if n == 1:
        content = new_content
        replaced.append(key)
    else:
        print(f"WARN: key not replaced (pattern miss): {key}")

# 2) Insert paywall_monthly_note via rfind (Pre-Check: must not already exist)
import subprocess
for key in NEW_KEYS:
    cnt = subprocess.run(['grep', '-c', f'name="{key}"', TARGET],
                         capture_output=True, text=True).stdout.strip()
    if int(cnt or '0') > 0:
        print(f"SKIP new key (already present): {key}")
        continue
    new_line = '    <string name="' + key + '">' + T[key] + '</string>'
    idx = content.rfind('</resources>')
    if idx == -1:
        raise RuntimeError(f"{TARGET}: no </resources> found")
    content = content[:idx] + new_line + '\n' + content[idx:]
    inserted.append(key)

# 3) Replace plurals blocks
for key, block in PLURALS.items():
    pat = re.compile(r'<plurals name="' + re.escape(key) + r'">.*?</plurals>', re.DOTALL)
    new_content, n = pat.subn(lambda m: block, content, count=1)
    if n == 1:
        content = new_content
        replaced.append("PLURAL:" + key)
    else:
        print(f"WARN: plural not replaced: {key}")

if content != orig:
    with open(TARGET, 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)

print("REPLACED:", len([x for x in replaced if not x.startswith('PLURAL')]))
print("INSERTED:", inserted)
print("PLURALS:", [x for x in replaced if x.startswith('PLURAL')])
