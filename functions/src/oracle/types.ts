export enum RequestType {
  TAROT_1 = 'TAROT_1',
  TAROT_3 = 'TAROT_3',
  ORACLE_1Q = 'ORACLE_1Q',
}

export enum ReadingTopic {
  GENERAL = 'GENERAL',
  LOVE = 'LOVE',
  WORK = 'WORK',
  HEALTH = 'HEALTH',
  SPIRITUAL = 'SPIRITUAL',
}

export enum ConsumeIntent {
  FREE_DAILY = 'FREE_DAILY',
  AD_UNLOCK = 'AD_UNLOCK',
  SUBSCRIPTION = 'SUBSCRIPTION',
}

export enum RequestStatus {
  QUEUED = 'QUEUED',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  DEGRADED = 'DEGRADED',
}

export interface TarotDrawData {
  requestType: RequestType.TAROT_1 | RequestType.TAROT_3;
  topic?: ReadingTopic;
  requestId?: string;
  lang?: string;
  question?: string;
  adUnlock?: {
    rewardedProof: string;
  };
}

export interface OracleAskData {
  requestType: RequestType.ORACLE_1Q;
  question: string;
  topic?: ReadingTopic;
  requestId?: string;
  lang?: string;
  adUnlock?: {
    rewardedProof: string;
  };
}
