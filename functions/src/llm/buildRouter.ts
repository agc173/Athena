import {ENV} from '../config/env';
import {LLMRouter} from './LLMRouter';
import {DeepSeekProvider} from './providers/DeepSeekProvider';
import {GeminiVertexProvider} from './providers/GeminiVertexProvider';
import {MockLLMProvider} from './providers/MockLLMProvider';

/**
 * Build a router that only enables fallback if GeminiVertexProvider is actually implemented.
 * This avoids the current situation where the fallback always throws "not implemented yet"
 * and turns recoverable DeepSeek failures into guaranteed failures.
 */
export function buildRouter(): LLMRouter {
  const gemini = new GeminiVertexProvider();

  const geminiImplemented =
    typeof (gemini as any).isImplemented === 'function' ?
      Boolean((gemini as any).isImplemented()) :
      false;

  const useMock = ENV.USE_MOCK_LLM;

  const primaryProvider = useMock ?
    new MockLLMProvider() :
    (ENV.DEEPSEEK_API_KEY ?
      new DeepSeekProvider() :
      new MockLLMProvider());


  return geminiImplemented ?
    LLMRouter.withFallback(primaryProvider, gemini) :
    new LLMRouter(primaryProvider);
}
