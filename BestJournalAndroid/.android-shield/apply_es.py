# -*- coding: utf-8 -*-
"""Apply Spanish (es-419) translations to values-es/strings.xml — finale Phase 3 tw-es.
FIN-048: target file touched ONLY here via open/read/write. FIN-040: pre-check + rfind for NEW key.
Register: tuteo (tu), no voseo, no usted. derecho de desistimiento = Widerruf. Placeholders exact.
"""
import os, re, json

F = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-es/strings.xml')
CKPT = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/worker-checkpoints/tw-es.jsonl')

def ckpt(step, **kw):
    rec = {"ts": "2026-06-10", "worker": "tw-es", "step": step}
    rec.update(kw)
    with open(CKPT, 'a', encoding='utf-8') as fh:
        fh.write(json.dumps(rec, ensure_ascii=False) + "\n")

# Spanish translations. \n kept as literal backslash-n (Android escape). Apostrophes escaped \'.
S = {}

S['ai_limits_dialog_body'] = (
    "CON SUSCRIPCIÓN PREMIUM\\n\\n"
    "Cada día tienes a tu disposición:\\n"
    "• 150 solicitudes por día y perfil — para los 4 predefinidos juntos 600/día, más 150/día por cada perfil propio que crees.\\n"
    "• Hasta 150 mejoras de texto\\n\\n"
    "Cómo funciona en detalle:\\n"
    "• Solicitudes 1 a 30: en la máxima calidad de IA\\n"
    "• Solicitudes 31 a 100: en calidad estándar rápida\\n"
    "• En la solicitud 101: pausa única de 30 min\\n"
    "• Solicitudes 101 a 150: calidad estándar rápida\\n"
    "• A partir de la solicitud 151: no hay más solicitudes hasta mañana\\n\\n"
    "Además, para todos los usuarios rige:\\n"
    "• Como máximo 50 solicitudes de IA por hora — después una pausa de 5 min\\n\\n"
    "¿Cuándo se reinician los cupos?\\n"
    "Cada día a las 0:00 (tu hora local) comienza un nuevo cupo completo.\\n\\n"
    "──────────\\n\\n"
    "SIN SUSCRIPCIÓN\\n\\n"
    "• 5 análisis de panel por semana y perfil\\n"
    "• 5 mejoras de texto por semana\\n"
    "• Siempre en calidad estándar rápida\\n"
    "• Se reinician cada lunes a las 0:00 (tu hora local)\\n\\n"
    "──────────\\n\\n"
    "La mayoría de los usuarios no alcanzarán estos límites en el día a día."
)

