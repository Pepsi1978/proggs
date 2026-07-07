#!/usr/bin/env python3
"""
Insert translated privacy/consent/revoke strings into values-{locale}/strings.xml
for batch 1 (fr, es, pt-rBR, pt-rPT, it). Each block is inserted before </resources>.
Keys match those newly added in values/strings.xml (51 keys).
"""
import os, sys, re

APP_DIR = os.path.expanduser("~/proggs/BestJournalAndroid/app/src/main/res")

TRANSLATIONS = {}

# ══════════════════════════════════════════════════════════════════
# FRENCH (fr) — Register: informal "tu", NBSP before : ; ! ?
# ══════════════════════════════════════════════════════════════════
TRANSLATIONS["fr"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">Confidentialité et consentement</string>
    <string name="consent_intro">Ton journal intime est un espace personnel, et nous le respectons. Tu vois ici en toute transparence comment Best Journal gère tes données.</string>
    <string name="consent_card1_title">Stockage local</string>
    <string name="consent_card1_body">Tes entrées restent sur ton appareil.</string>
    <string name="consent_card2_title">Fonctions IA (États-Unis)</string>
    <string name="consent_card2_body">Tes textes peuvent être envoyés à Google Gemini, tes enregistrements vocaux à Groq et les textes à lire à Microsoft Edge aux États-Unis (cadre EU-US Data Privacy Framework + clauses contractuelles types).</string>
    <string name="consent_card3_title">Statistiques anonymes</string>
    <string name="consent_card3_body">Firebase Analytics, en option, modifiable à tout moment dans les paramètres.</string>
    <string name="consent_links_header">Nos documents juridiques\u00A0:</string>
    <string name="consent_accept_all">Accepter et commencer</string>
    <string name="consent_disable_stats">Désactiver les statistiques et continuer</string>
    <string name="consent_confirmation">En appuyant sur «\u00A0Accepter et commencer\u00A0», tu confirmes avoir lu la politique de confidentialité, les conditions d\'utilisation et les mentions légales, et tu acceptes le traitement des données décrit. Tu peux modifier ta décision à tout moment dans les paramètres.</string>

    <string name="privacy_gate_groq_title">Envoyer l\'enregistrement vocal à Groq\u202F?</string>
    <string name="privacy_gate_groq_body">Pour la transcription cloud, ton enregistrement vocal est envoyé chiffré à Groq, Inc. (Mountain View, États-Unis) et y est converti en texte. Le fichier audio est supprimé après traitement et n\'est pas utilisé pour l\'entraînement.\n\nAlternative\u00A0: utilise la transcription locale sur l\'appareil (hors ligne, aucun transfert de données), réglable dans Paramètres → IA.</string>
    <string name="privacy_gate_groq_accept">Accepter et envoyer</string>
    <string name="privacy_gate_groq_local">Transcrire localement à la place</string>

    <string name="privacy_gate_gemini_title">Envoyer le texte à Google Gemini\u202F?</string>
    <string name="privacy_gate_gemini_body">Pour les fonctions IA (tableau de bord, résumés, rétrospectives, amélioration de texte), des extraits de tes entrées sont envoyés chiffrés à Google Gemini (Firebase AI, États-Unis). Base juridique\u00A0: cadre EU-US Data Privacy Framework + clauses contractuelles types. Les requêtes sont supprimées après traitement et ne sont pas utilisées pour l\'entraînement.</string>
    <string name="privacy_gate_gemini_accept">Accepter et envoyer</string>
    <string name="privacy_gate_gemini_cancel">Annuler</string>

    <string name="privacy_gate_tts_title">Envoyer le texte à Microsoft\u202F?</string>
    <string name="privacy_gate_tts_body">Pour la lecture à voix haute, le texte à lire est envoyé chiffré à Microsoft Bing Speech (États-Unis) et renvoyé sous forme audio. Base juridique\u00A0: cadre EU-US Data Privacy Framework + clauses contractuelles types.\n\nAlternative\u00A0: utilise la synthèse vocale hors ligne native d\'Android.</string>
    <string name="privacy_gate_tts_accept">Accepter et lire</string>
    <string name="privacy_gate_tts_cancel">Annuler</string>

    <string name="settings_privacy_header">Confidentialité</string>
    <string name="settings_analytics_title">Statistiques anonymes</string>
    <string name="settings_analytics_subtitle">Firebase Analytics pour l\'analyse des erreurs et l\'amélioration du produit</string>

    <string name="settings_delete_account_title">Supprimer le compte et les données</string>
    <string name="settings_delete_account_subtitle">Supprime définitivement toutes les données locales, ton compte Google et la sauvegarde Drive</string>
    <string name="settings_delete_account_confirm_title">Supprimer définitivement le compte\u202F?</string>
    <string name="settings_delete_account_confirm_body">Cette action est irréversible et supprime\u00A0:\n\n• Toutes les entrées, photos et vidéos locales\n• Ton compte Firebase\n• La sauvegarde Google Drive de l\'application\n\nL\'application redémarrera comme une nouvelle installation.</string>
    <string name="settings_delete_account_cancel">Annuler</string>
    <string name="settings_delete_account_confirm">Oui, tout supprimer</string>

    <string name="settings_report_ai_title">Signaler une réponse IA</string>
    <string name="settings_report_ai_subtitle">Sortie IA inappropriée ou erronée</string>
    <string name="settings_report_ai_confirm_title">Ouvrir un e-mail au support\u202F?</string>
    <string name="settings_report_ai_confirm_body">Ton application de messagerie s\'ouvre avec un message préparé pour dev.app.support@gmail.com. Tu peux compléter la description avant l\'envoi. Nous répondons sous 24 heures les jours ouvrés.\n\nMerci de signaler ici\u00A0: les sorties IA inappropriées, offensantes, fausses ou trompeuses provenant du tableau de bord, des résumés, des rétrospectives ou de l\'amélioration de texte.</string>
    <string name="settings_report_ai_confirm">Créer le signalement</string>
    <string name="settings_report_ai_cancel">Annuler</string>
    <string name="settings_report_ai_no_email">Aucune application de messagerie trouvée. Merci d\'envoyer le signalement à dev.app.support@gmail.com.</string>
    <string name="settings_report_ai_subject">Best Journal\u00A0: réponse IA inappropriée</string>
    <string name="settings_report_ai_body">Bonjour,\n\nje souhaite signaler une réponse IA inappropriée ou erronée dans Best Journal.\n\nDescription du problème\u00A0:\n[À compléter]\n\nContexte (quelle fonction, quelle saisie)\u00A0:\n[À compléter]\n\nMerci.</string>

    <string name="settings_revoke_title">Rétractation</string>
    <string name="settings_revoke_subtitle">Achat Premium</string>
    <string name="settings_revoke_confirm_title">Ouvrir un e-mail au support\u202F?</string>
    <string name="settings_revoke_confirm_body">Ton application de messagerie s\'ouvre avec un message préparé pour dev.app.support@gmail.com. Nous répondons sous 24 heures les jours ouvrés.\n\nLes informations complètes sur le droit de rétractation (directive UE 2011/83) se trouvent dans les conditions d\'utilisation (§ 16). Pour les abonnements, résilie également via Google Play → Abonnements.</string>
    <string name="settings_revoke_cancel">Annuler</string>
    <string name="settings_revoke_confirm">Créer la rétractation</string>
    <string name="settings_revoke_no_email">Aucune application de messagerie trouvée. Merci d\'envoyer la rétractation à dev.app.support@gmail.com.</string>
    <string name="settings_revoke_email_subject">Rétractation contrat Premium Best Journal</string>
    <string name="settings_revoke_email_body">Par la présente, j’exerce mon droit de rétractation sur le contrat relatif aux fonctions Premium de Best Journal.\n\nExpéditeur (compte Google)\u00A0: %1$s\nMoment de la rétractation\u00A0: %2$s\n\nCette rétractation a été déclenchée en deux étapes via le bouton de rétractation de l’application conforme au § 356a BGB et envoyée automatiquement via l’API Gmail.</string>
    <string name="settings_revoke_confirm_subject">Ton accusé de réception : rétractation chez Best Journal</string>
    <string name="settings_revoke_confirm_user_body">Bonjour,\n\nnous avons reçu ta rétractation du %1$s. Ceci est ton accusé de réception au titre du § 356a BGB.\n\nNous traiterons ta rétractation dès que possible et te contacterons à l’adresse dev.app.support@gmail.com en cas de questions.\n\nPour éviter toute nouvelle facturation, pense aussi à résilier ton abonnement dans le Google Play Store sous "Abonnements".\n\nMerci et bien cordialement\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">Aucune adresse de compte Google connectée n’a été trouvée. Merci de te connecter avec ton compte Google dans les paramètres ou d’envoyer la rétractation manuellement à dev.app.support@gmail.com.</string>
    <string name="settings_revoke_sending">Envoi de la rétractation…</string>
    <string name="settings_revoke_success_title">Rétractation reçue</string>
    <string name="settings_revoke_success_body">Ta rétractation a été envoyée à dev.app.support@gmail.com. L’accusé de réception se trouve aussi dans ta boîte de réception.</string>
    <string name="settings_revoke_success_close">Fermer</string>
    <string name="settings_revoke_error_title">Impossible d’envoyer la rétractation</string>
    <string name="settings_revoke_error_body">L’envoi automatique a échoué : %1$s\n\nTu peux aussi envoyer un e-mail manuel à dev.app.support@gmail.com. Clique sur "Ouvrir l’application e-mail" pour cela.</string>
    <string name="settings_revoke_error_email_fallback">Ouvrir l’application e-mail</string>
"""

# ══════════════════════════════════════════════════════════════════
# SPANISH (es) — Neutral LATAM, informal "tú", NO voseo, NO usted
# ══════════════════════════════════════════════════════════════════
TRANSLATIONS["es"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">Privacidad y consentimiento</string>
    <string name="consent_intro">Tu diario es un espacio personal, y lo respetamos. Aquí ves con total transparencia cómo Best Journal maneja tus datos.</string>
    <string name="consent_card1_title">Almacenamiento local</string>
    <string name="consent_card1_body">Tus entradas permanecen en tu dispositivo.</string>
    <string name="consent_card2_title">Funciones de IA (EE. UU.)</string>
    <string name="consent_card2_body">De forma opcional se envían textos a Google Gemini, grabaciones de voz a Groq y textos de lectura en voz alta a Microsoft Edge en EE. UU. (marco EU-US Data Privacy Framework + cláusulas contractuales tipo).</string>
    <string name="consent_card3_title">Estadísticas anónimas</string>
    <string name="consent_card3_body">Firebase Analytics, opcional, puedes cambiarlo en cualquier momento desde la configuración.</string>
    <string name="consent_links_header">Nuestros textos legales:</string>
    <string name="consent_accept_all">Aceptar y comenzar</string>
    <string name="consent_disable_stats">Desactivar estadísticas y continuar</string>
    <string name="consent_confirmation">Al tocar «Aceptar y comenzar» confirmas haber leído la política de privacidad, los términos de uso y el aviso legal, y aceptas el tratamiento de datos descrito. Puedes cambiar tu decisión en cualquier momento en la configuración.</string>

    <string name="privacy_gate_groq_title">¿Enviar la grabación de voz a Groq?</string>
    <string name="privacy_gate_groq_body">Para la transcripción en la nube, tu grabación de voz se envía cifrada a Groq, Inc. (Mountain View, EE. UU.) y allí se convierte en texto. El archivo de audio se elimina tras el procesamiento y no se usa para entrenamiento.\n\nAlternativa: usa la transcripción local en el dispositivo (sin conexión, sin transferencia de datos), ajustable en Configuración → IA.</string>
    <string name="privacy_gate_groq_accept">Aceptar y enviar</string>
    <string name="privacy_gate_groq_local">Transcribir localmente</string>

    <string name="privacy_gate_gemini_title">¿Enviar el texto a Google Gemini?</string>
    <string name="privacy_gate_gemini_body">Para las funciones de IA (panel, resúmenes, retrospectivas, mejora de texto) se envían fragmentos de tus entradas cifrados a Google Gemini (Firebase AI, EE. UU.). Base jurídica: marco EU-US Data Privacy Framework + cláusulas contractuales tipo. Las solicitudes se eliminan tras el procesamiento y no se usan para entrenamiento.</string>
    <string name="privacy_gate_gemini_accept">Aceptar y enviar</string>
    <string name="privacy_gate_gemini_cancel">Cancelar</string>

    <string name="privacy_gate_tts_title">¿Enviar el texto a Microsoft?</string>
    <string name="privacy_gate_tts_body">Para la lectura en voz alta, el texto se envía cifrado a Microsoft Bing Speech (EE. UU.) y se devuelve como audio. Base jurídica: marco EU-US Data Privacy Framework + cláusulas contractuales tipo.\n\nAlternativa: usa la síntesis de voz sin conexión nativa de Android.</string>
    <string name="privacy_gate_tts_accept">Aceptar y leer</string>
    <string name="privacy_gate_tts_cancel">Cancelar</string>

    <string name="settings_privacy_header">Privacidad</string>
    <string name="settings_analytics_title">Estadísticas anónimas</string>
    <string name="settings_analytics_subtitle">Firebase Analytics para análisis de errores y mejora del producto</string>

    <string name="settings_delete_account_title">Eliminar cuenta y datos</string>
    <string name="settings_delete_account_subtitle">Elimina de forma irreversible todos los datos locales, tu cuenta de Google y la copia de seguridad en Drive</string>
    <string name="settings_delete_account_confirm_title">¿Eliminar la cuenta de forma definitiva?</string>
    <string name="settings_delete_account_confirm_body">Esta acción es irreversible y eliminará:\n\n• Todas las entradas, fotos y videos locales\n• Tu cuenta de Firebase\n• La copia de seguridad de la app en Google Drive\n\nLa aplicación se reiniciará como una instalación nueva.</string>
    <string name="settings_delete_account_cancel">Cancelar</string>
    <string name="settings_delete_account_confirm">Sí, eliminar todo</string>

    <string name="settings_report_ai_title">Reportar respuesta de IA</string>
    <string name="settings_report_ai_subtitle">Salida de IA inapropiada o incorrecta</string>
    <string name="settings_report_ai_confirm_title">¿Abrir un correo al soporte?</string>
    <string name="settings_report_ai_confirm_body">Se abre tu aplicación de correo con un mensaje preparado para dev.app.support@gmail.com. Puedes completar la descripción antes de enviarlo. Respondemos en 24 horas hábiles.\n\nInforma aquí: salidas de IA inapropiadas, ofensivas, falsas o engañosas del panel, los resúmenes, las retrospectivas o la mejora de texto.</string>
    <string name="settings_report_ai_confirm">Crear reporte</string>
    <string name="settings_report_ai_cancel">Cancelar</string>
    <string name="settings_report_ai_no_email">No se encontró una aplicación de correo. Envía el reporte a dev.app.support@gmail.com.</string>
    <string name="settings_report_ai_subject">Best Journal: respuesta de IA inapropiada</string>
    <string name="settings_report_ai_body">Hola,\n\nquiero reportar una respuesta de IA inapropiada o incorrecta en Best Journal.\n\nDescripción del problema:\n[Completar]\n\nContexto (qué función, qué entrada):\n[Completar]\n\nGracias.</string>

    <string name="settings_revoke_title">Desistimiento</string>
    <string name="settings_revoke_subtitle">Compra Premium</string>
    <string name="settings_revoke_confirm_title">¿Abrir un correo al soporte?</string>
    <string name="settings_revoke_confirm_body">Se abre tu aplicación de correo con un mensaje preparado para dev.app.support@gmail.com. Respondemos en 24 horas hábiles.\n\nEncontrarás la información completa sobre el derecho de desistimiento (Directiva UE 2011/83) en los términos de uso (§ 16). Para las suscripciones, cancela también en Google Play → Suscripciones.</string>
    <string name="settings_revoke_cancel">Cancelar</string>
    <string name="settings_revoke_confirm">Crear desistimiento</string>
    <string name="settings_revoke_no_email">No se encontró una aplicación de correo. Envía el desistimiento a dev.app.support@gmail.com.</string>
    <string name="settings_revoke_email_subject">Desistimiento contrato Premium Best Journal</string>
    <string name="settings_revoke_email_body">Por la presente notifico mi desistimiento del contrato relativo a las funciones Premium de Best Journal.\n\nRemitente (cuenta de Google): %1$s\nMomento del desistimiento: %2$s\n\nEste desistimiento se activó en dos pasos mediante el botón de desistimiento de la aplicación conforme al § 356a BGB y se envió automáticamente a través de la API de Gmail.</string>
    <string name="settings_revoke_confirm_subject">Tu acuse de recibo: desistimiento en Best Journal</string>
    <string name="settings_revoke_confirm_user_body">Hola,\n\nhemos recibido tu desistimiento del %1$s. Este es tu acuse de recibo conforme al § 356a BGB.\n\nProcesaremos tu desistimiento lo antes posible y te escribiremos a dev.app.support@gmail.com si hay preguntas.\n\nPara evitar nuevos cobros, cancela también tu suscripción en Google Play Store, en "Suscripciones".\n\nGracias y saludos\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">No se ha encontrado ninguna dirección de cuenta de Google conectada. Inicia sesión con tu cuenta de Google en Ajustes o envía el desistimiento manualmente a dev.app.support@gmail.com.</string>
    <string name="settings_revoke_sending">Enviando desistimiento…</string>
    <string name="settings_revoke_success_title">Desistimiento recibido</string>
    <string name="settings_revoke_success_body">Tu desistimiento se ha enviado a dev.app.support@gmail.com. El acuse de recibo también está en tu bandeja de entrada.</string>
    <string name="settings_revoke_success_close">Cerrar</string>
    <string name="settings_revoke_error_title">No se pudo enviar el desistimiento</string>
    <string name="settings_revoke_error_body">El envío automático ha fallado: %1$s\n\nTambién puedes enviar manualmente un correo a dev.app.support@gmail.com. Pulsa "Abrir la app de correo" para hacerlo.</string>
    <string name="settings_revoke_error_email_fallback">Abrir la app de correo</string>
"""

# ══════════════════════════════════════════════════════════════════
# PORTUGUESE BRAZIL (pt-rBR) — você, usuário, aplicativo, salvar, configurações
# ══════════════════════════════════════════════════════════════════
TRANSLATIONS["pt-rBR"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">Privacidade e consentimento</string>
    <string name="consent_intro">Seu diário é um espaço pessoal, e nós respeitamos isso. Aqui você vê de forma transparente como o Best Journal cuida dos seus dados.</string>
    <string name="consent_card1_title">Armazenamento local</string>
    <string name="consent_card1_body">Suas entradas ficam no seu dispositivo.</string>
    <string name="consent_card2_title">Recursos de IA (EUA)</string>
    <string name="consent_card2_body">Opcionalmente, textos são enviados ao Google Gemini, gravações de voz ao Groq e textos de leitura em voz alta à Microsoft Edge nos EUA (estrutura EU-US Data Privacy Framework + cláusulas contratuais padrão).</string>
    <string name="consent_card3_title">Estatísticas anônimas</string>
    <string name="consent_card3_body">Firebase Analytics, opcional, você pode alterar a qualquer momento nas configurações.</string>
    <string name="consent_links_header">Nossos textos legais:</string>
    <string name="consent_accept_all">Concordar e começar</string>
    <string name="consent_disable_stats">Desativar estatísticas e continuar</string>
    <string name="consent_confirmation">Ao tocar em «Concordar e começar» você confirma ter lido a política de privacidade, os termos de uso e o aviso legal, e concorda com o tratamento de dados descrito. Você pode alterar sua decisão a qualquer momento nas configurações.</string>

    <string name="privacy_gate_groq_title">Enviar a gravação de voz ao Groq?</string>
    <string name="privacy_gate_groq_body">Para a transcrição na nuvem, sua gravação de voz é enviada criptografada ao Groq, Inc. (Mountain View, EUA) e convertida em texto lá. O arquivo de áudio é excluído após o processamento e não é usado para treinamento.\n\nAlternativa: use a transcrição local no dispositivo (offline, sem transferência de dados), ajustável em Configurações → IA.</string>
    <string name="privacy_gate_groq_accept">Concordar e enviar</string>
    <string name="privacy_gate_groq_local">Transcrever localmente</string>

    <string name="privacy_gate_gemini_title">Enviar o texto ao Google Gemini?</string>
    <string name="privacy_gate_gemini_body">Para recursos de IA (dashboard, resumos, retrospectivas, melhoria de texto), trechos das suas entradas são enviados criptografados ao Google Gemini (Firebase AI, EUA). Base jurídica: estrutura EU-US Data Privacy Framework + cláusulas contratuais padrão. As solicitações são excluídas após o processamento e não são usadas para treinamento.</string>
    <string name="privacy_gate_gemini_accept">Concordar e enviar</string>
    <string name="privacy_gate_gemini_cancel">Cancelar</string>

    <string name="privacy_gate_tts_title">Enviar o texto à Microsoft?</string>
    <string name="privacy_gate_tts_body">Para a leitura em voz alta, o texto é enviado criptografado à Microsoft Bing Speech (EUA) e retornado como áudio. Base jurídica: estrutura EU-US Data Privacy Framework + cláusulas contratuais padrão.\n\nAlternativa: use a síntese de voz offline nativa do Android.</string>
    <string name="privacy_gate_tts_accept">Concordar e ler</string>
    <string name="privacy_gate_tts_cancel">Cancelar</string>

    <string name="settings_privacy_header">Privacidade</string>
    <string name="settings_analytics_title">Estatísticas anônimas</string>
    <string name="settings_analytics_subtitle">Firebase Analytics para análise de erros e melhoria do produto</string>

    <string name="settings_delete_account_title">Excluir conta e dados</string>
    <string name="settings_delete_account_subtitle">Remove de forma irreversível todos os dados locais, sua conta do Google e o backup do Drive</string>
    <string name="settings_delete_account_confirm_title">Excluir a conta de forma definitiva?</string>
    <string name="settings_delete_account_confirm_body">Esta ação é irreversível e exclui:\n\n• Todas as entradas, fotos e vídeos locais\n• Sua conta do Firebase\n• O backup do aplicativo no Google Drive\n\nO aplicativo reiniciará como uma nova instalação.</string>
    <string name="settings_delete_account_cancel">Cancelar</string>
    <string name="settings_delete_account_confirm">Sim, excluir tudo</string>

    <string name="settings_report_ai_title">Reportar resposta da IA</string>
    <string name="settings_report_ai_subtitle">Saída de IA inadequada ou incorreta</string>
    <string name="settings_report_ai_confirm_title">Abrir e-mail para o suporte?</string>
    <string name="settings_report_ai_confirm_body">Seu aplicativo de e-mail abre com uma mensagem preparada para dev.app.support@gmail.com. Você pode completar a descrição antes de enviar. Respondemos em 24 horas em dias úteis.\n\nReporte aqui: saídas de IA inadequadas, ofensivas, falsas ou enganosas do dashboard, dos resumos, das retrospectivas ou da melhoria de texto.</string>
    <string name="settings_report_ai_confirm">Criar reporte</string>
    <string name="settings_report_ai_cancel">Cancelar</string>
    <string name="settings_report_ai_no_email">Nenhum aplicativo de e-mail encontrado. Envie o reporte para dev.app.support@gmail.com.</string>
    <string name="settings_report_ai_subject">Best Journal: resposta de IA inadequada</string>
    <string name="settings_report_ai_body">Olá,\n\nquero reportar uma resposta de IA inadequada ou incorreta no Best Journal.\n\nDescrição do problema:\n[Preencher]\n\nContexto (qual recurso, qual entrada):\n[Preencher]\n\nObrigado.</string>

    <string name="settings_revoke_title">Direito de arrependimento</string>
    <string name="settings_revoke_subtitle">Compra Premium</string>
    <string name="settings_revoke_confirm_title">Abrir e-mail para o suporte?</string>
    <string name="settings_revoke_confirm_body">Seu aplicativo de e-mail abre com uma mensagem preparada para dev.app.support@gmail.com. Respondemos em 24 horas em dias úteis.\n\nVocê encontra as informações completas sobre o direito de arrependimento (CDC art. 49 para o Brasil) nos termos de uso (§ 16). Para assinaturas, cancele também em Google Play → Assinaturas.</string>
    <string name="settings_revoke_cancel">Cancelar</string>
    <string name="settings_revoke_confirm">Criar solicitação</string>
    <string name="settings_revoke_no_email">Nenhum aplicativo de e-mail encontrado. Envie a solicitação para dev.app.support@gmail.com.</string>
    <string name="settings_revoke_email_subject">Arrependimento contrato Premium Best Journal</string>
    <string name="settings_revoke_email_body">Por meio desta notifico minha desistência do contrato relativo aos recursos Premium do Best Journal.\n\nRemetente (conta Google): %1$s\nMomento da desistência: %2$s\n\nEssa desistência foi acionada em duas etapas pelo botão de desistência no aplicativo, em conformidade com o § 356a BGB, e enviada automaticamente pela API do Gmail.</string>
    <string name="settings_revoke_confirm_subject">Seu comprovante de recebimento: desistência na Best Journal</string>
    <string name="settings_revoke_confirm_user_body">Olá,\n\nrecebemos sua desistência de %1$s. Este é o seu comprovante de recebimento nos termos do § 356a BGB.\n\nProcessaremos sua desistência o mais rápido possível e entraremos em contato pelo dev.app.support@gmail.com se houver dúvidas.\n\nPara evitar novas cobranças, cancele também sua assinatura na Google Play Store em "Assinaturas".\n\nObrigado e cordialmente\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">Nenhum endereço de conta Google conectado foi encontrado. Entre com sua conta Google em Configurações ou envie a desistência manualmente para dev.app.support@gmail.com.</string>
    <string name="settings_revoke_sending">Enviando desistência…</string>
    <string name="settings_revoke_success_title">Desistência recebida</string>
    <string name="settings_revoke_success_body">Sua desistência foi enviada para dev.app.support@gmail.com. O comprovante de recebimento também está na sua caixa de entrada.</string>
    <string name="settings_revoke_success_close">Fechar</string>
    <string name="settings_revoke_error_title">Não foi possível enviar a desistência</string>
    <string name="settings_revoke_error_body">O envio automático falhou: %1$s\n\nComo alternativa, você pode enviar um e-mail manual para dev.app.support@gmail.com. Toque em "Abrir app de e-mail" para isso.</string>
    <string name="settings_revoke_error_email_fallback">Abrir app de e-mail</string>
"""

# ══════════════════════════════════════════════════════════════════
# PORTUGUESE PORTUGAL (pt-rPT) — tu, utilizador, aplicação, guardar, definições
# AO 1990 reform, "a + infinitivo" progressive
# ══════════════════════════════════════════════════════════════════
TRANSLATIONS["pt-rPT"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">Privacidade e consentimento</string>
    <string name="consent_intro">O teu diário é um espaço pessoal e respeitamos isso. Aqui vês de forma transparente como o Best Journal trata os teus dados.</string>
    <string name="consent_card1_title">Armazenamento local</string>
    <string name="consent_card1_body">As tuas entradas ficam no teu dispositivo.</string>
    <string name="consent_card2_title">Funções de IA (EUA)</string>
    <string name="consent_card2_body">Opcionalmente, textos são enviados para o Google Gemini, gravações de voz para o Groq e textos de leitura em voz alta para a Microsoft Edge nos EUA (EU-US Data Privacy Framework + cláusulas contratuais-tipo).</string>
    <string name="consent_card3_title">Estatísticas anónimas</string>
    <string name="consent_card3_body">Firebase Analytics, opcional, podes alterar a qualquer momento nas definições.</string>
    <string name="consent_links_header">Os nossos documentos legais:</string>
    <string name="consent_accept_all">Aceitar e começar</string>
    <string name="consent_disable_stats">Desativar estatísticas e continuar</string>
    <string name="consent_confirmation">Ao tocar em «Aceitar e começar» confirmas ter lido a política de privacidade, os termos de utilização e a informação legal, e aceitas o tratamento de dados descrito. Podes alterar a tua decisão a qualquer momento nas definições.</string>

    <string name="privacy_gate_groq_title">Enviar a gravação de voz ao Groq?</string>
    <string name="privacy_gate_groq_body">Para a transcrição na cloud, a tua gravação de voz é enviada cifrada ao Groq, Inc. (Mountain View, EUA) e aí convertida em texto. O ficheiro de áudio é eliminado após o processamento e não é utilizado para treino.\n\nAlternativa: usa a transcrição local no dispositivo (offline, sem transferência de dados), ajustável em Definições → IA.</string>
    <string name="privacy_gate_groq_accept">Aceitar e enviar</string>
    <string name="privacy_gate_groq_local">Transcrever localmente</string>

    <string name="privacy_gate_gemini_title">Enviar o texto ao Google Gemini?</string>
    <string name="privacy_gate_gemini_body">Para as funções de IA (painel, resumos, retrospetivas, melhoria de texto), excertos das tuas entradas são enviados cifrados ao Google Gemini (Firebase AI, EUA). Base jurídica: EU-US Data Privacy Framework + cláusulas contratuais-tipo. Os pedidos são eliminados após o processamento e não são utilizados para treino.</string>
    <string name="privacy_gate_gemini_accept">Aceitar e enviar</string>
    <string name="privacy_gate_gemini_cancel">Cancelar</string>

    <string name="privacy_gate_tts_title">Enviar o texto à Microsoft?</string>
    <string name="privacy_gate_tts_body">Para a leitura em voz alta, o texto é enviado cifrado à Microsoft Bing Speech (EUA) e devolvido como áudio. Base jurídica: EU-US Data Privacy Framework + cláusulas contratuais-tipo.\n\nAlternativa: usa a síntese de voz offline nativa do Android.</string>
    <string name="privacy_gate_tts_accept">Aceitar e ler</string>
    <string name="privacy_gate_tts_cancel">Cancelar</string>

    <string name="settings_privacy_header">Privacidade</string>
    <string name="settings_analytics_title">Estatísticas anónimas</string>
    <string name="settings_analytics_subtitle">Firebase Analytics para análise de erros e melhoria do produto</string>

    <string name="settings_delete_account_title">Eliminar conta e dados</string>
    <string name="settings_delete_account_subtitle">Remove de forma irreversível todos os dados locais, a tua conta Google e a cópia de segurança do Drive</string>
    <string name="settings_delete_account_confirm_title">Eliminar a conta definitivamente?</string>
    <string name="settings_delete_account_confirm_body">Esta ação é irreversível e elimina:\n\n• Todas as entradas, fotografias e vídeos locais\n• A tua conta Firebase\n• A cópia de segurança da aplicação no Google Drive\n\nA aplicação reinicia como uma nova instalação.</string>
    <string name="settings_delete_account_cancel">Cancelar</string>
    <string name="settings_delete_account_confirm">Sim, eliminar tudo</string>

    <string name="settings_report_ai_title">Reportar resposta da IA</string>
    <string name="settings_report_ai_subtitle">Resultado da IA inadequado ou incorreto</string>
    <string name="settings_report_ai_confirm_title">Abrir e-mail para o suporte?</string>
    <string name="settings_report_ai_confirm_body">A tua aplicação de e-mail abre com uma mensagem preparada para dev.app.support@gmail.com. Podes completar a descrição antes de enviar. Respondemos em 24 horas em dias úteis.\n\nReporta aqui: resultados da IA inadequados, ofensivos, falsos ou enganosos do painel, dos resumos, das retrospetivas ou da melhoria de texto.</string>
    <string name="settings_report_ai_confirm">Criar reporte</string>
    <string name="settings_report_ai_cancel">Cancelar</string>
    <string name="settings_report_ai_no_email">Não foi encontrada uma aplicação de e-mail. Envia o reporte para dev.app.support@gmail.com.</string>
    <string name="settings_report_ai_subject">Best Journal: resposta de IA inadequada</string>
    <string name="settings_report_ai_body">Olá,\n\ngostaria de reportar uma resposta de IA inadequada ou incorreta no Best Journal.\n\nDescrição do problema:\n[Preencher]\n\nContexto (que função, que entrada):\n[Preencher]\n\nObrigado.</string>

    <string name="settings_revoke_title">Direito de rescisão</string>
    <string name="settings_revoke_subtitle">Compra Premium</string>
    <string name="settings_revoke_confirm_title">Abrir e-mail para o suporte?</string>
    <string name="settings_revoke_confirm_body">A tua aplicação de e-mail abre com uma mensagem preparada para dev.app.support@gmail.com. Respondemos em 24 horas em dias úteis.\n\nEncontras a informação completa sobre o direito de rescisão (Diretiva UE 2011/83) nos termos de utilização (§ 16). Para subscrições, cancela também em Google Play → Subscrições.</string>
    <string name="settings_revoke_cancel">Cancelar</string>
    <string name="settings_revoke_confirm">Criar pedido</string>
    <string name="settings_revoke_no_email">Não foi encontrada uma aplicação de e-mail. Envia o pedido para dev.app.support@gmail.com.</string>
    <string name="settings_revoke_email_subject">Rescisão contrato Premium Best Journal</string>
    <string name="settings_revoke_email_body">Pela presente notifico a minha livre resolução do contrato relativo às funcionalidades Premium do Best Journal.\n\nRemetente (conta Google): %1$s\nMomento da resolução: %2$s\n\nEsta resolução foi acionada em duas etapas através do botão de resolução da aplicação, em conformidade com o § 356a BGB, e enviada automaticamente através da API do Gmail.</string>
    <string name="settings_revoke_confirm_subject">O teu comprovativo de receção: resolução na Best Journal</string>
    <string name="settings_revoke_confirm_user_body">Olá,\n\nrecebemos a tua resolução de %1$s. Este é o teu comprovativo de receção ao abrigo do § 356a BGB.\n\nProcessaremos a tua resolução o mais rapidamente possível e contactaremos-te através de dev.app.support@gmail.com se houver dúvidas.\n\nPara evitar novas cobranças, cancela também a tua subscrição na Google Play Store em "Subscrições".\n\nObrigado e cumprimentos\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">Não foi encontrado nenhum endereço de conta Google com sessão iniciada. Inicia sessão com a tua conta Google nas Definições ou envia a resolução manualmente para dev.app.support@gmail.com.</string>
    <string name="settings_revoke_sending">A enviar resolução…</string>
    <string name="settings_revoke_success_title">Resolução recebida</string>
    <string name="settings_revoke_success_body">A tua resolução foi enviada para dev.app.support@gmail.com. O comprovativo de receção também está na tua caixa de entrada.</string>
    <string name="settings_revoke_success_close">Fechar</string>
    <string name="settings_revoke_error_title">Não foi possível enviar a resolução</string>
    <string name="settings_revoke_error_body">O envio automático falhou: %1$s\n\nEm alternativa, podes enviar um e-mail manual para dev.app.support@gmail.com. Toca em "Abrir app de e-mail" para isso.</string>
    <string name="settings_revoke_error_email_fallback">Abrir app de e-mail</string>
"""

# ══════════════════════════════════════════════════════════════════
# ITALIAN (it) — Informal "tu", NOT "Lei"
# ══════════════════════════════════════════════════════════════════
TRANSLATIONS["it"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">Privacy e consenso</string>
    <string name="consent_intro">Il tuo diario è uno spazio personale e lo rispettiamo. Qui vedi in modo trasparente come Best Journal gestisce i tuoi dati.</string>
    <string name="consent_card1_title">Archiviazione locale</string>
    <string name="consent_card1_body">Le tue voci restano sul tuo dispositivo.</string>
    <string name="consent_card2_title">Funzioni IA (USA)</string>
    <string name="consent_card2_body">Facoltativamente, i testi vengono inviati a Google Gemini, le registrazioni vocali a Groq e i testi da leggere a Microsoft Edge negli USA (EU-US Data Privacy Framework + clausole contrattuali standard).</string>
    <string name="consent_card3_title">Statistiche anonime</string>
    <string name="consent_card3_body">Firebase Analytics, opzionale, puoi modificarlo in qualsiasi momento dalle impostazioni.</string>
    <string name="consent_links_header">I nostri testi legali:</string>
    <string name="consent_accept_all">Accetta e inizia</string>
    <string name="consent_disable_stats">Disattiva statistiche e continua</string>
    <string name="consent_confirmation">Toccando «Accetta e inizia» confermi di aver letto l\'informativa sulla privacy, le condizioni d\'uso e le note legali, e accetti il trattamento dei dati descritto. Puoi modificare la tua decisione in qualsiasi momento dalle impostazioni.</string>

    <string name="privacy_gate_groq_title">Inviare la registrazione vocale a Groq?</string>
    <string name="privacy_gate_groq_body">Per la trascrizione in cloud, la tua registrazione vocale viene inviata crittografata a Groq, Inc. (Mountain View, USA) e lì convertita in testo. Il file audio viene eliminato dopo l\'elaborazione e non viene usato per l\'addestramento.\n\nAlternativa: usa la trascrizione locale sul dispositivo (offline, nessun trasferimento di dati), impostabile in Impostazioni → IA.</string>
    <string name="privacy_gate_groq_accept">Accetta e invia</string>
    <string name="privacy_gate_groq_local">Trascrivere localmente</string>

    <string name="privacy_gate_gemini_title">Inviare il testo a Google Gemini?</string>
    <string name="privacy_gate_gemini_body">Per le funzioni IA (dashboard, riassunti, retrospettive, miglioramento del testo), estratti delle tue voci vengono inviati crittografati a Google Gemini (Firebase AI, USA). Base giuridica: EU-US Data Privacy Framework + clausole contrattuali standard. Le richieste vengono eliminate dopo l\'elaborazione e non usate per l\'addestramento.</string>
    <string name="privacy_gate_gemini_accept">Accetta e invia</string>
    <string name="privacy_gate_gemini_cancel">Annulla</string>

    <string name="privacy_gate_tts_title">Inviare il testo a Microsoft?</string>
    <string name="privacy_gate_tts_body">Per la lettura ad alta voce, il testo viene inviato crittografato a Microsoft Bing Speech (USA) e restituito come audio. Base giuridica: EU-US Data Privacy Framework + clausole contrattuali standard.\n\nAlternativa: usa la sintesi vocale offline nativa di Android.</string>
    <string name="privacy_gate_tts_accept">Accetta e leggi</string>
    <string name="privacy_gate_tts_cancel">Annulla</string>

    <string name="settings_privacy_header">Privacy</string>
    <string name="settings_analytics_title">Statistiche anonime</string>
    <string name="settings_analytics_subtitle">Firebase Analytics per l\'analisi degli errori e il miglioramento del prodotto</string>

    <string name="settings_delete_account_title">Elimina account e dati</string>
    <string name="settings_delete_account_subtitle">Rimuove in modo irreversibile tutti i dati locali, il tuo account Google e il backup su Drive</string>
    <string name="settings_delete_account_confirm_title">Eliminare definitivamente l\'account?</string>
    <string name="settings_delete_account_confirm_body">Questa azione è irreversibile ed elimina:\n\n• Tutte le voci, foto e video locali\n• Il tuo account Firebase\n• Il backup dell\'app su Google Drive\n\nL\'app si riavvierà come nuova installazione.</string>
    <string name="settings_delete_account_cancel">Annulla</string>
    <string name="settings_delete_account_confirm">Sì, elimina tutto</string>

    <string name="settings_report_ai_title">Segnala risposta IA</string>
    <string name="settings_report_ai_subtitle">Output IA inappropriato o errato</string>
    <string name="settings_report_ai_confirm_title">Aprire un\'e-mail al supporto?</string>
    <string name="settings_report_ai_confirm_body">La tua app e-mail si apre con un messaggio preparato per dev.app.support@gmail.com. Puoi completare la descrizione prima di inviare. Rispondiamo entro 24 ore nei giorni lavorativi.\n\nSegnala qui: output IA inappropriati, offensivi, falsi o fuorvianti dalla dashboard, dai riassunti, dalle retrospettive o dal miglioramento del testo.</string>
    <string name="settings_report_ai_confirm">Crea segnalazione</string>
    <string name="settings_report_ai_cancel">Annulla</string>
    <string name="settings_report_ai_no_email">Nessuna app e-mail trovata. Invia la segnalazione a dev.app.support@gmail.com.</string>
    <string name="settings_report_ai_subject">Best Journal: risposta IA inappropriata</string>
    <string name="settings_report_ai_body">Salve,\n\nvorrei segnalare una risposta IA inappropriata o errata in Best Journal.\n\nDescrizione del problema:\n[Da compilare]\n\nContesto (quale funzione, quale input):\n[Da compilare]\n\nGrazie.</string>

    <string name="settings_revoke_title">Recesso</string>
    <string name="settings_revoke_subtitle">Acquisto Premium</string>
    <string name="settings_revoke_confirm_title">Aprire un\'e-mail al supporto?</string>
    <string name="settings_revoke_confirm_body">La tua app e-mail si apre con un messaggio preparato per dev.app.support@gmail.com. Rispondiamo entro 24 ore nei giorni lavorativi.\n\nTrovi le informazioni complete sul diritto di recesso (Direttiva UE 2011/83, Codice del Consumo) nelle condizioni d\'uso (§ 16). Per gli abbonamenti, disdici anche tramite Google Play → Abbonamenti.</string>
    <string name="settings_revoke_cancel">Annulla</string>
    <string name="settings_revoke_confirm">Crea recesso</string>
    <string name="settings_revoke_no_email">Nessuna app e-mail trovata. Invia il recesso a dev.app.support@gmail.com.</string>
    <string name="settings_revoke_email_subject">Recesso contratto Premium Best Journal</string>
    <string name="settings_revoke_email_body">Con la presente comunico il recesso dal contratto relativo alle funzioni Premium di Best Journal.\n\nMittente (account Google): %1$s\nMomento del recesso: %2$s\n\nQuesto recesso è stato avviato in due fasi tramite il pulsante di recesso dell’app conforme al § 356a BGB ed è stato inviato automaticamente tramite Gmail API.</string>
    <string name="settings_revoke_confirm_subject">La tua conferma di ricezione: recesso presso Best Journal</string>
    <string name="settings_revoke_confirm_user_body">Ciao,\n\nabbiamo ricevuto il tuo recesso del %1$s. Questa è la tua conferma di ricezione ai sensi del § 356a BGB.\n\nElaboreremo il tuo recesso il prima possibile e ti contatteremo all’indirizzo dev.app.support@gmail.com in caso di domande.\n\nPer evitare ulteriori addebiti, annulla anche il tuo abbonamento nel Google Play Store alla voce "Abbonamenti".\n\nGrazie e cordiali saluti\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">Non è stato trovato alcun indirizzo dell’account Google con accesso effettuato. Accedi con il tuo account Google nelle Impostazioni oppure invia manualmente il recesso a dev.app.support@gmail.com.</string>
    <string name="settings_revoke_sending">Invio del recesso…</string>
    <string name="settings_revoke_success_title">Recesso ricevuto</string>
    <string name="settings_revoke_success_body">Il tuo recesso è stato inviato a dev.app.support@gmail.com. La conferma di ricezione è anche nella tua casella di posta.</string>
    <string name="settings_revoke_success_close">Chiudi</string>
    <string name="settings_revoke_error_title">Impossibile inviare il recesso</string>
    <string name="settings_revoke_error_body">L’invio automatico non è riuscito: %1$s\n\nIn alternativa puoi inviare un’e-mail manuale a dev.app.support@gmail.com. Tocca "Apri app e-mail" per farlo.</string>
    <string name="settings_revoke_error_email_fallback">Apri app e-mail</string>
"""


def insert_before_closing_resources(path, block):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    if block.strip() in content:
        return f"SKIP (already present): {path}"
    marker = "</resources>"
    if marker not in content:
        return f"ERROR: no </resources> in {path}"
    new_content = content.replace(marker, block + "\n\n</resources>")
    # atomic write
    import tempfile
    d = os.path.dirname(os.path.abspath(path))
    with tempfile.NamedTemporaryFile("w", dir=d, suffix=".tmp", delete=False, encoding="utf-8", newline="\n") as tmp:
        tmp.write(new_content)
        tmp_path = tmp.name
    os.replace(tmp_path, path)
    return f"OK: {path}"


for locale, block in TRANSLATIONS.items():
    target = os.path.join(APP_DIR, f"values-{locale}", "strings.xml")
    if not os.path.exists(target):
        print(f"MISSING: {target}")
        continue
    print(insert_before_closing_resources(target, block))
