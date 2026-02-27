import type {LLMProvider, LLMGenerateParams, LLMGenerateResult} from '../LLMProvider';

function pickDeterministic<T>(arr: T[], seed: string): T {
  let h = 0;
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) >>> 0;
  return arr[h % arr.length];
}

export class MockLLMProvider implements LLMProvider {
  readonly name = 'mock-llm';

  async generate(params: LLMGenerateParams): Promise<LLMGenerateResult> {
    const userMsg = params.messages.find((m) => m.role === 'user')?.content ?? '';
    const sysMsg = params.messages.find((m) => m.role === 'system')?.content ?? '';

    // Intento â€œbaratoâ€ de extraer date/sign/lang del prompt para que el mock sea estable
    const dateIso = (userMsg.match(/date:\s*([0-9]{4}-[0-9]{2}-[0-9]{2})/i)?.[1]) ?? '1970-01-01';
    const sign = (userMsg.match(/sign:\s*([a-z]+)/i)?.[1]) ?? 'unknown';
    const lang = (sysMsg.match(/language:\s*([a-z]{2})/i)?.[1]) ?? 'es';

    const moods = ['Calma', 'Impulso creativo', 'Claridad', 'EnergÃ­a', 'Enfoque', 'Optimismo'];
    const colors = ['Azul', 'Rojo', 'Verde', 'Dorado', 'Violeta', 'Blanco'];

    const seed = `${dateIso}|${sign}|${lang}`;
    const mood = pickDeterministic(moods, seed);
    const luckyColor = pickDeterministic(colors, seed + '|c');

    const luckyNumber = (seed.split('').reduce((a, c) => a + c.charCodeAt(0), 0) % 99) + 1;

    const json = {
      text: `[MOCK ${lang}] ${sign.toUpperCase()} (${dateIso}): Hoy notas ${mood.toLowerCase()} y una oportunidad pequeÃ±a pero clara. MantÃ©n el ritmo, evita decisiones impulsivas y prioriza una conversaciÃ³n honesta. Un gesto sencillo te abre una puerta inesperada.`,
      mood,
      luckyNumber,
      luckyColor,
      shareText: `[MOCK ${lang}] ${sign.toUpperCase()}: ${mood}. NÃºmero ${luckyNumber}.`,
    };

    return {
      provider: this.name,
      text: JSON.stringify(json),
      raw: {mock: true, seed},
    };
  }
}