S['ai_prompt_custom_intro'] = (
    "Eres un analista de diario inteligente y atento Y UN ejecutor de tareas.\\n\\n"
    "FORMA DE TRABAJAR EN DOS PASOS:\\n\\n"
    "PASO 1 — CAPTAR EL CONTEXTO:\\n"
    "Lee primero TODAS las entradas de diario del usuario por completo. Comprende la situación, los patrones, los temas, las cosas mencionadas, los problemas, los deseos. Las entradas son tu CONTEXTO, tu punto de partida, tu base. Pero no te limitan.\\n\\n"
    "PASO 2 — EJECUTAR LA TAREA:\\n"
    "%1$s\\n\\n"
    "Lo de arriba es tu verdadero ENCARGO. Ejecuta este encargo sobre la base del contexto del paso 1. Si el encargo pide investigación, ideas, alternativas, sugerencias o información nueva, entonces entrégalas ACTIVAMENTE, aunque no aparezcan en las entradas. No des diagnósticos ni consejos vinculantes médicos, legales o financieros; en esos temas remite a personas especializadas. Las entradas informan tu resultado, no lo limitan.\\n\\n"
    "REGLA CENTRAL — EL PERFIL APORTA AL MENOS EL 50 POR CIENTO:\\n"
    "Todo el resultado (análisis general, medidas principales, categorías, consejos, avances) DEBE estar claramente vinculado en al menos un 50 por ciento con el encargo del usuario. Las conclusiones, puntos o temas que NO tengan relación de contenido con el encargo, déjalos fuera — no llenes el resultado con temas generales de la vida solo para que el JSON quede completo. Mejor menos puntos con relación clara que muchos genéricos.\\n\\n"
    "Importante sobre el lenguaje: Expresa la relación CON VARIEDAD. Usa sinónimos, términos afines, cercanías temáticas y paráfrasis. NO repitas mecánicamente las palabras exactas del encargo — eso resulta insistente. Ejemplo de encargo \\\"Metas\\\": Habla también de propósitos, deseos, ambiciones, lo que quieres lograr, tu rumbo, pasos de desarrollo. Ejemplo de encargo \\\"Deporte\\\": Habla también de movimiento, entrenamiento, actividad física, resistencia, estado físico, salud. La relación con el perfil debe ser PERCEPTIBLE, no literal.\\n\\n"
    "DECISIVO:\\n"
    "- Si el encargo dice \\\"analiza\\\", \\\"resume\\\", \\\"qué llama la atención\\\" — mantente cerca de las entradas.\\n"
    "- Si el encargo dice \\\"investiga\\\", \\\"encuentra alternativas\\\", \\\"sugiere\\\", \\\"recomienda\\\", \\\"complementa\\\", \\\"qué pasaría si\\\", \\\"ideas nuevas\\\" — ve ACTIVAMENTE más allá de las entradas y aporta contenidos propios y nuevos.\\n"
    "- El encargo tiene prioridad. Las entradas son el cimiento, no el muro."
)

S['ai_prompt_custom_rules'] = (
    "REGLA DE CONTEXTO — CADA ENTRADA SE LEE:\\n"
    "Recibes entradas numeradas. Lee CADA UNA por completo y tómalas como contexto. Cada detalle relevante fluye hacia tu resultado. Pero: el resultado puede y debe ir más allá de las entradas si el encargo lo exige.\\n\\n"
    "REGLA DEL ENCARGO — EL ENCARGO SE CUMPLE:\\n"
    "Ejecuta el encargo del usuario al pie de la letra. Si el encargo pide alternativas, entrega alternativas nuevas y reales, no solo cosas de las entradas. Si el encargo pide recomendaciones, investiga y recomienda activamente. Las entradas NO son la única fuente de tu respuesta.\\n\\n"
    "DISTINCIÓN EN EL RESULTADO:\\n"
    "Señala con claridad qué proviene de las entradas y qué agregas de nuevo. Así el usuario sabe cuáles son sus propios pensamientos y cuál es tu aporte.\\n\\n"
    "REGLA DE ASESORAMIENTO PROFESIONAL — SIN DIAGNÓSTICOS:\\n"
    "INDEPENDIENTEMENTE del encargo, NO das diagnósticos médicos, recomendaciones de terapia, ni asesoramiento legal o financiero. En esos temas remite de forma neutral a personas cualificadas.\\n\\n"
    "REGLAS DE LENGUAJE:\\n"
    "- Sencillo, claro. Sin tecnicismos.\\n"
    "- Empático y directo.\\n"
    "- Sin guiones largos (—). Usa comas o puntos.\\n\\n"
    "REGLA DE CANTIDAD:\\n"
    "Al menos 10 conclusiones en total — pero cada una debe encajar en su contenido con el encargo. Si no encuentras 10 conclusiones con relación real con el perfil, entrega mejor 7 sólidas en vez de 10 diluidas. En encargos de puro análisis, las conclusiones provienen de las entradas; en encargos creativos puedes aportar contenidos nuevos."
)

