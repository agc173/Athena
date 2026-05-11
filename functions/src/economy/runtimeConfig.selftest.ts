import test from 'node:test';
import assert from 'node:assert/strict';
import {
  getEconomyRuntimeConfigPath,
  isValidDocumentPath,
  parseEconomyRuntimeConfig,
} from './runtimeConfig';

test('runtime config path is a valid firestore document path', () => {
  assert.equal(isValidDocumentPath(getEconomyRuntimeConfigPath()), true);
});

test('runtime config parser falls back to production economy defaults when document does not exist', () => {
  const config = parseEconomyRuntimeConfig({});

  assert.deepEqual(config, {
    tarotEconomyV2Enabled: true,
    oracleEconomyV2Enabled: true,
    birthEssenceEconomyV2Enabled: true,
    synastryEconomyV2Enabled: true,
    pendulumEconomyV2Enabled: true,
  });
});

test('runtime config parser falls back to production economy defaults for invalid field types', () => {
  const config = parseEconomyRuntimeConfig({
    tarotEconomyV2Enabled: 'yes',
    oracleEconomyV2Enabled: 1,
    birthEssenceEconomyV2Enabled: null,
    synastryEconomyV2Enabled: 'no',
    pendulumEconomyV2Enabled: 0,
  });

  assert.deepEqual(config, {
    tarotEconomyV2Enabled: true,
    oracleEconomyV2Enabled: true,
    birthEssenceEconomyV2Enabled: true,
    synastryEconomyV2Enabled: true,
    pendulumEconomyV2Enabled: true,
  });
});
