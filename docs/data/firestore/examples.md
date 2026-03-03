# Firestore document examples

## users/{uid}
{
"username": "alfonso",
"zodiacSign": "leo",
"level": 3,
"xp": 420,
"createdAt": "<timestamp>",
"updatedAt": "<timestamp>"
}

## posts/{postId}
{
"authorId": "<uid>",
"text": "Hoy he tenido un sueño raro... ¿alguien más?",
"topic": "energy",
"createdAt": "<timestamp>",
"updatedAt": "<timestamp>",
"likeCount": 4,
"commentCount": 2,
"status": "active"
}

## posts/{postId}/comments/{commentId}
{
"authorId": "<uid>",
"text": "A mí me pasa cuando estoy cargado de energía.",
"createdAt": "<timestamp>",
"status": "active"
}

## posts/{postId}/reactions/{uid}
{
"type": "like",
"createdAt": "<timestamp>"
}

## oracleRequests/{requestId}
{
"uid": "<uid>",
"requestId": "req_123",
"requestType": "TAROT_1",
"lang": "es",
"question": "¿Qué debo priorizar esta semana?",
"dateIso": "2026-03-03",
"intent": "FREE_DAILY",
"status": "COMPLETED_SUCCESS",
"systemMode": "NORMAL",
"responsePayload": {"requestId": "req_123", "status": "COMPLETED_SUCCESS"},
"createdAt": "<timestamp>",
"updatedAt": "<timestamp>"
}

## tarotReadings/{uid}/items/{readingId}
{
"requestId": "req_123",
"requestType": "TAROT_3",
"lang": "es",
"draw": [
  {"id": "major-00-fool", "orientation": "upright", "position": "past"},
  {"id": "major-01-magician", "orientation": "reversed", "position": "present"},
  {"id": "major-21-world", "orientation": "upright", "position": "future"}
],
"reading": {"type": "TAROT_3"},
"llmMeta": {"provider": "deepseek", "inputTokens": 900, "outputTokens": 420},
"createdAt": "<timestamp>"
}
