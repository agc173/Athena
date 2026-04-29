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

test('runtime config parser falls back to safe defaults when document does not exist', () => {
  const config = parseEconomyRuntimeConfig({});

  assert.deepEqual(config, {
    tarotEconomyV2Enabled: false,
    oracleEconomyV2Enabled: false,
    birthEssenceEconomyV2Enabled: false,
  });
});

test('runtime config parser falls back to safe defaults for invalid field types', () => {
  const config = parseEconomyRuntimeConfig({
    tarotEconomyV2Enabled: 'yes',
    oracleEconomyV2Enabled: 1,
    birthEssenceEconomyV2Enabled: null,
  });

  assert.deepEqual(config, {
    tarotEconomyV2Enabled: false,
    oracleEconomyV2Enabled: false,
    birthEssenceEconomyV2Enabled: false,
  });
});
