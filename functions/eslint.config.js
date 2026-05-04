const js = require('@eslint/js');
const {FlatCompat} = require('@eslint/eslintrc');

const compat = new FlatCompat({
  baseDirectory: __dirname,
  recommendedConfig: js.configs.recommended,
});

module.exports = [
  {
    ignores: [
      'lib/**/*',
      'generated/**/*',
      '**/*.selftest.ts',
      '**/*SelfTest.ts',
      'eslint.config.js',
    ],
  },
  ...compat.extends(
    'eslint:recommended',
    'plugin:import/errors',
    'plugin:import/warnings',
    'plugin:import/typescript',
    'google',
    'plugin:@typescript-eslint/recommended',
  ),
  {
    files: ['**/*.{js,ts}'],
    languageOptions: {
      parser: require('@typescript-eslint/parser'),
      parserOptions: {
        project: ['tsconfig.json', 'tsconfig.dev.json', './tsconfig.tools.json'],
        sourceType: 'module',
        tsconfigRootDir: __dirname,
      },
    },
    plugins: {
      '@typescript-eslint': require('@typescript-eslint/eslint-plugin'),
      import: require('eslint-plugin-import'),
    },
    rules: {
      'import/no-unresolved': 'off',
      'require-jsdoc': 'off',
      'valid-jsdoc': 'off',
      'max-len': 'off',
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-non-null-assertion': 'off',
      'linebreak-style': 'off',
      'comma-dangle': 'off',
      indent: 'off',
      'quote-props': 'off',
    },
  },
];
