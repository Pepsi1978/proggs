### es — Spanisch

```
## Language-Specific Rules: Spanish (es-419)
- Register: Informal "tu" (tuteo). NEVER "usted", NEVER voseo. Specify "no uses voseo".
- Plurals: one, other
- Text: Spanish is 15-25% LONGER than German. Design with ~20% extra space.
- Vocabulary: Diario, Entrada, Estado de animo, Guardar, Eliminar, Configuracion, Buscar
- Use Neutral Latin American Spanish (es-419). LATAM has 470M speakers vs 47M in Spain.
  AVOID Castilian: "ordenador"→"computadora", "movil"→"celular", "vosotros"→"ustedes".
- WARNING — Voseo leakage: LLMs leak Argentine verb forms ("vos tenes", "vos podes") even
  when prompted for neutral Spanish. Unintelligible to 90% of Spanish speakers.
- WARNING — Spanglish: LLMs inject anglicisms ("deletear"→"eliminar", "printear"→"imprimir").
- WARNING — Register mixing: LLMs mix "tu" and "usted" within the same translation set.
```
