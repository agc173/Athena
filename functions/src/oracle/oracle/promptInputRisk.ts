export interface PromptInputRisk {
  mentionsIgnorePreviousInstructions: boolean;
  mentionsSystemPrompt: boolean;
  mentionsDeveloperMessage: boolean;
  containsHtmlScript: boolean;
  containsMarkdownFence: boolean;
  asksForSecretsOrKeys: boolean;
}

const IGNORE_PREVIOUS_INSTRUCTIONS_RE = /ignore\s+(all\s+)?(previous|prior)\s+instructions?|disregard\s+(all\s+)?(previous|prior)\s+instructions?/i;
const SYSTEM_PROMPT_RE = /system\s+prompt|prompt\s+interno|hidden\s+prompt/i;
const DEVELOPER_MESSAGE_RE = /developer\s+message|mensaje\s+del\s+desarrollador/i;
const HTML_SCRIPT_RE = /<\s*script\b[^>]*>/i;
const MARKDOWN_FENCE_RE = /```/;
const SECRETS_OR_KEYS_RE = /api\s*key|secret\s*key|private\s*key|token|password|contrase(?:n|ñ)a|secreto/i;

export function detectSuspiciousPromptPayload(text: string): PromptInputRisk {
  const value = text ?? '';
  return {
    mentionsIgnorePreviousInstructions: IGNORE_PREVIOUS_INSTRUCTIONS_RE.test(value),
    mentionsSystemPrompt: SYSTEM_PROMPT_RE.test(value),
    mentionsDeveloperMessage: DEVELOPER_MESSAGE_RE.test(value),
    containsHtmlScript: HTML_SCRIPT_RE.test(value),
    containsMarkdownFence: MARKDOWN_FENCE_RE.test(value),
    asksForSecretsOrKeys: SECRETS_OR_KEYS_RE.test(value),
  };
}
