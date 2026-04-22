#!/usr/bin/env python3
"""Batch 4: th, id (values-in/), bn, te, mr."""
import os, re, tempfile

APP_DIR = os.path.expanduser("~/proggs/BestJournalAndroid/app/src/main/res")
TRANSLATIONS = {}

# ═══════════ THAI (th) — No politeness particles, Arabic numerals ═══════════
TRANSLATIONS["th"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">ความเป็นส่วนตัวและการยินยอม</string>
    <string name="consent_intro">สมุดบันทึกคือพื้นที่ส่วนตัวของคุณ และเราเคารพสิ่งนี้ ที่นี่คุณเห็นได้อย่างโปร่งใสว่า Best Journal จัดการกับข้อมูลของคุณอย่างไร</string>
    <string name="consent_card1_title">การจัดเก็บในเครื่อง</string>
    <string name="consent_card1_body">บันทึกของคุณยังอยู่ในอุปกรณ์</string>
    <string name="consent_card2_title">ฟีเจอร์ AI (สหรัฐอเมริกา)</string>
    <string name="consent_card2_body">โดยเลือกได้ ข้อความจะถูกส่งไปยัง Google Gemini การบันทึกเสียงไปยัง Groq และข้อความอ่านออกเสียงไปยัง Microsoft Edge ในสหรัฐอเมริกา (EU-US Data Privacy Framework + ข้อสัญญามาตรฐาน)</string>
    <string name="consent_card3_title">สถิติแบบไม่ระบุตัวตน</string>
    <string name="consent_card3_body">Firebase Analytics แบบเลือกใช้ เปลี่ยนได้ตลอดในการตั้งค่า</string>
    <string name="consent_links_header">เอกสารทางกฎหมายของเรา:</string>
    <string name="consent_accept_all">ยอมรับและเริ่มต้น</string>
    <string name="consent_disable_stats">ปิดสถิติและดำเนินการต่อ</string>
    <string name="consent_confirmation">การแตะ \"ยอมรับและเริ่มต้น\" ยืนยันว่าคุณได้อ่านนโยบายความเป็นส่วนตัว เงื่อนไขการใช้งาน และประกาศทางกฎหมายแล้ว และยอมรับการประมวลผลข้อมูลตามที่อธิบายไว้ คุณสามารถเปลี่ยนการตัดสินใจได้ตลอดในการตั้งค่า</string>

    <string name="privacy_gate_groq_title">ส่งการบันทึกเสียงไปยัง Groq หรือไม่</string>
    <string name="privacy_gate_groq_body">สำหรับการถอดเสียงบนคลาวด์ การบันทึกเสียงของคุณจะถูกส่งแบบเข้ารหัสไปยัง Groq, Inc. (เมาน์เทนวิว สหรัฐอเมริกา) และแปลงเป็นข้อความที่นั่น ไฟล์เสียงจะถูกลบหลังการประมวลผลและไม่ใช้ในการฝึกโมเดล\n\nทางเลือก: ใช้การถอดเสียงในเครื่อง (ออฟไลน์ ไม่มีการส่งข้อมูล) ตั้งค่าได้ใน การตั้งค่า → AI</string>
    <string name="privacy_gate_groq_accept">ยอมรับและส่ง</string>
    <string name="privacy_gate_groq_local">ถอดเสียงในเครื่องแทน</string>

    <string name="privacy_gate_gemini_title">ส่งข้อความไปยัง Google Gemini หรือไม่</string>
    <string name="privacy_gate_gemini_body">สำหรับฟีเจอร์ AI (แดชบอร์ด สรุป ทบทวน ปรับปรุงข้อความ) ส่วนหนึ่งของบันทึกจะถูกส่งแบบเข้ารหัสไปยัง Google Gemini (Firebase AI สหรัฐอเมริกา) ฐานทางกฎหมาย: EU-US Data Privacy Framework + ข้อสัญญามาตรฐาน คำขอจะถูกลบหลังการประมวลผลและไม่ใช้ในการฝึกโมเดล</string>
    <string name="privacy_gate_gemini_accept">ยอมรับและส่ง</string>
    <string name="privacy_gate_gemini_cancel">ยกเลิก</string>

    <string name="privacy_gate_tts_title">ส่งข้อความไปยัง Microsoft หรือไม่</string>
    <string name="privacy_gate_tts_body">สำหรับการอ่านออกเสียง ข้อความจะถูกส่งแบบเข้ารหัสไปยัง Microsoft Bing Speech (สหรัฐอเมริกา) และส่งกลับเป็นเสียง ฐานทางกฎหมาย: EU-US Data Privacy Framework + ข้อสัญญามาตรฐาน\n\nทางเลือก: ใช้ TTS ออฟไลน์ในตัวของ Android</string>
    <string name="privacy_gate_tts_accept">ยอมรับและอ่าน</string>
    <string name="privacy_gate_tts_cancel">ยกเลิก</string>

    <string name="settings_privacy_header">ความเป็นส่วนตัว</string>
    <string name="settings_analytics_title">สถิติแบบไม่ระบุตัวตน</string>
    <string name="settings_analytics_subtitle">Firebase Analytics สำหรับการวิเคราะห์ข้อผิดพลาดและการปรับปรุงผลิตภัณฑ์</string>

    <string name="settings_delete_account_title">ลบบัญชีและข้อมูล</string>
    <string name="settings_delete_account_subtitle">ลบข้อมูลทั้งหมดในเครื่อง บัญชี Google และข้อมูลสำรองใน Drive อย่างถาวร</string>
    <string name="settings_delete_account_confirm_title">ลบบัญชีอย่างถาวรหรือไม่</string>
    <string name="settings_delete_account_confirm_body">การกระทำนี้ย้อนกลับไม่ได้ และจะลบ:\n\n• บันทึกไดอารี่ ภาพถ่าย และวิดีโอในเครื่องทั้งหมด\n• บัญชี Firebase ของคุณ\n• ข้อมูลสำรองของแอปใน Google Drive\n\nแอปจะเริ่มใหม่เสมือนเป็นการติดตั้งใหม่</string>
    <string name="settings_delete_account_cancel">ยกเลิก</string>
    <string name="settings_delete_account_confirm">ใช่ ลบทั้งหมด</string>

    <string name="settings_report_ai_title">รายงานคำตอบของ AI</string>
    <string name="settings_report_ai_subtitle">ผลลัพธ์ AI ที่ไม่เหมาะสมหรือผิดพลาด</string>
    <string name="settings_report_ai_confirm_title">เปิดอีเมลถึงฝ่ายสนับสนุนหรือไม่</string>
    <string name="settings_report_ai_confirm_body">แอปอีเมลจะเปิดพร้อมข้อความที่เตรียมไว้ถึง dev.app.support@gmail.com คุณสามารถเพิ่มคำอธิบายก่อนส่งได้ เราตอบกลับภายใน 24 ชั่วโมงในวันทำการ\n\nกรุณารายงานที่นี่: ผลลัพธ์ AI ที่ไม่เหมาะสม ก้าวร้าว ผิดพลาด หรือชี้นำผิดทางจากแดชบอร์ด สรุป ทบทวน หรือปรับปรุงข้อความ</string>
    <string name="settings_report_ai_confirm">สร้างรายงาน</string>
    <string name="settings_report_ai_cancel">ยกเลิก</string>
    <string name="settings_report_ai_no_email">ไม่พบแอปอีเมล โปรดส่งรายงานไปที่ dev.app.support@gmail.com</string>
    <string name="settings_report_ai_subject">Best Journal: คำตอบ AI ที่ไม่เหมาะสม</string>
    <string name="settings_report_ai_body">สวัสดี,\n\nฉันต้องการรายงานคำตอบ AI ที่ไม่เหมาะสมหรือผิดพลาดใน Best Journal\n\nคำอธิบายปัญหา:\n[กรุณากรอก]\n\nบริบท (ฟีเจอร์ใด อินพุตใด):\n[กรุณากรอก]\n\nขอบคุณ</string>

    <string name="settings_revoke_title">การเพิกถอน</string>
    <string name="settings_revoke_subtitle">การซื้อ Premium</string>
    <string name="settings_revoke_confirm_title">เปิดอีเมลถึงฝ่ายสนับสนุนหรือไม่</string>
    <string name="settings_revoke_confirm_body">แอปอีเมลจะเปิดพร้อมข้อความที่เตรียมไว้ถึง dev.app.support@gmail.com เราตอบกลับภายใน 24 ชั่วโมงในวันทำการ\n\nข้อมูลฉบับเต็มเกี่ยวกับสิทธิ์ในการเพิกถอนอยู่ในเงื่อนไขการใช้งาน (§ 16) สำหรับการสมัครสมาชิก กรุณายกเลิกเพิ่มเติมที่ Google Play → การสมัครสมาชิก</string>
    <string name="settings_revoke_cancel">ยกเลิก</string>
    <string name="settings_revoke_confirm">สร้างการเพิกถอน</string>
    <string name="settings_revoke_no_email">ไม่พบแอปอีเมล โปรดส่งการเพิกถอนไปที่ dev.app.support@gmail.com</string>
    <string name="settings_revoke_email_subject">เพิกถอนสัญญา Best Journal Premium</string>
    <string name="settings_revoke_email_body">ข้าพเจ้าขอแจ้งการถอนตัวจากสัญญาเกี่ยวกับฟีเจอร์ Premium ของ Best Journal ผ่านหนังสือนี้\n\nผู้ส่ง (บัญชี Google): %1$s\nเวลาที่ถอนตัว: %2$s\n\nการถอนตัวนี้ถูกดำเนินการแบบสองขั้นตอนผ่านปุ่มถอนตัวในแอปที่สอดคล้องกับ § 356a BGB และส่งโดยอัตโนมัติผ่าน Gmail API</string>
    <string name="settings_revoke_confirm_subject">การยืนยันการรับของคุณ: การถอนตัวใน Best Journal</string>
    <string name="settings_revoke_confirm_user_body">สวัสดี\n\nเราได้รับคำขอถอนตัวของคุณเมื่อ %1$s แล้ว นี่คือการยืนยันการรับตาม § 356a BGB\n\nเราจะดำเนินการคำขอถอนตัวของคุณโดยเร็วที่สุด และจะติดต่อคุณผ่าน dev.app.support@gmail.com หากมีคำถาม\n\nเพื่อไม่ให้มีการเรียกเก็บเงินเพิ่มเติม โปรดยกเลิกการสมัครของคุณใน Google Play Store ใต้ "Subscriptions" ด้วย\n\nขอบคุณและขอแสดงความนับถือ\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">ไม่พบบัญชี Google ที่ลงชื่อเข้าใช้อยู่ โปรดลงชื่อเข้าใช้ด้วยบัญชี Google ของคุณใน Settings หรือส่งการถอนตัวด้วยตนเองไปที่ dev.app.support@gmail.com</string>
    <string name="settings_revoke_sending">กำลังส่งการถอนตัว…</string>
    <string name="settings_revoke_success_title">ได้รับการถอนตัวแล้ว</string>
    <string name="settings_revoke_success_body">การถอนตัวของคุณถูกส่งไปที่ dev.app.support@gmail.com แล้ว การยืนยันการรับก็อยู่ในกล่องจดหมายของคุณด้วย</string>
    <string name="settings_revoke_success_close">ปิด</string>
    <string name="settings_revoke_error_title">ไม่สามารถส่งการถอนตัวได้</string>
    <string name="settings_revoke_error_body">การส่งอัตโนมัติล้มเหลว: %1$s\n\nคุณสามารถส่งอีเมลด้วยตนเองไปที่ dev.app.support@gmail.com ได้เช่นกัน ให้แตะ "เปิดแอปอีเมล" เพื่อทำเช่นนั้น</string>
    <string name="settings_revoke_error_email_fallback">เปิดแอปอีเมล</string>
"""

# ═══════════ INDONESIAN (id -> values-in/) — kamu (not Anda) ═══════════
TRANSLATIONS["in"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">Privasi dan persetujuan</string>
    <string name="consent_intro">Jurnalmu adalah ruang pribadi, dan kami menghormatinya. Di sini kamu melihat secara transparan bagaimana Best Journal menangani datamu.</string>
    <string name="consent_card1_title">Penyimpanan lokal</string>
    <string name="consent_card1_body">Catatanmu tetap di perangkatmu.</string>
    <string name="consent_card2_title">Fitur AI (AS)</string>
    <string name="consent_card2_body">Secara opsional, teks dikirim ke Google Gemini, rekaman suara ke Groq, dan teks pembacaan ke Microsoft Edge di AS (EU-US Data Privacy Framework + klausul kontrak standar).</string>
    <string name="consent_card3_title">Statistik anonim</string>
    <string name="consent_card3_body">Firebase Analytics, opsional, bisa diubah kapan saja di pengaturan.</string>
    <string name="consent_links_header">Dokumen hukum kami:</string>
    <string name="consent_accept_all">Setuju dan mulai</string>
    <string name="consent_disable_stats">Matikan statistik dan lanjut</string>
    <string name="consent_confirmation">Dengan menekan \"Setuju dan mulai\" kamu mengonfirmasi telah membaca kebijakan privasi, syarat penggunaan, dan keterangan hukum, dan menyetujui pemrosesan data yang dijelaskan. Kamu bisa mengubah keputusanmu kapan saja di pengaturan.</string>

    <string name="privacy_gate_groq_title">Kirim rekaman suara ke Groq?</string>
    <string name="privacy_gate_groq_body">Untuk transkripsi cloud, rekaman suaramu dikirim terenkripsi ke Groq, Inc. (Mountain View, AS) dan diubah menjadi teks di sana. File audio dihapus setelah pemrosesan dan tidak dipakai untuk pelatihan.\n\nAlternatif: gunakan transkripsi lokal di perangkat (offline, tanpa transfer data), bisa diatur di Pengaturan → AI.</string>
    <string name="privacy_gate_groq_accept">Setuju dan kirim</string>
    <string name="privacy_gate_groq_local">Transkripsi lokal saja</string>

    <string name="privacy_gate_gemini_title">Kirim teks ke Google Gemini?</string>
    <string name="privacy_gate_gemini_body">Untuk fitur AI (dasbor, ringkasan, retrospeksi, penyempurnaan teks), kutipan catatanmu dikirim terenkripsi ke Google Gemini (Firebase AI, AS). Dasar hukum: EU-US Data Privacy Framework + klausul kontrak standar. Permintaan dihapus setelah pemrosesan dan tidak dipakai untuk pelatihan.</string>
    <string name="privacy_gate_gemini_accept">Setuju dan kirim</string>
    <string name="privacy_gate_gemini_cancel">Batal</string>

    <string name="privacy_gate_tts_title">Kirim teks ke Microsoft?</string>
    <string name="privacy_gate_tts_body">Untuk pembacaan suara, teks dikirim terenkripsi ke Microsoft Bing Speech (AS) dan dikembalikan sebagai audio. Dasar hukum: EU-US Data Privacy Framework + klausul kontrak standar.\n\nAlternatif: gunakan TTS offline bawaan Android.</string>
    <string name="privacy_gate_tts_accept">Setuju dan bacakan</string>
    <string name="privacy_gate_tts_cancel">Batal</string>

    <string name="settings_privacy_header">Privasi</string>
    <string name="settings_analytics_title">Statistik anonim</string>
    <string name="settings_analytics_subtitle">Firebase Analytics untuk analisis kesalahan dan peningkatan produk</string>

    <string name="settings_delete_account_title">Hapus akun dan data</string>
    <string name="settings_delete_account_subtitle">Menghapus semua data lokal, akun Google, dan cadangan Drive secara permanen</string>
    <string name="settings_delete_account_confirm_title">Hapus akun secara permanen?</string>
    <string name="settings_delete_account_confirm_body">Tindakan ini tidak bisa dibatalkan dan menghapus:\n\n• Semua catatan jurnal, foto, dan video lokal\n• Akun Firebase-mu\n• Cadangan aplikasi di Google Drive\n\nAplikasi akan memulai ulang sebagai instalasi baru.</string>
    <string name="settings_delete_account_cancel">Batal</string>
    <string name="settings_delete_account_confirm">Ya, hapus semua</string>

    <string name="settings_report_ai_title">Laporkan jawaban AI</string>
    <string name="settings_report_ai_subtitle">Keluaran AI yang tidak pantas atau salah</string>
    <string name="settings_report_ai_confirm_title">Buka email ke dukungan?</string>
    <string name="settings_report_ai_confirm_body">Aplikasi emailmu terbuka dengan pesan siap kirim ke dev.app.support@gmail.com. Kamu bisa menambahkan deskripsi sebelum mengirim. Kami membalas dalam 24 jam pada hari kerja.\n\nLaporkan di sini: keluaran AI yang tidak pantas, menyinggung, salah, atau menyesatkan dari dasbor, ringkasan, retrospeksi, atau penyempurnaan teks.</string>
    <string name="settings_report_ai_confirm">Buat laporan</string>
    <string name="settings_report_ai_cancel">Batal</string>
    <string name="settings_report_ai_no_email">Aplikasi email tidak ditemukan. Kirim laporan ke dev.app.support@gmail.com.</string>
    <string name="settings_report_ai_subject">Best Journal: jawaban AI tidak pantas</string>
    <string name="settings_report_ai_body">Halo,\n\nsaya ingin melaporkan jawaban AI yang tidak pantas atau salah di Best Journal.\n\nDeskripsi masalah:\n[Isi]\n\nKonteks (fitur yang mana, input apa):\n[Isi]\n\nTerima kasih.</string>

    <string name="settings_revoke_title">Pembatalan kontrak</string>
    <string name="settings_revoke_subtitle">Pembelian Premium</string>
    <string name="settings_revoke_confirm_title">Buka email ke dukungan?</string>
    <string name="settings_revoke_confirm_body">Aplikasi emailmu terbuka dengan pesan siap kirim ke dev.app.support@gmail.com. Kami membalas dalam 24 jam pada hari kerja.\n\nInformasi lengkap tentang hak pembatalan ada di syarat penggunaan (§ 16). Untuk langganan, batalkan juga lewat Google Play → Langganan.</string>
    <string name="settings_revoke_cancel">Batal</string>
    <string name="settings_revoke_confirm">Buat pembatalan</string>
    <string name="settings_revoke_no_email">Aplikasi email tidak ditemukan. Kirim pembatalan ke dev.app.support@gmail.com.</string>
    <string name="settings_revoke_email_subject">Pembatalan kontrak Best Journal Premium</string>
    <string name="settings_revoke_email_body">Dengan ini saya memberitahukan pembatalan kontrak saya atas fitur Premium Best Journal.\n\nPengirim (akun Google): %1$s\nWaktu pembatalan: %2$s\n\nPembatalan ini dipicu dalam dua tahap melalui tombol pembatalan di aplikasi yang sesuai dengan § 356a BGB dan dikirim secara otomatis melalui Gmail API.</string>
    <string name="settings_revoke_confirm_subject">Tanda terima Anda: pembatalan di Best Journal</string>
    <string name="settings_revoke_confirm_user_body">Halo,\n\nkami telah menerima pembatalan Anda pada %1$s. Ini adalah tanda terima Anda sesuai § 356a BGB.\n\nKami akan memproses pembatalan Anda secepat mungkin dan akan menghubungi Anda melalui dev.app.support@gmail.com jika ada pertanyaan.\n\nAgar tidak ada penagihan baru, mohon batalkan juga langganan Anda di Google Play Store pada bagian "Subscriptions".\n\nTerima kasih dan salam hangat\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">Tidak ditemukan alamat akun Google yang sedang masuk. Silakan masuk dengan akun Google Anda di Settings atau kirim pembatalan secara manual ke dev.app.support@gmail.com.</string>
    <string name="settings_revoke_sending">Mengirim pembatalan…</string>
    <string name="settings_revoke_success_title">Pembatalan diterima</string>
    <string name="settings_revoke_success_body">Pembatalan Anda telah dikirim ke dev.app.support@gmail.com. Tanda terima juga ada di kotak masuk Anda.</string>
    <string name="settings_revoke_success_close">Tutup</string>
    <string name="settings_revoke_error_title">Tidak dapat mengirim pembatalan</string>
    <string name="settings_revoke_error_body">Pengiriman otomatis gagal: %1$s\n\nSebagai alternatif, Anda dapat mengirim email manual ke dev.app.support@gmail.com. Ketuk "Buka aplikasi email" untuk itu.</string>
    <string name="settings_revoke_error_email_fallback">Buka aplikasi email</string>
"""

# ═══════════ BENGALI (bn) — তুমি, Bengali script, Arabic numerals ═══════════
TRANSLATIONS["bn"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">গোপনীয়তা ও সম্মতি</string>
    <string name="consent_intro">তোমার ডায়েরি একটি ব্যক্তিগত জায়গা, এবং আমরা সেটাকে সম্মান করি। এখানে তুমি স্বচ্ছভাবে দেখতে পারো যে Best Journal কীভাবে তোমার ডেটা পরিচালনা করে।</string>
    <string name="consent_card1_title">স্থানীয় সংরক্ষণ</string>
    <string name="consent_card1_body">তোমার এন্ট্রি তোমার ডিভাইসে থাকে।</string>
    <string name="consent_card2_title">AI ফিচার (USA)</string>
    <string name="consent_card2_body">ঐচ্ছিকভাবে, টেক্সট Google Gemini-তে, ভয়েস রেকর্ডিং Groq-এ এবং পাঠ্য-থেকে-বক্তৃতার টেক্সট Microsoft Edge-এ USA-তে পাঠানো হয় (EU-US Data Privacy Framework + Standard Contractual Clauses)।</string>
    <string name="consent_card3_title">বেনামী পরিসংখ্যান</string>
    <string name="consent_card3_body">Firebase Analytics, ঐচ্ছিক, সেটিংস থেকে যেকোনো সময় পরিবর্তন করা যায়।</string>
    <string name="consent_links_header">আমাদের আইনি নথি:</string>
    <string name="consent_accept_all">সম্মত হয়ে শুরু করো</string>
    <string name="consent_disable_stats">পরিসংখ্যান বন্ধ করে চালিয়ে যাও</string>
    <string name="consent_confirmation">\"সম্মত হয়ে শুরু করো\"-তে ট্যাপ করে তুমি নিশ্চিত করছ যে তুমি গোপনীয়তা নীতি, ব্যবহারের শর্তাবলী এবং আইনি বিজ্ঞপ্তি পড়েছ এবং বর্ণিত ডেটা প্রক্রিয়াকরণে সম্মত আছ। তুমি সেটিংস থেকে যেকোনো সময় সিদ্ধান্ত পরিবর্তন করতে পারো।</string>

    <string name="privacy_gate_groq_title">Groq-এ ভয়েস রেকর্ডিং পাঠাবে?</string>
    <string name="privacy_gate_groq_body">ক্লাউড ট্রান্সক্রিপশনের জন্য তোমার ভয়েস রেকর্ডিং এনক্রিপ্টেড অবস্থায় Groq, Inc.-এ (Mountain View, USA) পাঠানো হয় এবং সেখানে টেক্সটে রূপান্তরিত হয়। অডিও ফাইল প্রক্রিয়াকরণের পর মুছে ফেলা হয় এবং প্রশিক্ষণের জন্য ব্যবহৃত হয় না।\n\nবিকল্প: ডিভাইসে স্থানীয় ট্রান্সক্রিপশন ব্যবহার করো (অফলাইন, ডেটা স্থানান্তর নেই), Settings → AI থেকে পরিবর্তনযোগ্য।</string>
    <string name="privacy_gate_groq_accept">সম্মত হয়ে পাঠাও</string>
    <string name="privacy_gate_groq_local">এর পরিবর্তে স্থানীয়ভাবে ট্রান্সক্রাইব করো</string>

    <string name="privacy_gate_gemini_title">Google Gemini-তে টেক্সট পাঠাবে?</string>
    <string name="privacy_gate_gemini_body">AI ফিচারগুলোর জন্য (ড্যাশবোর্ড, সারাংশ, পুনরাবলোকন, টেক্সট উন্নতি) তোমার এন্ট্রির অংশ এনক্রিপ্টেড অবস্থায় Google Gemini-তে (Firebase AI, USA) পাঠানো হয়। আইনি ভিত্তি: EU-US Data Privacy Framework + Standard Contractual Clauses। অনুরোধ প্রক্রিয়াকরণের পর মুছে ফেলা হয় এবং প্রশিক্ষণের জন্য ব্যবহৃত হয় না।</string>
    <string name="privacy_gate_gemini_accept">সম্মত হয়ে পাঠাও</string>
    <string name="privacy_gate_gemini_cancel">বাতিল</string>

    <string name="privacy_gate_tts_title">Microsoft-এ টেক্সট পাঠাবে?</string>
    <string name="privacy_gate_tts_body">পাঠ করে শোনানোর জন্য টেক্সট এনক্রিপ্টেড অবস্থায় Microsoft Bing Speech-এ (USA) পাঠানো হয় এবং অডিও হিসেবে ফিরে আসে। আইনি ভিত্তি: EU-US Data Privacy Framework + Standard Contractual Clauses।\n\nবিকল্প: Android-এর অন্তর্নির্মিত অফলাইন TTS ব্যবহার করো।</string>
    <string name="privacy_gate_tts_accept">সম্মত হয়ে পড়ে শোনাও</string>
    <string name="privacy_gate_tts_cancel">বাতিল</string>

    <string name="settings_privacy_header">গোপনীয়তা</string>
    <string name="settings_analytics_title">বেনামী পরিসংখ্যান</string>
    <string name="settings_analytics_subtitle">ত্রুটি বিশ্লেষণ ও পণ্য উন্নতির জন্য Firebase Analytics</string>

    <string name="settings_delete_account_title">অ্যাকাউন্ট ও ডেটা মুছে ফেলো</string>
    <string name="settings_delete_account_subtitle">সব স্থানীয় ডেটা, Google অ্যাকাউন্ট এবং Drive ব্যাকআপ স্থায়ীভাবে মুছে ফেলে</string>
    <string name="settings_delete_account_confirm_title">অ্যাকাউন্ট স্থায়ীভাবে মুছবে?</string>
    <string name="settings_delete_account_confirm_body">এই ক্রিয়া ফিরিয়ে আনা যাবে না এবং এগুলো মুছে ফেলবে:\n\n• সব স্থানীয় ডায়েরি এন্ট্রি, ছবি ও ভিডিও\n• তোমার Firebase অ্যাকাউন্ট\n• অ্যাপের Google Drive ব্যাকআপ\n\nঅ্যাপ নতুন ইনস্টলেশনের মতো পুনরায় চালু হবে।</string>
    <string name="settings_delete_account_cancel">বাতিল</string>
    <string name="settings_delete_account_confirm">হ্যাঁ, সব মুছে ফেলো</string>

    <string name="settings_report_ai_title">AI উত্তর রিপোর্ট করো</string>
    <string name="settings_report_ai_subtitle">অনুপযুক্ত বা ভুল AI আউটপুট</string>
    <string name="settings_report_ai_confirm_title">Support-কে ইমেইল খুলবে?</string>
    <string name="settings_report_ai_confirm_body">তোমার ইমেইল অ্যাপ dev.app.support@gmail.com-এ একটি প্রস্তুত বার্তা নিয়ে খুলবে। পাঠানোর আগে তুমি বিবরণ যোগ করতে পারো। আমরা কর্মদিবসে 24 ঘণ্টার মধ্যে উত্তর দিই।\n\nএখানে রিপোর্ট করো: ড্যাশবোর্ড, সারাংশ, পুনরাবলোকন বা টেক্সট উন্নতি থেকে আসা অনুপযুক্ত, আপত্তিজনক, ভুল বা বিভ্রান্তিকর AI আউটপুট।</string>
    <string name="settings_report_ai_confirm">রিপোর্ট তৈরি করো</string>
    <string name="settings_report_ai_cancel">বাতিল</string>
    <string name="settings_report_ai_no_email">কোনো ইমেইল অ্যাপ পাওয়া যায়নি। অনুগ্রহ করে রিপোর্ট dev.app.support@gmail.com-এ পাঠাও।</string>
    <string name="settings_report_ai_subject">Best Journal: অনুপযুক্ত AI উত্তর</string>
    <string name="settings_report_ai_body">নমস্কার,\n\nআমি Best Journal-এ একটি অনুপযুক্ত বা ভুল AI উত্তর রিপোর্ট করতে চাই।\n\nসমস্যার বিবরণ:\n[অনুগ্রহ করে পূরণ করো]\n\nপ্রসঙ্গ (কোন ফিচার, কোন ইনপুট):\n[অনুগ্রহ করে পূরণ করো]\n\nধন্যবাদ।</string>

    <string name="settings_revoke_title">চুক্তি প্রত্যাহার</string>
    <string name="settings_revoke_subtitle">Premium ক্রয়</string>
    <string name="settings_revoke_confirm_title">Support-কে ইমেইল খুলবে?</string>
    <string name="settings_revoke_confirm_body">তোমার ইমেইল অ্যাপ dev.app.support@gmail.com-এ একটি প্রস্তুত বার্তা নিয়ে খুলবে। আমরা কর্মদিবসে 24 ঘণ্টার মধ্যে উত্তর দিই।\n\nপ্রত্যাহার অধিকার সম্পর্কে সম্পূর্ণ তথ্য ব্যবহারের শর্তাবলীতে (§ 16) রয়েছে। সাবস্ক্রিপশনের জন্য অতিরিক্তভাবে Google Play → Subscriptions থেকে বাতিল করো।</string>
    <string name="settings_revoke_cancel">বাতিল</string>
    <string name="settings_revoke_confirm">প্রত্যাহার তৈরি করো</string>
    <string name="settings_revoke_no_email">কোনো ইমেইল অ্যাপ পাওয়া যায়নি। অনুগ্রহ করে প্রত্যাহার dev.app.support@gmail.com-এ পাঠাও।</string>
    <string name="settings_revoke_email_subject">Best Journal Premium চুক্তি প্রত্যাহার</string>
    <string name="settings_revoke_email_body">এর মাধ্যমে আমি Best Journal-এর Premium ফিচার সম্পর্কিত চুক্তি থেকে প্রত্যাহারের নোটিশ দিচ্ছি।\n\nপ্রেরক (Google অ্যাকাউন্ট): %1$s\nপ্রত্যাহারের সময়: %2$s\n\nএই প্রত্যাহারটি অ্যাপের § 356a BGB-সম্মত দুই-ধাপের প্রত্যাহার বোতামের মাধ্যমে শুরু করা হয়েছে এবং Gmail API-এর মাধ্যমে স্বয়ংক্রিয়ভাবে পাঠানো হয়েছে।</string>
    <string name="settings_revoke_confirm_subject">তোমার গ্রহণ-নিশ্চিতকরণ: Best Journal-এ প্রত্যাহার</string>
    <string name="settings_revoke_confirm_user_body">নমস্কার,\n\nআমরা %1$s-এর তোমার প্রত্যাহার পেয়েছি। এটি § 356a BGB অনুযায়ী তোমার গ্রহণ-নিশ্চিতকরণ।\n\nআমরা যত দ্রুত সম্ভব তোমার প্রত্যাহার প্রক্রিয়া করব এবং কোনো প্রশ্ন থাকলে dev.app.support@gmail.com-এ তোমার সঙ্গে যোগাযোগ করব।\n\nআর কোনো বিলিং না হওয়ার জন্য, Google Play Store-এর "Subscriptions" অংশেও তোমার সাবস্ক্রিপশন বাতিল করো।\n\nধন্যবাদ ও শুভেচ্ছা\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">কোনো সাইন-ইন করা Google অ্যাকাউন্টের ঠিকানা পাওয়া যায়নি। অনুগ্রহ করে Settings-এ তোমার Google অ্যাকাউন্ট দিয়ে সাইন ইন করো অথবা dev.app.support@gmail.com-এ প্রত্যাহারটি হাতে পাঠাও।</string>
    <string name="settings_revoke_sending">প্রত্যাহার পাঠানো হচ্ছে…</string>
    <string name="settings_revoke_success_title">প্রত্যাহার গৃহীত হয়েছে</string>
    <string name="settings_revoke_success_body">তোমার প্রত্যাহার dev.app.support@gmail.com-এ পাঠানো হয়েছে। গ্রহণ-নিশ্চিতকরণও তোমার ইনবক্সে রয়েছে।</string>
    <string name="settings_revoke_success_close">বন্ধ করো</string>
    <string name="settings_revoke_error_title">প্রত্যাহার পাঠানো যায়নি</string>
    <string name="settings_revoke_error_body">স্বয়ংক্রিয় পাঠানো ব্যর্থ হয়েছে: %1$s\n\nবিকল্প হিসেবে তুমি dev.app.support@gmail.com-এ হাতে একটি ইমেইল পাঠাতে পারো। এর জন্য "ইমেইল অ্যাপ খোলো"-এ ট্যাপ করো।</string>
    <string name="settings_revoke_error_email_fallback">ইমেইল অ্যাপ খোলো</string>
"""

# ═══════════ TELUGU (te) — మీరు, Telugu script, Arabic numerals ═══════════
TRANSLATIONS["te"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">గోప్యత మరియు సమ్మతి</string>
    <string name="consent_intro">మీ డైరీ ఒక వ్యక్తిగత స్థలం, దీన్ని మేము గౌరవిస్తాము. Best Journal మీ డేటాను ఎలా నిర్వహిస్తుందో ఇక్కడ పారదర్శకంగా చూడవచ్చు.</string>
    <string name="consent_card1_title">స్థానిక నిల్వ</string>
    <string name="consent_card1_body">మీ ఎంట్రీలు మీ పరికరంలోనే ఉంటాయి.</string>
    <string name="consent_card2_title">AI ఫీచర్లు (USA)</string>
    <string name="consent_card2_body">ఐచ్ఛికంగా, టెక్స్ట్ Google Gemini-కి, వాయిస్ రికార్డింగ్‌లు Groq-కి మరియు పఠన టెక్స్ట్ Microsoft Edge-కి USA లో పంపబడతాయి (EU-US Data Privacy Framework + Standard Contractual Clauses).</string>
    <string name="consent_card3_title">అనామక గణాంకాలు</string>
    <string name="consent_card3_body">Firebase Analytics, ఐచ్ఛికం, సెట్టింగ్స్‌లో ఎప్పుడైనా మార్చవచ్చు.</string>
    <string name="consent_links_header">మా చట్టపరమైన పత్రాలు:</string>
    <string name="consent_accept_all">అంగీకరించి ప్రారంభించండి</string>
    <string name="consent_disable_stats">గణాంకాలను ఆపి కొనసాగించండి</string>
    <string name="consent_confirmation">\"అంగీకరించి ప్రారంభించండి\"ని నొక్కడం ద్వారా మీరు గోప్యతా విధానం, ఉపయోగ నిబంధనలు మరియు చట్టపరమైన సమాచారం చదివారని మరియు వివరించిన డేటా ప్రాసెసింగ్‌కు అంగీకరిస్తున్నారని నిర్ధారిస్తున్నారు. మీ నిర్ణయాన్ని సెట్టింగ్స్‌లో ఎప్పుడైనా మార్చవచ్చు.</string>

    <string name="privacy_gate_groq_title">Groq-కి వాయిస్ రికార్డింగ్ పంపాలా?</string>
    <string name="privacy_gate_groq_body">క్లౌడ్ ట్రాన్స్‌క్రిప్షన్ కోసం మీ వాయిస్ రికార్డింగ్ ఎన్‌క్రిప్ట్ చేయబడి Groq, Inc.-కి (Mountain View, USA) పంపబడి అక్కడ టెక్స్ట్‌గా మార్చబడుతుంది. ఆడియో ఫైల్ ప్రాసెసింగ్ తర్వాత తొలగించబడుతుంది మరియు శిక్షణ కోసం ఉపయోగించబడదు.\n\nప్రత్యామ్నాయం: పరికరంలో స్థానిక ట్రాన్స్‌క్రిప్షన్ ఉపయోగించండి (ఆఫ్‌లైన్, డేటా బదిలీ లేదు), Settings → AI లో మార్చవచ్చు.</string>
    <string name="privacy_gate_groq_accept">అంగీకరించి పంపండి</string>
    <string name="privacy_gate_groq_local">బదులుగా స్థానికంగా లిప్యంతరీకరించండి</string>

    <string name="privacy_gate_gemini_title">Google Gemini-కి టెక్స్ట్ పంపాలా?</string>
    <string name="privacy_gate_gemini_body">AI ఫీచర్ల కోసం (డాష్‌బోర్డ్, సారాంశాలు, రెట్రోస్పెక్టివ్‌లు, టెక్స్ట్ మెరుగుదల) మీ ఎంట్రీల భాగాలు ఎన్‌క్రిప్ట్ చేయబడి Google Gemini-కి (Firebase AI, USA) పంపబడతాయి. చట్టపరమైన ఆధారం: EU-US Data Privacy Framework + Standard Contractual Clauses. అభ్యర్థనలు ప్రాసెసింగ్ తర్వాత తొలగించబడతాయి మరియు శిక్షణ కోసం ఉపయోగించబడవు.</string>
    <string name="privacy_gate_gemini_accept">అంగీకరించి పంపండి</string>
    <string name="privacy_gate_gemini_cancel">రద్దు</string>

    <string name="privacy_gate_tts_title">Microsoft-కి టెక్స్ట్ పంపాలా?</string>
    <string name="privacy_gate_tts_body">చదివి వినిపించడానికి టెక్స్ట్ ఎన్‌క్రిప్ట్ చేయబడి Microsoft Bing Speech-కి (USA) పంపబడి ఆడియోగా తిరిగి ఇవ్వబడుతుంది. చట్టపరమైన ఆధారం: EU-US Data Privacy Framework + Standard Contractual Clauses.\n\nప్రత్యామ్నాయం: Android యొక్క అంతర్నిర్మిత ఆఫ్‌లైన్ TTS ఉపయోగించండి.</string>
    <string name="privacy_gate_tts_accept">అంగీకరించి చదవండి</string>
    <string name="privacy_gate_tts_cancel">రద్దు</string>

    <string name="settings_privacy_header">గోప్యత</string>
    <string name="settings_analytics_title">అనామక గణాంకాలు</string>
    <string name="settings_analytics_subtitle">లోప విశ్లేషణ మరియు ఉత్పత్తి మెరుగుదల కోసం Firebase Analytics</string>

    <string name="settings_delete_account_title">ఖాతా మరియు డేటా తొలగించండి</string>
    <string name="settings_delete_account_subtitle">అన్ని స్థానిక డేటా, Google ఖాతా మరియు Drive బ్యాకప్‌ను శాశ్వతంగా తొలగిస్తుంది</string>
    <string name="settings_delete_account_confirm_title">ఖాతాను శాశ్వతంగా తొలగించాలా?</string>
    <string name="settings_delete_account_confirm_body">ఈ చర్య తిరిగి పొందలేనిది మరియు వీటిని తొలగిస్తుంది:\n\n• అన్ని స్థానిక డైరీ ఎంట్రీలు, ఫోటోలు మరియు వీడియోలు\n• మీ Firebase ఖాతా\n• యాప్ యొక్క Google Drive బ్యాకప్\n\nయాప్ కొత్త ఇన్‌స్టాలేషన్‌గా మళ్లీ ప్రారంభమవుతుంది.</string>
    <string name="settings_delete_account_cancel">రద్దు</string>
    <string name="settings_delete_account_confirm">అవును, అన్నీ తొలగించండి</string>

    <string name="settings_report_ai_title">AI ప్రతిస్పందనను నివేదించండి</string>
    <string name="settings_report_ai_subtitle">అనుచిత లేదా తప్పు AI అవుట్‌పుట్</string>
    <string name="settings_report_ai_confirm_title">Support-కి ఇమెయిల్ తెరవాలా?</string>
    <string name="settings_report_ai_confirm_body">మీ ఇమెయిల్ యాప్ dev.app.support@gmail.com-కి సిద్ధమైన సందేశంతో తెరవబడుతుంది. పంపే ముందు మీరు వివరణను జోడించవచ్చు. మేము పని దినాల్లో 24 గంటల లోపు ప్రతిస్పందిస్తాము.\n\nదయచేసి ఇక్కడ నివేదించండి: డాష్‌బోర్డ్, సారాంశాలు, రెట్రోస్పెక్టివ్‌లు లేదా టెక్స్ట్ మెరుగుదల నుండి అనుచిత, అనుచితమైన, తప్పు లేదా తప్పుదారి పట్టించే AI అవుట్‌పుట్‌లు.</string>
    <string name="settings_report_ai_confirm">నివేదిక సృష్టించండి</string>
    <string name="settings_report_ai_cancel">రద్దు</string>
    <string name="settings_report_ai_no_email">ఇమెయిల్ యాప్ కనుగొనబడలేదు. దయచేసి నివేదికను dev.app.support@gmail.com-కి పంపండి.</string>
    <string name="settings_report_ai_subject">Best Journal: అనుచిత AI ప్రతిస్పందన</string>
    <string name="settings_report_ai_body">నమస్కారం,\n\nనేను Best Journal లో ఒక అనుచిత లేదా తప్పు AI ప్రతిస్పందనను నివేదించాలనుకుంటున్నాను.\n\nసమస్య వివరణ:\n[దయచేసి పూరించండి]\n\nసందర్భం (ఏ ఫీచర్, ఏ ఇన్‌పుట్):\n[దయచేసి పూరించండి]\n\nధన్యవాదాలు.</string>

    <string name="settings_revoke_title">ఒప్పందం ఉపసంహరణ</string>
    <string name="settings_revoke_subtitle">Premium కొనుగోలు</string>
    <string name="settings_revoke_confirm_title">Support-కి ఇమెయిల్ తెరవాలా?</string>
    <string name="settings_revoke_confirm_body">మీ ఇమెయిల్ యాప్ dev.app.support@gmail.com-కి సిద్ధమైన సందేశంతో తెరవబడుతుంది. మేము పని దినాల్లో 24 గంటల లోపు ప్రతిస్పందిస్తాము.\n\nఉపసంహరణ హక్కుపై పూర్తి సమాచారం ఉపయోగ నిబంధనలలో (§ 16) ఉంది. సభ్యత్వాల కోసం అదనంగా Google Play → Subscriptions లో కూడా రద్దు చేయండి.</string>
    <string name="settings_revoke_cancel">రద్దు</string>
    <string name="settings_revoke_confirm">ఉపసంహరణ సృష్టించండి</string>
    <string name="settings_revoke_no_email">ఇమెయిల్ యాప్ కనుగొనబడలేదు. దయచేసి ఉపసంహరణను dev.app.support@gmail.com-కి పంపండి.</string>
    <string name="settings_revoke_email_subject">Best Journal Premium ఒప్పందం ఉపసంహరణ</string>
    <string name="settings_revoke_email_body">దీని ద్వారా Best Journal యొక్క Premium ఫీచర్లకు సంబంధించిన నా ఒప్పందాన్ని ఉపసంహరిస్తున్నానని తెలియజేస్తున్నాను.\n\nపంపినవారు (Google ఖాతా): %1$s\nఉపసంహరణ సమయం: %2$s\n\nఈ ఉపసంహరణ యాప్‌లోని § 356a BGB‌కు అనుగుణమైన రెండు-దశల ఉపసంహరణ బటన్ ద్వారా ప్రారంభించబడింది మరియు Gmail API ద్వారా స్వయంచాలకంగా పంపబడింది.</string>
    <string name="settings_revoke_confirm_subject">మీ స్వీకరణ నిర్ధారణ: Best Journal లో ఉపసంహరణ</string>
    <string name="settings_revoke_confirm_user_body">నమస్కారం,\n\n%1$s నాటి మీ ఉపసంహరణ మాకు అందింది. ఇది § 356a BGB ప్రకారం మీ స్వీకరణ నిర్ధారణ.\n\nమీ ఉపసంహరణను వీలైనంత త్వరగా ప్రాసెస్ చేస్తాము మరియు ఏవైనా ప్రశ్నలు ఉంటే dev.app.support@gmail.com ద్వారా మిమ్మల్ని సంప్రదిస్తాము.\n\nఇంకా బిల్లింగ్ జరగకుండా ఉండేందుకు, దయచేసి Google Play Store లోని "Subscriptions" వద్ద మీ సభ్యత్వాన్ని కూడా రద్దు చేయండి.\n\nధన్యవాదాలు మరియు శుభాకాంక్షలు\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">సైన్-ఇన్ చేసిన Google ఖాతా చిరునామా కనబడలేదు. దయచేసి Settings లో మీ Google ఖాతాతో సైన్ ఇన్ చేయండి లేదా ఉపసంహరణను dev.app.support@gmail.com కు చేతితో పంపండి.</string>
    <string name="settings_revoke_sending">ఉపసంహరణ పంపిస్తోంది…</string>
    <string name="settings_revoke_success_title">ఉపసంహరణ అందింది</string>
    <string name="settings_revoke_success_body">మీ ఉపసంహరణ dev.app.support@gmail.com కు పంపబడింది. స్వీకరణ నిర్ధారణ కూడా మీ ఇన్‌బాక్స్‌లో ఉంది.</string>
    <string name="settings_revoke_success_close">మూసివేయండి</string>
    <string name="settings_revoke_error_title">ఉపసంహరణను పంపలేకపోయాం</string>
    <string name="settings_revoke_error_body">స్వయంచాలక పంపింపు విఫలమైంది: %1$s\n\nప్రత్యామ్నాయంగా మీరు dev.app.support@gmail.com కు చేతితో ఈమెయిల్ పంపవచ్చు. అందుకు "ఈమెయిల్ యాప్ తెరవండి" ను తట్టండి.</string>
    <string name="settings_revoke_error_email_fallback">ఈమెయిల్ యాప్ తెరవండి</string>
"""

# ═══════════ MARATHI (mr) — तुम्ही, Devanagari, Arabic numerals ═══════════
TRANSLATIONS["mr"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">गोपनीयता आणि संमती</string>
    <string name="consent_intro">तुमची डायरी ही एक वैयक्तिक जागा आहे आणि आम्ही त्याचा आदर करतो. येथे तुम्ही पारदर्शकपणे पाहू शकता की Best Journal तुमचा डेटा कसा हाताळते.</string>
    <string name="consent_card1_title">स्थानिक संचय</string>
    <string name="consent_card1_body">तुमच्या नोंदी तुमच्या उपकरणावर राहतात.</string>
    <string name="consent_card2_title">AI वैशिष्ट्ये (USA)</string>
    <string name="consent_card2_body">पर्यायाने, मजकूर Google Gemini ला, आवाज रेकॉर्डिंग Groq ला आणि वाचनासाठी मजकूर Microsoft Edge ला USA मध्ये पाठवला जातो (EU-US Data Privacy Framework + Standard Contractual Clauses).</string>
    <string name="consent_card3_title">निनावी आकडेवारी</string>
    <string name="consent_card3_body">Firebase Analytics, पर्यायी, सेटिंग्जमध्ये कधीही बदलता येते.</string>
    <string name="consent_links_header">आमची कायदेशीर कागदपत्रे:</string>
    <string name="consent_accept_all">सहमत आणि सुरू करा</string>
    <string name="consent_disable_stats">आकडेवारी बंद करून सुरू ठेवा</string>
    <string name="consent_confirmation">\"सहमत आणि सुरू करा\" टॅप करून तुम्ही गोपनीयता धोरण, वापराच्या अटी आणि कायदेशीर सूचना वाचल्याची आणि वर्णन केलेल्या डेटा प्रक्रियेला सहमत असल्याची पुष्टी करता. तुमचा निर्णय सेटिंग्जमध्ये कधीही बदलू शकता.</string>

    <string name="privacy_gate_groq_title">आवाज रेकॉर्डिंग Groq ला पाठवायचे का?</string>
    <string name="privacy_gate_groq_body">क्लाउड ट्रान्सक्रिप्शनसाठी तुमचे आवाज रेकॉर्डिंग एन्क्रिप्ट करून Groq, Inc. (Mountain View, USA) ला पाठवले जाते आणि तिथे मजकुरात रूपांतरित होते. ऑडिओ फाइल प्रक्रियेनंतर हटवली जाते आणि प्रशिक्षणासाठी वापरली जात नाही.\n\nपर्याय: उपकरणावर स्थानिक ट्रान्सक्रिप्शन वापरा (ऑफलाइन, डेटा हस्तांतरण नाही), Settings → AI मध्ये बदलता येते.</string>
    <string name="privacy_gate_groq_accept">सहमत आणि पाठवा</string>
    <string name="privacy_gate_groq_local">त्याऐवजी स्थानिकरित्या ट्रान्सक्राइब करा</string>

    <string name="privacy_gate_gemini_title">मजकूर Google Gemini ला पाठवायचे का?</string>
    <string name="privacy_gate_gemini_body">AI वैशिष्ट्यांसाठी (डॅशबोर्ड, सारांश, रेट्रोस्पेक्टिव्ह, मजकूर सुधारणा) तुमच्या नोंदींचे भाग एन्क्रिप्ट करून Google Gemini (Firebase AI, USA) ला पाठवले जातात. कायदेशीर आधार: EU-US Data Privacy Framework + Standard Contractual Clauses. विनंत्या प्रक्रियेनंतर हटवल्या जातात आणि प्रशिक्षणासाठी वापरल्या जात नाहीत.</string>
    <string name="privacy_gate_gemini_accept">सहमत आणि पाठवा</string>
    <string name="privacy_gate_gemini_cancel">रद्द करा</string>

    <string name="privacy_gate_tts_title">मजकूर Microsoft ला पाठवायचे का?</string>
    <string name="privacy_gate_tts_body">वाचून दाखवण्यासाठी मजकूर एन्क्रिप्ट करून Microsoft Bing Speech (USA) ला पाठवला जातो आणि ऑडिओ म्हणून परत येतो. कायदेशीर आधार: EU-US Data Privacy Framework + Standard Contractual Clauses.\n\nपर्याय: Android चा बिल्ट-इन ऑफलाइन TTS वापरा.</string>
    <string name="privacy_gate_tts_accept">सहमत आणि वाचा</string>
    <string name="privacy_gate_tts_cancel">रद्द करा</string>

    <string name="settings_privacy_header">गोपनीयता</string>
    <string name="settings_analytics_title">निनावी आकडेवारी</string>
    <string name="settings_analytics_subtitle">त्रुटी विश्लेषण आणि उत्पादन सुधारणेसाठी Firebase Analytics</string>

    <string name="settings_delete_account_title">खाते आणि डेटा हटवा</string>
    <string name="settings_delete_account_subtitle">सर्व स्थानिक डेटा, Google खाते आणि Drive बॅकअप कायमस्वरूपी हटवते</string>
    <string name="settings_delete_account_confirm_title">खाते कायमस्वरूपी हटवायचे का?</string>
    <string name="settings_delete_account_confirm_body">ही क्रिया पूर्ववत करता येणार नाही आणि हे हटवते:\n\n• सर्व स्थानिक डायरी नोंदी, फोटो आणि व्हिडिओ\n• तुमचे Firebase खाते\n• अॅपचा Google Drive बॅकअप\n\nअॅप नवीन इन्स्टॉलेशनप्रमाणे पुन्हा सुरू होईल.</string>
    <string name="settings_delete_account_cancel">रद्द करा</string>
    <string name="settings_delete_account_confirm">होय, सर्व हटवा</string>

    <string name="settings_report_ai_title">AI उत्तराचा अहवाल द्या</string>
    <string name="settings_report_ai_subtitle">अयोग्य किंवा चुकीचे AI आउटपुट</string>
    <string name="settings_report_ai_confirm_title">सपोर्टला ईमेल उघडायचा का?</string>
    <string name="settings_report_ai_confirm_body">तुमचे ईमेल अॅप dev.app.support@gmail.com ला तयार संदेशासह उघडेल. पाठवण्यापूर्वी तुम्ही वर्णन जोडू शकता. आम्ही कार्य दिवशी 24 तासांच्या आत उत्तर देतो.\n\nकृपया येथे अहवाल द्या: डॅशबोर्ड, सारांश, रेट्रोस्पेक्टिव्ह किंवा मजकूर सुधारणातून अयोग्य, आक्षेपार्ह, चुकीचे किंवा दिशाभूल करणारे AI आउटपुट.</string>
    <string name="settings_report_ai_confirm">अहवाल तयार करा</string>
    <string name="settings_report_ai_cancel">रद्द करा</string>
    <string name="settings_report_ai_no_email">कोणतेही ईमेल अॅप सापडले नाही. कृपया अहवाल dev.app.support@gmail.com वर पाठवा.</string>
    <string name="settings_report_ai_subject">Best Journal: अयोग्य AI उत्तर</string>
    <string name="settings_report_ai_body">नमस्कार,\n\nमला Best Journal मधील एक अयोग्य किंवा चुकीचे AI उत्तर कळवायचे आहे.\n\nसमस्येचे वर्णन:\n[कृपया भरा]\n\nसंदर्भ (कोणते वैशिष्ट्य, कोणते इनपुट):\n[कृपया भरा]\n\nधन्यवाद.</string>

    <string name="settings_revoke_title">करार रद्दबातल</string>
    <string name="settings_revoke_subtitle">Premium खरेदी</string>
    <string name="settings_revoke_confirm_title">सपोर्टला ईमेल उघडायचा का?</string>
    <string name="settings_revoke_confirm_body">तुमचे ईमेल अॅप dev.app.support@gmail.com ला तयार संदेशासह उघडेल. आम्ही कार्य दिवशी 24 तासांच्या आत उत्तर देतो.\n\nरद्दबातल हक्काविषयी संपूर्ण माहिती वापराच्या अटींमध्ये (§ 16) आहे. सबस्क्रिप्शनसाठी Google Play → Subscriptions मधून देखील रद्द करा.</string>
    <string name="settings_revoke_cancel">रद्द करा</string>
    <string name="settings_revoke_confirm">रद्दबातल तयार करा</string>
    <string name="settings_revoke_no_email">कोणतेही ईमेल अॅप सापडले नाही. कृपया रद्दबातल dev.app.support@gmail.com वर पाठवा.</string>
    <string name="settings_revoke_email_subject">Best Journal Premium करार रद्दबातल</string>
    <string name="settings_revoke_email_body">याद्वारे मी Best Journal च्या Premium वैशिष्ट्यांबाबतच्या माझ्या करारातून माघार घेत असल्याची सूचना देत आहे.\n\nपाठवणारा (Google खाते): %1$s\nमाघारीची वेळ: %2$s\n\nही माघार अॅपमधील § 356a BGB-सुसंगत दोन-टप्प्यातील माघार बटणाद्वारे सुरू करण्यात आली आणि Gmail API मार्फत आपोआप पाठवण्यात आली.</string>
    <string name="settings_revoke_confirm_subject">तुमची प्राप्ती-पुष्टी: Best Journal मधील माघार</string>
    <string name="settings_revoke_confirm_user_body">नमस्कार,\n\n%1$s रोजीची तुमची माघार आम्हाला प्राप्त झाली आहे. ही § 356a BGB नुसार तुमची प्राप्ती-पुष्टी आहे.\n\nआम्ही तुमची माघार शक्य तितक्या लवकर प्रक्रिया करू आणि काही प्रश्न असल्यास dev.app.support@gmail.com वर तुमच्याशी संपर्क करू.\n\nपुढील बिलिंग थांबवण्यासाठी, कृपया Google Play Store मधील "Subscriptions" अंतर्गत तुमची सदस्यताही रद्द करा.\n\nधन्यवाद आणि शुभेच्छा\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">साइन-इन केलेला Google खात्याचा पत्ता सापडला नाही. कृपया Settings मध्ये तुमच्या Google खात्याने साइन इन करा किंवा माघार dev.app.support@gmail.com वर हाताने पाठवा.</string>
    <string name="settings_revoke_sending">माघार पाठवली जात आहे…</string>
    <string name="settings_revoke_success_title">माघार प्राप्त झाली</string>
    <string name="settings_revoke_success_body">तुमची माघार dev.app.support@gmail.com वर पाठवण्यात आली आहे. प्राप्ती-पुष्टीही तुमच्या इनबॉक्समध्ये आहे.</string>
    <string name="settings_revoke_success_close">बंद करा</string>
    <string name="settings_revoke_error_title">माघार पाठवता आली नाही</string>
    <string name="settings_revoke_error_body">स्वयंचलित पाठवणे अयशस्वी झाले: %1$s\n\nपर्यायाने तुम्ही dev.app.support@gmail.com वर हाताने ई-मेल पाठवू शकता. त्यासाठी "ई-मेल अॅप उघडा" वर टॅप करा.</string>
    <string name="settings_revoke_error_email_fallback">ई-मेल अॅप उघडा</string>
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
