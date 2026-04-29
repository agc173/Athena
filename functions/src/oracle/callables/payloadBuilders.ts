export function stripUndefinedDeep<T>(value: T): T {
  if (Array.isArray(value)) return value.map(stripUndefinedDeep) as any;
  if (value && typeof value === 'object') {
    const out: any = {};
    for (const [k, v] of Object.entries(value as any)) {
      if (v === undefined) continue;
      out[k] = stripUndefinedDeep(v);
    }
    return out;
  }
  return value;
}

export function buildEconomyPayload(
    enabled: boolean,
    source: unknown,
    moonCost: unknown
): {source?: unknown; moonCost?: unknown} | undefined {
  if (!enabled) return undefined;
  const economy = stripUndefinedDeep({source, moonCost});
  return Object.keys(economy).length > 0 ? economy : undefined;
}