S['ai_prompt_entropy_intro'] = (
    "Eres un analista de orden y patrones empático, muy inteligente y sensible.\\n\\n"
    "TU TAREA:\\n"
    "Analiza las entradas de diario de un usuario. Encuentra fuentes recurrentes de estrés, desorden y carga. Crea con ello un panel de consejos estructurado en formato JSON.\\n\\n"
    "LO QUE BUSCAMOS — CARGA PERSONAL:\\n"
    "Todo lo que genera desorden, estrés, pérdida de energía, agobio, presión de tiempo, carga emocional o pérdida de control en la vida del usuario.\\n\\n"
    "NO das consejos médicos, terapéuticos ni diagnósticos. En temas de salud remite de forma neutral a personas especializadas."
)

S['ai_prompt_goals_rules'] = (
    "REGLAS DE LENGUAJE:\\n"
    "- %1$s\\n"
    "- Motivador y honesto, celebra el avance, no maquilles nada.\\n"
    "- Sin guiones largos (—). Usa comas o puntos.\\n"
    "- En metas de salud o corporales solo acompañas de forma motivadora; NO des recomendaciones de dieta, medicamentos ni terapia.\\n\\n"
    "REGLA DE CANTIDAD, CADA META CUENTA:\\n"
    "Reconoce TODAS las metas — también las pequeñas. NO las resumas."
)

S['ai_prompt_insight_attitude'] = (
    "TU ACTITUD:\\n"
    "Eres un espejo benévolo. Le muestras al usuario con honestidad lo que reconoces en sus entradas — pero siempre con el objetivo de que pueda crecer a partir de ello. Cada conclusión debe ayudarle a entenderse mejor a sí mismo. También los patrones difíciles los nombras con claridad, pero de forma constructiva y sin reproche. Foco: ¿Qué puede aprender el usuario sobre sí mismo a partir de sus propias palabras?\\n\\n"
    "NO haces diagnósticos psicológicos ni nombras trastornos o enfermedades. Solo describes patrones observables a partir de las propias palabras del usuario.\\n\\n"
    "LO QUE BUSCAS:\\n"
    "- Sentimientos recurrentes: ¿Qué emociones aparecen una y otra vez?\\n"
    "- Patrones de pensamiento: ¿Cómo piensa el usuario sobre sí mismo, los demás, el mundo?\\n"
    "- Patrones de evitación: ¿Qué evita el usuario? ¿Sobre qué nunca escribe?\\n"
    "- Fortalezas: ¿Qué hace bien el usuario, aunque él mismo no lo vea?\\n"
    "- Valores: ¿Qué le importa de verdad al usuario (se muestra por sus actos, no sus palabras)?\\n"
    "- Detonantes: ¿Qué provoca reacciones fuertes — tanto positivas como negativas?\\n"
    "- Contradicciones: ¿Dice el usuario una cosa, pero actúa de otra manera?\\n"
    "- Necesidades: ¿Qué necesita el usuario, que se trasluce entre líneas?\\n"
    "- Crecimiento: ¿Dónde ha cambiado la perspectiva del usuario?"
)

S['ai_prompt_no_dates_rule'] = (
    "APTO PARA LECTURA EN VOZ ALTA (OBLIGATORIO — PROHIBICIÓN DE FECHAS EN TEXTOS VISIBLES):\\n"
    "En TODOS los campos de texto visibles (beschreibung, erklaerung, zusammenfassung, gesamtanalyse) NO PUEDES usar fechas — ni \\\"3.4.\\\", \\\"23.04.\\\", \\\"28.4.\\\", \\\"24.04.\\\", \\\"30.4.\\\" ni \\\"entrada del 3 de abril\\\", \\\"el 23.04.\\\", \\\"del 28.4.\\\" ni referencias similares a fechas concretas de entradas. Estos textos se le leen en voz alta al usuario — las cadenas de fechas suenan poco naturales al leerse y rompen el flujo de las conclusiones.\\n\\n"
    "EN SU LUGAR, FORMULA LA CONCLUSIÓN PURA:\\n"
    "- INCORRECTO: \\\"Las entradas del 3.4., 28.4. y 23.4. demuestran que tu biorritmo sufre por los horarios de sueño tardíos.\\\"\\n"
    "- CORRECTO: \\\"Los horarios de sueño tardíos destruyen tu biorritmo de forma sistemática.\\\"\\n"
    "- INCORRECTO: \\\"La entrada del 30 muestra que tu bandeja de entrada se desborda desde el 28.4. y te roba tiempo.\\\"\\n"
    "- CORRECTO: \\\"Tu bandeja de entrada se desborda desde hace días y te roba tiempo.\\\"\\n"
    "- INCORRECTO: \\\"Notas más visión de conjunto en el día a día desde las acciones de orden del 24.04.\\\"\\n"
    "- CORRECTO: \\\"El movimiento te da notablemente más visión de conjunto en el día a día.\\\"\\n\\n"
    "La conclusión de fondo se mantiene — solo desaparecen las referencias concretas de fechas. Puedes seguir usando referencias temporales generales (\\\"recurrente\\\", \\\"últimamente\\\", \\\"a lo largo de varios días\\\"). Las fechas van SOLO en el campo \\\"herleitung\\\" (procedencia, se usa internamente, no se muestra)."
)

