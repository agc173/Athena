import {type ReadingTopic} from '../types';
import {type LLMClient} from '../shared/llmClient';
import {buildOracleSystemPrompt, buildOracleUserPrompt} from './prompts';
import {parseStrictJsonObject, validateOracleReading, type OracleReading} from './schemas';

export async function generateOracleAnswer(params: {
  requestId: string;
  lang: string;
  question: string;
  topic?: ReadingTopic;
  llm: LLMClient;
}): Promise<{
  answer: OracleReading;
  llmMeta: {
    provider: string;
    inputTokens: number;
    outputTokens: number;
    costUsd?: number;
    durationMs?: number;
  };
}> {
  const systemPrompt = buildOracleSystemPrompt(params.lang);
  const userPrompt = buildOracleUserPrompt({
    lang: params.lang,
    question: params.question,
    topic: params.topic,
  });

  const llmResponse = await params.llm.generate({
    systemPrompt,
    userPrompt,
    maxOutputTokens: 900,
    temperature: 0.6,
  });

  const parsed = parseStrictJsonObject(llmResponse.text);
  const answer = validateOracleReading(parsed);

  return {
    answer,
    llmMeta: {
      provider: llmResponse.provider,
      inputTokens: llmResponse.inputTokens,
      outputTokens: llmResponse.outputTokens,
      costUsd: llmResponse.costUsd,
      durationMs: llmResponse.durationMs,
    },
  };
}
