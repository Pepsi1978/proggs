### ru — Russisch

```
## Language-Specific Rules: Russian (ru)
- Script: Cyrillic
- Register: Formal "Vy" (Вы, capitalized). Safe default for adult apps. Prevents gendered
  language issues (ty forms assume gender).
- Plurals: one (1,21,31...), few (2-4,22-24...), many (5-20,25-30...), other (decimals)
  — FOUR forms. LLMs frequently forget "many" — produces wrong output for 5-20.
- Text: ~15-20% LONGER than German.
- Vocabulary: Dnevnik (Дневник), Zapis (Запись), Nastroenie (Настроение),
  Sokhranit (Сохранить), Udalit (Удалить), Nastroyki (Настройки),
  Napominanie (Напоминание), Podpiska (Подписка) NOT "abonement"
- CRITICAL — Aspect system: "Save" = "Сохранить" (perfective) NOT "Сохранять" (imperfective).
  Wrong aspect in buttons sounds completely unnatural.
- Verb buttons: Use INFINITIVE (сохранить). LLMs sometimes produce imperative (сохрани) — rude.
- WARNING — Case endings with variables: "%d entries" requires different word forms at 1/2/5/21.
- Include "ё" (yo) — LLMs strip it, but omission changes meaning in some words.
```