S['ai_prompt_rerank_system'] = (
    "Eres un re-ordenador de perfil. Recibes un array JSON de 5 medidas principales de un análisis de panel, el foco de perfil del usuario y las entradas de diario originales.\\n\\n"
    "Tu tarea en 3 pasos:\\n"
    "1) Ordena las 5 medidas de modo que las que tengan la relación más clara con el foco del perfil vayan primero.\\n"
    "2) Comprueba si hasta 2 de las 5 medidas NO tienen relación de contenido con el foco del perfil. Si es así, reemplázalas por medidas con mayor relación con el perfil, que derives de las entradas de diario. Mantén la misma estructura JSON de cada medida (titel, beschreibung, erklaerung).\\n"
    "3) Expresa la relación con el perfil CON VARIEDAD mediante sinónimos y cercanías temáticas, NO con repetición mecánica de las palabras del perfil. La relación debe ser perceptible, no literal.\\n\\n"
    "IMPORTANTE:\\n"
    "- Devuelve al final EXACTAMENTE 5 entradas.\\n"
    "- Cambios solo si falta una relación real con el perfil — si el encaje ya es bueno, solo ordena el array, no reemplaces nada.\\n"
    "- Conserva la estructura JSON de cada entrada (las mismas claves: titel, beschreibung, erklaerung).\\n"
    "- Idioma español, sin guiones largos, beschreibung de 13 a 21 palabras.\\n"
    "- NO des diagnósticos ni consejos vinculantes médicos, legales o financieros; en esos temas remite de forma neutral a personas especializadas.\\n\\n"
    "APTO PARA LECTURA EN VOZ ALTA (OBLIGATORIO — PROHIBICIÓN DE FECHAS):\\n"
    "En beschreibung y erklaerung NO uses fechas (\\\"3.4.\\\", \\\"23.04.\\\", \\\"entrada del 30.\\\"). Estos textos se leen en voz alta — las cadenas de fechas suenan poco naturales. Formula la conclusión pura sin fechas concretas de entradas. Ejemplos:\\n"
    "- INCORRECTO: \\\"Las entradas del 3.4. y 23.04. demuestran que tu biorritmo sufre.\\\"\\n"
    "- CORRECTO: \\\"Los horarios de sueño tardíos destruyen tu biorritmo de forma sistemática.\\\"\\n"
    "Las referencias temporales generales están permitidas (\\\"recurrente\\\", \\\"últimamente\\\", \\\"a lo largo de varios días\\\"), las fechas no.\\n\\n"
    "FORMATO DE SALIDA:\\n"
    "- SOLO un array JSON (corchetes en el nivel raíz).\\n"
    "- Sin comillas invertidas de Markdown, sin texto antes/después.\\n"
    "- Comienza directamente con [ y termina con ]."
)

S['churn_offer_feature_perspectives'] = "Los 4 perfiles de IA predefinidos + tantos propios como necesites"

S['consent_card3_title'] = "Estadísticas seudónimas"

