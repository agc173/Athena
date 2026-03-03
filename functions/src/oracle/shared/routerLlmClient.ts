import {type LLMRouter} from '../../llm/LLMRouter';
import {type LlmScope} from '../../firestore/usageDaily';
import {type LLMClient} from './llmClient';

function readOptionalNumberField(obj: unknown, key: 'durationMs' | 'costUsd'): number | undefined {
  if (!obj || typeof obj !== 'object') return undefined;
  const value = (obj as Record<string, unknown>)[key];
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
}

export function createLlmClientFromRouter(
    router: LLMRouter,
    scope: LlmScope,
    options?: {
      usageDailyDateIso?: string;
      skipUsageReservation?: boolean;
      skipUsageTokenTracking?: boolean;
    }
): LLMClient {
  return {
    async generate(params) {
      const response = await router.generate({
        scope,
        usageDailyDateIso: options?.usageDailyDateIso,
        skipUsageReservation: options?.skipUsageReservation,
        skipUsageTokenTracking: options?.skipUsageTokenTracking,
        messages: [
          {role: 'system', content: params.systemPrompt},
          {role: 'user', content: params.userPrompt},
        ],
        temperature: params.temperature,
        maxTokens: params.maxOutputTokens,
      });

      return {
        text: response.text,
        provider: response.provider,
        inputTokens: response.inputTokens ?? 0,
        outputTokens: response.outputTokens ?? 0,
        durationMs: readOptionalNumberField(response, 'durationMs'),
        costUsd: readOptionalNumberField(response, 'costUsd'),
      };
    },
  };
}
