import type {LLMProvider, LLMGenerateParams, LLMGenerateResult} from "../LLMProvider";
import {ENV} from "../../config/env";
import {fetchWithTimeout} from "../../utils/fetchWithTimeout";

export class DeepSeekProvider implements LLMProvider {
  readonly name = "deepseek";

  async generate(params: LLMGenerateParams): Promise<LLMGenerateResult> {
    if (!ENV.DEEPSEEK_API_KEY) throw new Error("DEEPSEEK_API_KEY missing (DeepSeekProvider)");

    const body = {
      model: "deepseek-chat",
      messages: params.messages,
      temperature: params.temperature ?? 0.8,
      max_tokens: params.maxTokens ?? 400,
    };

    let res: Response;
    try {
      res = await fetchWithTimeout(
        "https://api.deepseek.com/chat/completions",
        {
          method: "POST",
          headers: {
            "Authorization": `Bearer ${ENV.DEEPSEEK_API_KEY}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify(body),
        },
        ENV.LLM_TIMEOUT_MS
      );
    } catch (e: any) {
      // AbortError -> withRetry lo verá como retryable por code/message
      throw e;
    }

    if (!res.ok) {
      const text = await res.text().catch(() => "");
      // Añadimos status para que withRetry detecte 429/5xx por status numérico
      const err: any = new Error(`DeepSeek error ${res.status}: ${text}`);
      err.status = res.status;
      throw err;
    }

    const json: any = await res.json();
    const text = json?.choices?.[0]?.message?.content?.trim();
    if (!text) throw new Error("DeepSeek empty response");

    return {provider: this.name, text, raw: json};
  }
}
