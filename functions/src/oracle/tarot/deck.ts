export type TarotLang = 'es' | 'en';

export interface TarotCardDef {
  id: string;
  nameByLang: Record<TarotLang, string>;
}

const MAJOR_ARCANA: TarotCardDef[] = [
  {id: 'major-00-fool', nameByLang: {es: 'El Loco', en: 'The Fool'}},
  {id: 'major-01-magician', nameByLang: {es: 'El Mago', en: 'The Magician'}},
  {id: 'major-02-high-priestess', nameByLang: {es: 'La Sacerdotisa', en: 'The High Priestess'}},
  {id: 'major-03-empress', nameByLang: {es: 'La Emperatriz', en: 'The Empress'}},
  {id: 'major-04-emperor', nameByLang: {es: 'El Emperador', en: 'The Emperor'}},
  {id: 'major-05-hierophant', nameByLang: {es: 'El Hierofante', en: 'The Hierophant'}},
  {id: 'major-06-lovers', nameByLang: {es: 'Los Enamorados', en: 'The Lovers'}},
  {id: 'major-07-chariot', nameByLang: {es: 'El Carro', en: 'The Chariot'}},
  {id: 'major-08-strength', nameByLang: {es: 'La Fuerza', en: 'Strength'}},
  {id: 'major-09-hermit', nameByLang: {es: 'El Ermitaño', en: 'The Hermit'}},
  {id: 'major-10-wheel-of-fortune', nameByLang: {es: 'La Rueda de la Fortuna', en: 'Wheel of Fortune'}},
  {id: 'major-11-justice', nameByLang: {es: 'La Justicia', en: 'Justice'}},
  {id: 'major-12-hanged-man', nameByLang: {es: 'El Colgado', en: 'The Hanged Man'}},
  {id: 'major-13-death', nameByLang: {es: 'La Muerte', en: 'Death'}},
  {id: 'major-14-temperance', nameByLang: {es: 'La Templanza', en: 'Temperance'}},
  {id: 'major-15-devil', nameByLang: {es: 'El Diablo', en: 'The Devil'}},
  {id: 'major-16-tower', nameByLang: {es: 'La Torre', en: 'The Tower'}},
  {id: 'major-17-star', nameByLang: {es: 'La Estrella', en: 'The Star'}},
  {id: 'major-18-moon', nameByLang: {es: 'La Luna', en: 'The Moon'}},
  {id: 'major-19-sun', nameByLang: {es: 'El Sol', en: 'The Sun'}},
  {id: 'major-20-judgement', nameByLang: {es: 'El Juicio', en: 'Judgement'}},
  {id: 'major-21-world', nameByLang: {es: 'El Mundo', en: 'The World'}},
];

const SUITS = ['wands', 'cups', 'swords', 'pentacles'] as const;
const SUIT_BY_LANG: Record<(typeof SUITS)[number], Record<TarotLang, string>> = {
  wands: {es: 'Bastos', en: 'Wands'},
  cups: {es: 'Copas', en: 'Cups'},
  swords: {es: 'Espadas', en: 'Swords'},
  pentacles: {es: 'Oros', en: 'Pentacles'},
};

const RANKS: Array<{id: string; es: string; en: string}> = [
  {id: 'ace', es: 'As', en: 'Ace'},
  {id: 'two', es: 'Dos', en: 'Two'},
  {id: 'three', es: 'Tres', en: 'Three'},
  {id: 'four', es: 'Cuatro', en: 'Four'},
  {id: 'five', es: 'Cinco', en: 'Five'},
  {id: 'six', es: 'Seis', en: 'Six'},
  {id: 'seven', es: 'Siete', en: 'Seven'},
  {id: 'eight', es: 'Ocho', en: 'Eight'},
  {id: 'nine', es: 'Nueve', en: 'Nine'},
  {id: 'ten', es: 'Diez', en: 'Ten'},
  {id: 'page', es: 'Paje', en: 'Page'},
  {id: 'knight', es: 'Caballero', en: 'Knight'},
  {id: 'queen', es: 'Reina', en: 'Queen'},
  {id: 'king', es: 'Rey', en: 'King'},
];

const MINOR_ARCANA: TarotCardDef[] = SUITS.flatMap((suit) =>
  RANKS.map((rank) => ({
    id: `minor-${rank.id}-${suit}`,
    nameByLang: {
      es: `${rank.es} de ${SUIT_BY_LANG[suit].es}`,
      en: `${rank.en} of ${SUIT_BY_LANG[suit].en}`,
    },
  }))
);

export const TAROT_DECK: TarotCardDef[] = [...MAJOR_ARCANA, ...MINOR_ARCANA];
