export type LLMRole = 'system' | 'user' | 'assistant';

export type LLMMessage = { role: LLMRole; content: string };

export type LLMGenerateParams = {
  messages: LLMMessage[];
  temperature?: number;
  maxTokens?: number;
};

export type LLMGenerateResult = {
  provider: string;
  text: string;
  raw?: unknown;

  // 🔹 Añadimos métricas opcionales
  inputTokens?: number;
  outputTokens?: number;
};

export interface LLMProvider {
  readonly name: string;
  generate(params: LLMGenerateParams): Promise<LLMGenerateResult>;
}
