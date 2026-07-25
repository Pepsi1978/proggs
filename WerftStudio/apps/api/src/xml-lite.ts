// Sehr kleiner, abhaengigkeitsfreier XML-Leser fuer Android-Ressourcen, XAML und Storyboards.
// Er muss keine DTDs oder Namespaces aufloesen, aber Verschachtelung und Attribute exakt erhalten —
// nur so lassen sich Layouts strukturtreu statt per Textsuche auswerten.
export type XmlNode = { tag: string; attributes: Record<string, string>; children: XmlNode[]; text: string };

const entities: Record<string, string> = { amp: "&", lt: "<", gt: ">", quot: '"', apos: "'", nbsp: " " };

export function decodeXmlText(value: string): string {
  return value.replace(/&(#x?[0-9a-f]+|[a-z]+);/gi, (all, entity: string) => {
    if (entity.startsWith("#x") || entity.startsWith("#X")) return String.fromCodePoint(Number.parseInt(entity.slice(2), 16));
    if (entity.startsWith("#")) return String.fromCodePoint(Number.parseInt(entity.slice(1), 10));
    return entities[entity.toLowerCase()] ?? all;
  });
}

const attributePattern = /([\w.:\-]+)\s*=\s*("([^"]*)"|'([^']*)')/g;

function parseAttributes(raw: string): Record<string, string> {
  const attributes: Record<string, string> = {};
  for (const match of raw.matchAll(attributePattern)) attributes[match[1]!] = decodeXmlText(match[3] ?? match[4] ?? "");
  return attributes;
}

export function parseXml(source: string): XmlNode | undefined {
  const root: XmlNode = { tag: "#root", attributes: {}, children: [], text: "" };
  const stack: XmlNode[] = [root];
  const tagPattern = /<(\/)?([\w.:\-]+)((?:[^>"']|"[^"]*"|'[^']*')*?)(\/)?>|<!--[\s\S]*?-->|<!\[CDATA\[([\s\S]*?)\]\]>|<[?!][^>]*>/g;
  let lastIndex = 0;
  for (const match of source.matchAll(tagPattern)) {
    const parent = stack.at(-1)!;
    const between = source.slice(lastIndex, match.index);
    if (between.trim()) parent.text += decodeXmlText(between);
    lastIndex = match.index + match[0].length;
    if (match[5] !== undefined) { parent.text += match[5]; continue; }
    if (!match[2]) continue;
    if (match[1]) { if (stack.length > 1 && stack.at(-1)!.tag === match[2]) stack.pop(); else { const index = stack.findLastIndex((node) => node.tag === match[2]); if (index > 0) stack.length = index; } continue; }
    const node: XmlNode = { tag: match[2], attributes: parseAttributes(match[3] ?? ""), children: [], text: "" };
    parent.children.push(node);
    if (!match[4]) stack.push(node);
  }
  const tail = source.slice(lastIndex);
  if (tail.trim()) stack.at(-1)!.text += decodeXmlText(tail);
  return root.children[0];
}

export function walkXml(node: XmlNode, visit: (node: XmlNode, parents: XmlNode[]) => void, parents: XmlNode[] = []): void {
  visit(node, parents);
  const nextParents = [...parents, node];
  for (const child of node.children) walkXml(child, visit, nextParents);
}

// Android-Ressourcen benutzen den `android:`-Namensraum, XAML gar keinen; die Suche ignoriert ihn,
// damit dieselbe Auswertung fuer beide Welten funktioniert.
export function attribute(node: XmlNode, name: string): string | undefined {
  const direct = node.attributes[name];
  if (direct !== undefined) return direct;
  const lower = name.toLowerCase();
  for (const [key, value] of Object.entries(node.attributes)) if (key.toLowerCase() === lower || key.slice(key.indexOf(":") + 1).toLowerCase() === lower) return value;
  return undefined;
}
