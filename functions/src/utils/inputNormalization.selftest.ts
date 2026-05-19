import assert from 'node:assert/strict';
import test from 'node:test';
import {
  normalizeMultilineInput,
  normalizeSingleLineInput,
  ORACLE_QUESTION_MAX_LENGTH,
  removeUnsafeControlChars,
} from './inputNormalization';

test('removeUnsafeControlChars strips dangerous controls and preserves unicode', () => {
  const value = 'Hola\u0000\u0007🌙\u001F mundo';
  assert.equal(removeUnsafeControlChars(value), 'Hola🌙 mundo');
});

test('normalizeMultilineInput preserves line breaks and trims abuse', () => {
  const value = '  Línea 1\r\n\r\n\r\nLínea 2\u0000\n';
  assert.equal(normalizeMultilineInput(value), 'Línea 1\n\nLínea 2');
});

test('normalizeSingleLineInput collapses whitespace and removes controls', () => {
  const value = '  foo\t\u0000  bar\n baz  ';
  assert.equal(normalizeSingleLineInput(value), 'foo bar baz');
});

test('oracle max length can be enforced after normalization', () => {
  const normalized = normalizeMultilineInput('á'.repeat(ORACLE_QUESTION_MAX_LENGTH + 20));
  assert.equal(normalized.slice(0, ORACLE_QUESTION_MAX_LENGTH).length, ORACLE_QUESTION_MAX_LENGTH);
});

test('blank strings normalize to empty', () => {
  assert.equal(normalizeMultilineInput(' \n\r\t '), '');
});
