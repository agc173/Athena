import type {LLMProvider, LLMGenerateParams, LLMGenerateResult} from '../LLMProvider';

export class GeminiVertexProvider implements LLMProvider {
  readonly name = 'gemini-vertex';

  async generate(_params: LLMGenerateParams): Promise<LLMGenerateResult> {
    throw new Error('GeminiVertexProvider not implemented yet');
  }
}
