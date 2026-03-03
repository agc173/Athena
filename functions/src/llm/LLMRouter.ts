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

  async generate(args: GenerateArgs & {
    scope?: LlmScope;
    usageDailyDateIso?: string;
    skipUsageReservation?: boolean;
    skipUsageTokenTracking?: boolean;
  }): Promise<GenerateResult> {
    const {
      scope: requestedScope,
      usageDailyDateIso,
      skipUsageReservation,
      skipUsageTokenTracking,
      ...providerArgs
    } = args;

    const scope = requestedScope ?? 'unknown';
    const shouldReserve = skipUsageReservation !== true;
    const shouldTrackTokens = skipUsageTokenTracking !== true;
    let trackedDateIso = usageDailyDateIso;

    if (shouldReserve) {
      const caps = this.getDailyCaps();
      const {dateIso} = await reserveLlmCallOrThrow(scope, caps);
      trackedDateIso = dateIso;
    }

    const res = await this.provider.generate(providerArgs as GenerateArgs);

    // best-effort: don't fail the whole request if token accounting fails
    if (shouldTrackTokens && trackedDateIso) {
      try {
        await addLlmTokens(scope, trackedDateIso, res.inputTokens ?? 0, res.outputTokens ?? 0);
      } catch {
        // no-op (optional: log)
      }
    }

    return res;
  }
}
