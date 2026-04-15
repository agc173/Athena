export type TarotLang = 'es' | 'en' | 'pt' | 'ru' | 'fr' | 'it' | 'de';

type DeckTarotLang = 'es' | 'en';

export interface TarotCardDef {
  id: string;
  nameByLang: Record<DeckTarotLang, string>;
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
const SUIT_BY_LANG: Record<(typeof SUITS)[number], Record<DeckTarotLang, string>> = {
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

const CARD_BY_ID = new Map<string, TarotCardDef>();

export const TAROT_DECK: TarotCardDef[] = [...MAJOR_ARCANA, ...MINOR_ARCANA];
TAROT_DECK.forEach((card) => CARD_BY_ID.set(card.id, card));

const MAJOR_BY_LANG: Record<TarotLang, Record<string, string>> = {
  es: {
    fool: 'El Loco', magician: 'El Mago', 'high_priestess': 'La Sacerdotisa', empress: 'La Emperatriz',
    emperor: 'El Emperador', hierophant: 'El Hierofante', lovers: 'Los Enamorados', chariot: 'El Carro',
    strength: 'La Fuerza', hermit: 'El Ermitaño', 'wheel_of_fortune': 'La Rueda de la Fortuna', justice: 'La Justicia',
    'hanged_man': 'El Colgado', death: 'La Muerte', temperance: 'La Templanza', devil: 'El Diablo',
    tower: 'La Torre', star: 'La Estrella', moon: 'La Luna', sun: 'El Sol', judgement: 'El Juicio', world: 'El Mundo',
  },
  en: {
    fool: 'The Fool', magician: 'The Magician', 'high_priestess': 'The High Priestess', empress: 'The Empress',
    emperor: 'The Emperor', hierophant: 'The Hierophant', lovers: 'The Lovers', chariot: 'The Chariot',
    strength: 'Strength', hermit: 'The Hermit', 'wheel_of_fortune': 'Wheel of Fortune', justice: 'Justice',
    'hanged_man': 'The Hanged Man', death: 'Death', temperance: 'Temperance', devil: 'The Devil',
    tower: 'The Tower', star: 'The Star', moon: 'The Moon', sun: 'The Sun', judgement: 'Judgement', world: 'The World',
  },
  pt: {
    fool: 'O Louco', magician: 'O Mago', 'high_priestess': 'A Sacerdotisa', empress: 'A Imperatriz',
    emperor: 'O Imperador', hierophant: 'O Hierofante', lovers: 'Os Enamorados', chariot: 'O Carro',
    strength: 'A Força', hermit: 'O Eremita', 'wheel_of_fortune': 'A Roda da Fortuna', justice: 'A Justiça',
    'hanged_man': 'O Enforcado', death: 'A Morte', temperance: 'A Temperança', devil: 'O Diabo',
    tower: 'A Torre', star: 'A Estrela', moon: 'A Lua', sun: 'O Sol', judgement: 'O Julgamento', world: 'O Mundo',
  },
  ru: {
    fool: 'Шут', magician: 'Маг', 'high_priestess': 'Верховная Жрица', empress: 'Императрица',
    emperor: 'Император', hierophant: 'Иерофант', lovers: 'Влюблённые', chariot: 'Колесница',
    strength: 'Сила', hermit: 'Отшельник', 'wheel_of_fortune': 'Колесо Фортуны', justice: 'Справедливость',
    'hanged_man': 'Повешенный', death: 'Смерть', temperance: 'Умеренность', devil: 'Дьявол',
    tower: 'Башня', star: 'Звезда', moon: 'Луна', sun: 'Солнце', judgement: 'Суд', world: 'Мир',
  },
  fr: {
    fool: 'Le Mat', magician: 'Le Bateleur', 'high_priestess': 'La Papesse', empress: "L'Impératrice",
    emperor: "L'Empereur", hierophant: 'Le Pape', lovers: "L'Amoureux", chariot: 'Le Chariot',
    strength: 'La Force', hermit: "L'Hermite", 'wheel_of_fortune': 'La Roue de Fortune', justice: 'La Justice',
    'hanged_man': 'Le Pendu', death: 'La Mort', temperance: 'Tempérance', devil: 'Le Diable',
    tower: 'La Maison Dieu', star: "L'Étoile", moon: 'La Lune', sun: 'Le Soleil', judgement: 'Le Jugement', world: 'Le Monde',
  },
  it: {
    fool: 'Il Matto', magician: 'Il Mago', 'high_priestess': 'La Papessa', empress: "L'Imperatrice",
    emperor: "L'Imperatore", hierophant: 'Il Papa', lovers: 'Gli Amanti', chariot: 'Il Carro',
    strength: 'La Forza', hermit: "L'Eremita", 'wheel_of_fortune': 'La Ruota della Fortuna', justice: 'La Giustizia',
    'hanged_man': "L'Appeso", death: 'La Morte', temperance: 'La Temperanza', devil: 'Il Diavolo',
    tower: 'La Torre', star: 'La Stella', moon: 'La Luna', sun: 'Il Sole', judgement: 'Il Giudizio', world: 'Il Mondo',
  },
  de: {
    fool: 'Der Narr', magician: 'Der Magier', 'high_priestess': 'Die Hohepriesterin', empress: 'Die Herrscherin',
    emperor: 'Der Herrscher', hierophant: 'Der Hierophant', lovers: 'Die Liebenden', chariot: 'Der Wagen',
    strength: 'Die Kraft', hermit: 'Der Eremit', 'wheel_of_fortune': 'Das Rad des Schicksals', justice: 'Die Gerechtigkeit',
    'hanged_man': 'Der Gehängte', death: 'Der Tod', temperance: 'Die Mäßigkeit', devil: 'Der Teufel',
    tower: 'Der Turm', star: 'Der Stern', moon: 'Der Mond', sun: 'Die Sonne', judgement: 'Das Gericht', world: 'Die Welt',
  },
};

const RANK_BY_LANG: Record<TarotLang, Record<string, string>> = {
  es: {ace: 'As', two: 'Dos', three: 'Tres', four: 'Cuatro', five: 'Cinco', six: 'Seis', seven: 'Siete', eight: 'Ocho', nine: 'Nueve', ten: 'Diez', page: 'Paje', knight: 'Caballero', queen: 'Reina', king: 'Rey'},
  en: {ace: 'Ace', two: 'Two', three: 'Three', four: 'Four', five: 'Five', six: 'Six', seven: 'Seven', eight: 'Eight', nine: 'Nine', ten: 'Ten', page: 'Page', knight: 'Knight', queen: 'Queen', king: 'King'},
  pt: {ace: 'Ás', two: 'Dois', three: 'Três', four: 'Quatro', five: 'Cinco', six: 'Seis', seven: 'Sete', eight: 'Oito', nine: 'Nove', ten: 'Dez', page: 'Pajem', knight: 'Cavaleiro', queen: 'Rainha', king: 'Rei'},
  ru: {ace: 'Туз', two: 'Двойка', three: 'Тройка', four: 'Четвёрка', five: 'Пятёрка', six: 'Шестёрка', seven: 'Семёрка', eight: 'Восьмёрка', nine: 'Девятка', ten: 'Десятка', page: 'Паж', knight: 'Рыцарь', queen: 'Королева', king: 'Король'},
  fr: {ace: 'As', two: 'Deux', three: 'Trois', four: 'Quatre', five: 'Cinq', six: 'Six', seven: 'Sept', eight: 'Huit', nine: 'Neuf', ten: 'Dix', page: 'Valet', knight: 'Chevalier', queen: 'Reine', king: 'Roi'},
  it: {ace: 'Asso', two: 'Due', three: 'Tre', four: 'Quattro', five: 'Cinque', six: 'Sei', seven: 'Sette', eight: 'Otto', nine: 'Nove', ten: 'Dieci', page: 'Fante', knight: 'Cavaliere', queen: 'Regina', king: 'Re'},
  de: {ace: 'Ass', two: 'Zwei', three: 'Drei', four: 'Vier', five: 'Fünf', six: 'Sechs', seven: 'Sieben', eight: 'Acht', nine: 'Neun', ten: 'Zehn', page: 'Page', knight: 'Ritter', queen: 'Königin', king: 'König'},
};

const SUIT_BY_TAROT_LANG: Record<TarotLang, Record<string, string>> = {
  es: {wands: 'Bastos', cups: 'Copas', swords: 'Espadas', pentacles: 'Oros'},
  en: {wands: 'Wands', cups: 'Cups', swords: 'Swords', pentacles: 'Pentacles'},
  pt: {wands: 'Paus', cups: 'Copas', swords: 'Espadas', pentacles: 'Ouros'},
  ru: {wands: 'Жезлов', cups: 'Кубков', swords: 'Мечей', pentacles: 'Пентаклей'},
  fr: {wands: 'Bâtons', cups: 'Coupes', swords: 'Épées', pentacles: 'Deniers'},
  it: {wands: 'Bastoni', cups: 'Coppe', swords: 'Spade', pentacles: 'Denari'},
  de: {wands: 'Stäbe', cups: 'Kelche', swords: 'Schwerter', pentacles: 'Münzen'},
};

function fallbackName(cardId: string): string {
  return cardId
      .replace(/^major-\d+-/, '')
      .replace(/^minor-/, '')
      .split('-')
      .join(' ')
      .replace(/_/g, ' ');
}

function majorArcanaName(cardId: string, lang: TarotLang): string {
  const slug = cardId.split('-').slice(2).join('_');
  return MAJOR_BY_LANG[lang][slug] ?? fallbackName(cardId);
}

function minorArcanaName(cardId: string, lang: TarotLang): string {
  const [, rank, suit] = cardId.split('-');
  if (!rank || !suit) return fallbackName(cardId);

  const rankLabel = RANK_BY_LANG[lang][rank];
  const suitLabel = SUIT_BY_TAROT_LANG[lang][suit];
  if (!rankLabel || !suitLabel) return fallbackName(cardId);

  if (lang === 'ru') return `${rankLabel} ${suitLabel}`;

  const linker = lang === 'en' ? 'of' :
    lang === 'it' ? 'di' :
    lang === 'de' ? 'der' :
    'de';

  return `${rankLabel} ${linker} ${suitLabel}`;
}

export function normalizeTarotLang(rawLang: string): TarotLang {
  const normalized = rawLang.trim().toLowerCase().split('-')[0].split('_')[0];
  switch (normalized) {
    case 'en':
    case 'pt':
    case 'ru':
    case 'fr':
    case 'it':
    case 'de':
    case 'es':
      return normalized;
    default:
      return 'es';
  }
}

export function resolveTarotCardName(cardId: string, lang: TarotLang): string {
  if (lang === 'es' || lang === 'en') {
    return CARD_BY_ID.get(cardId)?.nameByLang[lang] ?? fallbackName(cardId);
  }

  if (cardId.startsWith('major-')) {
    return majorArcanaName(cardId, lang);
  }

  if (cardId.startsWith('minor-')) {
    return minorArcanaName(cardId, lang);
  }

  return fallbackName(cardId);
}
