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

    if (sysMsg.includes('tarot reader')) {
      const tarotResponse = this.buildTarotResponse(userMsg);
      if (tarotResponse) {
        return {
          provider: this.name,
          text: JSON.stringify(tarotResponse),
          raw: {mock: true, scope: 'tarot'},
        };
      }
    }

    if (sysMsg.includes('oracle guide')) {
      return {
        provider: this.name,
        text: JSON.stringify({
          type: 'ORACLE_1Q',
          title: 'Brújula serena',
          guidance: {
            core: 'Avanza con calma: hoy una decisión simple vale más que diez ideas dispersas.',
            do: [
              'Elige una prioridad y termínala antes de abrir otra.',
              'Escucha tu cuerpo y baja el ritmo si notas tensión.',
            ],
            avoid: ['Responder desde el impulso o la prisa emocional.'],
            reflection: '¿Qué paso pequeño te daría paz esta noche?',
          },
        }),
        raw: {mock: true, scope: 'oracle'},
      };
    }

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

  private buildTarotResponse(userMsg: string): Record<string, unknown> | null {
    let payload: unknown;
    try {
      payload = JSON.parse(userMsg);
    } catch {
      return null;
    }

    if (!payload || typeof payload !== 'object') {
      return null;
    }

    const req = payload as {
      requestType?: string;
      draw?: {
        card?: {name?: string; orientation?: string};
        cards?: Array<{position?: string; name?: string; orientation?: string}>;
      };
    };

    if (req.requestType === 'TAROT_1' && req.draw?.card?.name && req.draw.card.orientation) {
      return {
        type: 'TAROT_1',
        card: {
          name: req.draw.card.name,
          orientation: req.draw.card.orientation,
        },
        interpretation: {
          theme: 'Enfoque interior',
          meaning: 'La carta señala una verdad clara: al simplificar tus decisiones, recuperas energía y dirección.',
          advice: 'Haz una sola acción concreta hoy y protégela de distracciones.',
          watchOut: 'No confundas pausa con estancamiento; el silencio también te ordena.',
        },
      };
    }

    const draw = req.draw;
    if (req.requestType === 'TAROT_3' && draw && Array.isArray(draw.cards)) {
      const drawCards = draw.cards;
      const drawByPosition = new Map(drawCards.map((card) => [card.position, card]));
      const ordered = ['past', 'present', 'future'] as const;
      const cards = ordered.map((position) => {
        const drawn = drawByPosition.get(position);
        return {
          position,
          name: drawn?.name ?? 'Unknown',
          orientation: drawn?.orientation ?? 'upright',
          meaning: position === 'past' ?
            'El pasado dejó una lección sobre límites y claridad emocional.' :
            position === 'present' ?
            'En el presente, tu intuición pide ordenar prioridades con honestidad.' :
            'El futuro se abre cuando sostienes constancia y decisiones simples.',
        };
      });

      return {
        type: 'TAROT_3',
        cards,
        summary: 'Tu proceso va de la confusión a la coherencia: menos ruido, más propósito.',
        advice: 'Honra lo aprendido, decide lo urgente y mantén un ritmo sostenible.',
      };
    }

    return null;
  }
}
