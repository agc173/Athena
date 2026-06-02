import assert from 'node:assert/strict';
import {ALL_ZODIAC_SIGNS, normalizeZodiacSign} from './zodiacSigns';

for (const sign of ALL_ZODIAC_SIGNS) {
  assert.equal(normalizeZodiacSign(sign), sign);
  assert.equal(normalizeZodiacSign(sign.toUpperCase()), sign);
}

assert.equal(normalizeZodiacSign('Tauro'), 'taurus');
assert.equal(normalizeZodiacSign(' TAURUS '), 'taurus');
assert.equal(normalizeZodiacSign('Escorpio'), 'scorpio');
assert.equal(normalizeZodiacSign('Sagitario'), 'sagittarius');
assert.equal(normalizeZodiacSign('Capricornio'), 'capricorn');
assert.equal(normalizeZodiacSign('Acuario'), 'aquarius');
assert.equal(normalizeZodiacSign('Piscis'), 'pisces');
assert.equal(normalizeZodiacSign('not-a-sign'), undefined);

console.log('zodiacSigns.selftest passed');
