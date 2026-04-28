export type HoroscopeUnlockDecision =
  | {source: 'PREMIUM_INCLUDED'; moonCost: 0}
  | {source: 'MOON'; moonCost: number}
  | {source: 'REJECT'; moonCost: 0; reason: 'INSUFFICIENT_MOON_BALANCE'};

export function resolveHoroscopeUnlockDecision(params: {
  isPremium: boolean;
  balance: number;
  moonCost: number;
}): HoroscopeUnlockDecision {
  if (params.isPremium) {
    return {source: 'PREMIUM_INCLUDED', moonCost: 0};
  }

  if (params.balance < params.moonCost) {
    return {source: 'REJECT', moonCost: 0, reason: 'INSUFFICIENT_MOON_BALANCE'};
  }

  return {source: 'MOON', moonCost: params.moonCost};
}
