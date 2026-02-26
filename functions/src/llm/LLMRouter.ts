import type {LLMProvider} from './LLMProvider';
import {FallbackProvider} from './providers/FallbackProvider';

export class LLMRouter {
  constructor(private readonly provider: LLMProvider) {}

  static withFallback(primary: LLMProvider, fallback: LLMProvider) {
    return new LLMRouter(new FallbackProvider(primary, fallback));
  }

  get name() {
    return this.provider.name;
  }

  generate = this.provider.generate.bind(this.provider);
}
