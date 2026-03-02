import type {LLMProvider, LLMGenerateParams, LLMGenerateResult} from '../LLMProvider';
import {ENV} from '../../config/env';
import {fetchWithTimeout} from '../../utils/fetchWithTimeout';

type HoroscopeJson = {
  text: string;
  mood: string;
  luckyNumber: number;
  luckyColor: string;
  shareText: string;
};

function estimateTokens(text: string): number {
  return Math.ceil(text.length / 4);
}

function isObject(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v);
}

function parseAndValidateHoroscopeJson(jsonText: string): HoroscopeJson {
  let parsed: unknown;
  try {
    parsed = JSON.parse(jsonText);
  } catch {
    throw new Error('LLM_JSON_INVALID: not valid JSON');
  }
  if (!isObject(parsed)) throw new Error('LLM_JSON_INVALID: root not object');

  const {text, mood, luckyNumber, luckyColor, shareText} = parsed;

  if (typeof text !== 'string' || !text.trim()) throw new Error('LLM_JSON_INVALID: text');
  if (typeof mood !== 'string' || !mood.trim()) throw new Error('LLM_JSON_INVALID: mood');
  if (typeof luckyNumber !== 'number' || !Number.isFinite(luckyNumber)) {
    throw new Error('LLM_JSON_INVALID: luckyNumber');
  }
  if (typeof luckyColor !== 'string' || !luckyColor.trim()) throw new Error('LLM_JSON_INVALID: luckyColor');
  if (typeof shareText !== 'string' || !shareText.trim()) throw new Error('LLM_JSON_INVALID: shareText');

  return {text, mood, luckyNumber, luckyColor, shareText};
}

// Para que withRetry lo trate como retryable incluso si el status es 200 pero el JSON es invÃ¡lido
function makeRetryableError(message: string): any {
  const err: any = new Error(message);
  // 529 no es estÃ¡ndar, pero sirve si tu withRetry reintenta 5xx/429 por status numÃ©rico.
  // Si tu withRetry SOLO reintenta 429/5xx, 529 entra como 5xx-ish si lo tratas por rango,
  // y si no, cÃ¡mbialo a 500.
  err.status = 500;
  return err;
}

export class DeepSeekProvider implements LLMProvider {
  readonly name = 'deepseek';

  async generate(params: LLMGenerateParams): Promise<LLMGenerateResult> {
    if (!ENV.DEEPSEEK_API_KEY) throw new Error('DEEPSEEK_API_KEY missing (DeepSeekProvider)');

    const temperature = params.temperature ?? ENV.LLM_TEMPERATURE ?? 0.3;
    const maxTokens = params.maxTokens ?? ENV.LLM_MAX_TOKENS ?? 500;

    // Importante para JSON output: pedir explÃ­citamente JSON estricto
    // y ademÃ¡s usar response_format json_object en el request.
    const messages = [
      {
        role: 'system' as const,
        content:
          'Devuelve SIEMPRE JSON estricto (sin markdown, sin texto fuera del JSON). ' +
          'Formato exacto: {"text":string,"mood":string,"luckyNumber":number,"luckyColor":string,"shareText":string}.',
      },
      ...params.messages,
    ];

    const body = {
      model: 'deepseek-chat',
      messages,
      temperature,
      max_tokens: maxTokens,
      // Fuerza salida en JSON vÃ¡lido (DeepSeek Chat Completions)
      response_format: {type: 'json_object' as const},
    };

    const startedAt = Date.now();
    const inputEstTokens = estimateTokens(messages.map((m) => m.content).join('\n'));

    const res = await fetchWithTimeout(
        'https://api.deepseek.com/chat/completions',
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${ENV.DEEPSEEK_API_KEY}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(body),
        },
        ENV.LLM_TIMEOUT_MS
    );

    const durationMs = Date.now() - startedAt;

    if (!res.ok) {
      const text = await res.text().catch(() => '');
      const err: any = new Error(`DeepSeek error ${res.status}: ${text}`);
      err.status = res.status; // para withRetry (429 + 5xx)
      console.warn('LLM_CALL_HTTP_ERROR', {provider: this.name, status: res.status, durationMs});
      throw err;
    }

    const json: any = await res.json();

    const content = json?.choices?.[0]?.message?.content?.trim();
    if (!content) throw new Error('DeepSeek empty response');

    // ValidaciÃ³n estricta del contrato
    let validated: HoroscopeJson;
    try {
      validated = parseAndValidateHoroscopeJson(content);
    } catch (e: any) {
      // Si el LLM devuelve JSON invÃ¡lido/incorrecto, esto debe reintentarse.
      console.warn('LLM_CALL_INVALID_JSON', {
        provider: this.name,
        durationMs,
        error: e?.message ?? String(e),
      });
      throw makeRetryableError(`DeepSeek invalid JSON contract: ${e?.message ?? String(e)}`);
    }

    // Tokens: si DeepSeek devuelve usage, genial; si no, estimamos
    const promptTokens = Number(json?.usage?.prompt_tokens);
    const completionTokens = Number(json?.usage?.completion_tokens);

    const inputTokens = Number.isFinite(promptTokens) ? promptTokens : inputEstTokens;
    const outputTokens = Number.isFinite(completionTokens) ? completionTokens : estimateTokens(content);

    console.info('LLM_CALL_SUCCESS', {
      provider: this.name,
      durationMs,
      inputTokens,
      outputTokens,
    });

    // Ojo: devolvemos el JSON VALIDADO serializado, para que el siguiente paso (guardar en Firestore)
    // sea determinista y no dependa de parseos posteriores.
    return {
      provider: this.name,
      text: JSON.stringify(validated),
      raw: json,
      inputTokens,
      outputTokens,
    };
  }
}
