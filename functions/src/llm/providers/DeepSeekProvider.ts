import type {LLMProvider, LLMGenerateParams, LLMGenerateResult, LLMMessage} from '../LLMProvider';
import {ENV} from '../../config/env';
import {fetchWithTimeout} from '../../utils/fetchWithTimeout';

function estimateTokens(text: string): number {
  return Math.ceil(text.length / 4);
}

function isObject(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v);
}

function parseStrictJsonObject(jsonText: string): Record<string, unknown> {
  let parsed: unknown;
  try {
    parsed = JSON.parse(jsonText);
  } catch {
    throw new Error('LLM_JSON_INVALID: not valid JSON');
  }
  if (!isObject(parsed)) throw new Error('LLM_JSON_INVALID: root not object');

  return parsed;
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

  private async callChatCompletions(
      messages: LLMMessage[],
      temperature: number,
      maxTokens: number,
      useResponseFormat: boolean
  ): Promise<{json: any; durationMs: number}> {
    const body: Record<string, unknown> = {
      model: 'deepseek-chat',
      messages,
      temperature,
      max_tokens: maxTokens,
    };

    if (useResponseFormat) {
      body.response_format = {type: 'json_object' as const};
    }

    const startedAt = Date.now();
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
      if (useResponseFormat && res.status >= 400 && res.status < 500 && /response_format/i.test(text)) {
        console.warn('LLM_RESPONSE_FORMAT_UNSUPPORTED', {provider: this.name, status: res.status});
        return this.callChatCompletions(messages, temperature, maxTokens, false);
      }

      const err: any = new Error(`DeepSeek error ${res.status}: ${text}`);
      err.status = res.status;
      console.warn('LLM_CALL_HTTP_ERROR', {provider: this.name, status: res.status, durationMs});
      throw err;
    }

    return {
      json: await res.json(),
      durationMs,
    };
  }

  private async repairJson(
      invalidText: string,
      maxTokens: number
  ): Promise<string> {
    const repairMessages: LLMMessage[] = [
      {
        role: 'system',
        content: 'You are a formatter. Output ONLY valid JSON. No markdown, no explanations.',
      },
      {
        role: 'user',
        content: `Fix this output and return only the corrected JSON object:\n\n${invalidText}`,
      },
    ];

    const {json} = await this.callChatCompletions(repairMessages, 0, maxTokens, true);
    const repairedContent = json?.choices?.[0]?.message?.content?.trim();
    if (!repairedContent) {
      throw new Error('LLM_JSON_INVALID: repair empty response');
    }
    parseStrictJsonObject(repairedContent);
    return repairedContent;
  }

  async generate(params: LLMGenerateParams): Promise<LLMGenerateResult> {
    if (!ENV.DEEPSEEK_API_KEY) throw new Error('DEEPSEEK_API_KEY missing (DeepSeekProvider)');

    const temperature = params.temperature ?? ENV.LLM_TEMPERATURE ?? 0.3;
    const maxTokens = params.maxTokens ?? ENV.LLM_MAX_TOKENS ?? 500;

    // Prompt hardening para minimizar salidas fuera de contrato.
    const messages = [
      {
        role: 'system' as const,
        content:
          'Return ONLY a single valid JSON object. No markdown. No explanations. No text outside JSON.',
      },
      ...params.messages,
    ];
    const inputEstTokens = estimateTokens(messages.map((m) => m.content).join('\n'));

    const {json, durationMs} = await this.callChatCompletions(messages, temperature, maxTokens, true);

    const content = json?.choices?.[0]?.message?.content?.trim();
    if (!content) throw new Error('DeepSeek empty response');

    // ValidaciÃ³n estricta JSON object + repair fallback
    let finalContent = content;
    try {
      parseStrictJsonObject(content);
    } catch (e: any) {
      const snippet = content.slice(0, 200);
      console.warn('LLM_CALL_INVALID_JSON', {
        provider: this.name,
        durationMs,
        error: e?.message ?? String(e),
        rawSnippet: snippet,
        rawLength: content.length,
      });

      try {
        finalContent = await this.repairJson(content, maxTokens);
      } catch (repairError: any) {
        throw makeRetryableError(`DeepSeek invalid JSON contract: ${repairError?.message ?? String(repairError)}`);
      }
    }

    // Tokens: si DeepSeek devuelve usage, genial; si no, estimamos
    const promptTokens = Number(json?.usage?.prompt_tokens);
    const completionTokens = Number(json?.usage?.completion_tokens);

    const inputTokens = Number.isFinite(promptTokens) ? promptTokens : inputEstTokens;
    const outputTokens = Number.isFinite(completionTokens) ? completionTokens : estimateTokens(finalContent);

    console.info('LLM_CALL_SUCCESS', {
      provider: this.name,
      durationMs,
      inputTokens,
      outputTokens,
    });

    return {
      provider: this.name,
      text: finalContent,
      raw: json,
      inputTokens,
      outputTokens,
    };
  }
}
