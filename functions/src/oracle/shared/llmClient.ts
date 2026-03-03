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
