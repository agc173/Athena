import type {LLMProvider, LLMGenerateParams, LLMGenerateResult} from '../LLMProvider';
import {ENV} from '../../config/env';

type DeepSeekResponse = {
  choices?: Array<{message?: {content?: string}}>
};

export class DeepSeekProvider implements LLMProvider {
  readonly name = 'deepseek';

  async generate(params: LLMGenerateParams): Promise<LLMGenerateResult> {
    if (!ENV.DEEPSEEK_API_KEY) throw new Error('DEEPSEEK_API_KEY missing (DeepSeekProvider)');

    const body = {
      model: 'deepseek-chat',
      messages: params.messages,
      temperature: params.temperature ?? 0.8,
      max_tokens: params.maxTokens ?? 400,
    };

    const res = await fetch('https://api.deepseek.com/chat/completions', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${ENV.DEEPSEEK_API_KEY}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    if (!res.ok) {
      const text = await res.text().catch(() => '');
      throw new Error(`DeepSeek error ${res.status}: ${text}`);
    }

    const json = await res.json() as DeepSeekResponse;
    const text = json.choices?.[0]?.message?.content?.trim();
    if (!text) throw new Error('DeepSeek empty response');

    return {provider: this.name, text, raw: json};
  }
}
