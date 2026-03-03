export enum RequestType {
  TAROT_1 = 'TAROT_1',
  TAROT_3 = 'TAROT_3',
  ORACLE_1Q = 'ORACLE_1Q',
}

export enum Intent {
  GENERAL = 'GENERAL',
  LOVE = 'LOVE',
  WORK = 'WORK',
  HEALTH = 'HEALTH',
  SPIRITUAL = 'SPIRITUAL',
}

export enum RequestStatus {
  QUEUED = 'QUEUED',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  DEGRADED = 'DEGRADED',
}

export interface OracleGetStatusData {
  requestId: string;
}

export interface TarotDrawData {
  requestType: RequestType.TAROT_1 | RequestType.TAROT_3;
  intent?: Intent;
  requestId?: string;
}

export interface OracleAskData {
  requestType: RequestType.ORACLE_1Q;
  question: string;
  intent?: Intent;
  requestId?: string;
}
