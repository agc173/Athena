import type {LLMProvider} from './LLMProvider';
import {FallbackProvider} from './providers/FallbackProvider';
import {addLlmTokens, reserveLlmCallOrThrow, type DailyCaps, type LlmScope} from '../firestore/usageDaily';
import {ENV} from '../config/env';

type GenerateArgs = Parameters<LLMProvider['generate']>[0];
type GenerateResult = Awaited<ReturnType<LLMProvider['generate']>>;

export class LLMRouter {
  constructor(private readonly provider: LLMProvider) {}

  static withFallback(primary: LLMProvider, fallback: LLMProvider) {
    return new LLMRouter(new FallbackProvider(primary, fallback));
  }

  get name() {
    return this.provider.name;
  }

  private getDailyCaps(): DailyCaps {
    // Defaults safe: if you forget to set tarot/oracle, they won't be blocked by horoscope cap.
    return {
      totalMaxCalls: ENV.DAILY_LLM_MAX_CALLS_TOTAL,
      scopeMaxCalls: {
        horoscope: ENV.DAILY_LLM_MAX_CALLS_HOROSCOPE,
        tarot: ENV.DAILY_LLM_MAX_CALLS_TAROT,
        oracle: ENV.DAILY_LLM_MAX_CALLS_ORACLE,
        unknown: ENV.DAILY_LLM_MAX_CALLS_UNKNOWN,
      },
    };
  }

  async generate(args: GenerateArgs & {scope?: LlmScope}): Promise<GenerateResult> {
    const scope = args.scope ?? 'unknown';

    // Strip scope before passing to provider
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const {scope: _scope, ...providerArgs} = args;

    const caps = this.getDailyCaps();
    const {dateIso} = await reserveLlmCallOrThrow(scope, caps);

    const res = await this.provider.generate(providerArgs as GenerateArgs);

    // best-effort: don't fail the whole request if token accounting fails
    try {
      await addLlmTokens(scope, dateIso, res.inputTokens ?? 0, res.outputTokens ?? 0);
    } catch {
      // no-op (optional: log)
    }

    return res;
  }
}
