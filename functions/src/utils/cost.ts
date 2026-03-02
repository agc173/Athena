import {ENV} from '../config/env';

function round6(n: number): number {
  return Math.round(n * 1_000_000) / 1_000_000;
}

export function estimateDeepSeekCostUsd(inputTokens: number, outputTokens: number): number {
  const inputCost =
    (inputTokens / 1_000_000) * ENV.DEEPSEEK_PRICE_INPUT_PER_MILLION_USD;

  const outputCost =
    (outputTokens / 1_000_000) * ENV.DEEPSEEK_PRICE_OUTPUT_PER_MILLION_USD;

  return round6(inputCost + outputCost);
}
