import type {LLMProvider, LLMGenerateParams, LLMGenerateResult} from '../LLMProvider';

export class FallbackProvider implements LLMProvider {
  readonly name: string;

  constructor(
    private readonly primary: LLMProvider,
    private readonly fallback: LLMProvider
  ) {
    this.name = `${primary.name}->${fallback.name}`;
  }

  async generate(params: LLMGenerateParams): Promise<LLMGenerateResult> {
    try {
      return await this.primary.generate(params);
    } catch (_e) {
      const res = await this.fallback.generate(params);
      return {...res, provider: `${res.provider} (fallback)`};
    }
  }
}