S['consent_toggle_analytics_body'] = (
    "Firebase Analytics — nos ayuda a mejorar la app. Sin contenidos del diario, solo estadísticas de uso seudónimas (p. ej. número de clics). Transferencia de datos: Google EE. UU. (Data Privacy Framework)."
)

S['consent_toggle_analytics_title'] = "Análisis de uso seudónimo"

S['consent_toggle_do_not_sell_body'] = (
    "Este interruptor está pensado solo para residentes del estado de California (EE. UU.). La ley de protección de datos de California (CCPA/CPRA) otorga a los residentes un derecho de oposición. Al activarlo, se desactivan todas las funciones opcionales en la nube (IA, copia de seguridad, reconocimiento de voz, análisis) — solo quedan activas las funciones locales.\\n\\n"
    "Si vives fuera de California, puedes ignorar este interruptor: Best Journal no vende ni comercializa tus datos en ningún lugar del mundo."
)

S['onboarding_feature_secure'] = "Tus entradas permanecen en el dispositivo; las solicitudes de IA se transmiten cifradas"

S['onboarding_premium_feature_noads'] = "Escribe y reflexiona sin interrupciones"

S['paywall_feature_noads'] = "Escribe y reflexiona sin interrupciones"

S['paywall_feature_profiles'] = "Perfiles de IA propios sin límite de cantidad"

S['paywall_from_per_day'] = "Suscripción anual, %1$s al año"

S['paywall_headline_clarity_sub'] = "La IA hace visibles los patrones recurrentes en tus entradas"

S['paywall_monthly_note'] = "Después %1$s al mes, se renueva automáticamente\\nCancelable en cualquier momento"

S['privacy_gate_groq_body'] = (
    "Esta función envía tu grabación de voz cifrada a Groq (EE. UU.) y allí la convierte en texto — más rápido y preciso que el reconocimiento local.\\n\\n"
    "✓ La grabación se elimina de inmediato tras la conversión\\n"
    "✓ Sin entrenamiento de IA con tus grabaciones\\n"
    "✓ Base jurídica: cláusulas contractuales tipo (SCC de la UE)\\n\\n"
    "Alternativa: reconocimiento local sin conexión en los ajustes."
)

S['privacy_gate_tts_body'] = (
    "Con las voces Premium, la lectura en voz alta suena más natural. Tu texto se envía cifrado (HTTPS/TLS) a un servicio de voz de Microsoft en EE. UU. y se devuelve como audio.\\n\\n"
    "✓ Transferencia exclusivamente para la síntesis de voz\\n"
    "✓ Transferencia a un tercer país (EE. UU.) (Microsoft está certificada conforme al EU-US Data Privacy Framework)"
)

S['privacy_gate_tts_title'] = "Activar voces Premium"

S['profile_entropy_long'] = (
    "La IA encuentra lo que te agobia en el día a día y lo ordena. Recibes una visión general de tus mayores fuentes de carga y 5 pasos concretos para poner orden. Ideal si quieres recuperar la visión de conjunto."
)

S['retro_benefit_weekly'] = "Todos los resúmenes semanales en vez de solo los primeros 2 – dentro de tu cuota diaria de IA"

S['settings_analytics_title'] = "Análisis de uso seudónimo"

S['settings_delete_account_confirm_body'] = (
    "Este proceso es irreversible y elimina:\\n\\n"
    "• Todas las entradas de diario, fotos, vídeos y grabaciones de audio locales\\n"
    "• La copia de seguridad de la app en Google Drive (solo los datos de la app, no tu cuenta de Google)\\n"
    "• Tu sesión en la app\\n\\n"
    "Tu cuenta de Google se mantiene. La app se reinicia luego como una instalación nueva."
)

S['settings_delete_account_subtitle'] = (
    "Elimina de forma irreversible todos los datos locales y la copia de seguridad en Drive. Tu cuenta de Google se mantiene."
)

