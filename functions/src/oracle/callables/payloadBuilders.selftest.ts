import assert from 'node:assert/strict';
import test from 'node:test';
import {buildEconomyPayload, stripUndefinedDeep} from './payloadBuilders';

test('stripUndefinedDeep removes nested undefined', () => {
  const payload = stripUndefinedDeep({
    ok: true,
    maybe: undefined,
    nested: {
      a: 1,
      b: undefined,
    },
  });

  assert.deepEqual(payload, {
    ok: true,
    nested: {
      a: 1,
    },
  });
});

test('buildEconomyPayload omits disabled/empty economy', () => {
  assert.equal(buildEconomyPayload(false, 'caps', 1), undefined);
  assert.equal(buildEconomyPayload(true, undefined, undefined), undefined);
});

test('buildEconomyPayload strips undefined fields', () => {
  assert.deepEqual(buildEconomyPayload(true, 'caps', undefined), {source: 'caps'});
});
