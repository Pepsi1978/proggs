### pt-PT — Portugiesisch Europa

```
## Language-Specific Rules: European Portuguese (pt-PT)
- Register: Use "tu" (informal 2nd person) throughout. NOT "voce" (that is Brazilian
  neutral OR European formal — both wrong for a modern PT-PT app). NOT "Vossa Excelencia".
  Modern Portugal apps (Google PT, Microsoft PT, Apple PT) all use "tu".
  Verb conjugation follows "tu": tens, podes, queres, ves, escreves, guardas.
  Implicit addressing (no pronoun) is also natural: "Escreve aqui..." / "Guarda a entrada".

- Plurals: one, many, other (CLDR: "many" for millions without decimals).
  CRITICAL: 0 falls into "other" in pt-PT (NOT into a "zero" category). "0 entradas"
  uses the "other" form, not singular. 2–999,999 use "other". Only exactly 1 uses "one".
  1,000,000 and multiples (without decimals) use "many".

- Text: European Portuguese is 10-20% LONGER than German. Design with ~15-20% extra space.
  Button texts can be critical: "Iniciar sessao" (14 chars) vs. English "Login" (5 chars).

- ACORDO ORTOGRAFICO 1990 (MANDATORY since 2015 in Portugal — obligatory in schools,
  government, and media. Pre-reform spellings are WRONG in official contexts):
  optimo NOT "optimo" | acao NOT "accao" | eletronico NOT "electronico"
  rececao NOT "recepcao" | direcao NOT "direccao" | fator NOT "factor"
  correto NOT "correcto" | ideia NOT "ideia" (no accent) | voo NOT "voo" (no circumflex)
  para NOT "para" (no accent on 3rd person of parar) | "fim de semana" NOT "fim-de-semana"
  LLMs frequently produce pre-reform spellings — verify EVERY removed silent consonant.

- CRITICAL — PT-PT vs PT-BR vocabulary (the #1 LLM failure mode for Portuguese):
  Standard multi-provider LLMs default to Brazilian Portuguese. Every single one of
  these terms MUST use the European variant. Grep for Brazilian variants after translation.

  | German       | pt-PT (CORRECT)          | pt-BR (WRONG for PT-PT)   |
  |--------------|--------------------------|---------------------------|
  | Benutzer     | utilizador               | usuario                   |
  | App          | aplicacao (fem.)         | aplicativo (masc.)        |
  | Speichern    | guardar                  | salvar                    |
  | Einstellungen| definicoes               | configuracoes             |
  | Passwort     | palavra-passe            | senha                     |
  | Herunterladen| transferir               | baixar / fazer download   |
  | Handy        | telemovel                | celular                   |
  | Datei        | ficheiro                 | arquivo                   |
  | Loeschen     | eliminar                 | excluir                   |
  | Bildschirm   | ecra                     | tela                      |
  | Anmelden     | iniciar sessao           | fazer login / entrar      |
  | Abmelden     | terminar sessao          | sair / fazer logout       |
  | Teilen       | partilhar                | compartilhar              |
  | Abonnement   | subscricao               | assinatura                |
  | Kamera       | camara                   | camera                    |
  | Foto         | fotografia / foto        | foto                      |
  | Backup       | copia de seguranca       | backup                    |
  | Suchen       | pesquisar                | buscar / pesquisar        |
  | Aufzeichnung | registo                  | registro                  |
  | Weiter (Btn) | seguinte / continuar     | proximo / continuar       |
  | Zurueck      | voltar / anterior        | voltar                    |
  | E-Mail       | correio eletronico / e-mail | e-mail                 |

  Journaling vocabulary for BestJournal and similar apps:
  diario (Tagebuch), entrada (Eintrag), nota (Notiz), registo (Aufzeichnung — NOT registro),
  humor / disposicao (Stimmung), escrever (schreiben), guardar (speichern).

- CRITICAL — Gerundio vs. Infinitivkonstruktion:
  Portugal uses "estar a + infinitive" for progressive actions. Portugal does NOT use
  Brazilian "-ando / -endo / -indo" gerund in the progressive. This is THE most visible
  PT-PT vs PT-BR marker.

  | Context            | pt-PT (CORRECT)        | pt-BR (WRONG for PT-PT) |
  |--------------------|------------------------|-------------------------|
  | Wird gespeichert   | A guardar...           | Salvando...             |
  | Wird geladen       | A carregar...          | Carregando...           |
  | Wird synchronisiert| A sincronizar...       | Sincronizando...        |
  | Wird gesucht       | A pesquisar...         | Pesquisando...          |
  | Wird verarbeitet   | A processar...         | Processando...          |
  | Wird gesendet      | A enviar...            | Enviando...             |
  | Wird heruntergeladen| A transferir...       | Baixando...             |
  | Wird erstellt      | A criar...             | Criando...              |

  Loading screens, progress indicators and toasts MUST use "A + infinitive" form.
  Grep after translation for: "ando\.\.\." and "endo\.\.\." and "indo\.\.\." to catch
  gerund leakage. Zero matches expected.

- WARNING — Pronoun position (enclisis vs. proclisis):
  Portugal prefers ENCLISIS (pronoun after the verb, hyphenated): "Diga-me", "Mostra-me".
  Brazil prefers PROCLISIS (pronoun before the verb): "Me diga", "Me mostra".
  LLMs default to Brazilian proclisis. Check all imperatives and simple tenses.
  CORRECT: "Mostra-me as entradas" / "Envia-me lembretes"
  WRONG (BR): "Me mostra as entradas" / "Me envia lembretes"

- Typography (pt-PT standard):
  Quotation marks: PRIMARY = guillemets «...», SECONDARY (nested) = "..." (curly).
  Decimal: comma. Thousands: period. Example: 1.234,56 €
  Euro symbol AFTER the number with a space: "9,99 €" — NOT "€9.99".
  Date: DD/MM/AAAA (e.g. 18/04/2026). Time: HH:mm (24h, no AM/PM).

- WARNING — Mes-Namen (Monate) are lowercase in pt-PT after the AO 1990:
  janeiro, fevereiro, marco, abril, maio, junho, julho, agosto, setembro,
  outubro, novembro, dezembro. Weekdays also lowercase: segunda-feira, terca-feira,
  quarta-feira, quinta-feira, sexta-feira, sabado, domingo.

- Tone for journaling apps: Portuguese users expect direct but restrained tone.
  Avoid American-style enthusiasm ("Incrivel!", "Fantastico!"). Emotional prompts
  should be neutral: "Escreve o que pensas" NOT "Partilha os teus sentimentos incriveis!".
  Portuguese tone is noticeably more reserved than Brazilian Portuguese.

- WARNING — Brand and anglicism balance: Portugal accepts "login", "email", "online"
  in casual speech, but professional apps prefer the localized forms: "iniciar sessao",
  "correio eletronico"/"e-mail", "em linha". When in doubt, use the PT-PT form.

- WARNING — LLM default drift: After translating 10+ strings, LLMs frequently lapse
  back into Brazilian vocabulary mid-file. Re-check EVERY critical term in the
  verification pass, especially "salvar", "usuario", "configuracoes", "arquivo",
  "compartilhar", "voce", and any "-ando/-endo/-indo" form.
```