S['settings_feedback_dialog_desc'] = (
    "Tu mensaje para los desarrolladores — leemos cada mensaje y te respondemos lo antes posible."
)

S['settings_premium_feature_5_perspectives'] = "Los 4 perfiles de IA predefinidos + tantos propios como necesites"

S['settings_premium_feature_profiles'] = "Perfiles de IA propios sin límite de cantidad"

S['settings_report_ai_confirm_body'] = (
    "Se abre tu programa de correo con un mensaje preparado para dev.app.support@gmail.com. Puedes completar la descripción antes de enviarlo. Por lo general respondemos en un plazo de 24 horas en días laborables.\\n\\n"
    "Reporta con esto: salidas de IA inapropiadas, ofensivas, falsas o engañosas del panel, los resúmenes, los retrospectivos o la mejora de texto."
)

# § 356a BGB kept 1:1; "Widerruf bestätigen" button label in quotes -> escape inner quotes; derecho de desistimiento
S['settings_revoke_confirm_body'] = (
    "Con un clic en \\\"Confirmar desistimiento\\\" enviamos tu desistimiento directamente a dev.app.support@gmail.com. Recibes de inmediato una confirmación de recepción por correo electrónico.\\n\\n"
    "Encontrarás la información completa sobre el desistimiento en las Condiciones de uso (§ 16). Cancela además las suscripciones a través de Google Play → Suscripciones, para que no se realicen más cobros."
)

S['settings_revoke_confirm_title'] = "¿Desistir del contrato?"

S['settings_revoke_subtitle'] = "§ 356a BGB — Desistimiento directo desde la app"

PLURALS = {
    'datetime_months_relative': {'one': 'hace %1$d mes', 'other': 'hace %1$d meses'},
    'datetime_years_relative': {'one': 'hace %1$d año', 'other': 'hace %1$d años'},
}

# ---- DE source for "must not be identical" check ----
DE = json.load(open(os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs/de-reference.json'), encoding='utf-8'))

with open(F, 'r', encoding='utf-8') as fh:
    content = fh.read()

applied = []
skipped_already_present = {}
new_inserts = []

# Replace existing <string> keys (all except paywall_monthly_note which is NEW)
for key, val in S.items():
    # NEW key handling: pre-check
    cnt = content.count('name="%s"' % key)
    if key == 'paywall_monthly_note':
        if cnt > 0:
            skipped_already_present.setdefault('es', []).append(key)
            continue
        # rfind insert (NEW)
        new_line = '    <string name="%s">%s</string>' % (key, val)
        idx = content.rfind('</resources>')
        if idx == -1:
            raise RuntimeError('no </resources> in target')
        content = content[:idx] + new_line + '\n' + content[idx:]
        new_inserts.append(key)
        applied.append(key)
        continue
    # existing key: replace value, preserve attributes (e.g. formatted="false")
    pat = re.compile(r'(<string name="%s"[^>]*>).*?(</string>)' % re.escape(key), re.S)
    if not pat.search(content):
        raise RuntimeError('expected existing key not found for replace: %s' % key)
    content = pat.sub(lambda m: m.group(1) + val + m.group(2), content, count=1)
    applied.append(key)

# Replace plurals (DE-stale)
for pkey, items in PLURALS.items():
    block = '<plurals name="%s">\n' % pkey
    block += '        <item quantity="one">%s</item>\n' % items['one']
    block += '        <item quantity="other">%s</item>\n' % items['other']
    block += '    </plurals>'
    pat = re.compile(r'<plurals name="%s">.*?</plurals>' % re.escape(pkey), re.S)
    if not pat.search(content):
        raise RuntimeError('plural not found: %s' % pkey)
    content = pat.sub(lambda m: block, content, count=1)
    applied.append(pkey)

with open(F, 'w', encoding='utf-8', newline='\n') as fh:
    fh.write(content)

print('APPLIED', len(applied), 'NEW', new_inserts, 'SKIPPED', skipped_already_present)
ckpt('applied', applied_count=len(applied), new_inserts=new_inserts)
