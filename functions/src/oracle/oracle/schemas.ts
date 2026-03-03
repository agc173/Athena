import {RequestType} from '../types';

export interface OracleReading {
  type: RequestType.ORACLE_1Q;
  title: string;
  guidance: {
    core: string;
    do: string[];
    avoid: string[];
    reflection: string;
  };
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function assertExactKeys(obj: Record<string, unknown>, allowed: string[], path: string): void {
  const actual = Object.keys(obj).sort().join(',');
  const expected = [...allowed].sort().join(',');
  if (actual !== expected) {
    throw new Error(`JSON_INVALID: invalid keys at ${path}`);
  }
}

function assertString(value: unknown, path: string, maxLength: number): string {
  if (typeof value !== 'string') {
    throw new Error(`JSON_INVALID: ${path} must be a string`);
  }
  const trimmed = value.trim();
  if (trimmed.length === 0 || trimmed.length > maxLength) {
    throw new Error(`JSON_INVALID: ${path} invalid length`);
  }
  return trimmed;
}

function assertStringArray(params: {
  value: unknown;
  path: string;
  minItems: number;
  maxItems: number;
  maxItemLength: number;
}): string[] {
  if (!Array.isArray(params.value)) {
    throw new Error(`JSON_INVALID: ${params.path} must be an array`);
  }
  if (params.value.length < params.minItems || params.value.length > params.maxItems) {
    throw new Error(`JSON_INVALID: ${params.path} invalid length`);
  }

  return params.value.map((item, index) => {
    return assertString(item, `${params.path}[${index}]`, params.maxItemLength);
  });
}

export function parseStrictJsonObject(text: string): Record<string, unknown> {
  const trimmed = text.trim();
  if (!trimmed.startsWith('{') || !trimmed.endsWith('}')) {
    throw new Error('JSON_INVALID: response is not a JSON object');
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(trimmed);
  } catch {
    throw new Error('JSON_INVALID: malformed JSON');
  }

  if (!isObject(parsed)) {
    throw new Error('JSON_INVALID: response root must be object');
  }

  return parsed;
}

export function validateOracleReading(obj: Record<string, unknown>): OracleReading {
  assertExactKeys(obj, ['type', 'title', 'guidance'], '$');

  if (obj.type !== RequestType.ORACLE_1Q) {
    throw new Error('JSON_INVALID: type must be ORACLE_1Q');
  }

  if (!isObject(obj.guidance)) {
    throw new Error('JSON_INVALID: guidance must be object');
  }
  assertExactKeys(obj.guidance, ['core', 'do', 'avoid', 'reflection'], '$.guidance');

  return {
    type: RequestType.ORACLE_1Q,
    title: assertString(obj.title, '$.title', 80),
    guidance: {
      core: assertString(obj.guidance.core, '$.guidance.core', 600),
      do: assertStringArray({
        value: obj.guidance.do,
        path: '$.guidance.do',
        minItems: 2,
        maxItems: 4,
        maxItemLength: 120,
      }),
      avoid: assertStringArray({
        value: obj.guidance.avoid,
        path: '$.guidance.avoid',
        minItems: 1,
        maxItems: 3,
        maxItemLength: 120,
      }),
      reflection: assertString(obj.guidance.reflection, '$.guidance.reflection', 200),
    },
  };
}
