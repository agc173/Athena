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

  if (!useMock && !ENV.DEEPSEEK_API_KEY) {
    throw new Error('DEEPSEEK_API_KEY is missing but USE_MOCK_LLM=false');
  }

  const primaryProvider = useMock ?
    new MockLLMProvider() :
    new DeepSeekProvider();

  console.info('LLM Router primary provider:', primaryProvider.constructor.name);

  return geminiImplemented ?
    LLMRouter.withFallback(primaryProvider, gemini) :
    new LLMRouter(primaryProvider);
}
