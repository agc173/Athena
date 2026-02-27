import type {LLMProvider, LLMGenerateParams, LLMGenerateResult} from '../LLMProvider';

export class GeminiVertexProvider implements LLMProvider {
  readonly name = 'gemini-vertex';

  isImplemented() {
    return false; // cambia a true cuando lo implementemos
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  async generate(_params: LLMGenerateParams): Promise<LLMGenerateResult> {
    throw new Error('GeminiVertexProvider not implemented yet');
  }
}
