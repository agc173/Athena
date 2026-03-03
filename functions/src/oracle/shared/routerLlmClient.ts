import {type LLMRouter} from '../../llm/LLMRouter';
import {type LLMClient} from './llmClient';

export function createLlmClientFromRouter(router: LLMRouter, scope: 'tarot' | 'oracle'): LLMClient {
  return {
    async generate(params) {
      const response = await router.generate({
        scope,
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
      };
    },
  };
}
