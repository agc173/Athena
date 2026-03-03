import {RequestType} from '../types';
import {drawTarotCards} from './draw';
import {buildSystemPrompt, buildUserPrompt} from './prompts';
import {parseStrictJsonObject, validateTarotReading, type Tarot1Reading, type Tarot3Reading} from './schemas';

export interface LLMClient {
  generate(params: {
    systemPrompt: string;
    userPrompt: string;
    maxOutputTokens: number;
    temperature: number;
  }): Promise<{
    text: string;
    provider: string;
    inputTokens: number;
    outputTokens: number;
    costUsd?: number;
    durationMs?: number;
  }>;
}

export async function generateTarotReading(params: {
  requestId: string;
  requestType: RequestType.TAROT_1 | RequestType.TAROT_3;
  lang: string;
  question?: string;
  llm: LLMClient;
}): Promise<{
  reading: Tarot1Reading | Tarot3Reading;
  draw: ReturnType<typeof drawTarotCards>;
  llmMeta: {
    provider: string;
    inputTokens: number;
    outputTokens: number;
    costUsd?: number;
    durationMs?: number;
  };
}> {
  const draw = drawTarotCards({
    requestId: params.requestId,
    requestType: params.requestType,
    lang: params.lang,
  });

  const systemPrompt = buildSystemPrompt(params.requestType);
  const userPrompt = buildUserPrompt({
    requestType: params.requestType,
    lang: params.lang,
    draw,
    question: params.question,
  });

  const llmResponse = await params.llm.generate({
    systemPrompt,
    userPrompt,
    maxOutputTokens: params.requestType === RequestType.TAROT_1 ? 800 : 1400,
    temperature: 0.6,
  });

  const parsed = parseStrictJsonObject(llmResponse.text);
  const reading = validateTarotReading(parsed, draw);

  return {
    reading,
    draw,
    llmMeta: {
      provider: llmResponse.provider,
      inputTokens: llmResponse.inputTokens,
      outputTokens: llmResponse.outputTokens,
      costUsd: llmResponse.costUsd,
      durationMs: llmResponse.durationMs,
    },
  };
}
