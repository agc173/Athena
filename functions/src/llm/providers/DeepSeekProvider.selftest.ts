/* eslint-disable @typescript-eslint/no-explicit-any */

async function main(): Promise<void> {
  process.env.DEEPSEEK_API_KEY = 'test-key';
  process.env.USE_MOCK_LLM = 'false';

  const calls: Array<Record<string, unknown>> = [];

  globalThis.fetch = (async (_input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
    const body = JSON.parse(String(init?.body ?? '{}')) as Record<string, unknown>;
    calls.push(body);

    const responsePayload = calls.length === 1 ?
      {
        choices: [
          {message: {content: 'not json at all'}},
        ],
      } :
      {
        choices: [
          {message: {content: '{"type":"TAROT_1","card":{"name":"The Fool","orientation":"upright"},"interpretation":{"theme":"new beginnings","meaning":"start fresh","advice":"trust your steps","watchOut":"avoid impulsiveness"}}'}},
        ],
      };

    return new Response(JSON.stringify(responsePayload), {
      status: 200,
      headers: {'Content-Type': 'application/json'},
    });
  }) as any;

  const {DeepSeekProvider} = await import('./DeepSeekProvider.js');
  const provider = new DeepSeekProvider();

  const result = await provider.generate({
    messages: [
      {role: 'system', content: 'Return tarot JSON.'},
      {role: 'user', content: 'Give me a reading.'},
    ],
    temperature: 0.3,
    maxTokens: 300,
  });

  const parsed = JSON.parse(result.text) as Record<string, unknown>;

  if (!parsed.type || calls.length !== 2) {
    throw new Error('DeepSeek repair path self-test failed');
  }

  const firstBody = calls[0];
  const secondBody = calls[1];
  if (!firstBody.response_format || !secondBody.response_format) {
    throw new Error('Expected response_format json_object in both calls');
  }
}

main();
